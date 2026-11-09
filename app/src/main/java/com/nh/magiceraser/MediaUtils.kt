package com.nh.magiceraser

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.max

object MediaUtils {

    fun loadBitmap(ctx: Context, uri: Uri, maxSide: Int = 4096): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bmp = ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null
            val orientation = ctx.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val m = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
            }
            val rotated = if (m.isIdentity) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (rotated.config == Bitmap.Config.ARGB_8888 && rotated.isMutable) rotated
            else rotated.copy(Bitmap.Config.ARGB_8888, true)
        } catch (e: Exception) {
            null
        }
    }

    fun saveBitmap(ctx: Context, bmp: Bitmap, png: Boolean, quality: Int): Uri? {
        val ext = if (png) "png" else "jpg"
        val mime = if (png) "image/png" else "image/jpeg"
        val name = "NHEraser_${System.currentTimeMillis()}.$ext"
        return saveToGallery(ctx, name, mime, false) { out ->
            bmp.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, quality, out)
        }
    }

    fun saveVideoFile(ctx: Context, file: File): Uri? {
        val name = "NHEraser_${System.currentTimeMillis()}.mp4"
        return saveToGallery(ctx, name, "video/mp4", true) { out ->
            file.inputStream().use { it.copyTo(out) }
        }
    }

    private fun saveToGallery(
        ctx: Context, name: String, mime: String, video: Boolean, write: (OutputStream) -> Unit
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        (if (video) "Movies" else "Pictures") + "/NH Magic Eraser"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val collection = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val uri = ctx.contentResolver.insert(collection, values) ?: return null
                ctx.contentResolver.openOutputStream(uri)?.use { write(it) }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                ctx.contentResolver.update(uri, values, null, null)
                uri
            } else {
                val base = Environment.getExternalStoragePublicDirectory(
                    if (video) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                )
                val dir = File(base, "NH Magic Eraser").apply { mkdirs() }
                val file = File(dir, name)
                FileOutputStream(file).use { write(it) }
                MediaScannerConnection.scanFile(ctx, arrayOf(file.absolutePath), arrayOf(mime), null)
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun share(ctx: Context, uri: Uri, mime: String) {
        try {
            val i = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(i, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    fun view(ctx: Context, uri: Uri, mime: String) {
        try {
            val i = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(i)
        } catch (_: Exception) {
        }
    }
}
