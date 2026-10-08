package com.example.baobab

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class RecruitmentDraft(
    val title: String = "", val organization: String = "", val purpose: String = "",
    val activityType: String = "EXPERIMENT", val participationMode: String = "OFFLINE",
    val description: String = "", val eligibility: String = "", val location: String = "",
    val duration: String = "60", val count: String = "1", val reward: String = "1000",
    val deadline: String = "", val startsAt: String = "", val endsAt: String = "", val step: Int = 1
)

internal data class SavedRecruitmentDraft(val id: String, val updatedAt: Long, val draft: RecruitmentDraft)

internal class RecruitmentDraftStore(private val preferences: android.content.SharedPreferences) {
    constructor(context: Context) : this(context.applicationContext.getSharedPreferences("baobab_recruitment_drafts", Context.MODE_PRIVATE))
    private val gson = Gson()
    private val type = object : TypeToken<List<SavedRecruitmentDraft>>() {}.type
    private fun key(owner: String) = "${draftKey(owner)}_list"
    private fun read(owner: String): List<SavedRecruitmentDraft> {
        val json = preferences.getString(key(owner), null) ?: return emptyList()
        return requireNotNull(gson.fromJson<List<SavedRecruitmentDraft>>(json, type))
    }
    private fun write(owner: String, drafts: List<SavedRecruitmentDraft>) {
        check(preferences.edit().putString(key(owner), gson.toJson(drafts)).commit()) { "임시저장에 실패했습니다." }
    }
    suspend fun loadAll(owner: String): List<SavedRecruitmentDraft> = withContext(Dispatchers.IO) {
        mutex.withLock { read(owner) }
    }
    suspend fun save(owner: String, draft: SavedRecruitmentDraft): List<SavedRecruitmentDraft> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val drafts = (read(owner).filterNot { it.id == draft.id } + draft).sortedByDescending { it.updatedAt }
            write(owner, drafts)
            drafts
        }
    }
    suspend fun delete(owner: String, id: String): List<SavedRecruitmentDraft> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val drafts = read(owner).filterNot { it.id == id }
            write(owner, drafts)
            drafts
        }
    }
    private companion object { val mutex = Mutex() }
}
