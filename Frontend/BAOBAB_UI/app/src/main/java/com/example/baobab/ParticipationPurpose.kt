package com.example.baobab

val ParticipationPurposes = listOf("학술·연구", "가게·서비스", "지역·정책", "기타")

// Preserve stored categories and images while presenting the new purpose taxonomy.
fun participationPurpose(category: String): String = when (category.trim()) {
    in ParticipationPurposes -> category.trim()
    "교육·학습", "건강·의료" -> "학술·연구"
    "경제·상권", "카페", "먹거리" -> "가게·서비스"
    "지역·사회" -> "지역·정책"
    else -> "기타"
}

// Recruitment API has no purpose field yet. Keep an explicit, readable purpose
// line in the description so it survives reloads and is readable by older clients.
fun recruitmentDescription(purpose: String, description: String) =
    "모집 목적: $purpose\n\n${description.trim()}"

fun RecruitmentItem.purpose(): String = description.lineSequence().firstOrNull()
    ?.removePrefix("모집 목적: ")?.takeIf { it in ParticipationPurposes } ?: "기타"

fun RecruitmentItem.descriptionBody(): String =
    if (description.startsWith("모집 목적: ") && description.lineSequence().first().removePrefix("모집 목적: ") in ParticipationPurposes)
        description.substringAfter("\n\n", "") else description

fun RecruitmentItem.feedItem() = SurveyItem(
    id = "recruitment-$id", category = purpose(), title = title,
    author = organization, points = "%,dP".format(rewardPoint),
    description = descriptionBody(), endDate = applicationDeadline,
    participationLabel = when (activityType) {
        "EXPERIMENT" -> "실험"
        "INTERVIEW" -> "인터뷰"
        "USABILITY" -> "사용성 테스트"
        else -> "참여 신청"
    }, recruitmentId = id
)
