package com.example.baobab

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val QuestionBackground = Color(0xFFFDF9F1)
private val QuestionSurface = Color(0xFFF4F1E9)
private val QuestionGreen = Color(0xFF2F5539)
private val QuestionBorder = Color(0x80545454)

@Composable
fun SurveyCreationStepTwoScreen(
    onBackClick: () -> Unit = {},
    onNextClick: (List<SurveyQuestionDraft>) -> Unit = {}
) {
    val questions = remember {
        mutableStateListOf(
            SurveyQuestionDraft(
                type = SurveyQuestionType.MULTIPLE_CHOICE,
                options = mutableListOf("정답 옵션 1", "정답 옵션 2")
            ),
            SurveyQuestionDraft(type = SurveyQuestionType.SHORT_ANSWER)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(QuestionBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(54.dp))
            QuestionHeader(onBackClick = onBackClick)
            QuestionProgress()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp, 20.dp, 0.dp, 110.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                questions.forEachIndexed { index, question ->
                    when (question.type) {
                        SurveyQuestionType.MULTIPLE_CHOICE -> {
                            MultipleChoiceCard(
                                question = question,
                                onRemove = {
                                    if (questions.size > 1) questions.removeAt(index)
                                }
                            )
                        }
                        SurveyQuestionType.SHORT_ANSWER -> {
                            ShortAnswerCard(
                                question = question,
                                onRemove = {
                                    if (questions.size > 1) questions.removeAt(index)
                                }
                            )
                        }
                    }
                }
                NewQuestionButton(
                    onClick = {
                        questions.add(
                            SurveyQuestionDraft(type = SurveyQuestionType.SHORT_ANSWER)
                        )
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 30.dp)
                .height(55.dp)
                .background(QuestionGreen, RoundedCornerShape(40.dp))
                .clickable { onNextClick(questions.toList()) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "다음 단계로",
                color = Color.White,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

enum class SurveyQuestionType {
    MULTIPLE_CHOICE,
    SHORT_ANSWER
}

data class SurveyQuestionDraft(
    val type: SurveyQuestionType,
    var title: String = "제목 없는 설문지",
    val options: MutableList<String> = mutableListOf()
)

@Composable
private fun QuestionHeader(onBackClick: () -> Unit) {
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
            fontSize = 23.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun QuestionProgress() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(81.dp)
            .padding(horizontal = 42.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProgressCircle(number = "1", active = true)
        ProgressConnector(active = true)
        ProgressCircle(number = "2", active = true)
        ProgressConnector(active = false)
        ProgressCircle(number = "3", active = false)
    }
}

@Composable
private fun ProgressCircle(number: String, active: Boolean) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(
                color = if (active) QuestionGreen else Color(0xFF9E9E9E),
                shape = RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            color = Color.White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RowScope.ProgressConnector(active: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(2.dp)
            .background(if (active) QuestionGreen else Color(0x80545454))
    )
}

@Composable
private fun MultipleChoiceCard(
    question: SurveyQuestionDraft,
    onRemove: () -> Unit
) {
    QuestionCardContainer(height = 200.dp) {
        QuestionCardHeader(
            title = question.title,
            typeLabel = "객관식 질문"
        )
        question.options.forEachIndexed { index, option ->
            ChoiceOptionRow(
                option = option,
                selected = index == 0,
                onRemove = {
                    if (question.options.size > 1) question.options.removeAt(index)
                }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AddCircleOutline,
                contentDescription = "선택지 추가",
                tint = QuestionGreen,
                modifier = Modifier
                    .size(30.dp)
                    .clickable { question.options.add("정답 옵션 ${question.options.size + 1}") }
            )
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "질문 삭제",
                tint = QuestionGreen,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp)
                    .size(30.dp)
                    .clickable(onClick = onRemove)
            )
        }
    }
}

@Composable
private fun ChoiceOptionRow(
    option: String,
    selected: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (selected) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
            contentDescription = "정답 선택",
            tint = if (selected) QuestionGreen else Color(0xFF606060),
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = option,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            color = if (selected) QuestionGreen else Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Icon(
            imageVector = Icons.Outlined.RemoveCircleOutline,
            contentDescription = "선택지 삭제",
            tint = Color.Black,
            modifier = Modifier
                .size(30.dp)
                .clickable(onClick = onRemove)
        )
        Spacer(modifier = Modifier.width(10.dp))
    }
}

@Composable
private fun ShortAnswerCard(
    question: SurveyQuestionDraft,
    onRemove: () -> Unit
) {
    QuestionCardContainer(height = 150.dp) {
        QuestionCardHeader(
            title = question.title,
            typeLabel = "단답형 질문"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "단답형 대답",
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Outlined.AddCircleOutline,
                contentDescription = "답변 추가",
                tint = QuestionGreen,
                modifier = Modifier.size(30.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "질문 삭제",
                tint = QuestionGreen,
                modifier = Modifier
                    .padding(end = 20.dp)
                    .size(30.dp)
                    .clickable(onClick = onRemove)
            )
        }
    }
}

@Composable
private fun QuestionCardHeader(
    title: String,
    typeLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(start = 20.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.Black,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .border(1.dp, QuestionBorder, RoundedCornerShape(20.dp))
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = typeLabel,
                color = Color.Black,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun QuestionCardContainer(
    height: androidx.compose.ui.unit.Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .border(1.dp, QuestionBorder, RoundedCornerShape(20.dp))
            .background(QuestionSurface, RoundedCornerShape(20.dp)),
        content = content
    )
}

@Composable
private fun NewQuestionButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(1.dp, QuestionBorder, RoundedCornerShape(20.dp))
            .background(QuestionSurface, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.AddCircleOutline,
            contentDescription = "새 질문 생성",
            tint = Color.Black,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = "새로운 질문 생성",
            modifier = Modifier.padding(start = 20.dp),
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
