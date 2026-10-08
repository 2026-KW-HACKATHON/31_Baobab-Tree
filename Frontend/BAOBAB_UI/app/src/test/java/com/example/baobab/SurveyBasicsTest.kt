package com.example.baobab

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class SurveyBasicsTest {
    private val today = LocalDate.of(2026, 10, 3)
    private val valid = SurveyDraftStepOne("주제", "학술·연구", "소개", "주민", "2026. 10. 03.")

    @Test fun requiresEachFieldAndExistingCategory() {
        assertTrue(isSurveyBasicsComplete(valid, today))
        for (draft in listOf(valid.copy(title = " "), valid.copy(category = ""),
            valid.copy(category = "없는 카테고리"), valid.copy(category = "생활·편의"), valid.copy(introduction = " "),
            valid.copy(audience = ""), valid.copy(deadline = ""))) {
            assertFalse(isSurveyBasicsComplete(draft, today))
        }
    }

    @Test fun rejectsPastAndImpossibleDatesButAcceptsFutureDeadline() {
        assertFalse(isSurveyBasicsComplete(valid.copy(deadline = "2026. 10. 02."), today))
        assertFalse(isSurveyBasicsComplete(valid.copy(deadline = "2026. 02. 30."), today))
        assertTrue(isSurveyBasicsComplete(valid.copy(deadline = "2026. 12. 31."), today))
    }
}
