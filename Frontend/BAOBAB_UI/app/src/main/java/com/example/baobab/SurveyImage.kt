package com.example.baobab

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import java.io.ByteArrayOutputStream
import java.util.Base64

fun readSurveyImage(context: Context, uri: Uri): String {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "이미지 파일을 읽을 수 없습니다." }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1536) sample *= 2
    val decoded = resolver.openInputStream(uri).use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: throw IllegalArgumentException("이미지 파일을 읽을 수 없습니다.")
    try {
        val scale = minOf(1f, 768f / maxOf(decoded.width, decoded.height))
        val resized = Bitmap.createScaledBitmap(decoded, maxOf(1, (decoded.width * scale).toInt()),
            maxOf(1, (decoded.height * scale).toInt()), true)
        val opaque = Bitmap.createBitmap(resized.width, resized.height, Bitmap.Config.ARGB_8888)
        try {
            Canvas(opaque).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(resized, 0f, 0f, null) }
            val bytes = ByteArrayOutputStream().use { stream ->
                opaque.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                stream.toByteArray()
            }
            require(bytes.size <= 700000) { "이미지가 너무 큽니다. 더 작은 사진을 선택해주세요." }
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes)
        } finally {
            opaque.recycle()
            if (resized !== decoded) resized.recycle()
        }
    } finally { decoded.recycle() }
}

@Composable
fun SurveyImage(survey: SurveyItem, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val bitmap = remember(survey.imageData) {
        runCatching {
            survey.imageData?.takeIf { it.length <= 1000000 }?.substringAfter("base64,")?.let {
                val bytes = Base64.getDecoder().decode(it)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        }.getOrNull()
    }
    if (bitmap != null) Image(bitmap, contentDescription = null, modifier = modifier, contentScale = contentScale)
    else Image(painterResource(surveyCategoryImage(survey.category)), contentDescription = null, modifier = modifier, contentScale = contentScale)
}
