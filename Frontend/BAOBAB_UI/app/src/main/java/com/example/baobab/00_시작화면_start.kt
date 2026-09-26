package com.example.baobab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

@Composable
fun SplashScreen() {
    val logo = buildAnnotatedString {
        withStyle(
            SpanStyle(color = Color(0xFF2D4F37))
        ) {
            append("BA")
        }

        withStyle(
            SpanStyle(color = Color(0xFF563C28))
        ) {
            append("OB")
        }

        withStyle(
            SpanStyle(color = Color(0xFF2D4F37))
        ) {
            append("AB")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF9F1)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = logo,
            fontFamily = FontFamily(Font(R.font.jaro_regular)),
            fontSize = 35.sp,
            textAlign = TextAlign.Center,
            lineHeight = 35.sp
        )
    }
}
