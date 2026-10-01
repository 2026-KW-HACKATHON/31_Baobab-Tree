package com.example.baobab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val StepThreeBackground = Color(0xFFFDF9F1)
private val StepThreeSurface = Color(0xFFF4F1E9)
private val StepThreeGreen = Color(0xFF2F5539)
private val StepThreeBorder = Color(0x80545454)
private val StepThreeInter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Bold)
)

@Composable
fun SurveyCreationStepThreeScreen(
    onBackClick: () -> Unit = {},
    onImageAttachClick: () -> Unit = {},
    onCompleteClick: (SurveySettingsDraft) -> Unit = {}
) {
    var rewardPerPerson by remember { mutableStateOf("100P") }
    var rewardRecipients by remember { mutableStateOf("10명") }
    var selectedDuration by remember { mutableStateOf("5분 이하") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StepThreeBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(54.dp))
            StepThreeHeader(onBackClick = onBackClick)
            StepThreeProgress()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 20.dp,
                        bottom = 30.dp
                    )
            ) {
                SectionLabel(number = 1, text = "리워드 지급")
                Spacer(modifier = Modifier.height(14.dp))
                RewardSettingsCard(
                    rewardPerPerson = rewardPerPerson,
                    rewardRecipients = rewardRecipients,
                    onRewardPerPersonChange = { rewardPerPerson = it },
                    onRewardRecipientsChange = { rewardRecipients = it }
                )
                Spacer(modifier = Modifier.height(14.dp))
                TimeSectionLabel()
                Spacer(modifier = Modifier.height(14.dp))
                DurationSelector(
                    selectedDuration = selectedDuration,
                    onDurationSelected = { selectedDuration = it }
                )
                Spacer(modifier = Modifier.height(14.dp))
                ImageAttachmentSection(onImageAttachClick = onImageAttachClick)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(120.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp, start = 18.dp, end = 18.dp)
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(StepThreeGreen)
                    .clickable {
                        onCompleteClick(
                            SurveySettingsDraft(
                                rewardPerPerson = rewardPerPerson,
                                rewardRecipients = rewardRecipients,
                                duration = selectedDuration
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "설문 작성 완료",
                    color = Color.White,
                    fontFamily = StepThreeInter,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

data class SurveySettingsDraft(
    val rewardPerPerson: String,
    val rewardRecipients: String,
    val duration: String
)

@Composable
private fun StepThreeHeader(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .clickable(onClick = onBackClick),
            horizontalArrangement = Arrangement.spacedBy((-8).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ChevronLeft,
                contentDescription = "뒤로가기",
                tint = Color.Black,
                modifier = Modifier.size(40.dp)
            )
            Icon(
                imageVector = Icons.Outlined.ChevronLeft,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            text = "설문 작성하기",
            modifier = Modifier.align(Alignment.Center),
            color = Color.Black,
            fontFamily = StepThreeInter,
            fontSize = 23.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StepThreeProgress() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(81.dp)
    ) {
        ProgressConnector(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 72.dp, top = 34.dp),
            active = true
        )
        ProgressConnector(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 216.dp, top = 34.dp),
            active = true
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 27.dp,
                    end = 27.dp,
                    top = 20.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ProgressStepCircle(number = "1", label = "기본 정보")
            ProgressStepCircle(number = "2", label = "문항 작성")
            ProgressStepCircle(number = "3", label = "설문 설정")
        }
    }
}

@Composable
private fun ProgressStepCircle(number: String, label: String) {
    Column(
        modifier = Modifier.width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(50))
                .background(StepThreeGreen),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontFamily = StepThreeInter,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = label,
            modifier = Modifier.padding(top = 4.dp),
            color = StepThreeGreen,
            fontFamily = StepThreeInter,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ProgressConnector(
    modifier: Modifier = Modifier,
    active: Boolean
) {
    Box(
        modifier = modifier
            .width(114.dp)
            .height(2.dp)
            .background(if (active) StepThreeGreen else StepThreeBorder)
    )
}

@Composable
private fun SectionLabel(number: Int, text: String) {
    Text(
        text = "$number. $text",
        modifier = Modifier
            .height(20.dp)
            .padding(start = 16.dp),
        color = StepThreeGreen,
        fontFamily = StepThreeInter,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun RewardSettingsCard(
    rewardPerPerson: String,
    rewardRecipients: String,
    onRewardPerPersonChange: (String) -> Unit,
    onRewardRecipientsChange: (String) -> Unit
) {
    val points = (rewardPerPerson.filter(Char::isDigit).toIntOrNull() ?: 0) *
        (rewardRecipients.filter(Char::isDigit).toIntOrNull() ?: 0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(StepThreeSurface)
            .border(1.dp, StepThreeBorder, RoundedCornerShape(20.dp))
    ) {
        RewardRow(
            label = "1인당 지급할 리워드",
            value = rewardPerPerson,
            valueWidth = 75.dp,
            onValueChange = onRewardPerPersonChange
        )
        RewardRow(
            label = "리워드 지급 인원 수",
            value = rewardRecipients,
            valueWidth = 68.dp,
            onValueChange = onRewardRecipientsChange
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "필요한 포인트",
                color = Color.Black,
                fontFamily = StepThreeInter,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(150.dp)
            )
            Text(
                text = "${points}P",
                color = StepThreeGreen,
                modifier = Modifier.padding(start = 17.dp),
                fontFamily = StepThreeInter,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RewardRow(
    label: String,
    value: String,
    valueWidth: Dp,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.Black,
            fontFamily = StepThreeInter,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(150.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(valueWidth)
                .height(37.dp),
            singleLine = true,
            textStyle = TextStyle(
                color = Color.Black,
                fontFamily = StepThreeInter,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, StepThreeBorder, RoundedCornerShape(10.dp))
                        .background(Color.Transparent, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun DurationSelector(
    selectedDuration: String,
    onDurationSelected: (String) -> Unit
) {
    val durations = listOf("5분 이하", "5분", "10분", "15분", "20분", "20분 이상")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        durations.forEach { duration ->
            DurationOption(
                text = duration,
                selected = selectedDuration == duration,
                onClick = { onDurationSelected(duration) }
            )
        }
    }
}

@Composable
private fun TimeSectionLabel() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(
            text = "2. 소요 시간",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 31.dp),
            color = StepThreeGreen,
            fontFamily = StepThreeInter,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ImageAttachmentSection(onImageAttachClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
    ) {
        Text(
            text = "3. 이미지 첨부",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 41.dp),
            color = StepThreeGreen,
            fontFamily = StepThreeInter,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 141.dp, top = 35.dp)
                .width(90.dp)
                .height(31.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(StepThreeGreen)
                .clickable(onClick = onImageAttachClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "파일 첨부 >",
                color = Color.White,
                fontFamily = StepThreeInter,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DurationOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(37.dp)
            .width(
                when (text) {
                    "5분 이하" -> 93.dp
                    "20분 이상" -> 103.dp
                    else -> 71.dp
                }
            )
            .border(
                width = 1.dp,
                color = StepThreeBorder,
                shape = RoundedCornerShape(40.dp)
            )
            .background(Color.White, RoundedCornerShape(40.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontFamily = StepThreeInter,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SurveyCreationStepThreePreview() {
    SurveyCreationStepThreeScreen()
}
