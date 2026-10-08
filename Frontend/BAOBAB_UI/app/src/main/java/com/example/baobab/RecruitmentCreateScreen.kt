package com.example.baobab

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RecruitmentCreateScreen(
    token: String,
    onBack: () -> Unit,
    onCreated: (RecruitmentItem) -> Unit
) {
    val context = LocalContext.current
    val owner = remember(token) { draftOwner(token) }
    val draftStore = remember(context) { RecruitmentDraftStore(context) }
    val green = Color(0xFF2F5539)
    val repository = remember { HttpSurveyRepository(BuildConfig.SURVEY_API_BASE_URL) }
    val scope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf("") }
    var organization by rememberSaveable { mutableStateOf("") }
    var purpose by rememberSaveable { mutableStateOf("") }
    var activityType by rememberSaveable { mutableStateOf("EXPERIMENT") }
    var participationMode by rememberSaveable { mutableStateOf("OFFLINE") }
    var description by rememberSaveable { mutableStateOf("") }
    var eligibility by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var duration by rememberSaveable { mutableStateOf("60") }
    var count by rememberSaveable { mutableStateOf("1") }
    var reward by rememberSaveable { mutableStateOf("1000") }
    var deadline by rememberSaveable { mutableStateOf("") }
    var startsAt by rememberSaveable { mutableStateOf("") }
    var endsAt by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmation by remember { mutableStateOf(false) }
    var step by rememberSaveable { mutableIntStateOf(1) }
    var activeDraftId by rememberSaveable { mutableStateOf<String?>(null) }
    var promptedDrafts by rememberSaveable { mutableStateOf(false) }
    var showDraftList by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var deletingDraft by remember { mutableStateOf<SavedRecruitmentDraft?>(null) }
    var savedDrafts by remember { mutableStateOf(emptyList<SavedRecruitmentDraft>()) }
    var lastSaved by remember { mutableStateOf(RecruitmentDraft()) }
    fun snapshot() = RecruitmentDraft(title, organization, purpose, activityType, participationMode,
        description, eligibility, location, duration, count, reward, deadline, startsAt, endsAt, step)
    fun restore(draft: RecruitmentDraft) {
        title = draft.title; organization = draft.organization; purpose = draft.purpose
        activityType = draft.activityType; participationMode = draft.participationMode
        description = draft.description; eligibility = draft.eligibility; location = draft.location
        duration = draft.duration; count = draft.count; reward = draft.reward
        deadline = draft.deadline; startsAt = draft.startsAt; endsAt = draft.endsAt
        step = draft.step.coerceIn(1, 3); error = null; lastSaved = snapshot()
    }
    fun saveDraft(exitAfterSaving: Boolean = false) {
        if (busy) return
        val savedOwner = owner ?: run { error = "로그인 정보를 확인할 수 없습니다. 다시 로그인해주세요."; return }
        val id = activeDraftId ?: java.util.UUID.randomUUID().toString()
        val draft = snapshot()
        busy = true; error = null
        scope.launch {
            try {
                savedDrafts = draftStore.save(savedOwner, SavedRecruitmentDraft(id, System.currentTimeMillis(), draft))
                activeDraftId = id; lastSaved = draft
                android.widget.Toast.makeText(context, "모집글을 임시저장했습니다.", android.widget.Toast.LENGTH_SHORT).show()
                if (exitAfterSaving) { showExitDialog = false; onBack() }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "임시저장에 실패했습니다. 다시 시도해주세요." }
            finally { busy = false }
        }
    }
    LaunchedEffect(owner) {
        if (owner != null) {
            busy = true
            try {
                savedDrafts = draftStore.loadAll(owner)
                if (!promptedDrafts && activeDraftId == null && savedDrafts.isNotEmpty()) showDraftList = true
                promptedDrafts = true
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = "임시저장 목록을 불러오지 못했습니다." }
            finally { busy = false }
        }
    }
    val scrollState = rememberScrollState()
    LaunchedEffect(step) { scrollState.scrollTo(0) }
    val budget = (count.toLongOrNull() ?: 0L) * (reward.toLongOrNull() ?: 0L)

    fun validatedInput(): RecruitmentCreateInput {
        require(title.isNotBlank() && title.length <= 150) { "모집 제목을 150자 이내로 입력해주세요." }
        require(organization.isNotBlank() && organization.length <= 100) { "기관명을 100자 이내로 입력해주세요." }
        require(purpose in ParticipationPurposes) { "모집 목적을 선택해주세요." }
        require(description.isNotBlank() && recruitmentDescription(purpose, description).length <= 5000) { "모집 설명을 더 짧게 입력해주세요." }
        require(eligibility.isNotBlank() && eligibility.length <= 2000) { "참여 조건을 2,000자 이내로 입력해주세요." }
        require(location.isNotBlank() && location.length <= 300) { "장소 또는 온라인 진행 방법을 300자 이내로 입력해주세요." }
        val minutes = duration.toIntOrNull()
        val target = count.toIntOrNull()
        val points = reward.toIntOrNull()
        require(minutes != null && minutes in 1..1440) { "소요 시간은 1~1,440분으로 입력해주세요." }
        require(target != null && target in 1..1000) { "모집 인원은 1~1,000명으로 입력해주세요." }
        require(points != null && points in 0..100000) { "1인당 보상은 0~100,000P로 입력해주세요." }
        require(deadline.isNotEmpty() && startsAt.isNotEmpty() && endsAt.isNotEmpty()) { "신청 마감과 참여 시작·종료 일시를 모두 선택해주세요." }
        val deadlineDate = OffsetDateTime.parse(deadline)
        val startDate = OffsetDateTime.parse(startsAt)
        val endDate = OffsetDateTime.parse(endsAt)
        require(deadlineDate.isAfter(OffsetDateTime.now())) { "신청 마감은 현재보다 이후여야 합니다." }
        require(startDate.isAfter(deadlineDate)) { "참여 시작은 신청 마감보다 이후여야 합니다." }
        require(endDate.isAfter(startDate)) { "참여 종료는 시작보다 이후여야 합니다." }
        return RecruitmentCreateInput(
            title = title.trim(), organization = organization.trim(),
            activityType = activityType, participationMode = participationMode,
            description = recruitmentDescription(purpose, description), eligibility = eligibility.trim(), location = location.trim(),
            durationMinutes = minutes, targetCount = target, rewardPoint = points,
            applicationDeadline = deadline,
            slots = listOf(RecruitmentSlotInput(startsAt, endsAt))
        )
    }

    fun back() {
        if (!busy) {
            if (step > 1) { step--; error = null }
            else if (snapshot() != lastSaved) showExitDialog = true
            else onBack()
        }
    }
    val ready = when (step) {
        1 -> title.isNotBlank() && organization.isNotBlank() && purpose in ParticipationPurposes
        2 -> description.isNotBlank() && eligibility.isNotBlank() && location.isNotBlank()
        else -> duration.toIntOrNull()?.let { it in 1..1440 } == true &&
            count.toIntOrNull()?.let { it in 1..1000 } == true &&
            reward.toIntOrNull()?.let { it in 0..100000 } == true &&
            deadline.isNotBlank() && startsAt.isNotBlank() && endsAt.isNotBlank()
    }
    BackHandler { back() }
    Box(Modifier.fillMaxSize().background(Color(0xFFFDF9F1)).safeDrawingPadding().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            SurveyFormHeader(onBackClick = { back() }, title = "참여자 모집하기",
                onSaveDraft = { saveDraft() }, enabled = !busy)
            SurveyFormProgress(step, listOf("기본 정보", "참여 안내", "모집 설정"))
            if (savedDrafts.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { showDraftList = true }, enabled = !busy) {
                    Text("임시저장 목록 (${savedDrafts.size})", color = green, fontSize = 13.sp)
                }
            }
            Column(Modifier.weight(1f).verticalScroll(scrollState).padding(top = 2.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                when (step) {
                    1 -> {
                        RecruitmentCreateField("1. 모집 제목 (필수)", title, 150, busy) { title = it }
                        SurveyFormChoiceField("2. 모집 목적 (필수)", purpose,
                            ParticipationPurposes.map { it to it }, busy) { purpose = it }
                        RecruitmentCreateField("3. 기관·가게·연구실 이름 (필수)", organization, 100, busy) { organization = it }
                        SurveyFormChoiceField("4. 참여 방식", activityType,
                            listOf("EXPERIMENT" to "실험", "INTERVIEW" to "인터뷰", "USABILITY" to "사용성 테스트", "OTHER" to "기타"), busy) { activityType = it }
                        SurveyFormChoiceField("5. 진행 방식", participationMode,
                            listOf("OFFLINE" to "오프라인", "ONLINE" to "온라인"), busy) { participationMode = it }
                    }
                    2 -> {
                        RecruitmentCreateField("1. 모집 소개 (필수)", description, 5000, busy, multiline = true) { description = it }
                        RecruitmentCreateField("2. 참여 조건 (필수)", eligibility, 2000, busy, multiline = true) { eligibility = it }
                        RecruitmentCreateField(if (participationMode == "ONLINE") "3. 온라인 진행 방법 (필수)" else "3. 참여 장소 (필수)",
                            location, 300, busy, multiline = true) { location = it }
                    }
                    3 -> {
                        RecruitmentCreateField("1. 소요 시간 (분)", duration, 4, busy, numeric = true) { duration = it }
                        RecruitmentCreateField("2. 모집 인원 (명)", count, 4, busy, numeric = true) { count = it }
                        RecruitmentCreateField("3. 1인당 보상 (P)", reward, 6, busy, numeric = true) { reward = it }
                        RecruitmentDateField("4. 신청 마감 (필수)", deadline, busy) { deadline = it }
                        RecruitmentDateField("5. 참여 시작 (필수)", startsAt, busy) { startsAt = it }
                        RecruitmentDateField("6. 참여 종료 (필수)", endsAt, busy) { endsAt = it }
                        Surface(Modifier.padding(horizontal = 16.dp), color = Color(0xFFEAF0E3), shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("총 보상 예산: ${String.format("%,d", budget)}P", fontWeight = FontWeight.Bold, color = green, fontSize = 16.sp)
                                Text("등록 시 보유 포인트에서 예산을 차감합니다. 참여 완료 처리 시 보상을 지급하고, 모집 종료 시 미사용 예산을 환급합니다.",
                                    fontSize = 13.sp, lineHeight = 20.sp, color = Color(0xFF454545))
                            }
                        }
                    }
                }
                error?.let { Text(it, Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.error) }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp), color = green)
            }
        }
        Button(onClick = {
            error = null
            if (step < 3) step++
            else try { validatedInput(); confirmation = true } catch (e: Exception) { error = e.message }
        }, enabled = ready && !busy, shape = RoundedCornerShape(16.dp),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(horizontal = 18.dp).padding(bottom = 16.dp).height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = green)) {
            Text(if (busy) "등록 중…" else if (!ready) "필수 정보를 모두 입력해주세요" else if (step < 3) "다음 단계로" else "모집 등록하기",
                color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
    if (showDraftList) AlertDialog(
        onDismissRequest = { if (!busy) showDraftList = false },
        title = { Text("임시저장 모집") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (savedDrafts.isEmpty()) Text("임시저장한 모집글이 없습니다.")
                savedDrafts.forEach { saved ->
                    Column {
                        Text(saved.draft.title.ifBlank { "제목 없는 모집" }, fontWeight = FontWeight.Bold)
                        Text("${saved.draft.step}단계 작성 중 · " + java.text.SimpleDateFormat("yyyy.MM.dd HH:mm", java.util.Locale.KOREA)
                            .format(java.util.Date(saved.updatedAt)), fontSize = 12.sp, color = Color(0xFF697369))
                        Row {
                            TextButton(onClick = {
                                activeDraftId = saved.id; restore(saved.draft); showDraftList = false
                            }, enabled = !busy) { Text("이어 작성", color = green) }
                            BaobabActionButton(BaobabActionIcons.Delete, "임시저장 모집 삭제",
                                { deletingDraft = saved }, enabled = !busy, destructive = true)
                        }
                    }
                    HorizontalDivider(color = Color(0xFFD9E0D5))
                }
            }
        },
        confirmButton = { TextButton(onClick = { showDraftList = false }, enabled = !busy) { Text("닫기") } },
        dismissButton = { TextButton(onClick = {
            activeDraftId = null; restore(RecruitmentDraft()); showDraftList = false
        }, enabled = !busy) { Text("새 모집 작성") } }
    )
    deletingDraft?.let { saved ->
        AlertDialog(onDismissRequest = { if (!busy) deletingDraft = null },
            title = { Text("임시저장 모집 삭제") },
            icon = { Icon(BaobabActionIcons.Delete, null, tint = MaterialTheme.colorScheme.error) },
            text = { Text("${saved.draft.title.ifBlank { "제목 없는 모집" }}\n이 저장본을 삭제하시겠어요?") },
            confirmButton = { TextButton(onClick = {
                val savedOwner = owner
                if (savedOwner != null && !busy) {
                    busy = true
                    scope.launch {
                        try {
                            savedDrafts = draftStore.delete(savedOwner, saved.id)
                            if (activeDraftId == saved.id) { activeDraftId = null; lastSaved = RecruitmentDraft() }
                            deletingDraft = null
                        } catch (e: CancellationException) { throw e }
                        catch (_: Exception) { error = "임시저장본을 삭제하지 못했습니다." }
                        finally { busy = false }
                    }
                }
            }, enabled = !busy) { Text("삭제") } },
            dismissButton = { TextButton(onClick = { deletingDraft = null }, enabled = !busy) { Text("취소") } }
        )
    }
    if (showExitDialog) AlertDialog(
        onDismissRequest = { if (!busy) showExitDialog = false },
        title = { Text("작성 중인 모집을 저장할까요?") },
        text = { Text(error ?: "임시저장하면 나중에 이어서 작성할 수 있어요.") },
        confirmButton = { TextButton(onClick = { saveDraft(exitAfterSaving = true) }, enabled = !busy) { Text("임시저장 후 나가기") } },
        dismissButton = {
            Row {
                TextButton(onClick = { showExitDialog = false; onBack() }, enabled = !busy) { Text("저장하지 않고 나가기") }
                TextButton(onClick = { showExitDialog = false }, enabled = !busy) { Text("계속 작성") }
            }
        }
    )
    if (confirmation) AlertDialog(
        onDismissRequest = { confirmation = false },
        title = { Text("모집을 등록할까요?") },
        text = { Text("총 ${String.format("%,d", budget)}P를 보상 예산으로 차감합니다.") },
        dismissButton = { TextButton(onClick = { confirmation = false }) { Text("취소") } },
        confirmButton = {
            TextButton(onClick = {
                confirmation = false
                if (!busy) {
                    val input = try { validatedInput() } catch (e: Exception) { error = e.message; null }
                    if (input != null) {
                        busy = true
                        scope.launch {
                            try {
                                val created = withContext(Dispatchers.IO) { repository.createRecruitment(token, input) }
                                if (owner != null && activeDraftId != null) {
                                    try { savedDrafts = draftStore.delete(owner, requireNotNull(activeDraftId)) }
                                    catch (e: CancellationException) { throw e }
                                    catch (_: Exception) {
                                        android.widget.Toast.makeText(context, "모집은 등록됐지만 임시저장본 삭제에 실패했습니다.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                                onCreated(created)
                            } catch (e: CancellationException) { throw e }
                            catch (e: Exception) { error = e.message ?: "등록에 실패했습니다." }
                            finally { busy = false }
                        }
                    }
                }
            }) { Text("등록") }
        }
    )
}

@Composable
private fun RecruitmentCreateField(
    label: String, value: String, maxLength: Int, busy: Boolean,
    multiline: Boolean = false, numeric: Boolean = false, onChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, Modifier.padding(start = 20.dp, bottom = 11.dp),
            color = Color(0xFF2F5539), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        SurveyInput(value = value, placeholder = if (numeric) "숫자를 입력해주세요" else "내용을 입력해주세요",
            onValueChange = { if (it.length <= maxLength && (!numeric || it.all { character -> character in '0'..'9' })) onChange(it) },
            multiline = multiline, enabled = !busy,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = if (numeric) androidx.compose.ui.text.input.KeyboardType.Number else androidx.compose.ui.text.input.KeyboardType.Text
            ))
    }
}

@Composable
private fun RecruitmentDateField(label: String, value: String, busy: Boolean, onChange: (String) -> Unit) {
    val context = LocalContext.current
    val display = runCatching {
        OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm"))
    }.getOrDefault("날짜와 시간 선택")
    SurveySelectionField(label, display, Icons.Outlined.EventAvailable, {
        val initial = runCatching { OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime() }
            .getOrDefault(LocalDateTime.now().plusDays(1))
        DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->
                onChange(LocalDateTime.of(year, month + 1, day, hour, minute)
                    .atZone(ZoneId.systemDefault()).toOffsetDateTime().toString())
            }, initial.hour, initial.minute, true).show()
        }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
    }, enabled = !busy)
}
