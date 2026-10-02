package com.example.baobab

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class UserProfile(val id: Int, val name: String, val email: String, val loginId: String, val point: Int,
    val ageGroup: String? = null, val region: String? = null)
data class AnswerResult(val option: String, val count: Int, val percentage: Int)
data class QuestionResults(val questionId: Int, val results: List<AnswerResult>, val responseCount: Int? = null)
data class SurveyResults(val surveyId: Int, val totalResponses: Int, val questions: List<QuestionResults>)
data class AnswerHistory(val question: String, val answer: String)
data class ParticipationHistory(val id: Int, val surveyId: String, val title: String, val category: String,
    val rewardPoint: Int, val createdAt: String, val answers: List<AnswerHistory>)

class MyPageViewModel(
    private val repository: HttpSurveyRepository = HttpSurveyRepository(BuildConfig.SURVEY_API_BASE_URL),
    private val worker: ExecutorService = Executors.newSingleThreadExecutor(),
    private val ui: Executor = Executor { Handler(Looper.getMainLooper()).post(it) }
) : ViewModel() {
    var profile by mutableStateOf<UserProfile?>(null); private set
    var surveys by mutableStateOf<List<SurveyItem>>(emptyList()); private set
    var participationCount by mutableStateOf(0); private set
    var participations by mutableStateOf<List<ParticipationHistory>>(emptyList()); private set
    var sessionExpired by mutableStateOf(false); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var selectedSurvey by mutableStateOf<SurveyItem?>(null); private set
    var results by mutableStateOf<SurveyResults?>(null); private set
    var resultsLoading by mutableStateOf(false); private set
    var resultsError by mutableStateOf<String?>(null); private set
    var savingProfile by mutableStateOf(false); private set
    var profileError by mutableStateOf<String?>(null); private set
    fun clearProfileError() { profileError = null }
    private var generation = 0
    private var resultGeneration = 0
    private var closed = false

    fun load(token: String?) {
        val request = ++generation
        closeResults()
        profile = null; surveys = emptyList(); participationCount = 0; error = null
        participations = emptyList(); sessionExpired = false
        savingProfile = false; profileError = null
        loading = token != null
        if (token == null) return
        worker.submit {
            val response = runCatching {
                val user = repository.getProfile(token)
                Triple(user, repository.getSurveys().filter { it.userId == user.id }, repository.getParticipations(token))
            }
            ui.execute {
                if (!closed && request == generation) {
                    loading = false
                    response.fold({ (user, owned, history) ->
                        profile = user; surveys = owned; participationCount = history.size; participations = history
                    }, { sessionExpired = it is SurveyApiException && it.status == 401
                        error = it.message ?: "내 정보를 불러오지 못했습니다." })
                }
            }
        }
    }

    fun openResults(survey: SurveyItem, token: String) {
        val request = ++resultGeneration
        selectedSurvey = survey; results = null; resultsError = null; resultsLoading = true
        worker.submit {
            val response = runCatching { repository.getResults(survey.id, token) }
            ui.execute {
                if (!closed && request == resultGeneration) {
                    resultsLoading = false
                    response.fold({ results = it }, {
                        sessionExpired = it is SurveyApiException && it.status == 401
                        resultsError = it.message ?: "통계를 불러오지 못했습니다." })
                }
            }
        }
    }

    fun closeResults() {
        resultGeneration++
        selectedSurvey = null; results = null; resultsError = null; resultsLoading = false
    }

    fun saveProfile(token: String, name: String, email: String, region: String, ageGroup: String,
        success: () -> Unit) {
        if (savingProfile || closed) return
        val request = generation
        savingProfile = true; profileError = null
        worker.submit {
            val response = runCatching { repository.updateProfile(token, name, email, region, ageGroup) }
            ui.execute {
                if (!closed && request == generation) {
                    savingProfile = false
                    response.fold({ user ->
                        profile = user
                        surveys = surveys.map { it.copy(author = user.name) }
                        success()
                    }, { sessionExpired = it is SurveyApiException && it.status == 401
                        profileError = it.message ?: "계정 정보를 저장하지 못했습니다." })
                }
            }
        }
    }

    override fun onCleared() { closed = true; generation++; resultGeneration++; worker.shutdownNow() }
}
