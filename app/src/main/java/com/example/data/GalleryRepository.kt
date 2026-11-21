package com.example.data

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.model.PhotoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface DeleteBatchResult {
    data class RequiresConfirmation(val intentSender: IntentSender) : DeleteBatchResult
    data class Completed(val deletedCount: Int) : DeleteBatchResult
    data class Failed(val message: String) : DeleteBatchResult
}

class GalleryRepository(private val context: Context) {

    suspend fun loadDevicePhotos(): List<PhotoItem> = withContext(Dispatchers.IO) {
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
                        ),
                    )
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            // Return whatever rows were safely loaded instead of crashing
        }

        photos
    }

    suspend fun requestDeletePhotos(uris: List<Uri>): DeleteBatchResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext DeleteBatchResult.Completed(0)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
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
            DeleteBatchResult.Failed(e.message ?: "Unable to delete selected photos")
        }
    }
}
