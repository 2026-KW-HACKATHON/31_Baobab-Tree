package com.example.baobab

import android.content.SharedPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class RecruitmentDraftStoreTest {
    @Test fun incompleteDraftSurvivesNewStoreAndStaysSeparateFromOtherAccounts() = runBlocking {
        val preferences = preferences()
        val draft = SavedRecruitmentDraft("a", 123L, RecruitmentDraft(title = "주민 인터뷰", step = 2))
        RecruitmentDraftStore(preferences).save("1", draft)
        val reopened = RecruitmentDraftStore(preferences)
        assertEquals(listOf(draft), reopened.loadAll("1"))
        assertTrue(reopened.loadAll("2").isEmpty())
    }

    @Test fun updatingAndDeletingOneDraftPreservesOtherSavedWork() = runBlocking {
        val store = RecruitmentDraftStore(preferences())
        val first = SavedRecruitmentDraft("a", 100L, RecruitmentDraft(title = "첫 모집"))
        val second = SavedRecruitmentDraft("b", 200L, RecruitmentDraft(title = "다른 모집"))
        store.save("1", first)
        store.save("1", second)
        val updated = first.copy(updatedAt = 300L, draft = first.draft.copy(purpose = "지역·정책", step = 3))
        assertEquals(listOf(updated, second), store.save("1", updated))
        assertEquals(listOf(second), store.delete("1", "a"))
        assertEquals(listOf(second), store.loadAll("1"))
    }

    private fun preferences(): SharedPreferences {
        val values = mutableMapOf<String, String>()
        val pending = mutableMapOf<String, String>()
        val editor = Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
            when (method.name) {
                "putString" -> { pending[args!![0] as String] = args[1] as String; proxy }
                "commit" -> { values.putAll(pending); pending.clear(); true }
                else -> throw UnsupportedOperationException(method.name)
            }
        } as SharedPreferences.Editor
        return Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getString" -> values[args!![0] as String] ?: args[1]
                "edit" -> editor
                else -> throw UnsupportedOperationException(method.name)
            }
        } as SharedPreferences
    }
}
