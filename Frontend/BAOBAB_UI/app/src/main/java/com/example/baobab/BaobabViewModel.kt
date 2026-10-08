package com.example.baobab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle

enum class BaobabScreen {
    LOGIN, START, HOME, SEARCH, DETAIL, PARTICIPATE, SURVEY_COMPLETE, RECRUITMENTS,
    CREATE_ONE, CREATE_TWO, CREATE_THREE, CREATE_COMPLETE, MY, POINTS, POINT_HISTORY, EDIT
}

class SurveyCreationState {
    var title by mutableStateOf("")
    var category by mutableStateOf("")
    var introduction by mutableStateOf("")
    var audience by mutableStateOf("")
    var deadline by mutableStateOf("")
    val questions = mutableStateListOf(
        SurveyQuestionDraft(
            type = SurveyQuestionType.MULTIPLE_CHOICE,
            options = listOf("정답 옵션 1", "정답 옵션 2")
        ),
        SurveyQuestionDraft(type = SurveyQuestionType.SHORT_ANSWER)
    )
    var rewardPerPerson by mutableStateOf("100P")
    var rewardRecipients by mutableStateOf("10명")
    var selectedDuration by mutableStateOf("5분 이하")
    var imageData by mutableStateOf<String?>(null)
}

data class SurveyQuestionSnapshot(
    val type: SurveyQuestionType,
    val title: String,
    val required: Boolean,
    val selectedOptionIndex: Int,
    val options: List<String>
)

data class CompletedSurveyDraft(
    val basicInfo: SurveyDraftStepOne,
    val questions: List<SurveyQuestionSnapshot>,
    val settings: SurveySettingsDraft
)

class BaobabViewModel(private val savedState: SavedStateHandle = SavedStateHandle()) : ViewModel() {
    private val backStack = mutableStateListOf(BaobabScreen.LOGIN)
    val currentScreen: BaobabScreen get() = backStack.last()
    val canGoBack: Boolean get() = backStack.size > 1

    var editingSurvey by mutableStateOf<SurveyItem?>(null); private set
    var editState by mutableStateOf(SurveyCreationState()); private set
    fun beginEditing(survey: SurveyItem) {
        editingSurvey = survey
        editState = surveyEditState(survey)
        navigate(BaobabScreen.EDIT)
    }

    var creationState by mutableStateOf(SurveyCreationState())
        private set
    var completedDraft by mutableStateOf<CompletedSurveyDraft?>(null)
        private set
    private var startNewDraft = false
    var resumeCreationAfterLogin by mutableStateOf(false)
    var resumeParticipationAfterLogin by mutableStateOf(false)

    var selectedSurvey by mutableStateOf<SurveyItem?>(null)
        private set
    var homeSearchTerm by mutableStateOf("")
    var searchTerm by mutableStateOf("")
    var searchCategory by mutableStateOf("전체")
    var resumeRecruitmentAfterLogin by mutableStateOf(false)
    var recruitmentEntry by mutableStateOf("APPLICATIONS")
        private set
    var selectedRecruitmentId by mutableStateOf(0)
        private set

    fun openRecruitment(id: Int = 0, page: String = "DETAIL") {
        selectedRecruitmentId = id
        recruitmentEntry = page
        navigate(BaobabScreen.RECRUITMENTS)
    }

    fun referralFor(surveyId: String): String? =
        if (savedState.get<String>("referralSurveyId") == surveyId) savedState["referralToken"] else null

    fun clearReferral(surveyId: String) {
        if (savedState.get<String>("referralSurveyId") == surveyId) {
            savedState.remove<String>("referralSurveyId")
            savedState.remove<String>("referralToken")
        }
    }

    fun openSurvey(survey: SurveyItem, referralToken: String? = null) {
        savedState["referralSurveyId"] = survey.id
        savedState["referralToken"] = referralToken
        selectedSurvey = survey
        navigate(BaobabScreen.DETAIL)
    }

    fun openSearch(query: String) {
        searchTerm = query
        searchCategory = "전체"
        navigate(BaobabScreen.SEARCH)
    }

    fun navigate(screen: BaobabScreen) {
        if (currentScreen != screen) backStack.add(screen)
    }

    fun goBack() {
        when (currentScreen) {
            BaobabScreen.CREATE_COMPLETE, BaobabScreen.SURVEY_COMPLETE -> goHome()
            else -> if (canGoBack) backStack.removeAt(backStack.lastIndex)
        }
    }

    fun goHome() {
        backStack.clear()
        backStack.add(BaobabScreen.HOME)
    }

    fun beginCreation() {
        if (startNewDraft) {
            creationState = SurveyCreationState()
            startNewDraft = false
        }
        navigate(BaobabScreen.CREATE_ONE)
    }

    fun snapshotDraft(settings: SurveySettingsDraft): CompletedSurveyDraft = CompletedSurveyDraft(
            basicInfo = with(creationState) {
                SurveyDraftStepOne(title, category, introduction, audience, deadline)
            },
            questions = creationState.questions.map {
                SurveyQuestionSnapshot(
                    it.type, it.title, it.required, it.selectedOptionIndex, it.options.toList()
                )
            },
            settings = settings.copy(imageData = creationState.imageData)
        )
    fun completeCreation(settings: SurveySettingsDraft) = completeCreation(snapshotDraft(settings))

    fun completeCreation(draft: CompletedSurveyDraft) {
        completedDraft = draft
        startNewDraft = true
        navigate(BaobabScreen.CREATE_COMPLETE)
    }
}
