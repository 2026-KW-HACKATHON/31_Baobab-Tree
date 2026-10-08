package com.example.baobab

import android.content.Context
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.security.MessageDigest
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val LocalSurveyDraftSave =
    staticCompositionLocalOf<(() -> Unit)?> { null }

val LocalSurveyDraftList =
    staticCompositionLocalOf<(() -> Unit)?> { null }

data class SavedSurveyDraft(
    val id: String,
    val updatedAt: Long,
    val step: Int,
    val draft: CompletedSurveyDraft
)
/**
 * JWT의 userId는 기기 안의 저장본을 구분하는 용도로만 사용합니다.
 * 서버 인증은 기존 로그인 처리에서 수행합니다.
 */
internal fun draftOwner(token: String?): String? {
    if (token == null) return null

    return runCatching {
        val payload = token.split(".")[1]
        val json = String(
            Base64.getUrlDecoder().decode(payload),
            Charsets.UTF_8
        )

        JsonParser.parseString(json)
            .asJsonObject
            .get("userId")
            .asLong
            .takeIf { it > 0 }
            ?.toString()
    }.getOrNull()
}

internal fun draftKey(owner: String): String {
    val server = MessageDigest.getInstance("SHA-256")
        .digest(BuildConfig.SURVEY_API_BASE_URL.toByteArray())
        .joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }

    return "draft_${server}_$owner"
}

private fun SurveyCreationState.savedDraft(): CompletedSurveyDraft {
    return CompletedSurveyDraft(
        basicInfo = SurveyDraftStepOne(
            title,
            category,
            introduction,
            audience,
            deadline
        ),
        questions = questions.map {
            SurveyQuestionSnapshot(
                type = it.type,
                title = it.title,
                required = it.required,
                selectedOptionIndex = it.selectedOptionIndex,
                options = it.options.toList()
            )
        },
        settings = SurveySettingsDraft(
            rewardPerPerson,
            rewardRecipients,
            selectedDuration,
            imageData,
            requiresRegionVerification
        )
    )
}

private fun SurveyCreationState.restoreDraft(
    draft: CompletedSurveyDraft
) {
    title = draft.basicInfo.title
    category = draft.basicInfo.category.takeIf { it.isNotBlank() }?.let { participationPurpose(it) }.orEmpty()
    introduction = draft.basicInfo.introduction
    audience = draft.basicInfo.audience
    deadline = draft.basicInfo.deadline

    rewardPerPerson = draft.settings.rewardPerPerson
    rewardRecipients = draft.settings.rewardRecipients
    selectedDuration = draft.settings.duration
    imageData = draft.settings.imageData
    requiresRegionVerification = draft.settings.requiresRegionVerification

    questions.clear()
    questions.addAll(
        draft.questions.map { saved ->
            SurveyQuestionDraft(
                type = saved.type,
                title = saved.title,
                options = saved.options
            ).apply {
                required = saved.required
                selectedOptionIndex = saved.selectedOptionIndex
            }
        }
    )
}

private class SurveyDraftStore(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            "baobab_survey_drafts",
            Context.MODE_PRIVATE
        )

    private val gson = Gson()
    private val mutex = Mutex()

    private fun listKey(owner: String): String {
        return "${draftKey(owner)}_list"
    }

    private fun validate(draft: CompletedSurveyDraft) {
        checkNotNull(draft.basicInfo)
        checkNotNull(draft.settings)
        checkNotNull(draft.questions)

        with(draft.basicInfo) {
            checkNotNull(title)
            checkNotNull(category)
            checkNotNull(introduction)
            checkNotNull(audience)
            checkNotNull(deadline)
        }

        with(draft.settings) {
            checkNotNull(rewardPerPerson)
            checkNotNull(rewardRecipients)
            checkNotNull(duration)
        }

        draft.questions.forEach {
            checkNotNull(it.type)
            checkNotNull(it.title)
            checkNotNull(it.options)
            it.options.forEach { option ->
                checkNotNull(option)
            }
        }
    }

    // mutex 안에서만 호출합니다.
    private fun read(owner: String): List<SavedSurveyDraft> {
        val json = preferences.getString(listKey(owner), null)

        if (json != null) {
            val saved = checkNotNull(
                gson.fromJson(
                    json,
                    Array<SavedSurveyDraft>::class.java
                )
            ).toList()

            saved.forEach {
                check(it.id.isNotBlank())
                check(it.step in 1..3)
                validate(it.draft)
            }

            check(saved.map { it.id }.distinct().size == saved.size)

            return saved.sortedByDescending { it.updatedAt }
        }

        // 이전에 저장한 설문 하나를 새 목록으로 옮깁니다.
        val oldJson = preferences.getString(draftKey(owner), null)
            ?: return emptyList()

        val oldDraft = checkNotNull(
            gson.fromJson(
                oldJson,
                CompletedSurveyDraft::class.java
            )
        )

        validate(oldDraft)

        val migrated = listOf(
            SavedSurveyDraft(
                id = UUID.randomUUID().toString(),
                updatedAt = System.currentTimeMillis(),
                step = 1,
                draft = oldDraft
            )
        )

        write(owner, migrated)
        return migrated
    }

    private fun write(
        owner: String,
        drafts: List<SavedSurveyDraft>
    ) {
        check(
            preferences.edit()
                .putString(listKey(owner), gson.toJson(drafts))
                .remove(draftKey(owner))
                .commit()
        ) {
            "임시저장 내용을 저장하지 못했습니다."
        }
    }

    suspend fun loadAll(owner: String): List<SavedSurveyDraft> {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                read(owner)
            }
        }
    }

    suspend fun save(
        owner: String,
        id: String,
        step: Int,
        draft: CompletedSurveyDraft
    ) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                validate(draft)

                val saved = SavedSurveyDraft(
                    id = id,
                    updatedAt = System.currentTimeMillis(),
                    step = step,
                    draft = draft
                )

                val updated = read(owner)
                    .filterNot { it.id == id } + saved

                write(
                    owner,
                    updated.sortedByDescending { it.updatedAt }
                )
            }
        }
    }

    suspend fun delete(owner: String, id: String) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val remaining = read(owner).filterNot { it.id == id }
                write(owner, remaining)
            }
        }
    }
}


