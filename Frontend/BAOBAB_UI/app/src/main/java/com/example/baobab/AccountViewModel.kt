package com.example.baobab

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.Executors
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

fun surveyPayload(draft: CompletedSurveyDraft): Map<String, Any?> {
    fun number(value: String, suffix: String): Int = value.trim().removeSuffix(suffix).trim()
        .toIntOrNull()?.takeIf { it >= 0 }
        ?: throw IllegalArgumentException("리워드와 인원은 0 이상의 정수로 입력해주세요.")
    require(draft.basicInfo.title.isNotBlank()) { "설문 제목을 입력해주세요." }
    require(draft.questions.isNotEmpty()) { "질문을 추가해주세요." }
    val questions = draft.questions.mapIndexed { index, question ->
        require(question.title.isNotBlank()) { "${index + 1}번 질문을 입력해주세요." }
        val single = question.type == SurveyQuestionType.MULTIPLE_CHOICE
        val options = if (single) question.options.map { it.trim() } else emptyList()
        require(!single || (options.size >= 2 && options.none { it.isBlank() } && options.distinct().size == options.size)) {
            "${index + 1}번 질문에 서로 다른 선택지를 두 개 이상 입력해주세요."
        }
        mapOf("question" to question.title.trim(), "questionType" to if (single) "single" else "short",
            "options" to options.map { mapOf("optionText" to it) })
    }
    val date = draft.basicInfo.deadline.trim().takeIf { it.isNotEmpty() }?.let {
        val parts = it.split('.').map { part -> part.trim() }.filter { part -> part.isNotEmpty() }
        try {
            require(parts.size == 3)
            LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                .plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toOffsetDateTime().toString()
        } catch (_: Exception) { throw IllegalArgumentException("마감일을 yyyy. MM. dd. 형식으로 입력해주세요.") }
    }
    return mapOf("title" to draft.basicInfo.title.trim(), "category" to draft.basicInfo.category.trim().ifEmpty { null },
        "rewardPoint" to number(draft.settings.rewardPerPerson, "P"),
        "targetCount" to number(draft.settings.rewardRecipients, "명"), "endDate" to date, "questions" to questions)
}

class AccountViewModel(
    private val repository: HttpSurveyRepository = HttpSurveyRepository(BuildConfig.SURVEY_API_BASE_URL),
    private val worker: ExecutorService = Executors.newSingleThreadExecutor(),
    private val uiExecutor: Executor = Executor { Handler(Looper.getMainLooper()).post(it) }
) : ViewModel() {
    private val closed = AtomicBoolean(false)
    var token by mutableStateOf<String?>(null); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var point by mutableStateOf<Int?>(null); private set
    var participationResult by mutableStateOf<ParticipationResult?>(null); private set
    var participationNotice by mutableStateOf<String?>(null); private set
    fun dismissParticipationNotice() { participationNotice = null }
    fun clearError() { error = null }

    private fun <T> runRequest(action: () -> T, success: (T) -> Unit, failure: (String) -> Unit = {}) {
        if (busy || closed.get()) return
        busy = true
        error = null
        worker.submit {
            val result = runCatching(action)
            uiExecutor.execute {
                if (!closed.get()) {
                    busy = false
                    result.fold(success) {
                        if (it is SurveyApiException && it.status == 401) token = null
                        error = it.message ?: "요청에 실패했습니다. 다시 시도해주세요."
                        failure(requireNotNull(error))
                    }
                }
            }
        }
    }

    fun login(id: String, password: String, success: () -> Unit) = runRequest(
        { repository.login(id, password) }, { token = it; point = null; participationResult = null; success() })

    fun signup(name: String, email: String, id: String, password: String, success: () -> Unit) = runRequest({
        repository.signup(name, email, id, password)
    }, { success() })

    fun create(draft: CompletedSurveyDraft, success: () -> Unit) {
        val credential = token ?: run { error = "설문 등록에는 로그인이 필요합니다."; return }
        runRequest({
            surveyPayload(draft)
            repository.createSurvey(draft, credential)
        }, { success() })
    }

    fun participate(survey: SurveyItem, answers: Map<Int, String>, success: () -> Unit) {
        if (busy) return
        val credential = token ?: run { error = "설문 참여에는 로그인이 필요합니다."; return }
        val snapshot = answers.toMap()
        runRequest({ repository.submitAnswers(survey, snapshot, credential) }, {
            point = it.point
            participationResult = it
            success()
        })
    }

    fun checkParticipation(survey: SurveyItem, success: () -> Unit) {
        val credential = token ?: return
        runRequest({ repository.hasParticipated(survey.id, credential) }, {
            if (it) participationNotice = "이미 참여한 설문입니다."
            else success()
        }, { participationNotice = it })
    }

    override fun onCleared() { closed.set(true); worker.shutdownNow() }
}
