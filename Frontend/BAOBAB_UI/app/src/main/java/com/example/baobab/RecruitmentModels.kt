package com.example.baobab

data class RecruitmentAuthor(
    val id: Int,
    val name: String
)

data class RecruitmentSlot(
    val id: Int,
    val recruitmentId: Int,
    val startsAt: String,
    val endsAt: String
)

data class RecruitmentItem(
    val id: Int,
    val authorId: Int,
    val author: RecruitmentAuthor,
    val title: String,
    val organization: String,
    val activityType: String,
    val participationMode: String,
    val description: String,
    val eligibility: String,
    val location: String,
    val durationMinutes: Int,
    val targetCount: Int,
    val acceptedCount: Int,
    val rewardPoint: Int,
    val applicationDeadline: String,
    val status: String,
    val createdAt: String,
    val slots: List<RecruitmentSlot>
)

data class RecruitmentApplicationItem(
    val id: Int,
    val status: String,
    val message: String?,
    val createdAt: String,
    val selectedAt: String?,
    val completedAt: String?,
    val paidAt: String?,
    val slot: RecruitmentSlot,
    val recruitment: RecruitmentItem
)

data class RecruitmentApplicantUser(
    val id: Int,
    val name: String,
    val ageGroup: String?,
    val region: String?,
    val memberType: String?
)

data class RecruitmentApplicantItem(
    val id: Int,
    val status: String,
    val message: String?,
    val createdAt: String,
    val selectedAt: String?,
    val completedAt: String?,
    val paidAt: String?,
    val slot: RecruitmentSlot,
    val user: RecruitmentApplicantUser
)

data class RecruitmentSlotInput(
    val startsAt: String,
    val endsAt: String
)

data class RecruitmentCreateInput(
    val title: String,
    val organization: String,
    val activityType: String,
    val participationMode: String,
    val description: String,
    val eligibility: String,
    val location: String,
    val durationMinutes: Int,
    val targetCount: Int,
    val rewardPoint: Int,
    val applicationDeadline: String,
    val slots: List<RecruitmentSlotInput>
)

data class RecruitmentActionResult(
    val id: Int,
    val status: String,
    val rewardPoint: Int? = null,
    val refundedPoint: Int? = null
)