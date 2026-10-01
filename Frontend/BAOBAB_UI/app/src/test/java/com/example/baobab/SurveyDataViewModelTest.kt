package com.example.baobab

import org.junit.Assert.*
import org.junit.Test
import java.util.ArrayDeque
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class SurveyDataViewModelTest {
    private class Tasks : AbstractExecutorService() {
        private val tasks = ArrayDeque<Runnable>()
        private var closed = false
        override fun execute(command: Runnable) { tasks.add(command) }
        fun runNext() { tasks.removeFirst().run() }
        override fun shutdown() { closed = true }
        override fun shutdownNow(): MutableList<Runnable> { closed = true; return mutableListOf() }
        override fun isShutdown() = closed
        override fun isTerminated() = closed
        override fun awaitTermination(timeout: Long, unit: TimeUnit) = closed
    }
    private class UiTasks : Executor {
        private val tasks = ArrayDeque<Runnable>()
        override fun execute(command: Runnable) { tasks.add(command) }
        fun runNext() { tasks.removeFirst().run() }
    }

    @Test
    fun failureCanBeRetriedAndSuccessfulListIsReusedAcrossScreens() {
        val worker = Tasks()
        val ui = UiTasks()
        var calls = 0
        val item = SurveyItem(id = "1", title = "Server survey")
        val repository = object : SurveyRepository {
            override fun getSurveys(): List<SurveyItem> {
                calls++
                if (calls == 1) throw SurveyApiException("Offline")
                return listOf(item)
            }
            override fun getSurvey(id: String) = item
        }
        val model = SurveyDataViewModel(repository, worker, ui)
        model.loadSurveys()
        assertTrue(model.listState.loading)
        worker.runNext()
        assertTrue(model.listState.loading)
        ui.runNext()
        assertEquals("Offline", model.listState.error)
        model.loadSurveys(force = true)
        worker.runNext()
        ui.runNext()
        assertEquals(listOf(item), model.listState.data)
        assertFalse(model.listState.loading)
        assertNull(model.listState.error)
        model.loadSurveys()
        assertEquals(2, calls)
    }

    @Test
    fun delayedDetailResultCannotOverwriteNewlySelectedSurvey() {
        val worker = Tasks()
        val ui = UiTasks()
        val repository = object : SurveyRepository {
            override fun getSurveys() = emptyList<SurveyItem>()
            override fun getSurvey(id: String) = SurveyItem(id = id, title = "Survey $id")
        }
        val model = SurveyDataViewModel(repository, worker, ui)
        model.loadDetail("1")
        worker.runNext() // First network result has completed but UI callback is delayed.
        model.loadDetail("2")
        worker.runNext()
        ui.runNext()
        assertTrue(model.detailState.loading)
        assertNull(model.detailState.data)
        ui.runNext()
        assertEquals("2", model.detailId)
        assertEquals("2", model.detailState.data?.id)
        assertFalse(model.detailState.loading)
    }
}
