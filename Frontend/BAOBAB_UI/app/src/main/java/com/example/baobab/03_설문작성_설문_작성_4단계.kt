package com.example.baobab

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Button
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CreationCompleteBackground = Color(0xFFFDF9F1)
private val CreationCompleteGreen = Color(0xFF2F5539)
private val CreationCompleteGray = Color(0xCC545454)
private val CreationCompleteInter = FontFamily.SansSerif

@Composable
fun SurveyCreationCompleteScreen(onHomeClick: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().background(CreationCompleteBackground)
            .safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.survey_creation_complete),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(1f)
            )
            Text("설문이 등록되었어요!", color = CreationCompleteGreen,
                fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp))
            Text("다양한 사람들의 소중한 의견을 기다려보세요.",
                color = CreationCompleteGray, fontSize = 16.sp, lineHeight = 24.sp,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp, bottom = 24.dp))
        }
        Button(onClick = onHomeClick, modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)) {
            Text("홈 화면으로 돌아가기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SurveyCreationCompletePreview() {
    SurveyCreationCompleteScreen()
}
