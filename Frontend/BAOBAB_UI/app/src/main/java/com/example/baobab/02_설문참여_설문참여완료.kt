package com.example.baobab

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SurveyCompletionScreen(result: ParticipationResult? = null, onHomeClick: () -> Unit = {}) {
    val green = Color(0xFF2F5539)
    Column(Modifier.fillMaxSize().background(Color(0xFFFDF9F1)).safeDrawingPadding()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Image(painterResource(R.drawable.survey_completion_illustration), "설문 참여 완료 축하 그림",
                Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Fit)
            Text("설문 참여가\n완료되었습니다!", fontSize = 28.sp, lineHeight = 36.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("소중한 의견을 남겨주셔서 감사합니다.\n여러분의 답변이 더 나은 동네를 만들어요.",
                Modifier.fillMaxWidth(), fontSize = 17.sp, lineHeight = 26.sp, textAlign = TextAlign.Center,
                color = Color(0xFF697369))
            Surface(color = Color(0xFFFFF0C8), shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${result?.rewardPoint ?: 0} P", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = green)
                    Text(if ((result?.rewardPoint ?: 0) > 0) "리워드가 지급되었어요!" else "설문 참여가 기록되었어요.", color = green)
                    result?.let { Text("보유 포인트: ${it.point} P", color = green) }
                }
            }
        }
        Button(onHomeClick, Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = green)) { Text("홈 화면으로 돌아가기") }
    }
}
