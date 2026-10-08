package com.example.baobab

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ParticipationCreationDialog(onDismiss: () -> Unit, onSurvey: () -> Unit, onRecruitment: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CreationChoice("설문조사", "온라인으로\n바로 응답해요", Icons.AutoMirrored.Outlined.Assignment,
                Color(0xFF2F5539), Modifier.weight(1f), onSurvey)
            CreationChoice("참여자 모집", "실험·인터뷰 등\n일정을 정해 만나요", Icons.Outlined.Groups,
                Color(0xFF436B4B), Modifier.weight(1f), onRecruitment)
        }
    }
}

@Composable
private fun CreationChoice(title: String, description: String, icon: ImageVector,
    color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp), color = color) {
        Column(Modifier.heightIn(min = 176.dp).padding(horizontal = 12.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.13f)) {
                Icon(icon, null, Modifier.padding(12.dp).size(30.dp), tint = Color(0xFFE9DBB8))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(description, color = Color(0xFFE0EADC), fontSize = 12.sp, lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
