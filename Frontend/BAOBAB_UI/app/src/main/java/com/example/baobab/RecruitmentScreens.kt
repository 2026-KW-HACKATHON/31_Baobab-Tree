package com.example.baobab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val RecruitmentGreen = Color(0xFF2F5539)

private fun recruitmentDate(value: String): String = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("M월 d일 HH:mm"))
}.getOrDefault(value)

private fun recruitmentStatus(value: String): String = when (value) {
    "APPLIED" -> "신청 중"
    "ACCEPTED" -> "선정됨"
    "COMPLETED" -> "참여 완료"
    "CANCELLED" -> "취소됨"
    "REJECTED" -> "미선정"
    else -> value
}

@Composable
fun RecruitmentHubScreen(token: String?, onBack: () -> Unit, onLogin: () -> Unit) {
    val repository = remember { HttpSurveyRepository(BuildConfig.SURVEY_API_BASE_URL) }
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf("LIST") }
    var previousPage by rememberSaveable { mutableStateOf("LIST") }
    var detailId by rememberSaveable { mutableStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }
    var submittedSearch by rememberSaveable { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf(emptyList<RecruitmentItem>()) }
    var applications by remember { mutableStateOf(emptyList<RecruitmentApplicationItem>()) }
    var detail by remember { mutableStateOf<RecruitmentItem?>(null) }

    fun back() {
        if (!busy) {
            if (page == "LIST") onBack() else page = if (page == "DETAIL") previousPage else "LIST"
        }
    }
    BackHandler { back() }

    if (page == "MANAGE" && token != null) {
        RecruitmentManageScreen(
            token = token,
            onBack = { page = "LIST"; refresh++ }
        )
        return
    }

    if (page == "CREATE" && token != null) {
        RecruitmentCreateScreen(
            token = token,
            onBack = { page = "LIST" },
            onCreated = { created ->
                previousPage = "LIST"
                detailId = created.id
                notice = "모집을 등록했습니다."
                page = "DETAIL"
                refresh++
            }
        )
        return
    }

    LaunchedEffect(page, detailId, submittedSearch, refresh, token) {
        loading = true
        error = null
        try {
            when (page) {
                "LIST" -> items = withContext(Dispatchers.IO) {
                    repository.getRecruitments(search = submittedSearch.takeIf { it.isNotBlank() })
                }
                "DETAIL" -> {
                    detail = null
                    detail = withContext(Dispatchers.IO) { repository.getRecruitment(detailId) }
                }
                "APPLICATIONS" -> applications = if (token == null) emptyList() else {
                    withContext(Dispatchers.IO) { repository.getMyRecruitmentApplications(token) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "불러오지 못했습니다."
        } finally {
            loading = false
        }
    }

    fun action(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        notice = null
        scope.launch {
            try {
                block()
                refresh++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "요청에 실패했습니다."
            } finally {
                busy = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xFFFDF9F1))
            .safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { back() }, enabled = !busy) { Text("❮", color = RecruitmentGreen) }
            Text(when (page) {
                "DETAIL" -> "모집 상세"
                "APPLICATIONS" -> "내 신청 내역"
                else -> "참여자 모집"
            }, style = MaterialTheme.typography.headlineSmall, color = RecruitmentGreen)
        }
        if (page == "LIST") {
            OutlinedTextField(value = search, onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("모집 제목·기관 검색") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { submittedSearch = search.trim(); refresh++ }, enabled = !loading) { Text("검색") }
                OutlinedButton(onClick = { page = "APPLICATIONS" }) { Text("내 신청 내역") }
            }
            Button(
                onClick = { if (token == null) onLogin() else page = "CREATE" },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RecruitmentGreen)
            ) { Text("+ 참여자 모집하기") }
            OutlinedButton(
                onClick = { if (token == null) onLogin() else page = "MANAGE" },
                modifier = Modifier.fillMaxWidth()
            ) { Text("내 모집글 · 신청자 관리") }
        }
        notice?.let { Text(it, color = RecruitmentGreen) }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { refresh++ }, enabled = !busy) { Text("다시 불러오기") }
        }
        if (loading || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!loading) when (page) {
            "LIST" -> {
                if (items.isEmpty() && error == null) Text("현재 모집 중인 글이 없습니다.")
                items.forEach { item ->
                    RecruitmentPanel {
                        Text(item.title, style = MaterialTheme.typography.titleLarge)
                        Text(item.organization)
                        Text("${if (item.participationMode == "ONLINE") "온라인" else "오프라인"} · ${item.durationMinutes}분 · ${item.location}")
                        Text("보상 ${item.rewardPoint}P · 선정 ${item.acceptedCount}/${item.targetCount}명", color = RecruitmentGreen)
                        Text("신청 마감 ${recruitmentDate(item.applicationDeadline)}")
                        TextButton(onClick = { previousPage = "LIST"; detailId = item.id; page = "DETAIL" }) { Text("자세히 보기") }
                    }
                }
            }
            "DETAIL" -> detail?.let { item ->
                RecruitmentDetailContent(item, busy, token != null, onLogin) { slotId, message ->
                    val currentToken = token ?: return@RecruitmentDetailContent
                    action {
                        withContext(Dispatchers.IO) {
                            repository.applyRecruitment(currentToken, item.id, slotId, message, true)
                        }
                        notice = "신청이 처리되었습니다. 내 신청 내역에서 상태를 확인해주세요."
                        page = "APPLICATIONS"
                    }
                }
            }
            "APPLICATIONS" -> {
                if (token == null) Button(onClick = onLogin) { Text("로그인하고 신청 내역 보기") }
                else {
                    if (applications.isEmpty() && error == null) Text("신청한 모집이 없습니다.")
                    applications.forEach { application ->
                        RecruitmentPanel {
                            Text(application.recruitment.title, style = MaterialTheme.typography.titleLarge)
                            Text(recruitmentStatus(application.status), color = RecruitmentGreen)
                            Text("참여 일정 ${recruitmentDate(application.slot.startsAt)}")
                            Text("보상 ${application.recruitment.rewardPoint}P" + if (application.paidAt != null) " · 지급 완료" else " · 미지급")
                            TextButton(onClick = {
                                previousPage = "APPLICATIONS"; detailId = application.recruitment.id; page = "DETAIL"
                            }, enabled = !busy) { Text("모집글 보기") }
                            if (application.status in listOf("APPLIED", "ACCEPTED")) {
                                OutlinedButton(onClick = {
                                    action {
                                        withContext(Dispatchers.IO) { repository.cancelRecruitmentApplication(token, application.id) }
                                        notice = "신청을 취소했습니다."
                                    }
                                }, enabled = !busy) { Text("신청 취소") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecruitmentPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = Color.White,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD9E0D5))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun RecruitmentDetailContent(
    item: RecruitmentItem, busy: Boolean, loggedIn: Boolean, onLogin: () -> Unit,
    onApply: (Int, String) -> Unit
) {
    var slotId by rememberSaveable(item.id) { mutableStateOf(0) }
    var message by rememberSaveable(item.id) { mutableStateOf("") }
    var agreed by rememberSaveable(item.id) { mutableStateOf(false) }
    RecruitmentPanel {
        Text(item.title, style = MaterialTheme.typography.headlineSmall)
        Text(item.organization)
        Text("보상 ${item.rewardPoint}P · ${item.durationMinutes}분", color = RecruitmentGreen)
        Text("장소: ${item.location}")
        Text("참여 대상: ${item.eligibility}")
        Text("신청 마감: ${recruitmentDate(item.applicationDeadline)}")
        Text("선정 인원: ${item.acceptedCount}/${item.targetCount}명")
        HorizontalDivider()
        Text(item.description)
    }
    Text("참여 일정 선택", style = MaterialTheme.typography.titleMedium)
    item.slots.forEach { slot ->
        OutlinedButton(onClick = { slotId = slot.id }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text((if (slotId == slot.id) "✓ " else "") + recruitmentDate(slot.startsAt) + " ~ " + recruitmentDate(slot.endsAt))
        }
    }
    OutlinedTextField(value = message, onValueChange = { if (it.length <= 2000) message = it },
        modifier = Modifier.fillMaxWidth(), enabled = !busy, label = { Text("신청 메시지 (선택)") })
    Row {
        Checkbox(checked = agreed, onCheckedChange = { agreed = it }, enabled = !busy)
        Text("모집자가 신청자 이름과 프로필 정보 및 신청 메시지를 확인하는 데 동의합니다.", modifier = Modifier.padding(top = 12.dp))
    }
    if (!loggedIn) Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("로그인하고 신청하기") }
    else Button(onClick = { onApply(slotId, message.trim()) }, modifier = Modifier.fillMaxWidth(),
        enabled = !busy && agreed && slotId != 0 && item.status == "OPEN",
        colors = ButtonDefaults.buttonColors(containerColor = RecruitmentGreen)) { Text("참가 신청하기") }
}
