package com.example.baobab

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

private val LogoFont = FontFamily(Font(R.font.jaro_regular))

@Composable
fun BaobabLogo(modifier: Modifier = Modifier) {
    val logo = buildAnnotatedString {
        withStyle(SpanStyle(color = Color(0xFF2D4F37))) { append("BA") }
        withStyle(SpanStyle(color = Color(0xFF66472E))) { append("OB") }
        withStyle(SpanStyle(color = Color(0xFF2D4F37))) { append("AB") }
    }
    Text(
        text = logo,
        modifier = modifier,
        fontFamily = LogoFont,
        fontSize = 35.sp,
        lineHeight = 35.sp,
        maxLines = 1,
        softWrap = false
    )
}
