package com.example.baobab

import org.junit.Assert.*
import org.junit.Test

class ParticipationPurposeTest {
    @Test fun legacyCategoriesUsePurposeWithoutChangingImages() {
        assertEquals("학술·연구", participationPurpose("교육·학습"))
        assertEquals("가게·서비스", participationPurpose("경제·상권"))
        assertEquals("지역·정책", participationPurpose("지역·사회"))
        assertEquals("기타", participationPurpose("생활·편의"))
        assertEquals("기타", participationPurpose("unknown"))
        assertEquals(R.drawable.search_card_02, surveyCategoryImage("학술·연구"))
    }

    @Test fun explicitRecruitmentPurposeSurvivesReloadAndFeedKeepsDetailIdentity() {
        val description = recruitmentDescription("지역·정책", "주민 의견을 듣습니다.\n참여해주세요.")
        val item = recruitment(description)
        assertEquals("지역·정책", item.purpose())
        assertEquals("주민 의견을 듣습니다.\n참여해주세요.", item.descriptionBody())
        assertEquals("recruitment-42", item.feedItem().id)
        assertEquals(42, item.feedItem().recruitmentId)
        assertEquals("인터뷰", item.feedItem().participationLabel)
    }

    @Test fun oldRecruitmentDoesNotGuessPurposeFromItsActivityType() {
        val item = recruitment("연구와 가게에 대한 인터뷰")
        assertEquals("기타", item.purpose())
        assertEquals(item.description, item.descriptionBody())
    }

    private fun recruitment(description: String) = RecruitmentItem(
        id = 42, authorId = 1, author = RecruitmentAuthor(1, "작성자"),
        title = "주민 인터뷰", organization = "월계1동", activityType = "INTERVIEW",
        participationMode = "OFFLINE", description = description, eligibility = "주민",
        location = "월계1동", durationMinutes = 30, targetCount = 5, acceptedCount = 0,
        rewardPoint = 1000, applicationDeadline = "2026-12-01T00:00:00Z",
        status = "OPEN", createdAt = "2026-10-08T00:00:00Z", slots = emptyList()
    )
}
