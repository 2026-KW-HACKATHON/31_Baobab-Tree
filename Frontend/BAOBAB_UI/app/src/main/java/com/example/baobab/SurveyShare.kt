package com.example.baobab

import android.content.Context
import android.content.Intent
import java.net.URI

fun surveyShareLink(id: String): String {
    require(id.toIntOrNull()?.let { it > 0 } == true)
    return "baobab://surveys/$id"
}

fun sharedSurveyId(link: String?): String? = runCatching {
    val uri = URI(link ?: return null)
    if (uri.scheme != "baobab" || uri.host != "surveys" || uri.query != null || uri.fragment != null) return null
    uri.path.removePrefix("/").takeIf { id -> id.toIntOrNull()?.let { it > 0 } == true && id.all(Char::isDigit) }
}.getOrNull()

fun shareSurvey(context: Context, survey: SurveyItem) {
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, survey.title)
        putExtra(Intent.EXTRA_TEXT, "${survey.title}\nBAOBAB에서 설문에 참여해보세요.\n${surveyShareLink(survey.id)}")
    }
    context.startActivity(Intent.createChooser(share, "설문 공유"))
}
