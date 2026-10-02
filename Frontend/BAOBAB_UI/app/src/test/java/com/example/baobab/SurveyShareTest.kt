package com.example.baobab

import org.junit.Assert.*
import org.junit.Test

class SurveyShareTest {
    @Test
    fun sharingLinkOpensTheSameSurveyId() {
        assertEquals("baobab://surveys/42", surveyShareLink("42"))
        assertEquals("42", sharedSurveyId(surveyShareLink("42")))
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
