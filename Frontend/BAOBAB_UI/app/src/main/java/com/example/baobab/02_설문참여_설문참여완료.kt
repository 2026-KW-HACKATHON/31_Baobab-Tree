package com.example.baobab

import android.view.View
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val CompletionBackground = Color(0xFFFDF9F1)
private val CompletionGreen = Color(0xFF2F5539)
private val CompletionGold = Color(0xFFFDC854)
private val CompletionBrown = Color(0xFF8C510A)
private val CompletionGray = Color(0xCC545454)
private val CompletionInter = FontFamily.SansSerif

@Composable
fun SurveyCompletionScreen(
    result: ParticipationResult? = null,
    onHomeClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CompletionBackground)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 113.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.survey_completion_illustration),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(246.dp)
            )
            CompletionTitle()
            CompletionMessage()
            CompletionReward()
            result?.let {
                Text("지급 ${it.rewardPoint}P · 보유 ${it.point}P", color = CompletionGreen)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(90.dp)
                .padding(horizontal = 18.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(15.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(CompletionGreen)
                        .border(1.dp, CompletionGreen, RoundedCornerShape(40.dp))
                        .clickable(onClick = onHomeClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "홈 화면으로 돌아가기",
                        color = Color.White,
                        fontFamily = CompletionInter,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun CompletionTitle() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "설문 참여가",
            color = Color.Black,
            fontFamily = CompletionInter,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "완료되었습니다!",
            color = Color.Black,
            fontFamily = CompletionInter,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CompletionMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(top = 10.dp, bottom = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "설문 참여 감사멘트",
                color = CompletionGray,
                fontFamily = CompletionInter,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "소중한 의견을 남겨주셔서 감사합니다",
                color = CompletionGray,
                fontFamily = CompletionInter,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CompletionReward() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 27.dp)
            .clip(RoundedCornerShape(15.dp))
            .border(3.dp, CompletionGold, RoundedCornerShape(15.dp))
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 11.dp)
                .width(144.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                MedalRewardIcon(modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Point",
                color = Color.Black,
                fontFamily = CompletionInter,
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 42.sp
            )
        }
        Text(
            text = "리워드가 지급되었어요!",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
                .width(293.dp),
            color = CompletionBrown,
            fontFamily = CompletionInter,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MedalRewardIcon(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        SvgAsset(
            assetName = "completion_medal_front.svg",
            modifier = Modifier
                .align(Alignment.Center)
                .size(28.dp)
        )
        SvgAsset(
            assetName = "completion_medal_body.svg",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 1.dp, y = (-2).dp)
                .size(width = 20.dp, height = 19.dp)
        )
        SvgAsset(
            assetName = "completion_medal_left_ribbon.svg",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-1).dp, y = (-2).dp)
                .size(width = 20.dp, height = 19.dp)
        )
        SvgAsset(
            assetName = "completion_medal_right_ribbon.svg",
            modifier = Modifier
                .align(Alignment.Center)
                .size(12.dp)
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
private fun SurveyCompletionScreenPreview() {
    SurveyCompletionScreen()
}
