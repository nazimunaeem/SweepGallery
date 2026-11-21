package com.example.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class SeedResult(
    val insertedCount: Int,
    val errorMessage: String? = null,
)

/**
 * Generates real JPEG sample photos directly into MediaStore (Pictures/SweepGallerySamples)
 * when explicitly requested on an empty gallery/emulator so users can immediately test
 * swiping and real MediaStore deletion.
 */
class SamplePhotoSeeder(private val context: Context) {

    private data class SampleSpec(
        val baseName: String,
        val topColor: Int,
        val bottomColor: Int,
        val accentColor: Int,
        val style: Int,
        val monthsAgo: Int,
    )

    suspend fun seedSamplePhotos(): Int = seedSamplePhotosResult().insertedCount

    suspend fun seedSamplePhotosResult(): SeedResult = withContext(Dispatchers.IO) {
        val specs = listOf(
            SampleSpec(
                baseName = "IMG_Sunset_Coast",
                topColor = Color.rgb(30, 22, 68),
                bottomColor = Color.rgb(255, 102, 82),
                accentColor = Color.rgb(255, 206, 107),
                style = 0,
                monthsAgo = 0,
            ),
            SampleSpec(
                baseName = "IMG_Alpine_Morning",
                topColor = Color.rgb(28, 78, 128),
                bottomColor = Color.rgb(164, 214, 232),
                accentColor = Color.rgb(245, 248, 255),
                style = 1,
                monthsAgo = 0,
            ),
            SampleSpec(
                baseName = "IMG_Neon_Night",
                topColor = Color.rgb(15, 12, 29),
                bottomColor = Color.rgb(54, 18, 72),
                accentColor = Color.rgb(16, 214, 142),
                style = 2,
                monthsAgo = 1,
            ),
            SampleSpec(
                baseName = "Screenshot_Receipt_Old",
                topColor = Color.rgb(236, 234, 228),
                bottomColor = Color.rgb(212, 208, 198),
                accentColor = Color.rgb(90, 92, 102),
                style = 3,
                monthsAgo = 1,
            ),
            SampleSpec(
                baseName = "IMG_Desert_Dunes",
                topColor = Color.rgb(255, 169, 98),
                bottomColor = Color.rgb(168, 66, 44),
                accentColor = Color.rgb(255, 232, 190),
                style = 4,
                monthsAgo = 2,
            ),
            SampleSpec(
                baseName = "IMG_Pocket_Blur_Test",
                topColor = Color.rgb(38, 36, 40),
                bottomColor = Color.rgb(22, 20, 24),
                accentColor = Color.rgb(255, 77, 103),
                style = 5,
                monthsAgo = 2,
            ),
        )

        val resolver = context.contentResolver
        val batchSuffix = (System.currentTimeMillis() % 100_000L).toString()
        var inserted = 0
        var lastError: String? = null

        for ((index, spec) in specs.withIndex()) {
            var bitmap: Bitmap? = null
            var insertedUri: Uri? = null
            try {
                bitmap = renderSampleBitmap(spec)
                val dateTaken = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -spec.monthsAgo)
                    add(Calendar.DAY_OF_MONTH, -(index % 5))
                }.timeInMillis

                val fileName = "${spec.baseName}_$batchSuffix.jpg"
                insertedUri = insertMediaEntry(
                    resolver = resolver,
                    fileName = fileName,
                    dateTakenMillis = dateTaken,
                )

                val uri = insertedUri
                if (uri != null) {
                    val wroteSuccessfully = resolver.openOutputStream(uri)?.use { out ->
                        val ok = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        out.flush()
                        ok
                    } ?: false

                    if (wroteSuccessfully) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            runCatching {
                                val done = ContentValues().apply {
                                    put(MediaStore.Images.Media.IS_PENDING, 0)
                                }
                                resolver.update(uri, done, null, null)
                            }
                        }
                        inserted++
                    } else {
                        runCatching { resolver.delete(uri, null, null) }
                        lastError = "Unable to write image bytes to storage"
                    }
                } else {
                    lastError = "Storage rejected new sample photo entry"
                }
            } catch (ce: CancellationException) {
                if (insertedUri != null) {
                    runCatching { resolver.delete(insertedUri, null, null) }
                }
                throw ce
            } catch (t: Throwable) {
                if (insertedUri != null) {
                    runCatching { resolver.delete(insertedUri, null, null) }
                }
                lastError = t.localizedMessage ?: t.javaClass.simpleName
            } finally {
                bitmap?.let {
                    if (!it.isRecycled) {
                        it.recycle()
                    }
                }
            }
        }

        SeedResult(
            insertedCount = inserted,
            errorMessage = if (inserted == 0) lastError else null,
        )
    }

    private fun insertMediaEntry(
        resolver: ContentResolver,
        fileName: String,
        dateTakenMillis: Long,
    ): Uri? {
        val candidateUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            )
        } else {
            listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        }

        val fullValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_TAKEN, dateTakenMillis)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/SweepGallerySamples",
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        for (targetUri in candidateUris) {
            val uri = runCatching { resolver.insert(targetUri, fullValues) }.getOrNull()
            if (uri != null) return uri
        }

        // Minimal fallback for strict MediaStore providers that reject DATE_TAKEN or subfolder paths
        val minimalValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        for (targetUri in candidateUris) {
            val uri = runCatching { resolver.insert(targetUri, minimalValues) }.getOrNull()
            if (uri != null) return uri
        }

        return null
    }

    private fun renderSampleBitmap(spec: SampleSpec): Bitmap {
        val width = 720
        val height = 960
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width * 0.4f, height.toFloat(),
                spec.topColor, spec.bottomColor, Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        when (spec.style) {
            0 -> { // Sunset horizon & glowing orb
                paint.shader = RadialGradient(
                    width * 0.5f, height * 0.54f, 180f,
                    spec.accentColor, Color.TRANSPARENT, Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(width * 0.5f, height * 0.54f, 180f, paint)
                paint.shader = null
                paint.color = spec.accentColor
                canvas.drawCircle(width * 0.5f, height * 0.54f, 84f, paint)
                paint.color = Color.argb(210, 22, 14, 34)
                canvas.drawRect(0f, height * 0.62f, width.toFloat(), height.toFloat(), paint)
            }
            1 -> { // Alpine peaks
                val ridge = Path().apply {
                    moveTo(0f, height.toFloat())
                    lineTo(0f, height * 0.68f)
                    lineTo(width * 0.34f, height * 0.42f)
                    lineTo(width * 0.62f, height * 0.58f)
                    lineTo(width * 0.88f, height * 0.36f)
                    lineTo(width.toFloat(), height * 0.52f)
                    lineTo(width.toFloat(), height.toFloat())
                    close()
                }
                paint.color = Color.rgb(26, 44, 72)
                canvas.drawPath(ridge, paint)
                paint.color = spec.accentColor
                canvas.drawCircle(width * 0.22f, height * 0.22f, 48f, paint)
            }
            2 -> { // Neon rings
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 12f
                paint.color = spec.accentColor
                canvas.drawCircle(width * 0.5f, height * 0.46f, 148f, paint)
                paint.color = Color.rgb(255, 77, 103)
                canvas.drawCircle(width * 0.5f, height * 0.46f, 100f, paint)
                paint.style = Paint.Style.FILL
                paint.color = Color.argb(120, 255, 255, 255)
                canvas.drawRoundRect(108f, height * 0.72f, width - 108f, height * 0.78f, 22f, 22f, paint)
            }
            3 -> { // Receipt screenshot look
                paint.color = Color.WHITE
                canvas.drawRoundRect(92f, 120f, width - 92f, height - 120f, 24f, 24f, paint)
                paint.color = spec.accentColor
                for (i in 0 until 9) {
                    val y = 200f + i * 70f
                    val lineW = if (i % 3 == 0) 280f else 420f
                    canvas.drawRoundRect(140f, y, 140f + lineW, y + 18f, 8f, 8f, paint)
                }
            }
            4 -> { // Desert dunes
                val dune1 = Path().apply {
                    moveTo(0f, height.toFloat())
                    lineTo(0f, height * 0.55f)
                    quadTo(width * 0.45f, height * 0.40f, width.toFloat(), height * 0.62f)
                    lineTo(width.toFloat(), height.toFloat())
                    close()
                }
                paint.color = Color.rgb(198, 88, 52)
                canvas.drawPath(dune1, paint)
                paint.color = spec.accentColor
                canvas.drawCircle(width * 0.74f, height * 0.24f, 58f, paint)
            }
            else -> { // Blurry dark pocket shot
                paint.shader = RadialGradient(
                    width * 0.35f, height * 0.4f, 280f,
                    Color.argb(140, 255, 77, 103), Color.TRANSPARENT, Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(width * 0.35f, height * 0.4f, 280f, paint)
                paint.shader = RadialGradient(
                    width * 0.7f, height * 0.65f, 240f,
                    Color.argb(120, 255, 160, 80), Color.TRANSPARENT, Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(width * 0.7f, height * 0.65f, 240f, paint)
            }
        }
        return bitmap
    }
}
