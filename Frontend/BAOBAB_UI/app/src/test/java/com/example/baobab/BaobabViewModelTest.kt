package com.example.baobab

import org.junit.Assert.*
import org.junit.Test

class BaobabViewModelTest {
    @Test
    fun selectedSurveyKeepsAllCardDataAndReturnsToItsOrigin() {
        val model = BaobabViewModel()
        model.goHome()
        val homeSurvey = SampleSurveys.first()
        model.openSurvey(homeSurvey)
        assertSame(homeSurvey, model.selectedSurvey)
        assertEquals(BaobabScreen.DETAIL, model.currentScreen)
        model.goBack()
        assertEquals(BaobabScreen.HOME, model.currentScreen)

        model.openSearch("plastic")
        val searchSurvey = SampleSurveys.last()
        model.openSurvey(searchSurvey)
        assertSame(searchSurvey, model.selectedSurvey)
        model.goBack()
        assertEquals(BaobabScreen.SEARCH, model.currentScreen)
    }

    @Test
    fun searchStateSurvivesDetailAndRelatedSurveyNavigation() {
        val model = BaobabViewModel()
        model.goHome()
        model.homeSearchTerm = "initial query"
        model.openSearch(model.homeSearchTerm)
        assertEquals("initial query", model.searchTerm)
        model.searchTerm = "edited query"
        model.searchCategory = SampleSurveys.first().category
        model.openSurvey(SampleSurveys.first())
        model.openSurvey(SampleSurveys[1])
        assertSame(SampleSurveys[1], model.selectedSurvey)
        model.goBack()
        assertEquals(BaobabScreen.SEARCH, model.currentScreen)
        assertEquals("edited query", model.searchTerm)
        assertEquals(SampleSurveys.first().category, model.searchCategory)
        model.goBack()
        assertEquals("initial query", model.homeSearchTerm)
    }

    @Test
    fun draftSurvivesBackNavigationAndReentry() {
        val model = BaobabViewModel()
        model.goHome()
        model.beginCreation()
        model.creationState.title = "My survey"
        model.navigate(BaobabScreen.CREATE_TWO)
        model.creationState.questions.clear()
        model.creationState.questions.add(SurveyQuestionDraft(SurveyQuestionType.SHORT_ANSWER, "Question"))
        model.navigate(BaobabScreen.CREATE_THREE)
        model.creationState.rewardPerPerson = "300P"

        model.goBack()
        assertEquals(BaobabScreen.CREATE_TWO, model.currentScreen)
        assertEquals("Question", model.creationState.questions.single().title)
        model.goBack()
        assertEquals(BaobabScreen.CREATE_ONE, model.currentScreen)
        assertEquals("My survey", model.creationState.title)
        model.goBack()
        model.beginCreation()
        assertEquals("My survey", model.creationState.title)
        assertEquals("300P", model.creationState.rewardPerPerson)
    }

    @Test
    fun backReturnsToCreationEntryScreen() {
        val model = BaobabViewModel()
        model.goHome()
        model.navigate(BaobabScreen.SEARCH)
        model.beginCreation()
        model.goBack()
        assertEquals(BaobabScreen.SEARCH, model.currentScreen)
        model.goBack()
        assertEquals(BaobabScreen.HOME, model.currentScreen)
        assertFalse(model.canGoBack)
        model.goBack()
        assertEquals(BaobabScreen.HOME, model.currentScreen)
    }

    @Test
    fun completionCapturesIndependentDraftAndNextCreationStartsFresh() {
        val model = BaobabViewModel()
        model.goHome()
        model.beginCreation()
        model.creationState.title = "Completed survey"
        val question = model.creationState.questions.first()
        question.required = false
        val settings = SurveySettingsDraft("300P", "20", "10 minutes")
        model.completeCreation(settings)
        val completed = requireNotNull(model.completedDraft)
        question.options.clear()
        question.title = "Changed"
        assertEquals("Completed survey", completed.basicInfo.title)
        assertFalse(completed.questions.first().required)
        assertEquals(2, completed.questions.first().options.size)
        assertNotEquals("Changed", completed.questions.first().title)
        assertEquals(settings, completed.settings)

        model.goBack()
        assertEquals(BaobabScreen.HOME, model.currentScreen)
        assertFalse(model.canGoBack)
        model.beginCreation()
        assertEquals("", model.creationState.title)
        assertEquals(2, model.creationState.questions.size)
        assertEquals("100P", model.creationState.rewardPerPerson)
        assertSame(completed, model.completedDraft)
    }
}
