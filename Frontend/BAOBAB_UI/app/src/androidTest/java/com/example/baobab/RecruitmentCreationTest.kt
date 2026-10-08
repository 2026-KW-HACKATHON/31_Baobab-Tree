package com.example.baobab

import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.baobab.ui.theme.BAOBABTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Base64

class RecruitmentCreationTest {
    @get:Rule val compose = createComposeRule()
    private val owner = "987654321"
    private val token = "header." + Base64.getUrlEncoder().withoutPadding()
        .encodeToString("{\"userId\":987654321}".toByteArray()) + ".signature"
    private val preferences get() = InstrumentationRegistry.getInstrumentation().targetContext
        .getSharedPreferences("baobab_recruitment_drafts", android.content.Context.MODE_PRIVATE)
    private val storageKey get() = "${draftKey(owner)}_list"

    @Before fun before() { preferences.edit().remove(storageKey).commit() }
    @After fun after() { preferences.edit().remove(storageKey).commit() }

    @Test fun purposeOptionsExpandVerticallyAndHeaderOffersSave() {
        compose.setContent { BAOBABTheme { RecruitmentCreateScreen(token, {}, {}) } }
        waitReady()
        compose.onNodeWithText("임시 저장").assertExists()
        compose.onNodeWithText("모집 목적을 선택해주세요").performClick()
        val bounds = ParticipationPurposes.map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot }
        bounds.zipWithNext().forEach { (first, second) ->
            assertTrue("Options must appear below each other", second.top >= first.bottom)
            assertEquals(first.left, second.left, 1f)
        }
    }

    @Test fun incompleteSavedDraftCanBeResumedAfterScreenIsRecreated() {
        val generation = mutableIntStateOf(0)
        compose.setContent {
            BAOBABTheme { key(generation.intValue) { RecruitmentCreateScreen(token, {}, {}) } }
        }
        waitReady()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("저장 확인용 모집")
        compose.onNodeWithText("임시 저장").performClick()
        compose.waitUntil(10_000) { preferences.getString(storageKey, null)?.contains("저장 확인용 모집") == true }
        compose.runOnIdle { generation.intValue++ }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("임시저장 모집").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("이어 작성").performClick()
        compose.onNode(hasSetTextAction() and hasText("저장 확인용 모집")).assertExists()
    }

    private fun waitReady() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("임시 저장").fetchSemanticsNodes().singleOrNull()
                ?.config?.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) == false
        }
    }
}
