package com.example.baobab

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

data class SurveyLoadState<T>(val data: T? = null, val loading: Boolean = false, val error: String? = null)

class SurveyDataViewModel(
    private val repository: SurveyRepository = HttpSurveyRepository(BuildConfig.SURVEY_API_BASE_URL),
    private val worker: ExecutorService = Executors.newFixedThreadPool(2),
    private val uiExecutor: Executor = Executor { Handler(Looper.getMainLooper()).post(it) }
) : ViewModel() {
    var listState by mutableStateOf(SurveyLoadState<List<SurveyItem>>())
        private set
    var detailState by mutableStateOf(SurveyLoadState<SurveyItem>())
        private set
    var recruitmentState by mutableStateOf(SurveyLoadState<List<RecruitmentItem>>())
        private set
    private var recruitmentInitialized = false
    private var recruitmentVersion = 0
    private var recruitmentTask: Future<*>? = null

    val feedItems: List<SurveyItem>
        get() = listState.data.orEmpty() + recruitmentState.data.orEmpty().map { it.feedItem() }

    fun loadRecruitments(force: Boolean = false) {
        if (closed.get() || (recruitmentInitialized && !force)) return
        recruitmentInitialized = true
        val version = ++recruitmentVersion
        recruitmentTask?.cancel(true)
        val previous = recruitmentState.data
        recruitmentState = SurveyLoadState(data = previous, loading = true)
        recruitmentTask = worker.submit {
            val result = runCatching { HttpSurveyRepository(BuildConfig.SURVEY_API_BASE_URL).getRecruitments() }
            uiExecutor.execute {
                if (!closed.get() && version == recruitmentVersion) {
                    recruitmentState = result.fold(
                        onSuccess = { SurveyLoadState(data = it) },
                        onFailure = { SurveyLoadState(data = previous, error = it.message ?: "모집글을 불러오지 못했습니다.") }
                    )
                }
            }
        }
    }
    var detailId by mutableStateOf<String?>(null)
        private set
    private var listInitialized = false
    private var listVersion = 0
    private var detailVersion = 0
    private var listTask: Future<*>? = null
    private var detailTask: Future<*>? = null
    private val closed = AtomicBoolean(false)

    fun loadSurveys(force: Boolean = false) {
        if (closed.get() || (listInitialized && !force)) return
        listInitialized = true
        val version = ++listVersion
        listTask?.cancel(true)
        val previous = listState.data
        listState = SurveyLoadState(data = previous, loading = true)
        listTask = worker.submit {
            val result = runCatching { repository.getSurveys() }
            uiExecutor.execute {
                if (!closed.get() && version == listVersion) {
                    listState = result.fold(
                        onSuccess = { SurveyLoadState(data = it) },
                        onFailure = { SurveyLoadState(data = previous, error = message(it)) }
                    )
                }
            }
        }
    }

    fun loadDetail(id: String) {
        val previous = detailState.data?.takeIf { detailId == id }
        if (closed.get()) return
        detailId = id
        val version = ++detailVersion
        detailTask?.cancel(true)
        detailState = SurveyLoadState(loading = true)
        detailTask = worker.submit {
            val result = runCatching { repository.getSurvey(id) }
            uiExecutor.execute {
                if (!closed.get() && version == detailVersion) {
                    detailState = result.fold(
                        onSuccess = { SurveyLoadState(data = it) },
                        onFailure = { SurveyLoadState(data = previous, error = message(it)) }
                    )
                }
            }
        }
    }

    private fun message(error: Throwable) =
        if (error is SurveyApiException) error.message ?: "설문을 불러오지 못했습니다."
        else "설문을 불러오지 못했습니다. 다시 시도해주세요."

    override fun onCleared() {
        closed.set(true)
        worker.shutdownNow()
    }
}
