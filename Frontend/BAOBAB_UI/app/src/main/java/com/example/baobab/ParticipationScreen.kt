package com.example.baobab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel

data class ParticipationResult(val responseId: Int, val rewardPoint: Int, val point: Int)

fun answerPayload(survey: SurveyItem, answers: Map<Int, String>): Map<String, Any> {
    require(survey.id.toIntOrNull()?.let { it > 0 } == true) { "설문 정보를 다시 불러와주세요." }
    require(survey.questions.isNotEmpty()) { "질문이 없는 설문입니다." }
    require(survey.questions.map { it.id }.distinct().size == survey.questions.size) { "설문 정보를 다시 불러와주세요." }
    require(answers.keys.all { id -> survey.questions.any { it.id == id } }) { "설문에 없는 질문입니다." }
    return mapOf("answers" to survey.questions.mapIndexedNotNull { index, question ->
        val answer = answers[question.id]?.trim().orEmpty()
        if (answer.isBlank() && !question.required) return@mapIndexedNotNull null
        require(answer.isNotBlank()) { "${index + 1}번 질문에 답해주세요." }
        require(question.questionType in listOf("single", "short")) { "지원하지 않는 질문 형식입니다." }
        require(question.questionType != "single" || question.options.any { it.optionText == answer }) {
            "${index + 1}번 질문의 선택지를 확인해주세요."
        }
        mapOf("questionId" to question.id, "answer" to answer)
    })
}

class ParticipationViewModel : ViewModel() {
    var survey by mutableStateOf<SurveyItem?>(null); private set
    val answers = mutableStateMapOf<Int, String>()
    fun begin(item: SurveyItem) {
        if (survey?.id != item.id) answers.clear()
        survey = item
        val ids = item.questions.map { it.id }.toSet()
        answers.keys.toList().filter { it !in ids }.forEach { answers.remove(it) }
    }
}

@Composable
fun ParticipationScreen(survey: SurveyItem, answers: Map<Int, String>, busy: Boolean, error: String?,
    onAnswer: (Int, String) -> Unit, onSubmit: () -> Unit, onBack: () -> Unit,
    needsLogin: Boolean, onLogin: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFFFDF9F1)).safeDrawingPadding().imePadding()
        .verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onBack, enabled = !busy) { Text("뒤로") }
        Text(survey.title, style = MaterialTheme.typography.headlineSmall)
        Text("필수 질문에 답해주세요. 선택 질문은 건너뛸 수 있어요.")
        survey.questions.forEachIndexed { index, question ->
            Text("${index + 1}. ${question.question} (${if (question.required) "필수" else "선택"})", style = MaterialTheme.typography.titleMedium)
            if (question.questionType == "single") {
                question.options.forEach { option ->
                    Row(Modifier.fillMaxWidth().clickable(enabled = !busy) { onAnswer(question.id, option.optionText) }) {
                        RadioButton(selected = answers[question.id] == option.optionText,
                            onClick = { onAnswer(question.id, option.optionText) }, enabled = !busy)
                        Text(option.optionText, Modifier.padding(top = 12.dp))
                    }
                }
                if (!question.required && !answers[question.id].isNullOrBlank()) {
                    TextButton(onClick = { onAnswer(question.id, "") }, enabled = !busy) { Text("선택 해제") }
                }
            } else {
                OutlinedTextField(value = answers[question.id].orEmpty(), onValueChange = { onAnswer(question.id, it) },
                    label = { Text("답변") }, enabled = !busy, modifier = Modifier.fillMaxWidth())
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = if (needsLogin) onLogin else onSubmit, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (busy) "제출 중…" else if (needsLogin) "로그인 후 참여하기" else "답변 제출")
        }
    }
}
