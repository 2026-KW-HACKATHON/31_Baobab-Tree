package com.example.baobab

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SurveyUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun completionShowsIllustrationAndUnclippedMessage() {
        var returned = false
        compose.setContent { MaterialTheme { SurveyCompletionScreen(ParticipationResult(1, 100, 400)) { returned = true } } }
        compose.onNodeWithContentDescription("설문 참여 완료 축하 그림").assertIsDisplayed()
        compose.onNodeWithText("소중한 의견을 남겨주셔서 감사합니다.\n여러분의 답변이 더 나은 동네를 만들어요.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("홈 화면으로 돌아가기").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(returned) }
    }

    @Test fun searchBackReturnsToPreviousScreen() {
        var returned = false
        compose.setContent { MaterialTheme { SearchResultsScreen(onBack = { returned = true }) } }
        compose.onNodeWithContentDescription("뒤로가기").performClick()
        compose.runOnIdle { assertTrue(returned) }
    }

    @Test fun homePullDownRequestsRefreshEvenWhenListIsEmpty() {
        var refreshed = false
        compose.setContent { MaterialTheme { HomeScreen(onRetry = { refreshed = true }) } }
        compose.onRoot().performTouchInput { swipeDown(startY = height * 0.25f, endY = height * 0.75f) }
        compose.runOnIdle { assertTrue(refreshed) }
    }

    @Test fun signupRequiresEmailDomainAndMembershipWithOtherDescription() {
        compose.setContent { MaterialTheme { AccountScreen(true, AccountViewModel(), {}, {}, {}) } }
        compose.onNodeWithText("이름").performTextInput("테스트")
        compose.onNodeWithText("이메일 아이디").performTextInput("member")
        compose.onNodeWithText("도메인 선택").performClick()
        compose.onNodeWithText("직접 입력").performClick()
        compose.onNodeWithText("도메인 직접 입력").performTextInput("example.com")
        compose.onNodeWithText("아이디").performScrollTo().performTextInput("member")
        compose.onNodeWithText("비밀번호").performScrollTo().performTextInput("password")
        compose.onNodeWithText("가입하기").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("가입 유형을 선택해주세요").performScrollTo().performClick()
        compose.onNodeWithText("지자체").assertExists()
        compose.onNodeWithText("기타").performClick()
        compose.onNodeWithText("가입하기").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("기타 소속 직접 입력 (필수)").performScrollTo().performTextInput("동네 상인")
        compose.onNodeWithText("가입하기").performScrollTo().assertIsEnabled()
    }

    @Test fun surveyAudienceExcludesGovernmentAndOtherNeedsText() {
        compose.setContent { MaterialTheme { AudienceField("", {}) } }
        compose.onNodeWithText("대상을 선택해주세요").performClick()
        compose.onNodeWithText("광운대학교 학생").assertExists()
        compose.onNodeWithText("월계1동 주민").assertExists()
        compose.onNodeWithText("지자체").assertDoesNotExist()
        compose.onNodeWithText("기타").performClick()
        compose.onNodeWithText("기타 대상 직접 입력 (필수)").assertIsDisplayed()
    }

    @Test fun statisticsHideAnswersUntilExpandedInBothModes() {
        val survey = SurveyItem(id = "7", title = "통계 테스트", questions = listOf(SurveyQuestion(11, "의견", "short", emptyList())))
        val results = SurveyResults(7, 1, listOf(QuestionResults(11, listOf(AnswerResult("동네 도서관", 1, 100)), 1)),
            listOf(SurveyResponseResult(3, "2026-10-05", listOf(ResponseAnswerResult(11, "의견", "동네 도서관")))))
        compose.setContent { MaterialTheme { ResultsContent(survey, results, {}, {}, {}) } }
        compose.onNodeWithText("동네 도서관").assertDoesNotExist()
        compose.onNodeWithText("전체 답변 펼치기 ▾").performScrollTo().performClick()
        compose.onNodeWithText("동네 도서관").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("응답별 전체 답변").performScrollTo().performClick()
        compose.onNodeWithText("동네 도서관").assertDoesNotExist()
        compose.onNodeWithText("전체 답변 펼치기 ▾").performScrollTo().performClick()
        compose.onNodeWithText("동네 도서관").performScrollTo().assertIsDisplayed()
    }
}
