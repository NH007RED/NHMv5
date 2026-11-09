package com.nh.magiceraser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/** Optional on-device LaMa model. Place lama_inpaint.tflite in app/src/main/assets/. */
class LamaModel private constructor(private val interp: Interpreter) {
    private var imgIdx = 0
    private var maskIdx = 1
    private var size = 512

    init {
        for (i in 0 until interp.inputTensorCount) {
            val sh = interp.getInputTensor(i).shape()
            if (sh.last() == 3) {
                imgIdx = i
                size = sh[1]
            } else {
                maskIdx = i
            }
        }
    }

    fun run(px: IntArray, cw: Int, ch: Int, mask: BooleanArray): IntArray? {
        return try {
            val s = size
            val bmp = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888)
            bmp.setPixels(px, 0, cw, 0, 0, cw, ch)
            val sb = Bitmap.createScaledBitmap(bmp, s, s, true)
            val sp = IntArray(s * s)
            sb.getPixels(sp, 0, s, 0, 0, s, s)

            val imgBuf = ByteBuffer.allocateDirect(s * s * 3 * 4).order(ByteOrder.nativeOrder())
            val mBuf = ByteBuffer.allocateDirect(s * s * 4).order(ByteOrder.nativeOrder())
            for (y in 0 until s) {
                val sy = min(y * ch / s, ch - 1)
                for (x in 0 until s) {
                    val sx = min(x * cw / s, cw - 1)
                    val c = sp[y * s + x]
                    imgBuf.putFloat(((c shr 16) and 255) / 255f)
                    imgBuf.putFloat(((c shr 8) and 255) / 255f)
                    imgBuf.putFloat((c and 255) / 255f)
                    mBuf.putFloat(if (mask[sy * cw + sx]) 1f else 0f)
                }
            }
            imgBuf.rewind(); mBuf.rewind()

            val inputs = Array<Any>(interp.inputTensorCount) { if (it == imgIdx) imgBuf else mBuf }
            val outBuf = ByteBuffer.allocateDirect(s * s * 3 * 4).order(ByteOrder.nativeOrder())
            interp.runForMultipleInputsOutputs(inputs, mapOf(0 to outBuf))
            outBuf.rewind()

            val f = FloatArray(s * s * 3)
            outBuf.asFloatBuffer().get(f)
            var mx = 0f
            for (v in f) if (v > mx) mx = v
            val k = if (mx <= 2f) 255f else 1f
            val outPx = IntArray(s * s)
            for (i in 0 until s * s) {
                val r = (f[i * 3] * k).toInt().coerceIn(0, 255)
                val g = (f[i * 3 + 1] * k).toInt().coerceIn(0, 255)
                val b = (f[i * 3 + 2] * k).toInt().coerceIn(0, 255)
                outPx[i] = (255 shl 24) or (r shl 16) or (g shl 8) or b
            }
            val ob = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
            ob.setPixels(outPx, 0, s, 0, 0, s, s)
            val up = Bitmap.createScaledBitmap(ob, cw, ch, true)
            val upPx = IntArray(cw * ch)
            up.getPixels(upPx, 0, cw, 0, 0, cw, ch)
            val result = px.copyOf()
            for (i in result.indices) if (mask[i]) result[i] = upPx[i]
            result
        } catch (t: Throwable) {
            null
        }
    }

    companion object {
        @Volatile private var cached: LamaModel? = null
        @Volatile private var tried = false

        fun get(ctx: Context): LamaModel? {
            if (tried) return cached
            synchronized(this) {
                if (tried) return cached
                cached = try {
                    val bytes = ctx.assets.open("lama_inpaint.tflite").use { it.readBytes() }
                    val bb = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
                    bb.put(bytes); bb.rewind()
                    LamaModel(Interpreter(bb, Interpreter.Options().setNumThreads(4)))
                } catch (t: Throwable) {
                    null
                }
                tried = true
                return cached
            }
        }
    }
}

object Inpainter {

