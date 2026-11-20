package com.example.data

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
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generates real JPEG sample photos directly into MediaStore (Pictures/TindPhotoSamples)
 * when explicitly requested on an empty gallery/emulator so users can immediately test
 * swiping and real MediaStore deletion.
 */
class SamplePhotoSeeder(private val context: Context) {

    private data class SampleSpec(
        val fileName: String,
        val topColor: Int,
        val bottomColor: Int,
        val accentColor: Int,
        val style: Int,
    )

    suspend fun seedSamplePhotos(): Int = withContext(Dispatchers.IO) {
        val specs = listOf(
            SampleSpec(
                fileName = "IMG_Sunset_Coast.jpg",
                topColor = Color.rgb(30, 22, 68),
                bottomColor = Color.rgb(255, 102, 82),
                accentColor = Color.rgb(255, 206, 107),
                style = 0,
            ),
            SampleSpec(
                fileName = "IMG_Alpine_Morning.jpg",
                topColor = Color.rgb(28, 78, 128),
                bottomColor = Color.rgb(164, 214, 232),
                accentColor = Color.rgb(245, 248, 255),
                style = 1,
            ),
            SampleSpec(
                fileName = "IMG_Neon_Night.jpg",
                topColor = Color.rgb(15, 12, 29),
                bottomColor = Color.rgb(54, 18, 72),
                accentColor = Color.rgb(16, 214, 142),
                style = 2,
            ),
            SampleSpec(
                fileName = "Screenshot_Receipt_Old.jpg",
                topColor = Color.rgb(236, 234, 228),
                bottomColor = Color.rgb(212, 208, 198),
                accentColor = Color.rgb(90, 92, 102),
                style = 3,
            ),
            SampleSpec(
                fileName = "IMG_Desert_Dunes.jpg",
                topColor = Color.rgb(255, 169, 98),
                bottomColor = Color.rgb(168, 66, 44),
                accentColor = Color.rgb(255, 232, 190),
                style = 4,
            ),
            SampleSpec(
                fileName = "IMG_Pocket_Blur_Test.jpg",
                topColor = Color.rgb(38, 36, 40),
                bottomColor = Color.rgb(22, 20, 24),
                accentColor = Color.rgb(255, 77, 103),
                style = 5,
            ),
        )

        var inserted = 0
        for (spec in specs) {
            val bitmap = renderSampleBitmap(spec)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, spec.fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.WIDTH, bitmap.width)
                put(MediaStore.Images.Media.HEIGHT, bitmap.height)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/TindPhotoSamples",
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                try {
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val done = ContentValues().apply {
                            put(MediaStore.Images.Media.IS_PENDING, 0)
                        }
                        resolver.update(uri, done, null, null)
                    }
                    inserted++
                } catch (_: Exception) {
                    resolver.delete(uri, null, null)
                }
            }
            bitmap.recycle()
        }
        inserted
    }

    private fun renderSampleBitmap(spec: SampleSpec): Bitmap {
        val width = 1080
        val height = 1440
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
                    width * 0.5f, height * 0.54f, 260f,
                    spec.accentColor, Color.TRANSPARENT, Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(width * 0.5f, height * 0.54f, 260f, paint)
                paint.shader = null
                paint.color = spec.accentColor
                canvas.drawCircle(width * 0.5f, height * 0.54f, 125f, paint)
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
                canvas.drawCircle(width * 0.22f, height * 0.22f, 70f, paint)
            }
            2 -> { // Neon rings
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 18f
                paint.color = spec.accentColor
                canvas.drawCircle(width * 0.5f, height * 0.46f, 220f, paint)
                paint.color = Color.rgb(255, 77, 103)
                canvas.drawCircle(width * 0.5f, height * 0.46f, 150f, paint)
                paint.style = Paint.Style.FILL
                paint.color = Color.argb(120, 255, 255, 255)
                canvas.drawRoundRect(160f, height * 0.72f, width - 160f, height * 0.78f, 32f, 32f, paint)
            }
            3 -> { // Receipt screenshot look
                paint.color = Color.WHITE
                canvas.drawRoundRect(140f, 180f, width - 140f, height - 180f, 36f, 36f, paint)
                paint.color = spec.accentColor
                for (i in 0 until 9) {
                    val y = 300f + i * 105f
                    val lineW = if (i % 3 == 0) 420f else 640f
                    canvas.drawRoundRect(210f, y, 210f + lineW, y + 26f, 12f, 12f, paint)
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
                canvas.drawCircle(width * 0.74f, height * 0.24f, 88f, paint)
            }
            else -> { // Blurry dark pocket shot
                paint.shader = RadialGradient(
                    width * 0.35f, height * 0.4f, 420f,
                    Color.argb(140, 255, 77, 103), Color.TRANSPARENT, Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(width * 0.35f, height * 0.4f, 420f, paint)
                paint.shader = RadialGradient(
                    width * 0.7f, height * 0.65f, 360f,
                    Color.argb(120, 255, 160, 80), Color.TRANSPARENT, Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(width * 0.7f, height * 0.65f, 360f, paint)
            }
        }
        return bitmap
    }
}
