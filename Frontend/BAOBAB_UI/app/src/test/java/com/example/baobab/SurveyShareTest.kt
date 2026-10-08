package com.example.baobab

import org.junit.Assert.*
import org.junit.Test

class SurveyShareTest {
    @Test
    fun sharingLinkOpensTheSameSurveyId() {
        assertEquals("baobab://surveys/42", surveyShareLink("42"))
        assertEquals("42", sharedSurveyId(surveyShareLink("42")))
    }

    @Test fun referralSurvivesLinkParsingAndLoginNavigationButDoesNotLeakToAnotherSurvey() {
        val token = "header.payload.signature"
        val shared = sharedSurvey(surveyShareLink("42", token))!!
        assertEquals(SharedSurvey("42", token), shared)
        val state = androidx.lifecycle.SavedStateHandle()
        val model = BaobabViewModel(state)
        model.openSurvey(SurveyItem(id = shared.id), shared.referralToken)
        model.navigate(BaobabScreen.LOGIN)
        model.navigate(BaobabScreen.PARTICIPATE)
        assertEquals(token, model.referralFor("42"))
        assertNull(model.referralFor("43"))
        assertEquals(token, BaobabViewModel(state).referralFor("42"))
        model.openSurvey(SurveyItem(id = "43"))
        assertNull(model.referralFor("42"))
        model.openSurvey(SurveyItem(id = "42"), token)
        model.clearReferral("42")
        assertNull(model.referralFor("42"))
    }

    @Test fun webLinkPreservesTokenAndRejectsMalformedReferralQuery() {
        assertEquals("https://example.com/api/surveys/42/share?referral=a.b.c",
            surveyWebShareLink("42", "a.b.c", "https://example.com/api/"))
        for (query in listOf("referral=", "referral=bad", "referral=a.b.c&referral=x.y.z", "referral=a.b.c&other=1"))
            assertNull(sharedSurvey("baobab://surveys/42?$query"))
        assertNull(sharedSurvey("baobab://someone@surveys/42?referral=a.b.c"))
        assertThrows(IllegalArgumentException::class.java) { surveyShareLink("42", "bad&injected=1") }
    }

    @Test
    fun ignoresMalformedAndUnrelatedLinks() {
        for (link in listOf(null, "", "https://example.com/surveys/42", "baobab://other/42",
            "baobab://surveys/-1", "baobab://surveys/0", "baobab://surveys/42/extra",
            "baobab://surveys/42?other=1", "baobab://surveys/2147483648")) {
            assertNull(sharedSurveyId(link))
        }
        assertThrows(IllegalArgumentException::class.java) { surveyShareLink("invalid") }
    }
}
