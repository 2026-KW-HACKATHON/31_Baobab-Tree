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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SurveyFormBackground = Color(0xFFFDF9F1)
private val SurveyFormGreen = Color(0xFF2F5539)
private val SurveyFormField = Color.White
private val SurveyFormBorder = Color(0xFFD9E0D5)
private val SurveyFormPlaceholder = Color(0xFF6F786E)
private val SurveyFormInter = FontFamily.SansSerif

@Composable
fun SurveyCreationStepOneScreen(
    onBackClick: () -> Unit = {},
    onNextClick: (SurveyDraftStepOne) -> Unit = {},
    state: SurveyCreationState = remember { SurveyCreationState() }
) {
    var title by state::title
    var category by state::category
    var introduction by state::introduction
    var audience by state::audience
    var deadline by state::deadline

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurveyFormBackground)
            .safeDrawingPadding().imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SurveyFormHeader(onBackClick = onBackClick)
            SurveyFormProgress(currentStep = 1)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 2.dp, bottom = 110.dp)
            ) {
                SurveyFieldSection(
                    number = 1,
                    label = "설문 주제",
                    value = title,
                    placeholder = "설문 주제를 입력하세요. (필수)",
                    onValueChange = { title = it }
                )
                Spacer(modifier = Modifier.height(24.dp))
                SurveyFieldSection(
                    number = 2,
                    label = "카테고리",
                    value = category,
                    placeholder = "카테고리를 선택하십시오. (필수)",
                    onValueChange = { category = it }
                )
                Spacer(modifier = Modifier.height(24.dp))
                SurveyFieldSection(
                    number = 3,
                    label = "설문 소개",
                    value = introduction,
                    placeholder = "카테고리를 선택하십시오. (필수)",
                    onValueChange = { introduction = it },
                    multiline = true
                )
                Spacer(modifier = Modifier.height(24.dp))
                SurveyFieldSection(
                    number = 4,
                    label = "설문 대상",
                    value = audience,
                    placeholder = "예) 월계 1동 주민, 광운대학생 등",
                    onValueChange = { audience = it }
                )
                Spacer(modifier = Modifier.height(24.dp))
                SurveyDeadlineSection(
                    value = deadline,
                    onValueChange = { deadline = it }
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 16.dp)
                .height(55.dp)
                .background(SurveyFormGreen, RoundedCornerShape(16.dp))
                .clickable {
                    onNextClick(
                        SurveyDraftStepOne(
                            title = title,
                            category = category,
                            introduction = introduction,
                            audience = audience,
                            deadline = deadline
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "다음 단계로",
                color = Color.White,
                fontSize = 17.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SurveyFormInter,
                textAlign = TextAlign.Center
            )
        }
    }
}

data class SurveyDraftStepOne(
    val title: String,
    val category: String,
    val introduction: String,
    val audience: String,
    val deadline: String
)

@Composable
private fun SurveyFieldSection(
    number: Int,
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    multiline: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$number. $label",
            modifier = Modifier.padding(start = 20.dp, bottom = 11.dp),
            color = SurveyFormGreen,
            fontFamily = SurveyFormInter,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold
        )
        SurveyInput(
            value = value,
            placeholder = placeholder,
            onValueChange = onValueChange,
            multiline = multiline
        )
    }
}

@Composable
private fun SurveyInput(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    multiline: Boolean = false
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = if (multiline) 120.dp else 52.dp),
        singleLine = !multiline,
        textStyle = TextStyle(
            color = Color.Black,
            fontFamily = SurveyFormInter,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, SurveyFormBorder, RoundedCornerShape(10.dp))
                    .background(SurveyFormField, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = if (multiline) Alignment.TopStart else Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = SurveyFormPlaceholder,
                        fontFamily = SurveyFormInter,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun SurveyDeadlineSection(
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(start = 20.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "5. 설문 마감일",
            color = SurveyFormGreen,
            fontFamily = SurveyFormInter,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            singleLine = true,
            textStyle = TextStyle(
                color = Color.Black,
                fontFamily = SurveyFormInter,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, SurveyFormBorder, RoundedCornerShape(10.dp))
                        .background(SurveyFormField, RoundedCornerShape(10.dp))
                        .padding(horizontal = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EventAvailable,
                        contentDescription = "마감일",
                        tint = SurveyFormGreen,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    innerTextField()
                }
            }
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SurveyCreationStepOnePreview() {
    SurveyCreationStepOneScreen()
}
