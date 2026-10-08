package com.example.baobab

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
    val green = Color(0xFF2F5539)
    val repository = remember { HttpSurveyRepository(BuildConfig.SURVEY_API_BASE_URL) }
    val scope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf("") }
    var organization by rememberSaveable { mutableStateOf("") }
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
    val budget = (count.toLongOrNull() ?: 0L) * (reward.toLongOrNull() ?: 0L)

    fun validatedInput(): RecruitmentCreateInput {
        require(title.isNotBlank() && title.length <= 150) { "모집 제목을 150자 이내로 입력해주세요." }
        require(organization.isNotBlank() && organization.length <= 100) { "기관명을 100자 이내로 입력해주세요." }
        require(description.isNotBlank() && description.length <= 5000) { "모집 설명을 5,000자 이내로 입력해주세요." }
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
            description = description.trim(), eligibility = eligibility.trim(), location = location.trim(),
            durationMinutes = minutes, targetCount = target, rewardPoint = points,
            applicationDeadline = deadline,
            slots = listOf(RecruitmentSlotInput(startsAt, endsAt))
        )
    }

    BackHandler { if (!busy) onBack() }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFFDF9F1)).safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack, enabled = !busy) { Text("❮", color = green) }
            Text("참여자 모집하기", style = MaterialTheme.typography.headlineSmall, color = green)
        }
        RecruitmentCreateField("모집 제목", title, 150, busy) { title = it }
        RecruitmentCreateField("기관·연구실 이름", organization, 100, busy) { organization = it }
        Text("모집 유형", fontWeight = FontWeight.Bold, color = green)
        listOf("EXPERIMENT" to "실험", "INTERVIEW" to "인터뷰", "USABILITY" to "사용성 테스트", "OTHER" to "기타").forEach { (value, label) ->
            OutlinedButton(onClick = { activityType = value }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text((if (activityType == value) "✓ " else "") + label, color = green)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("OFFLINE" to "오프라인", "ONLINE" to "온라인").forEach { (value, label) ->
                OutlinedButton(onClick = { participationMode = value }, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text((if (participationMode == value) "✓ " else "") + label)
                }
            }
        }
        RecruitmentCreateField("모집 설명", description, 5000, busy, multiline = true) { description = it }
        RecruitmentCreateField("참여 조건", eligibility, 2000, busy, multiline = true) { eligibility = it }
        RecruitmentCreateField(if (participationMode == "ONLINE") "온라인 진행 방법" else "참여 장소", location, 300, busy) { location = it }
        RecruitmentCreateField("소요 시간 (분)", duration, 4, busy, numeric = true) { duration = it }
        RecruitmentCreateField("모집 인원 (명)", count, 4, busy, numeric = true) { count = it }
        RecruitmentCreateField("1인당 보상 (P)", reward, 6, busy, numeric = true) { reward = it }
        RecruitmentDateField("신청 마감", deadline, busy) { deadline = it }
        RecruitmentDateField("참여 시작", startsAt, busy) { startsAt = it }
        RecruitmentDateField("참여 종료", endsAt, busy) { endsAt = it }
        Surface(color = Color(0xFFEAF0E3), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("총 보상 예산: ${String.format("%,d", budget)}P", fontWeight = FontWeight.Bold, color = green)
                Text("등록 시 보유 포인트에서 예산을 차감합니다. 참여 완료 처리 시 보상을 지급하고, 모집 종료 시 미사용 예산을 환급합니다.")
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(onClick = {
            error = null
            try { validatedInput(); confirmation = true } catch (e: Exception) { error = e.message }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = green)) { Text("모집 등록하기") }
    }
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
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength && (!numeric || it.all { character -> character in '0'..'9' })) onChange(it) },
        label = { Text(label) }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
        singleLine = !multiline, minLines = if (multiline) 3 else 1,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = if (numeric) androidx.compose.ui.text.input.KeyboardType.Number else androidx.compose.ui.text.input.KeyboardType.Text
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White, focusedBorderColor = Color(0xFF2F5539),
            unfocusedBorderColor = Color(0xFFD9E0D5)
        )
    )
}

@Composable
private fun RecruitmentDateField(label: String, value: String, busy: Boolean, onChange: (String) -> Unit) {
    val context = LocalContext.current
    val display = runCatching {
        OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm"))
    }.getOrDefault("날짜와 시간 선택")
    OutlinedButton(onClick = {
        val initial = runCatching { OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime() }
            .getOrDefault(LocalDateTime.now().plusDays(1))
        DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->
                onChange(LocalDateTime.of(year, month + 1, day, hour, minute)
                    .atZone(ZoneId.systemDefault()).toOffsetDateTime().toString())
            }, initial.hour, initial.minute, true).show()
        }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
    }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)) {
        Text("$label: $display", modifier = Modifier.padding(vertical = 8.dp))
    }
}