    /** Full-size mask version used by the image editor. */
    fun inpaint(src: Bitmap, mask: BooleanArray, dilate: Int, lama: LamaModel?): Bitmap {
        val w = src.width
        val h = src.height
        var minX = w; var minY = h; var maxX = -1; var maxY = -1
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) if (mask[row + x]) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
        if (maxX < 0) return src
        val context = max(48, max(maxX - minX, maxY - minY) / 2)
        val pad = dilate + context
        val x0 = max(0, minX - pad)
        val y0 = max(0, minY - pad)
        val x1 = min(w, maxX + 1 + pad)
        val y1 = min(h, maxY + 1 + pad)
        val cw = x1 - x0
        val ch = y1 - y0
        val m = BooleanArray(cw * ch)
        for (y in 0 until ch) for (x in 0 until cw) m[y * cw + x] = mask[(y0 + y) * w + x0 + x]
        return fillCrop(src, x0, y0, cw, ch, dilateMask(m, cw, ch, dilate), lama)
    }

    /** Rectangle version used by the video exporter (cheap per frame). */
    fun inpaintRects(src: Bitmap, rects: List<Rect>, lama: LamaModel?): Bitmap {
        val w = src.width
        val h = src.height
        val rs = rects.map {
            Rect(it.left.coerceIn(0, w), it.top.coerceIn(0, h), it.right.coerceIn(0, w), it.bottom.coerceIn(0, h))
        }.filter { it.width() > 0 && it.height() > 0 }
        if (rs.isEmpty()) return src
        val minX = rs.minOf { it.left }
        val minY = rs.minOf { it.top }
        val maxX = rs.maxOf { it.right }
        val maxY = rs.maxOf { it.bottom }
        val context = max(24, max(maxX - minX, maxY - minY) / 3)
        val x0 = max(0, minX - context)
        val y0 = max(0, minY - context)
        val x1 = min(w, maxX + context)
        val y1 = min(h, maxY + context)
        val cw = x1 - x0
        val ch = y1 - y0
        val m = BooleanArray(cw * ch)
        for (r in rs) {
            for (y in r.top until r.bottom) for (x in r.left until r.right) m[(y - y0) * cw + (x - x0)] = true
        }
        return fillCrop(src, x0, y0, cw, ch, m, lama)
    }

    private fun fillCrop(
        src: Bitmap, x0: Int, y0: Int, cw: Int, ch: Int, m: BooleanArray, lama: LamaModel?
    ): Bitmap {
        val px = IntArray(cw * ch)
        src.getPixels(px, 0, cw, x0, y0, cw, ch)
        val filled = lama?.run(px, cw, ch, m) ?: FallbackInpaint.run(px, cw, ch, m)
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        out.setPixels(filled, 0, cw, x0, y0, cw, ch)
        return out
    }

    private fun dilateMask(m: BooleanArray, w: Int, h: Int, r: Int): BooleanArray {
        if (r <= 0) return m
        val tmp = BooleanArray(w * h)
        for (y in 0 until h) for (x in 0 until w) if (m[y * w + x]) {
            for (xx in max(0, x - r)..min(w - 1, x + r)) tmp[y * w + xx] = true
        }
        val out = BooleanArray(w * h)
        for (y in 0 until h) for (x in 0 until w) if (tmp[y * w + x]) {
            for (yy in max(0, y - r)..min(h - 1, y + r)) out[yy * w + x] = true
        }
        return out
    }
}

/** Pure Kotlin fallback: fills the hole layer by layer from the border inward, then smooths. */
object FallbackInpaint {
    fun run(px: IntArray, w: Int, h: Int, mask: BooleanArray): IntArray {
        val out = px.copyOf()
        val known = BooleanArray(w * h) { !mask[it] }
        if (known.none { it }) return out
        val queued = BooleanArray(w * h)
        var frontier = ArrayList<Int>()

        fun forNeighbors(i: Int, f: (Int) -> Unit) {
            val x = i % w
            val y = i / w
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until w && ny in 0 until h) f(ny * w + nx)
            }
        }

        for (i in px.indices) if (mask[i]) {
            var has = false
            forNeighbors(i) { if (known[it]) has = true }
            if (has) { queued[i] = true; frontier.add(i) }
        }

        val rad = 3
        while (frontier.isNotEmpty()) {
            val vals = IntArray(frontier.size)
            for (k in frontier.indices) {
                val i = frontier[k]
                val x = i % w
                val y = i / w
                var sr = 0f; var sg = 0f; var sb = 0f; var sw = 0f
                for (dy in -rad..rad) for (dx in -rad..rad) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                    val n = ny * w + nx
                    if (!known[n]) continue
                    val wt = 1f / (dx * dx + dy * dy + 1)
                    val c = out[n]
                    sr += ((c shr 16) and 255) * wt
                    sg += ((c shr 8) and 255) * wt
                    sb += (c and 255) * wt
                    sw += wt
                }
                vals[k] = if (sw > 0f) {
                    (255 shl 24) or ((sr / sw).toInt() shl 16) or ((sg / sw).toInt() shl 8) or (sb / sw).toInt()
                } else out[i]
            }
            val next = ArrayList<Int>()
            for (k in frontier.indices) {
                out[frontier[k]] = vals[k]
                known[frontier[k]] = true
            }
            for (i in frontier) forNeighbors(i) {
                if (mask[it] && !known[it] && !queued[it]) { queued[it] = true; next.add(it) }
            }
            frontier = next
        }

        // light smoothing of the filled area only
        repeat(3) {
            val copy = out.copyOf()
            for (i in px.indices) if (mask[i]) {
                val x = i % w
                val y = i / w
                var sr = 0; var sg = 0; var sb = 0; var n = 0
                for (dy in -1..1) for (dx in -1..1) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                    val c = copy[ny * w + nx]
                    sr += (c shr 16) and 255
                    sg += (c shr 8) and 255
                    sb += c and 255
                    n++
                }
                out[i] = (255 shl 24) or ((sr / n) shl 16) or ((sg / n) shl 8) or (sb / n)
            }
        }
        return out
    }
}
