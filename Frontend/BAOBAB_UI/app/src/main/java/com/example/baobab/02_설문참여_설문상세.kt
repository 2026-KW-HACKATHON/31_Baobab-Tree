package com.example.baobab

import android.view.View
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign

private val SurveyDetailBackground = Color(0xFFFDF9F1)
private val SurveyDetailGreen = Color(0xFF2F5539)
private val SurveyDetailSecondary = Color(0xFF545454)
private val SurveyDetailPanel = Color(0x80FDF9F1)
private val SurveyDetailBorder = Color(0x80545454)
private val SurveyDetailInter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Bold)
)

private data class RelatedSurvey(
    val title: String,
    val author: String,
    val points: String,
    val categoryFontSize: Int
)

private val RelatedSurveys = listOf(
    RelatedSurvey("분리수거 플라스틱&유리 구분 문의", "골목 한바퀴", "400P", 12),
    RelatedSurvey("무분별한 거리 현수막 설치에 대한 주민 인식 조사", "골목 한바퀴", "400P", 12),
    RelatedSurvey("광운대 앞 지쿠터 마구잡이 주차로 인한 불편성", "도보 통학러", "500P", 11)
)

@Composable
fun SurveyDetailScreen(
    onParticipateClick: () -> Unit = {}
) {
    val panelShape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurveyDetailBackground)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),//임시로 바꿔놓음
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(240.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 220.dp)
                .fillMaxWidth()
                .height(768.dp)
                .shadow(2.dp, panelShape)
                .clip(panelShape)
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 107.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 23.dp)
                ) {
                    SurveyCategory()
                    SurveyTitleBlock()
                    SurveyMetaRow()
                    SurveyStats()
                    SurveyDescription()
                    RelatedSurveySection()
                }
            }
        }

        SurveyParticipateButton(
            onClick = onParticipateClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SurveyCategory() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(width = 70.dp, height = 32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFC9D7ED)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "지역·사회",
                color = Color.Black,
                fontFamily = SurveyDetailInter,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SurveyTitleBlock() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = "설문 조사 제목",
                modifier = Modifier.padding(top = 10.dp),
                color = Color.Black,
                fontFamily = SurveyDetailInter,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 1.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = "설문조사 소개 글",
                color = SurveyDetailSecondary,
                fontFamily = SurveyDetailInter,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun SurveyMetaRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),//임시로 바꿈
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 15.dp)
                .size(40.dp)
                .clip(CircleShape)
        )
        Text(
            text = "작성자 이름",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 50.dp, top = 26.dp)
                .width(117.dp)
                .height(18.dp),
            color = Color.Black,
            fontFamily = SurveyDetailInter,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        SvgAsset(
            assetName = "survey_divider.svg",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 177.dp, top = 20.dp)
                .width(1.dp)
                .height(30.dp)
        )
        SvgAsset(
            assetName = "survey_calendar_icon.svg",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 187.dp, top = 20.dp)
                .size(30.dp)
        )
        Text(
            text = "설문 마감 날짜",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 224.5.dp, top = 26.dp)
                .width(117.dp)
                .height(18.dp),
            color = Color.Black,
            fontFamily = SurveyDetailInter,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SurveyStats() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(102.dp)
            .border(1.dp, SurveyDetailBorder, RoundedCornerShape(15.dp))
            .clip(RoundedCornerShape(15.dp))
            .background(SurveyDetailBackground),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SurveyStat(
            icon = "survey_participants_icon.svg",
            label = "30명 참여",
            modifier = Modifier.weight(1f)
        )
        SurveyStat(
            icon = "survey_hourglass_icon.svg",
            label = "소요시간",
            modifier = Modifier.weight(1f)
        )
        SurveyStat(
            icon = "survey_points_icon.svg",
            label = "포인트",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SurveyStat(
    icon: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.height(100.dp)) {
        SvgAsset(
            assetName = icon,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
                .size(40.dp)
        )
        Text(
            text = label,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 65.dp)
                .width(83.dp),
            color = Color.Black,
            fontFamily = SurveyDetailInter,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun SurveyDescription() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = "설문 소개",
                modifier = Modifier.padding(top = 20.dp),
                color = Color.Black,
                fontFamily = SurveyDetailInter,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 29.sp
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SurveyDetailBorder, RoundedCornerShape(15.dp))
                .clip(RoundedCornerShape(15.dp))
                .background(SurveyDetailPanel)
                .padding(horizontal = 1.dp, vertical = 5.dp)
        ) {
            SurveyDescriptionRow(label = "대상", value = "월계 1동 주민 누구나")
            SurveyDescriptionRow(label = "주제", value = "교통/주차 공간 부족")
            SurveyDescriptionRow(label = "문항 수", value = "12문항")
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = "같은 카테고리의 설문",
                modifier = Modifier.padding(top = 20.dp),
                color = Color.Black,
                fontFamily = SurveyDetailInter,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 29.sp
            )
        }
    }
}

@Composable
private fun SurveyDescriptionRow(
    label: String,
    value: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp)
                .width(100.dp),
            color = Color.Black,
            fontFamily = SurveyDetailInter,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 18.sp
        )
        Text(
            text = value,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 110.dp)
                .width(234.dp),
            color = SurveyDetailSecondary,
            fontFamily = SurveyDetailInter,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun RelatedSurveySection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        RelatedSurveys.forEach { survey ->
            RelatedSurveyCard(survey)
        }
    }
}

@Composable
private fun RelatedSurveyCard(survey: RelatedSurvey) {
    val cardShape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .size(180.dp)
            .shadow(2.dp, cardShape)
            .clip(cardShape)
            .background(Color.White)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),//임시
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 5.dp)
                .size(width = 170.dp, height = 85.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Text(
            text = survey.title,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 105.dp)
                .size(width = 160.dp, height = 36.dp),
            color = Color.Black,
            fontFamily = SurveyDetailInter,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Clip
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 157.dp)
                .width(160.dp)
                .height(13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = survey.author,
                modifier = Modifier.width(75.dp),
                color = SurveyDetailSecondary,
                fontFamily = SurveyDetailInter,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = survey.points,
                modifier = Modifier.width(75.dp),
                color = SurveyDetailGreen,
                fontFamily = SurveyDetailInter,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 75.dp)
                .clip(CircleShape)
                .background(Color(0xFFE1ECF4))
                .border(2.dp, Color.White, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "지역·사회",
                color = Color(0xFF173F6B),
                fontFamily = SurveyDetailInter,
                fontSize = survey.categoryFontSize.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SurveyParticipateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .height(55.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(SurveyDetailGreen)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "설문 참여하기",
            color = Color.White,
            fontFamily = SurveyDetailInter,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SvgAsset(
    assetName: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                settings.javaScriptEnabled = false
                loadDataWithBaseURL(
                    "file:///android_asset/",
                    """<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\"></head><body style=\"margin:0;background:transparent;width:100%;height:100%;overflow:hidden\"><img src=\"$assetName\" style=\"display:block;width:100%;height:100%;object-fit:contain\"></body></html>""",
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SurveyDetailScreenPreview() {
    SurveyDetailScreen()
}