private fun isCreationScreen(screen: BaobabScreen): Boolean {
    return screen == BaobabScreen.CREATE_ONE ||
            screen == BaobabScreen.CREATE_TWO ||
            screen == BaobabScreen.CREATE_THREE
}

@Composable
fun SurveyDraftSupport(
    model: BaobabViewModel,
    token: String?,
    submitting: Boolean,
    content: @Composable (
        onCreate: () -> Unit,
        onBack: () -> Unit,
        onCreated: () -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val store = remember { SurveyDraftStore(context) }
    val scope = rememberCoroutineScope()

    val owner = remember(token) { draftOwner(token) }
    val currentOwner by rememberUpdatedState(owner)

    var activeId by rememberSaveable(owner) {
        mutableStateOf<String?>(null)
    }

    var lastOwner by rememberSaveable {
        mutableStateOf(owner)
    }

    var working by remember { mutableStateOf(false) }
    var showList by remember(owner) { mutableStateOf(false) }
    var showExitDialog by remember(owner) { mutableStateOf(false) }

    var drafts by remember(owner) {
        mutableStateOf<List<SavedSurveyDraft>>(emptyList())
    }

    var listError by remember(owner) {
        mutableStateOf<String?>(null)
    }

    var deleting by remember(owner) {
        mutableStateOf<SavedSurveyDraft?>(null)
    }

    fun notify(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(owner) {
        if (lastOwner != owner) {
            model.creationState.restoreDraft(
                SurveyCreationState().savedDraft()
            )
            lastOwner = owner
        }
    }

    fun startCreation(saved: SavedSurveyDraft?) {
        activeId = saved?.id

        model.beginCreation()
        model.creationState.restoreDraft(
            saved?.draft ?: SurveyCreationState().savedDraft()
        )

        // 이전 단계로 돌아갈 수 있도록 순서대로 연결합니다.
        if (saved != null && saved.step >= 2) {
            model.navigate(BaobabScreen.CREATE_TWO)
        }

        if (saved != null && saved.step >= 3) {
            model.navigate(BaobabScreen.CREATE_THREE)
        }
    }

    fun openList() {
        val savedOwner = owner

        if (savedOwner == null) {
            notify("로그인 후 임시저장 목록을 볼 수 있습니다.")
            return
        }

        if (working || submitting) return

        showList = true
        listError = null
        drafts = emptyList()
        working = true

        scope.launch {
            try {
                val loaded = store.loadAll(savedOwner)

                if (currentOwner == savedOwner) {
                    drafts = loaded
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (currentOwner == savedOwner) {
                    listError = "임시저장 목록을 불러오지 못했습니다."
                }
            } finally {
                working = false
            }
        }
    }

    fun saveDraft(exitAfterSaving: Boolean) {
        val savedOwner = owner

        if (savedOwner == null) {
            notify("로그인 후 임시저장할 수 있습니다.")
            return
        }

        if (working || submitting) return

        val id = activeId ?: UUID.randomUUID().toString()
        activeId = id

        val step = when (model.currentScreen) {
            BaobabScreen.CREATE_TWO -> 2
            BaobabScreen.CREATE_THREE -> 3
            else -> 1
        }

        val snapshot = model.creationState.savedDraft()
        working = true

        scope.launch {
            try {
                store.save(savedOwner, id, step, snapshot)

                if (currentOwner == savedOwner) {
                    notify("설문을 임시저장했습니다.")

                    if (exitAfterSaving) {
                        showExitDialog = false
                        model.goBack()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (currentOwner == savedOwner) {
                    notify("임시저장에 실패했습니다. 다시 시도해주세요.")
                }
            } finally {
                working = false
            }
        }
    }

    val onCreate: () -> Unit = {
        if (owner == null) {
            notify("로그인 후 설문을 작성해주세요.")
        } else if (!working && !submitting) {
            // 기존 저장본은 남겨두고 새로운 설문을 시작합니다.
            startCreation(null)
        }
    }

    val onBack: () -> Unit = {
        if (!working && !submitting) {
            if (model.currentScreen == BaobabScreen.CREATE_ONE) {
                showExitDialog = true
            } else {
                model.goBack()
            }
        }
    }

    val onCreated: () -> Unit = {
        val savedOwner = owner
        val completedId = activeId
        activeId = null

        // 등록한 설문의 저장본만 삭제합니다.
        if (savedOwner != null && completedId != null) {
            scope.launch {
                try {
                    store.delete(savedOwner, completedId)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    if (currentOwner == savedOwner) {
                        notify(
                            "설문은 등록됐지만 임시저장본 삭제에 실패했습니다."
                        )
                    }
                }
            }
        }
    }

    if (showList) {
        AlertDialog(
            onDismissRequest = {
                if (!working) showList = false
            },
            title = { Text("임시저장 설문") },
            text = {
                when {
                    working -> Text("불러오는 중…")

                    listError != null -> Text(
                        listError ?: "목록 조회 실패"
                    )

                    drafts.isEmpty() -> Text(
                        "임시저장한 설문이 없습니다."
                    )

                    else -> LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(drafts, key = { it.id }) { saved ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement =
                                    Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    saved.draft.basicInfo.title.ifBlank {
                                        "제목 없는 설문"
                                    }
                                )

                                Text(
                                    "${saved.step}단계 작성 중 · " +
                                            SimpleDateFormat(
                                                "yyyy.MM.dd HH:mm",
                                                Locale.KOREA
                                            ).format(Date(saved.updatedAt))
                                )

                                Row {
                                    TextButton(
                                        enabled = !submitting,
                                        onClick = {
                                            showList = false
                                            startCreation(saved)
                                        }
                                    ) {
                                        Text("이어서 작성")
                                    }

                                    TextButton(
                                        enabled = !submitting,
                                        onClick = { deleting = saved }
                                    ) {
                                        Text("삭제")
                                    }
                                }

                                androidx.compose.material3.HorizontalDivider()
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !working,
                    onClick = { showList = false }
                ) {
                    Text("닫기")
                }
            },
            dismissButton = {
                if (listError != null) {
                    TextButton(
                        enabled = !working,
                        onClick = { openList() }
                    ) {
                        Text("다시 불러오기")
                    }
                }
            }
        )
    }

    deleting?.let { saved ->
        AlertDialog(
            onDismissRequest = {
                if (!working) deleting = null
            },
            title = { Text("임시저장 설문 삭제") },
            text = {
                Text(
                    saved.draft.basicInfo.title.ifBlank {
                        "제목 없는 설문"
                    } + "\n이 저장본을 삭제하시겠어요?"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !working,
                    onClick = {
                        val savedOwner = owner

                        if (savedOwner != null && !working) {
                            working = true

                            scope.launch {
                                try {
                                    store.delete(savedOwner, saved.id)

                                    if (currentOwner == savedOwner) {
                                        drafts = drafts.filterNot {
                                            it.id == saved.id
                                        }

                                        if (activeId == saved.id) {
                                            activeId = null
                                        }

                                        deleting = null
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    if (currentOwner == savedOwner) {
                                        notify("삭제하지 못했습니다.")
                                    }
                                } finally {
                                    working = false
                                }
                            }
                        }
                    }
                ) {
                    Text(if (working) "삭제 중…" else "삭제")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !working,
                    onClick = { deleting = null }
                ) {
                    Text("취소")
                }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!working) showExitDialog = false
            },
            title = { Text("작성을 나가시겠어요?") },
            text = {
                Text(
                    "임시저장하면 마이페이지에서 이어서 작성할 수 있습니다."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !working,
                    onClick = { saveDraft(true) }
                ) {
                    Text(
                        if (working) "저장 중…" else "저장하고 나가기"
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !working,
                    onClick = {
                        showExitDialog = false
                        model.goBack()
                    }
                ) {
                    Text("저장하지 않고 나가기")
                }
            }
        )
    }

    val saveAction: (() -> Unit)? =
        if (
            isCreationScreen(model.currentScreen) &&
            !working &&
            !submitting
        ) {
            { saveDraft(false) }
        } else {
            null
        }

    CompositionLocalProvider(
        LocalSurveyDraftSave provides saveAction,
        LocalSurveyDraftList provides { openList() }
    ) {
        content(onCreate, onBack, onCreated)
    }
}
