package com.example.baobab

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val ActivityGreen = Color(0xFF2F5539)
internal val ActivityInk = Color(0xFF26372B)
internal val ActivityMuted = Color(0xFF697369)
internal val ActivityCream = Color(0xFFFDF9F1)
internal fun activityDate(value: String): String = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREA))
}.getOrDefault(value)
internal fun activityStatus(value: String) = when (value) {
    "APPLIED" -> "신청 중"
    "ACCEPTED" -> "선정 완료"
    "COMPLETED" -> "참여 완료"
    "CANCELLED" -> "신청 취소"
    "REJECTED" -> "미선정"
    else -> value
}

@Composable
internal fun ActivityScreenHeader(title: String, onBack: () -> Unit, enabled: Boolean = true,
    onRefresh: (() -> Unit)? = null, refreshEnabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, enabled = enabled) {
            Icon(Icons.Outlined.ChevronLeft, "뒤로가기", tint = ActivityGreen)
        }
        Text(title, Modifier.weight(1f), color = ActivityInk,
            fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
        if (onRefresh != null) IconButton(onClick = onRefresh, enabled = enabled && refreshEnabled) {
            Icon(Icons.Outlined.Refresh, "새로고침", tint = ActivityGreen)
        }
    }
}

@Composable
internal fun ActivityBadge(label: String) {
    Surface(color = Color(0xFFEAF0E4), contentColor = ActivityGreen, shape = RoundedCornerShape(8.dp)) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = ActivityGreen,
            fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun RecruitmentApplicationCard(item: RecruitmentApplicationItem, enabled: Boolean,
    onDetail: () -> Unit, onCancel: () -> Unit, onPointHistory: () -> Unit) {
    Card(onClick = onDetail, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = ActivityInk)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ActivityBadge("${item.recruitment.feedItem().participationLabel} · ${activityStatus(item.status)}")
            Text(item.recruitment.title, color = ActivityInk, fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
            Text(item.recruitment.organization, color = ActivityMuted, fontSize = 13.sp, lineHeight = 20.sp)
            Text("참여 일정 · ${activityDate(item.slot.startsAt)}\n${item.recruitment.location}",
                color = ActivityMuted, fontSize = 13.sp, lineHeight = 21.sp)
            Text(when (item.status) {
                "APPLIED" -> "선정 결과를 기다리고 있어요."
                "ACCEPTED" -> "참여 예정 · %,dP".format(item.recruitment.rewardPoint)
                "COMPLETED" -> if (item.paidAt != null) "%,dP 지급 완료".format(item.recruitment.rewardPoint) else "참여 완료"
                else -> activityStatus(item.status)
            }, color = ActivityGreen, fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
            Text("모집 상세 보기 →", color = ActivityGreen, fontSize = 14.sp, lineHeight = 22.sp)
            if (item.status in listOf("APPLIED", "ACCEPTED")) OutlinedButton(onClick = onCancel,
                enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ActivityGreen)) {
                Text("신청 취소", fontSize = 14.sp, lineHeight = 22.sp)
            }
            if (item.status == "COMPLETED") TextButton(onClick = onPointHistory, enabled = enabled) {
                Text("포인트 내역 보기", color = ActivityGreen, fontSize = 14.sp, lineHeight = 22.sp)
            }
        }
    }
}
