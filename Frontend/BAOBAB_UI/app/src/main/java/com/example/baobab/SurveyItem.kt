package com.example.baobab

import androidx.compose.ui.graphics.Color

val SurveyCategories = ParticipationPurposes

fun surveyCategoryImage(category: String): Int = when (category.trim()) {
    "생활·편의" -> R.drawable.search_card_01
    "교육·학습", "학술·연구" -> R.drawable.search_card_02
    "지역·사회", "지역·정책" -> R.drawable.search_card_03
    "문화·스포츠" -> R.drawable.search_card_04
    "경제·상권", "가게·서비스" -> R.drawable.search_card_05
    "건강·의료" -> R.drawable.search_card_06
    else -> R.drawable.search_card_01
}
data class SurveyItem(
    val id: String = "preview",
    val category: String = "",
    val title: String = "",
    val author: String = "",
    val points: String = "",
    val imageRes: Int = surveyCategoryImage(category),
    val badgeColor: Color = Color(0xFFC9D7ED),
    val badgeTextColor: Color = Color(0xFF173F6B),
    val badgeFontSize: Int = 11,
    val description: String? = null,
    val deadline: String? = null,
    val participantCount: Int? = null,
    val duration: String? = null,
    val audience: String? = null,
    val questionCount: Int? = null,
    val status: String = "OPEN",
    val endDate: String? = null,
    val questions: List<SurveyQuestion> = emptyList(),
    val userId: Int? = null,
    val targetCount: Int? = null,
    val imageData: String? = null,
    val participationLabel: String = "온라인 설문",
    val recruitmentId: Int? = null
)

data class SurveyQuestion(
    val id: Int,
    val question: String,
    val questionType: String,
    val options: List<SurveyOption>,
    val required: Boolean = true
)

data class SurveyOption(val id: Int, val optionText: String)

val SampleSurveys = listOf(
    SurveyItem(
        id = "sample-1",
        category = "생활·편의",
        title = "일상 속 플라스틱 제품 사용 실태 조사",
        author = "자취3년차",
        points = "500P",
        badgeColor = Color(0xFFF6E2E3),
        badgeTextColor = Color(0xFF800000)
    ),
    SurveyItem(
        id = "sample-2",
        category = "생활·편의",
        title = "플라스틱 빨대 VS 종이빨대 무엇이 불편하신가요?",
        author = "1일1아아",
        points = "100P",
        badgeColor = Color(0xFFF6E2E3),
        badgeTextColor = Color(0xFF800000)
    ),
    SurveyItem(
        id = "sample-3",
        category = "문화·스포츠",
        title = "플라스틱 다회용 그릇 재활용 참여 조사",
        author = "잠실 가고 싶다",
        points = "100P",
        badgeColor = Color(0xFFF6F0D9),
        badgeTextColor = Color(0xFF806A00)
    ),
    SurveyItem(
        id = "sample-4",
        category = "경제·상권",
        title = "플라스틱 빨대 vs 종이 빨대 고객 수요조사",
        author = "OO 24시 카페 운영자",
        points = "500P",
        badgeColor = Color(0xFFF6E8DC),
        badgeTextColor = Color(0xFF803C00)
    ),
    SurveyItem(
        id = "sample-5",
        category = "건강·의료",
        title = "미세플라스틱 관련 인식조사",
        author = "보건동아리 BOGEON",
        points = "500P",
        badgeColor = Color(0xFFEBE3F2),
        badgeTextColor = Color(0xFF502060)
    ),
    SurveyItem(
        id = "sample-6",
        category = "지역·사회",
        title = "분리수거 플라스틱&유리 구분 문의",
        author = "골목 한바퀴",
        points = "400P",
        badgeColor = Color(0xFFE1ECF4),
        badgeTextColor = Color(0xFF173F6B),
        badgeFontSize = 12
    )
)
