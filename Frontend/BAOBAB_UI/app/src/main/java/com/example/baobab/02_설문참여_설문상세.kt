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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DetailBackground = Color(0xFFE9E9E9)
private val DetailSurface = Color.White
private val DetailGreen = Color(0xFF2D5A3A)
private val DetailChip = Color(0xFFC9D7ED)
private val DetailPanel = Color(0xFFFDF9F1)
private val DetailSecondaryText = Color(0xFF606060)

@Composable
fun SurveyDetailScreen(
    onBackClick: () -> Unit = {},
    onParticipateClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DetailBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SurveyImageHeader(onBackClick = onBackClick)
            SurveyDetailPanel(onParticipateClick = onParticipateClick)
        }
    }
}

@Composable
private fun SurveyImageHeader(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(205.dp)
            .background(DetailBackground)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 21.dp, top = 52.dp)
                .clickable(onClick = onBackClick),
            horizontalArrangement = Arrangement.spacedBy((-8).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ChevronLeft,
                contentDescription = "뒤로가기",
                tint = Color.Black,
                modifier = Modifier.size(30.dp)
            )
            Icon(
                imageVector = Icons.Outlined.ChevronLeft,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(30.dp)
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = Color(0xFF9C9C9C),
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "NO IMAGE",
                color = Color(0xFF9C9C9C),
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun SurveyDetailPanel(onParticipateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
            .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
            .background(DetailSurface)
            .padding(start = 23.dp, top = 13.dp, end = 23.dp, bottom = 28.dp)
    ) {
        Text(
            text = "지역·사회",
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(DetailChip)
                .padding(horizontal = 9.dp, vertical = 7.dp),
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "설문 제목",
            modifier = Modifier.padding(top = 19.dp),
            color = Color.Black,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "설문 소개글",
            modifier = Modifier.padding(top = 4.dp),
            color = DetailSecondaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        SurveyMetaRow()
        SurveyStatsRow()
        Text(
            text = "설문 소개",
            modifier = Modifier.padding(top = 18.dp),
            color = Color.Black,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold
        )
        SurveyDescriptionTable()
        Text(
            text = "같은 카테고리의 설문",
            modifier = Modifier.padding(top = 18.dp),
            color = Color.Black,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(DetailGreen)
                .clickable(onClick = onParticipateClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "설문 참여하기",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SurveyMetaRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 45.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFE2E2E2))
                .border(1.dp, Color.White, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "작성자",
                tint = Color.White,
                modifier = Modifier.size(31.dp)
            )
        }
        Text(
            text = "작성자 이름",
            modifier = Modifier.padding(start = 10.dp),
            color = Color.Black,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.width(15.dp))
        Box(
            modifier = Modifier
                .height(30.dp)
                .width(1.dp)
                .background(Color(0xFF777777))
        )
        Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = "마감일",
            tint = Color.Black,
            modifier = Modifier
                .padding(start = 11.dp)
                .size(27.dp)
        )
        Text(
            text = "설문 마감 날짜",
            modifier = Modifier.padding(start = 9.dp),
            color = Color.Black,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SurveyStatsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(102.dp)
            .border(1.dp, Color(0xFF999999), RoundedCornerShape(15.dp))
            .clip(RoundedCornerShape(15.dp))
            .background(DetailPanel)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SurveyStat(
            icon = Icons.Outlined.Groups,
            value = "00명 참여",
            modifier = Modifier.weight(1f)
        )
        SurveyStat(
            icon = Icons.Outlined.HourglassEmpty,
            value = "소요시간",
            modifier = Modifier.weight(1f)
        )
        SurveyStat(
            icon = Icons.Outlined.AccountBalanceWallet,
            value = "포인트",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SurveyStat(
    icon: ImageVector,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DetailGreen,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = value,
            modifier = Modifier.padding(top = 6.dp),
            color = Color.Black,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SurveyDescriptionTable() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 9.dp)
            .border(1.dp, Color(0xFF999999), RoundedCornerShape(15.dp))
            .clip(RoundedCornerShape(15.dp))
            .background(DetailPanel)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DescriptionRow(label = "대상", value = "월계 1동 주민 누구나")
        DescriptionRow(label = "주제", value = "교통/주차 공간 부족")
        DescriptionRow(label = "문항 수", value = "12문항")
    }
}

@Composable
private fun DescriptionRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(100.dp),
            color = Color.Black,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = DetailSecondaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
