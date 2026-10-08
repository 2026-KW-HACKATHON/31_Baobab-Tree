package com.example.baobab

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Toll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DetailGreen = Color(0xFF2F5539)
private val DetailMuted = Color(0xFF697369)

@Composable
fun SurveyDetailScreen(onParticipateClick: () -> Unit = {}, survey: SurveyItem = SampleSurveys.first(),
    onRelatedSurveyClick: (SurveyItem) -> Unit = {}, relatedSurveys: List<SurveyItem> = emptyList(),
    canDelete: Boolean = false, onDelete: () -> Unit = {}, onBack: () -> Unit = {}, onEdit: () -> Unit = {},
    onShare: (SurveyItem) -> Unit = {}, shareBusy: Boolean = false) {
    val related = relatedSurveys.filter { participationPurpose(it.category) == participationPurpose(survey.category) && it.id != survey.id }
    Column(Modifier.fillMaxSize().background(Color(0xFFFDF9F1)).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.Outlined.ChevronLeft, "뒤로가기", tint = DetailGreen) }
            Text("설문 상세", Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (canDelete) BaobabActionButton(BaobabActionIcons.Edit, "설문 수정", onEdit)
            BaobabActionButton(BaobabActionIcons.Share, "설문 공유", { onShare(survey) }, enabled = !shareBusy)
            if (canDelete) BaobabActionButton(BaobabActionIcons.Delete, "설문 삭제", onDelete, destructive = true)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SurveyImage(survey, Modifier.fillMaxWidth().aspectRatio(1.7f).clip(RoundedCornerShape(24.dp)))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = participationCategorySelectedColor(survey.category), shape = RoundedCornerShape(8.dp)) {
                    Text(participationPurpose(survey.category), Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = participationCategoryTextColor(survey.category), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(survey.title, fontSize = 25.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, color = DetailGreen)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Person, null, tint = DetailMuted, modifier = Modifier.size(20.dp))
                    Text(survey.author.ifBlank { "설문 작성자" }, Modifier.weight(1f), color = DetailMuted, fontSize = 14.sp)
                }
            }
            Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DetailMetric(Icons.Outlined.Toll, "참여 보상", survey.points.ifBlank { "0P" }, true)
                    HorizontalDivider(color = Color(0xFFE8ECE4))
                    DetailMetric(Icons.Outlined.Groups, "참여 현황", "${survey.participantCount ?: 0}명 참여")
                    DetailMetric(Icons.Outlined.Schedule, "예상 소요시간", survey.duration?.takeIf { it.isNotBlank() } ?: "안내 없음")
                }
            }
            Text("설문 소개", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(survey.description?.takeIf { it.isNotBlank() } ?: "등록된 소개가 없습니다. 아래 참여 정보를 확인해주세요.",
                fontSize = 15.sp, lineHeight = 24.sp, color = DetailMuted)
            Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailInfo("참여 대상", survey.audience?.takeIf { it.isNotBlank() } ?: "누구나 참여 가능")
                    HorizontalDivider(color = Color(0xFFE8ECE4))
                    DetailInfo("마감일", survey.deadline?.takeIf { it.isNotBlank() } ?: "별도 마감일 없음")
                    DetailInfo("문항 수", "${survey.questionCount ?: survey.questions.size}문항")
                }
            }
            if (survey.status != "CLOSED") {
                Surface(color = Color(0xFFEAF0E4), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("설문 공유하고 10P 받기", color = DetailGreen, fontSize = 17.sp,
                            lineHeight = 25.sp, fontWeight = FontWeight.Bold)
                        Text("내 링크로 다른 사람이 설문을 완료하면 1명당 10P가 적립돼요.",
                            color = DetailMuted, fontSize = 14.sp, lineHeight = 22.sp)
                        TextButton(onClick = { onShare(survey) }, enabled = !shareBusy) {
                            Icon(BaobabActionIcons.Share, null, Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (shareBusy) "잠시만 기다려주세요" else "링크 공유하기")
                        }
                    }
                }
            }
            if (related.isNotEmpty()) {
                Text("같은 카테고리의 설문", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    related.take(6).forEach { item ->
                        Box(Modifier.width(200.dp)) { SurveyFeedCard(item, onClick = { onRelatedSurveyClick(item) }) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Surface(color = Color(0xFFFDF9F1), shadowElevation = 4.dp) {
            Button(onParticipateClick, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp).heightIn(min = 54.dp),
                shape = RoundedCornerShape(16.dp), enabled = survey.status != "CLOSED") {
                Text(if (survey.status == "CLOSED") "마감된 설문입니다" else "설문 참여하기", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DetailMetric(icon: ImageVector, label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = DetailGreen, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = DetailMuted, fontSize = 13.sp)
            Text(value, fontSize = if (highlight) 24.sp else 16.sp, fontWeight = FontWeight.Bold,
                color = DetailGreen, lineHeight = if (highlight) 30.sp else 23.sp)
        }
    }
}

@Composable
private fun DetailInfo(label: String, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = DetailMuted, fontSize = 13.sp)
        Text(value, fontSize = 15.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SurveyDetailScreenPreview() { SurveyDetailScreen() }
