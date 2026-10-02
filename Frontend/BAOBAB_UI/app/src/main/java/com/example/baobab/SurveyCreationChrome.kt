package com.example.baobab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SurveyFormHeader(onBackClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(60.dp)) {
        IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) {
            Icon(Icons.Outlined.ChevronLeft, "뒤로가기", tint = Color(0xFF2F5539), modifier = Modifier.size(30.dp))
        }
        Text("설문 작성하기", modifier = Modifier.align(Alignment.Center),
            fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFF26372B))
    }
}

@Composable
fun SurveyFormProgress(currentStep: Int) {
    val labels = listOf("기본 정보", "문항 작성", "설문 설정")
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top) {
        labels.forEachIndexed { index, label ->
            val active = index + 1 <= currentStep
            val color = if (active) Color(0xFF2F5539) else Color(0xFF788079)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(32.dp).background(if (active) color else Color(0xFFE5E9E2), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text("${index + 1}", color = if (active) Color.White else color,
                        fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            if (index < labels.lastIndex) {
                Box(Modifier.weight(1f).padding(horizontal = 10.dp).padding(top = 15.dp)
                    .height(2.dp).background(if (index + 1 < currentStep) Color(0xFF2F5539) else Color(0xFFDEE3DA)))
            }
        }
    }
}
