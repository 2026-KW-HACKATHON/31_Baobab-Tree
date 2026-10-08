package com.example.baobab

import android.content.Context
import android.content.Intent
import java.net.URI

data class SharedSurvey(val id: String, val referralToken: String? = null)
data class SurveyShareReward(val shareToken: String, val rewardPoint: Int)

private fun validReferralToken(value: String) = value.length <= 2048 &&
    Regex("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+").matches(value)

fun surveyShareLink(id: String, referralToken: String? = null): String {
    require(id.toIntOrNull()?.let { it > 0 } == true && id.all(Char::isDigit))
    require(referralToken == null || validReferralToken(referralToken))
    return "baobab://surveys/$id" + (referralToken?.let { "?referral=$it" } ?: "")
}

fun surveyWebShareLink(id: String, referralToken: String, apiBaseUrl: String): String {
    surveyShareLink(id, referralToken)
    val base = URI(apiBaseUrl.trimEnd('/') + "/")
    require(base.scheme in listOf("https", "http") && base.host != null && base.query == null && base.fragment == null)
    return base.resolve("surveys/$id/share?referral=$referralToken").toString()
}

fun sharedSurvey(link: String?): SharedSurvey? = runCatching {
    val uri = URI(link ?: return null)
    if (uri.scheme != "baobab" || uri.host != "surveys" || uri.fragment != null || uri.userInfo != null || uri.port != -1) return null
    val id = uri.path.removePrefix("/")
    if (id.toIntOrNull()?.let { it > 0 } != true || !id.all(Char::isDigit)) return null
    val referral = uri.rawQuery?.let { query ->
        if (!query.startsWith("referral=")) return null
        query.removePrefix("referral=").takeIf(::validReferralToken) ?: return null
    }
    SharedSurvey(id, referral)
}.getOrNull()

fun sharedSurveyId(link: String?): String? = sharedSurvey(link)?.id

fun shareSurvey(context: Context, survey: SurveyItem, link: String = surveyShareLink(survey.id)) {
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, survey.title)
        putExtra(Intent.EXTRA_TEXT, "${survey.title}\nBAOBAB에서 설문에 참여해보세요.\n$link")
    }
    context.startActivity(Intent.createChooser(share, "설문 공유"))
}
