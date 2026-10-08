package com.example.baobab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ManageRecruitmentGreen = Color(0xFF2F5539)

private data class RecruitmentManagementAction(
    val type: String,
    val recruitmentId: Int,
    val applicationId: Int? = null,
    val participantName: String = "",
    val rewardPoint: Int = 0
)

private fun managementDate(value: String): String = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("M월 d일 HH:mm"))
}.getOrDefault(value)

private fun managementStatus(value: String): String = when (value) {
    "APPLIED" -> "신청 중"
    "ACCEPTED" -> "선정됨"
    "COMPLETED" -> "참여 완료"
    "CANCELLED" -> "취소됨"
    "REJECTED" -> "미선정"
    else -> value
}

@Composable
fun RecruitmentManageScreen(token: String, onBack: () -> Unit, initialRecruitmentId: Int? = null) {
    val repository = remember { HttpSurveyRepository(BuildConfig.SURVEY_API_BASE_URL) }
    val scope = rememberCoroutineScope()
    var selectedId by rememberSaveable { mutableStateOf<Int?>(initialRecruitmentId) }
    var refresh by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var recruitments by remember { mutableStateOf(emptyList<RecruitmentItem>()) }
    var recruitment by remember { mutableStateOf<RecruitmentItem?>(null) }
    var applicants by remember { mutableStateOf(emptyList<RecruitmentApplicantItem>()) }
    var pending by remember { mutableStateOf<RecruitmentManagementAction?>(null) }

    fun back() {
        if (!busy) {
            pending = null
            error = null
            notice = null
            if (selectedId == null || initialRecruitmentId != null) onBack() else {
                loading = true
                selectedId = null
            }
        }
    }
    BackHandler { back() }

    LaunchedEffect(token, selectedId, refresh) {
        loading = true
        error = null
        recruitment = null
        applicants = emptyList()
        try {
            val id = selectedId
            if (id == null) {
                recruitments = withContext(Dispatchers.IO) { repository.getMyRecruitments(token) }
            } else {
                val loaded = withContext(Dispatchers.IO) {
                    val item = repository.getRecruitment(id)
                    val people = repository.getRecruitmentApplicants(token, id)
                    item to people
                }
                recruitment = loaded.first
                applicants = loaded.second
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "모집 정보를 불러오지 못했습니다."
        } finally {
            loading = false
        }
    }

    fun perform(action: RecruitmentManagementAction) {
        if (busy || loading) return
        pending = null
        busy = true
        error = null
        notice = null
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    when (action.type) {
                        "ACCEPTED", "REJECTED" -> repository.decideRecruitmentApplication(
                            token, requireNotNull(action.applicationId), action.type
                        )
                        "COMPLETE" -> repository.completeRecruitmentApplication(token, requireNotNull(action.applicationId))
                        "CLOSE" -> repository.closeRecruitment(token, action.recruitmentId)
                        else -> throw IllegalArgumentException("알 수 없는 요청입니다.")
                    }
                }
                notice = when (action.type) {
                    "ACCEPTED" -> "${action.participantName} 님을 선정했습니다."
                    "REJECTED" -> "${action.participantName} 님을 미선정 처리했습니다."
                    "COMPLETE" -> if ((result.rewardPoint ?: 0) > 0) {
                        "참여 완료 처리 및 ${result.rewardPoint}P 보상 지급이 완료되었습니다."
                    } else "참여 완료 처리했습니다. (보상 0P)"
                    else -> "모집을 종료했습니다. 미사용 예산 ${result.refundedPoint ?: 0}P를 환급했습니다."
                }
                loading = true
                refresh++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "처리하지 못했습니다. 새로고침 후 상태를 확인해주세요."
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize().background(ActivityCream).safeDrawingPadding()) {
        ActivityScreenHeader(if (selectedId == null) "내가 만든 모집" else "신청자 관리", onBack = { back() },
            enabled = !busy, onRefresh = { loading = true; refresh++ }, refreshEnabled = !loading)
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            notice?.let { Text(it, color = ManageRecruitmentGreen, modifier = Modifier.padding(vertical = 8.dp)) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
            if (loading || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        if (!loading && selectedId == null && error == null) {
            if (recruitments.isEmpty()) item { Text("등록한 모집글이 없습니다.") }
            items(recruitments, key = { it.id }) { item ->
                RecruitmentManagementPanel {
                    Text(item.title, style = MaterialTheme.typography.titleLarge, color = ActivityInk, lineHeight = 29.sp)
                    Text(item.organization)
                    Text(if (item.status == "CLOSED") "종료됨" else "진행 중", color = ManageRecruitmentGreen)
                    Text("선정 ${item.acceptedCount}/${item.targetCount}명 · 1인당 ${item.rewardPoint}P")
                    Text("신청 마감 ${managementDate(item.applicationDeadline)}")
                    Button(onClick = { loading = true; notice = null; selectedId = item.id }, enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = ManageRecruitmentGreen, contentColor = Color.White)) { Text("신청자 관리") }
                }
            }
        }
        val current = recruitment
        if (!loading && current != null && error == null) {
            val open = current.status == "OPEN"
            val uncompleted = applicants.count { it.status == "ACCEPTED" }
            item {
                RecruitmentManagementPanel {
                    Text(current.title, style = MaterialTheme.typography.titleLarge, color = ActivityInk, lineHeight = 29.sp)
                    Text("선정 ${current.acceptedCount}/${current.targetCount}명 · 참여 완료 ${applicants.count { it.status == "COMPLETED" }}명")
                    Text("1인당 보상 ${current.rewardPoint}P", color = ManageRecruitmentGreen)
                    Text(if (open) "선정된 참가자는 일정 종료 후 실제 참여를 확인하고 완료 처리해주세요." else "종료된 모집입니다. 신청 내역을 확인할 수 있습니다.")
                    if (open) {
                        if (uncompleted > 0) Text("선정 후 미처리된 참가자 ${uncompleted}명을 처리해야 모집을 종료할 수 있습니다.")
                        OutlinedButton(onClick = {
                            pending = RecruitmentManagementAction("CLOSE", current.id)
                        }, enabled = !busy && uncompleted == 0) { Text("모집 종료 · 남은 예산 환급") }
                    }
                }
            }
            if (applicants.isEmpty()) item { Text("아직 신청자가 없습니다.") }
            items(applicants, key = { it.id }) { person ->
                RecruitmentManagementPanel {
                    Text(person.user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(managementStatus(person.status), color = ManageRecruitmentGreen)
                    val profile = listOfNotNull(person.user.ageGroup, person.user.region, person.user.memberType).filter { it.isNotBlank() }
                    if (profile.isNotEmpty()) Text(profile.joinToString(" · "))
                    Text("일정 ${managementDate(person.slot.startsAt)} ~ ${managementDate(person.slot.endsAt)}")
                    if (!person.message.isNullOrBlank()) Text("신청 메시지: ${person.message}")
                    if (person.status == "COMPLETED") Text(if (person.paidAt != null) "보상 지급 완료" else "참여 완료 · 지급할 보상 없음")
                    if (open && person.status in listOf("APPLIED", "ACCEPTED")) {
                        if (person.status == "APPLIED") {
                            Button(onClick = {
                                pending = RecruitmentManagementAction("ACCEPTED", current.id, person.id, person.user.name)
                            }, enabled = !busy && current.acceptedCount < current.targetCount,
                                colors = ButtonDefaults.buttonColors(containerColor = ManageRecruitmentGreen, contentColor = Color.White)) { Text("참가자 선정") }
                        } else {
                            Text("참여 완료 버튼은 일정 종료 후 실제 참여를 확인했을 때 눌러주세요.")
                            Button(onClick = {
                                pending = RecruitmentManagementAction("COMPLETE", current.id, person.id, person.user.name, current.rewardPoint)
                            }, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = ManageRecruitmentGreen, contentColor = Color.White)) {
                                Text(if (current.rewardPoint > 0) "참여 완료 · ${current.rewardPoint}P 지급" else "참여 완료 처리")
                            }
                        }
                        OutlinedButton(onClick = {
                            pending = RecruitmentManagementAction("REJECTED", current.id, person.id, person.user.name)
                        }, enabled = !busy) { Text(if (person.status == "ACCEPTED") "선정 취소 · 미선정 처리" else "미선정 처리") }
                    }
                }
            }
        }
    }
    }
    pending?.let { action ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(when (action.type) {
                "ACCEPTED" -> "참가자를 선정할까요?"
                "REJECTED" -> "미선정 처리할까요?"
                "COMPLETE" -> "참여 완료 처리할까요?"
                else -> "모집을 종료할까요?"
            }) },
            text = { Text(when (action.type) {
                "ACCEPTED" -> "${action.participantName} 님을 선정합니다. 보상은 참여 완료 처리 시 지급합니다."
                "REJECTED" -> "${action.participantName} 님을 미선정 처리합니다. 보상은 지급하지 않습니다."
                "COMPLETE" -> "${action.participantName} 님의 실제 참여를 확인했나요? 일정 종료 후 완료 처리하며, 보상 ${action.rewardPoint}P를 지급합니다."
                else -> "새 신청을 마감하고, 신청 중인 참가자를 미선정 처리합니다. 남은 보상 예산은 보유 포인트로 환급합니다."
            }) },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("취소") } },
            confirmButton = { TextButton(onClick = { perform(action) }, enabled = !busy && !loading) { Text("확인") } }
        )
    }
}

@Composable
private fun RecruitmentManagementPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = Color.White, contentColor = ActivityInk, shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFD9E0D5))) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
