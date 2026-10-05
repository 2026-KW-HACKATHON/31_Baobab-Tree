package com.example.baobab

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun surveyEditState(survey: SurveyItem) = SurveyCreationState().apply {
    title = survey.title; category = survey.category; introduction = survey.description.orEmpty()
    audience = survey.audience.orEmpty(); deadline = survey.deadline.orEmpty()
    rewardPerPerson = survey.points.ifBlank { "0P" }; rewardRecipients = "${survey.targetCount ?: 0}명"
    selectedDuration = survey.duration.orEmpty(); imageData = survey.imageData
    questions.clear()
    questions.addAll(survey.questions.map { q -> SurveyQuestionDraft(
        if (q.questionType == "single") SurveyQuestionType.MULTIPLE_CHOICE else SurveyQuestionType.SHORT_ANSWER,
        q.question, q.options.map { it.optionText }).apply { required = q.required } })
}

@Composable
fun SurveyEditScreen(survey: SurveyItem, state: SurveyCreationState, busy: Boolean, error: String?,
    onBack: () -> Unit, onSave: (CompletedSurveyDraft, String) -> Unit) {
    var step by rememberSaveable(survey.id) { mutableIntStateOf(1) }
    var status by rememberSaveable(survey.id) { mutableStateOf(survey.status) }
    val canEditQuestions = (survey.participantCount ?: 0) == 0
    val back = { if (!busy) { if (step == 1) onBack() else step = if (step == 3 && !canEditQuestions) 1 else step - 1 } }
    BackHandler { back() }
    when (step) {
        1 -> SurveyCreationStepOneScreen(onBackClick = back,
            onNextClick = { step = if (canEditQuestions) 2 else 3 }, state = state, editing = true)
        2 -> SurveyCreationStepTwoScreen(onBackClick = back, onNextClick = { step = 3 }, state = state, editing = true)
        else -> {
            val context = LocalContext.current
            var selectedImage by remember { mutableStateOf<Uri?>(null) }
            var imageLoading by remember { mutableStateOf(false) }
            var imageError by remember { mutableStateOf<String?>(null) }
            val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) { imageLoading = true; selectedImage = uri }
            }
            LaunchedEffect(selectedImage) {
                val uri = selectedImage ?: return@LaunchedEffect
                withContext(Dispatchers.IO) { runCatching { readSurveyImage(context, uri) } }
                    .fold({ state.imageData = it; imageError = null }, { imageError = it.message })
                imageLoading = false; selectedImage = null
            }
            Column(Modifier.fillMaxSize().background(Color(0xFFFDF9F1)).safeDrawingPadding().imePadding()) {
                SurveyFormHeader(back, "설문 수정하기")
                SurveyFormProgress(3)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(state.title, style = MaterialTheme.typography.titleLarge)
                    Text("${state.questions.size}개 문항 · ${survey.participantCount ?: 0}명 참여")
                    if (!canEditQuestions) Text("이미 응답이 있어 문항은 유지됩니다. 기본 정보와 설문 설정을 수정할 수 있어요.")
                    Text("등록한 리워드 ${state.rewardPerPerson} · 지급 인원 ${state.rewardRecipients}")
                    Text("리워드와 지급 인원은 등록 시 확정됩니다.", style = MaterialTheme.typography.bodySmall)
                    ChoiceField("설문 상태", if (status == "OPEN") "진행 중" else "마감", listOf("진행 중", "마감"),
                        { status = if (it == "진행 중") "OPEN" else "CLOSED" }, enabled = !busy)
                    ChoiceField("예상 소요시간", state.selectedDuration, listOf("5분 이하", "10분 이하", "15분 이하", "20분 이상"),
                        { state.selectedDuration = it }, enabled = !busy)
                    state.imageData?.let {
                        SurveyImage(SurveyItem(imageData = it), Modifier.fillMaxWidth().height(160.dp))
                        TextButton({ state.imageData = null }, enabled = !busy && !imageLoading) { Text("사진 제거") }
                    }
                    OutlinedButton({ picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !busy && !imageLoading) { Text(if (imageLoading) "사진 준비 중…" else "사진 변경") }
                    (imageError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                Button(onClick = {
                    val draft = CompletedSurveyDraft(
                        SurveyDraftStepOne(state.title, state.category, state.introduction, state.audience, state.deadline),
                        state.questions.map { SurveyQuestionSnapshot(it.type, it.title, it.required, it.selectedOptionIndex, it.options.toList()) },
                        SurveySettingsDraft(state.rewardPerPerson, state.rewardRecipients, state.selectedDuration, state.imageData))
                    onSave(draft, status)
                }, enabled = !busy && !imageLoading, modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp)) {
                    Text(if (busy) "저장 중…" else "설문 수정 완료")
                }
            }
        }
    }
}
