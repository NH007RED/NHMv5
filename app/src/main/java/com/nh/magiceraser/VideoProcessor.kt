package com.nh.magiceraser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object VideoProcessor {

    /**
     * Removes static regions (normalized rects 0..1) from every frame and re-encodes as H.264.
     * Original audio track is copied untouched when the container allows it.
     */
    suspend fun process(
        ctx: Context,
        uri: Uri,
        rects: List<RectF>,
        expandPct: Float,
        onProgress: (Int, Int) -> Unit,
        onPreview: (Bitmap) -> Unit
    ): Uri? = withContext(Dispatchers.Default) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(ctx, uri)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            ?: return@withContext null
        var srcW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        var srcH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        if (rot == 90 || rot == 270) { val t = srcW; srcW = srcH; srcH = t }
        if (srcW <= 0 || srcH <= 0) return@withContext null

        // output size: cap long side at 1920, force even numbers
        val capScale = min(1f, 1920f / max(srcW, srcH))
        val w = (srcW * capScale).toInt() and 1.inv()
        val h = (srcH * capScale).toInt() and 1.inv()

        // frame rate
        var fps = 30
        val probe = MediaExtractor()
        var audioFormat: MediaFormat? = null
        var audioIdx = -1
        try {
            probe.setDataSource(ctx, uri, null)
            for (i in 0 until probe.trackCount) {
                val f = probe.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") && f.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                    fps = f.getInteger(MediaFormat.KEY_FRAME_RATE)
                } else if (mime.startsWith("audio/") && audioIdx < 0) {
                    audioIdx = i; audioFormat = f
                }
            }
        } finally {
            probe.release()
        }
        fps = fps.coerceIn(10, 30)
        val frameCount = max(1L, durationMs * fps / 1000L).toInt()

        val pad = (expandPct / 100f * min(w, h)).roundToInt()
        val pxRects = rects.map {
            android.graphics.Rect(
                (it.left * w).roundToInt() - pad, (it.top * h).roundToInt() - pad,
                (it.right * w).roundToInt() + pad, (it.bottom * h).roundToInt() + pad
            )
        }

        val tmp = File(ctx.cacheDir, "nh_eraser_${System.currentTimeMillis()}.mp4")
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, (w.toLong() * h * fps / 10).toInt().coerceIn(1_000_000, 16_000_000))
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val muxer = MediaMuxer(tmp.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        var videoTrack = -1
        var audioTrack = -1
        val info = MediaCodec.BufferInfo()
        var ok = false

        fun drain(eos: Boolean) {
            while (true) {
                val idx = encoder.dequeueOutputBuffer(info, if (eos) 10_000L else 0L)
                when {
                    idx == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!eos) return
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        videoTrack = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    idx >= 0 -> {
                        val buf = encoder.getOutputBuffer(idx)!!
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                        if (info.size > 0 && muxerStarted) {
                            buf.position(info.offset)
                            buf.limit(info.offset + info.size)
                            muxer.writeSampleData(videoTrack, buf, info)
                        }
                        encoder.releaseOutputBuffer(idx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                    }
                }
            }
        }

        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            if (audioFormat != null) {
                audioTrack = try { muxer.addTrack(audioFormat) } catch (e: Exception) { -1 }
            }

            var last: Bitmap? = null
            for (i in 0 until frameCount) {
                coroutineContext.ensureActive()
                val tUs = i * 1_000_000L / fps
                var frame = retriever.getFrameAtTime(tUs, MediaMetadataRetriever.OPTION_CLOSEST) ?: last
                if (frame == null) continue
                if (frame.width != w || frame.height != h) frame = Bitmap.createScaledBitmap(frame, w, h, true)
                val cleaned = if (pxRects.isEmpty()) frame else Inpainter.inpaintRects(frame, pxRects, null)
                last = frame
                if (i % 2 == 0) {
                    val pw = min(480, w)
                    val ph = (h.toFloat() * pw / w).toInt().coerceAtLeast(2)
                    onPreview(Bitmap.createScaledBitmap(cleaned, pw, ph, true))
                }

                val argb = IntArray(w * h)
                cleaned.getPixels(argb, 0, w, 0, 0, w, h)

                var inIdx = encoder.dequeueInputBuffer(10_000L)
                while (inIdx < 0) {
                    drain(false)
                    inIdx = encoder.dequeueInputBuffer(10_000L)
                }
                val image = encoder.getInputImage(inIdx)!!
                fillImage(image, argb, w, h)
                encoder.queueInputBuffer(inIdx, 0, w * h * 3 / 2, tUs, 0)
                drain(false)
                onProgress(i + 1, frameCount)
            }

            var eosIdx = encoder.dequeueInputBuffer(10_000L)
            while (eosIdx < 0) {
                drain(false)
                eosIdx = encoder.dequeueInputBuffer(10_000L)
            }
            encoder.queueInputBuffer(eosIdx, 0, 0, frameCount * 1_000_000L / fps, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            drain(true)

            if (audioTrack >= 0 && muxerStarted && audioIdx >= 0) {
                val aex = MediaExtractor()
                try {
                    aex.setDataSource(ctx, uri, null)
                    aex.selectTrack(audioIdx)
                    val buf = ByteBuffer.allocate(1 shl 20)
                    val bi = MediaCodec.BufferInfo()
                    while (true) {
                        val size = aex.readSampleData(buf, 0)
                        if (size < 0) break
                        bi.set(0, size, aex.sampleTime, aex.sampleFlags)
                        muxer.writeSampleData(audioTrack, buf, bi)
                        aex.advance()
                    }
                } finally {
                    aex.release()
                }
            }
            ok = true
        } finally {
            try { encoder.stop() } catch (_: Exception) {}
            encoder.release()
            try { if (muxerStarted) muxer.stop() } catch (_: Exception) { ok = false }
            muxer.release()
            retriever.release()
        }

        val result = if (ok) MediaUtils.saveVideoFile(ctx, tmp) else null
        tmp.delete()
        onProgress(frameCount, frameCount)
        result
    }

    private fun fillImage(img: Image, argb: IntArray, w: Int, h: Int) {
        val yP = img.planes[0]
        val uP = img.planes[1]
        val vP = img.planes[2]
        val yB = yP.buffer
        val uB = uP.buffer
        val vB = vP.buffer
        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = argb[y * w + x]
                val r = (c shr 16) and 255
                val g = (c shr 8) and 255
                val b = c and 255
                val yy = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yB.put(y * yP.rowStride + x * yP.pixelStride, yy.coerceIn(0, 255).toByte())
                if ((y and 1) == 0 && (x and 1) == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    uB.put((y / 2) * uP.rowStride + (x / 2) * uP.pixelStride, u.coerceIn(0, 255).toByte())
                    vB.put((y / 2) * vP.rowStride + (x / 2) * vP.pixelStride, v.coerceIn(0, 255).toByte())
                }
            }
        }
    }
}
