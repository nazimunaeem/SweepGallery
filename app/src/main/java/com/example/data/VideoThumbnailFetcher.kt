package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.pxOrElse

/**
 * Lightweight zero-dependency Coil Fetcher for video content URIs.
 * Uses Android's MediaStore thumbnail API on Q+ with MediaMetadataRetriever fallback.
 */
class VideoThumbnailFetcher(
    private val context: Context,
    private val uri: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val width = options.size.width.pxOrElse { 720 }.coerceIn(128, 1080)
        val height = options.size.height.pxOrElse { 720 }.coerceIn(128, 1080)

        val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                context.contentResolver.loadThumbnail(uri, Size(width, height), null)
            }.getOrNull()
        } else {
            null
        } ?: extractFrameFallback(uri)

        return bitmap?.let {
            DrawableResult(
                drawable = BitmapDrawable(context.resources, it),
                isSampled = true,
                dataSource = DataSource.DISK,
            )
        }
    }

    private fun extractFrameFallback(uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val mime = context.contentResolver.getType(data)
            val isVideoUri = (mime != null && mime.startsWith("video/")) ||
                data.toString().contains("/video/", ignoreCase = true)
            return if (isVideoUri) {
                VideoThumbnailFetcher(context, data, options)
            } else {
                null
            }
        }
    }
}
