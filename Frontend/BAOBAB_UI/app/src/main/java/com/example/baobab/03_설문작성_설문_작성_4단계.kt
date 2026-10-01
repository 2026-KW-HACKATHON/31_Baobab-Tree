package com.example.baobab

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SurveyRegisteredBackground = Color(0xFFFDF9F1)
private val SurveyRegisteredGreen = Color(0xFF2F5539)
private val SurveyRegisteredMessage = Color(0xCC545454)

@Composable
fun SurveyCreationStepFourScreen(
    onOtherSurveyClick: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurveyRegisteredBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 108.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SurveyRegisteredImage()
            Text(
                text = "설문이 등록되었어요!",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 16.dp),
                color = Color.Black,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "다양한 사람들의 소중한 의견이",
                    color = SurveyRegisteredMessage,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "모이길 기다려주세요.",
                    color = SurveyRegisteredMessage,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            RegisteredActionButton(
                text = "다른 설문 참여하기",
                background = SurveyRegisteredGreen,
                textColor = Color.White,
                onClick = onOtherSurveyClick
            )
            Spacer(modifier = Modifier.height(35.dp))
            RegisteredActionButton(
                text = "홈 화면으로 돌아가기",
                background = Color.White,
                textColor = SurveyRegisteredGreen,
                borderColor = SurveyRegisteredGreen,
                onClick = onHomeClick
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SurveyRegisteredImage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(354.dp)
            .background(Color(0xFFECEAE7)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "NO IMAGE",
                color = Color(0xFFBDBDBD),
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun RegisteredActionButton(
    text: String,
    background: Color,
    textColor: Color,
    onClick: () -> Unit,
    borderColor: Color? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .then(
                if (borderColor != null) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(40.dp))
                } else {
                    Modifier
                }
            )
            .background(background, RoundedCornerShape(40.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
