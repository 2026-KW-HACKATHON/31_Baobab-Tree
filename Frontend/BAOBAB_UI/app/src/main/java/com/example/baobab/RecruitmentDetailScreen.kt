package com.example.baobab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.OffsetDateTime

@Composable
internal fun RecruitmentDetailScreen(item: RecruitmentItem?, loading: Boolean, busy: Boolean,
    error: String?, notice: String?, loggedIn: Boolean, owner: Boolean,
    onBack: () -> Unit, onRetry: () -> Unit, onLogin: () -> Unit, onManage: () -> Unit,
    onApply: (Int, String) -> Unit) {
    var slotId by rememberSaveable(item?.id) { mutableIntStateOf(0) }
    var agreed by rememberSaveable(item?.id) { mutableStateOf(false) }
    var message by rememberSaveable(item?.id) { mutableStateOf("") }
    val selected = item?.slots?.firstOrNull { it.id == slotId }
    val accepting = item != null && item.status == "OPEN" && runCatching {
        OffsetDateTime.parse(item.applicationDeadline).isAfter(OffsetDateTime.now())
    }.getOrDefault(false)
    Column(Modifier.fillMaxSize().background(ActivityCream).safeDrawingPadding().imePadding()) {
        ActivityScreenHeader("모집 상세", onBack, !busy, onRetry, !loading)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            notice?.let { Text(it, color = ActivityGreen, fontSize = 14.sp, lineHeight = 22.sp) }
            error?.let {
                Text(it, color = Color(0xFFB3261E), fontSize = 14.sp, lineHeight = 22.sp)
                TextButton(onClick = onRetry, enabled = !busy) { Text("다시 시도", color = ActivityGreen) }
            }
            if (loading || busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = ActivityGreen)
            if (item != null) {
                SurveyImage(item.feedItem(), Modifier.fillMaxWidth().aspectRatio(1.7f).clip(RoundedCornerShape(24.dp)))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActivityBadge("${item.purpose()} · ${item.feedItem().participationLabel}")
                    Text(item.title, color = ActivityGreen, fontSize = 25.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)
                    Text(item.organization, color = ActivityMuted, fontSize = 14.sp, lineHeight = 22.sp)
                }
                DetailPanel {
                    DetailValue("참여 보상", "%,d P".format(item.rewardPoint), true)
                    HorizontalDivider(color = Color(0xFFE8ECE4))
                    DetailValue("참여 현황", "${item.acceptedCount} / ${item.targetCount}명 선정")
                    DetailValue("예상 소요시간", "약 ${item.durationMinutes}분")
                }
                Text("모집 소개", color = ActivityInk, fontSize = 19.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
                Text(item.descriptionBody(), color = ActivityMuted, fontSize = 15.sp, lineHeight = 25.sp)
                DetailPanel {
                    DetailValue("참여 대상", item.eligibility)
                    DetailValue(if (item.participationMode == "ONLINE") "온라인 진행 방법" else "참여 장소", item.location)
                    DetailValue("신청 마감", activityDate(item.applicationDeadline))
                }
                if (!owner && accepting) {
                    Text("참여 일정 선택", color = ActivityInk, fontSize = 19.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
                    DetailPanel {
                        item.slots.forEach { slot ->
                            val future = runCatching { OffsetDateTime.parse(slot.startsAt).isAfter(OffsetDateTime.now()) }.getOrDefault(false)
                            Surface(onClick = { slotId = slot.id }, enabled = !busy && future,
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                color = if (slotId == slot.id) Color(0xFFEAF0E4) else Color(0xFFF8F8F4), contentColor = ActivityInk) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(slotId == slot.id, onClick = null, enabled = !busy && future,
                                        colors = RadioButtonDefaults.colors(selectedColor = ActivityGreen))
                                    Text("${activityDate(slot.startsAt)}\n~ ${activityDate(slot.endsAt)}",
                                        Modifier.weight(1f).padding(start = 8.dp), color = if (future) ActivityInk else ActivityMuted,
                                        fontSize = 14.sp, lineHeight = 23.sp)
                                }
                            }
                        }
                        if (item.slots.isEmpty()) Text("신청 가능한 일정이 없습니다.", color = ActivityMuted)
                    }
                    OutlinedTextField(message, { if (it.length <= 2000) message = it }, modifier = Modifier.fillMaxWidth(),
                        enabled = !busy, label = { Text("신청 메시지 (선택)", color = ActivityMuted) },
                        minLines = 3, shape = RoundedCornerShape(12.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 23.sp),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                            focusedTextColor = ActivityInk, unfocusedTextColor = ActivityInk, cursorColor = ActivityGreen,
                            focusedBorderColor = ActivityGreen, unfocusedBorderColor = Color(0xFFD9E0D5)))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Checkbox(agreed, { agreed = it }, enabled = !busy, colors = CheckboxDefaults.colors(checkedColor = ActivityGreen))
                        Text("모집자가 신청자 이름·프로필과 신청 메시지를 확인하는 데 동의합니다.",
                            Modifier.weight(1f).padding(top = 12.dp), color = ActivityMuted, fontSize = 13.sp, lineHeight = 21.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        if (item != null) Surface(color = ActivityCream, shadowElevation = 4.dp) {
            val enabled = !busy && !loading && (owner || (accepting && (!loggedIn || (agreed && selected != null))))
            Button(onClick = {
                when {
                    owner -> onManage()
                    !loggedIn -> onLogin()
                    selected != null -> onApply(selected.id, message.trim())
                }
            }, enabled = enabled, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp).heightIn(min = 54.dp),
                shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = ActivityGreen,
                    contentColor = Color.White, disabledContainerColor = Color(0xFFE1E5DD), disabledContentColor = ActivityMuted)) {
                Text(when {
                    busy -> "처리 중…"
                    owner -> "신청자 관리"
                    !accepting -> "신청이 마감된 모집입니다"
                    !loggedIn -> "로그인하고 신청하기"
                    else -> "참가 신청하기"
                }, fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DetailPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = Color.White, contentColor = ActivityInk, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
private fun DetailValue(label: String, value: String, highlight: Boolean = false) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = ActivityMuted, fontSize = 13.sp, lineHeight = 20.sp)
        Text(value, color = if (highlight) ActivityGreen else ActivityInk, fontSize = if (highlight) 24.sp else 15.sp,
            lineHeight = if (highlight) 34.sp else 24.sp, fontWeight = FontWeight.Bold)
    }
}
