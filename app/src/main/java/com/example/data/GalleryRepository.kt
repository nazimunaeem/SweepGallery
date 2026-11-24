package com.example.data

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.example.model.MediaKind
import com.example.model.PhotoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

sealed interface DeleteBatchResult {
    data class RequiresConfirmation(val intentSender: IntentSender) : DeleteBatchResult
    data class Completed(val deletedCount: Int) : DeleteBatchResult
    data class Failed(val message: String) : DeleteBatchResult
}

class GalleryRepository(private val context: Context) {

    suspend fun loadDevicePhotos(
        includeImages: Boolean = true,
        includeVideos: Boolean = true,
    ): List<PhotoItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<PhotoItem>()
        if (includeImages) {
            items.addAll(queryImages())
        }
        if (includeVideos) {
            items.addAll(queryVideos())
        }
        items.sortedByDescending { it.dateAddedSeconds }
    }

    private fun queryImages(): List<PhotoItem> {
        val photos = mutableListOf<PhotoItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Images.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Images.Media.DATA)
            }
        }

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "${MediaStore.Images.Media.IS_TRASHED} = 0 AND ${MediaStore.Images.Media.SIZE} > 0"
        } else {
            "${MediaStore.Images.Media.SIZE} > 0"
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection.toTypedArray(),
                selection,
                null,
                sortOrder,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val dateTakenCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                } else {
                    -1
                }
                @Suppress("DEPRECATION")
                val dataCol = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                } else {
                    -1
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Photo_$id"
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else 0L
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val bucket = if (bucketCol >= 0) cursor.getString(bucketCol) else null
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol).orEmpty() else ""
                    val directory = when {
                        relPathCol >= 0 -> cursor.getString(relPathCol).orEmpty().trim('/')
                        dataCol >= 0 -> {
                            val rawPath = cursor.getString(dataCol).orEmpty()
                            val parent = java.io.File(rawPath).parent.orEmpty()
                            parent.substringAfter("/0/", parent).trim('/')
                        }
                        else -> ""
                    }
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id,
                    )

                    photos.add(
                        PhotoItem(
                            id = id,
                            uri = contentUri,
                            displayName = name,
                            sizeBytes = size,
                            dateTakenMillis = dateTaken,
                            dateAddedSeconds = dateAdded,
                            width = width,
                            height = height,
                            albumName = bucket?.takeIf { it.isNotBlank() } ?: "Gallery",
                            mimeType = mime,
                            directoryPath = directory,
                            mediaKind = MediaKind.PHOTO,
                        ),
                    )
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            // Return whatever rows were safely loaded
        }

        return photos
    }

    private fun queryVideos(): List<PhotoItem> {
        val videos = mutableListOf<PhotoItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Video.Media.DATA)
            }
        }

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "${MediaStore.Video.Media.IS_TRASHED} = 0 AND ${MediaStore.Video.Media.SIZE} > 0"
        } else {
            "${MediaStore.Video.Media.SIZE} > 0"
        }

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection.toTypedArray(),
                selection,
                null,
                sortOrder,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dateTakenCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else {
                    -1
                }
                @Suppress("DEPRECATION")
                val dataCol = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                } else {
                    -1
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Video_$id"
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else 0L
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                    val bucket = if (bucketCol >= 0) cursor.getString(bucketCol) else null
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol).orEmpty() else ""
                    val directory = when {
                        relPathCol >= 0 -> cursor.getString(relPathCol).orEmpty().trim('/')
                        dataCol >= 0 -> {
                            val rawPath = cursor.getString(dataCol).orEmpty()
                            val parent = java.io.File(rawPath).parent.orEmpty()
                            parent.substringAfter("/0/", parent).trim('/')
                        }
                        else -> ""
                    }
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id,
                    )

                    videos.add(
                        PhotoItem(
                            id = id,
                            uri = contentUri,
                            displayName = name,
                            sizeBytes = size,
                            dateTakenMillis = dateTaken,
                            dateAddedSeconds = dateAdded,
                            width = width,
                            height = height,
                            albumName = bucket?.takeIf { it.isNotBlank() } ?: "Videos",
                            mimeType = mime,
                            directoryPath = directory,
                            mediaKind = MediaKind.VIDEO,
                            durationMillis = duration,
                        ),
                    )
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            // Return whatever rows were safely loaded
        }

        return videos
    }

    fun observeMediaChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }

        runCatching {
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer,
            )
            context.contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                observer,
            )
        }

        awaitClose {
            runCatching { context.contentResolver.unregisterContentObserver(observer) }
        }
    }

    suspend fun requestDeletePhotos(
        uris: List<Uri>,
        moveToSystemTrash: Boolean = false,
    ): DeleteBatchResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext DeleteBatchResult.Completed(0)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = if (moveToSystemTrash) {
                    MediaStore.createTrashRequest(context.contentResolver, uris, true)
                } else {
                    MediaStore.createDeleteRequest(context.contentResolver, uris)
                }
                DeleteBatchResult.RequiresConfirmation(pendingIntent.intentSender)
            } else {
                var deletedCount = 0
                for (uri in uris) {
                    try {
                        val rows = context.contentResolver.delete(uri, null, null)
                        if (rows > 0) deletedCount++
                    } catch (securityException: SecurityException) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            securityException is RecoverableSecurityException
                        ) {
                            return@withContext DeleteBatchResult.RequiresConfirmation(
                                securityException.userAction.actionIntent.intentSender,
                            )
                        }
                    }
                }
                DeleteBatchResult.Completed(deletedCount)
            }
        } catch (e: Exception) {
            DeleteBatchResult.Failed(e.message ?: "Unable to delete selected media")
        }
    }
}
