import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private val QuestionBackground = Color(0xFFFDF9F1)
private val QuestionSurface = Color(0xFFF4F1E9)
private val QuestionGreen = Color(0xFF2F5539)
private val QuestionBorder = Color(0x80545454)
private val QuestionMuted = Color(0xFF545454)
private val QuestionInter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Bold)
)

@Composable
fun SurveyCreationStepTwoScreen(
    onBackClick: () -> Unit = {},
    onCreatePageClick: () -> Unit = {},
    onNextClick: (List<SurveyQuestionDraft>) -> Unit = {}
) {
    val questions = remember {
        mutableStateListOf(
            SurveyQuestionDraft(
                type = SurveyQuestionType.MULTIPLE_CHOICE,
                options = listOf("정답 옵션 1", "정답 옵션 2")
            ),
            SurveyQuestionDraft(type = SurveyQuestionType.SHORT_ANSWER)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QuestionBackground)
    ) {
        Spacer(modifier = Modifier.height(54.dp))
        QuestionHeader(onBackClick = onBackClick)
        QuestionProgress()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, top = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(21.dp)
        ) {
            questions.forEach { question ->
                when (question.type) {
                    SurveyQuestionType.MULTIPLE_CHOICE -> MultipleChoiceCard(
                        question = question,
                        onTypeChange = { type ->
                            question.type = type
                            if (type == SurveyQuestionType.MULTIPLE_CHOICE && question.options.isEmpty()) {
                                question.options.addAll(listOf("정답 옵션 1", "정답 옵션 2"))
                            }
                        },
                        onRemove = { questions.remove(question) }
                    )
                    SurveyQuestionType.SHORT_ANSWER -> ShortAnswerCard(
                        question = question,
                        onTypeChange = { type ->
                            question.type = type
                            if (type == SurveyQuestionType.MULTIPLE_CHOICE && question.options.isEmpty()) {
                                question.options.addAll(listOf("정답 옵션 1", "정답 옵션 2"))
                            }
                        },
                        onAddQuestion = {
                            questions.add(SurveyQuestionDraft(type = question.type))
                        },
                        onRemove = { questions.remove(question) }
                    )
                }
            }
            NewQuestionButton(
                onClick = {
                    questions.add(SurveyQuestionDraft(type = SurveyQuestionType.SHORT_ANSWER))
                }
            )
        }
        QuestionBottomActions(
            onCreatePageClick = onCreatePageClick,
            onNextClick = { onNextClick(questions.toList()) }
        )
    }
}

class SurveyQuestionDraft(
    type: SurveyQuestionType,
    title: String = "제목 없는 설문지",
    options: List<String> = emptyList()
) {
    var type by mutableStateOf(type)
    var title by mutableStateOf(title)
    var required by mutableStateOf(true)
    var selectedOptionIndex by mutableIntStateOf(0)
    val options = mutableStateListOf<String>().apply { addAll(options) }
}

enum class SurveyQuestionType {
    MULTIPLE_CHOICE,
    SHORT_ANSWER
}

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
            fontFamily = QuestionInter,
            fontSize = 23.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun QuestionProgress() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(81.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 72.dp, top = 34.dp)
                .width(114.dp)
                .height(2.dp)
                .background(QuestionGreen)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 216.dp, top = 34.dp)
                .width(114.dp)
                .height(2.dp)
                .background(QuestionBorder)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 27.dp, top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ProgressStep(number = "1", label = "기본 정보", active = true)
            ProgressStep(number = "2", label = "문항 작성", active = true)
            ProgressStep(number = "3", label = "설문 설정")
        }
    }
}

@Composable
private fun ProgressStep(number: String, label: String, active: Boolean = false) {
    Column(
        modifier = Modifier.width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(50))
                .background(if (active) QuestionGreen else Color(0xFFA8A8A8)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontFamily = QuestionInter,
                fontSize = 23.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = label,
            modifier = Modifier.padding(top = 4.dp),
            color = if (active) QuestionGreen else Color(0xFFA8A8A8),
            fontFamily = QuestionInter,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun MultipleChoiceCard(
    question: SurveyQuestionDraft,
    onTypeChange: (SurveyQuestionType) -> Unit,
    onRemove: () -> Unit
) {
    QuestionCardContainer(height = (102 + question.options.size * 50).dp) {
        QuestionCardHeader(question = question, onTypeChange = onTypeChange)
        question.options.forEachIndexed { index, _ ->
            ChoiceOptionRow(
                question = question,
                index = index,
                onRemove = {
                    question.options.removeAt(index)
                    if (question.selectedOptionIndex >= question.options.size) {
                        question.selectedOptionIndex = (question.options.size - 1).coerceAtLeast(0)
                    }
                }
            )
        }
        QuestionCardControls(
            question = question,
            onAddOption = {
                question.options.add("정답 옵션 ${question.options.size + 1}")
            },
            onRemoveQuestion = onRemove
        )
    }
}

@Composable
private fun ChoiceOptionRow(
    question: SurveyQuestionDraft,
    index: Int,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(start = 13.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (question.selectedOptionIndex == index) {
                Icons.Outlined.CheckBox
            } else {
                Icons.Outlined.CheckBoxOutlineBlank
            },
            contentDescription = "정답 선택",
            tint = if (question.selectedOptionIndex == index) QuestionGreen else QuestionMuted,
            modifier = Modifier
                .size(26.dp)
                .clickable { question.selectedOptionIndex = index }
        )
        BasicTextField(
            value = question.options[index],
            onValueChange = { question.options[index] = it },
            modifier = Modifier
                .weight(1f)
                .padding(start = 11.dp),
            singleLine = true,
            textStyle = TextStyle(
                color = if (question.selectedOptionIndex == index) QuestionGreen else Color.Black,
                fontFamily = QuestionInter,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Icon(
            imageVector = Icons.Outlined.RemoveCircleOutline,
            contentDescription = "선택지 삭제",
            tint = Color.Black,
            modifier = Modifier
                .size(30.dp)
                .clickable(onClick = onRemove)
        )
    }
}

@Composable
private fun ShortAnswerCard(
    question: SurveyQuestionDraft,
    onAddQuestion: () -> Unit,
    onTypeChange: (SurveyQuestionType) -> Unit,
    onRemove: () -> Unit
) {
    QuestionCardContainer(height = 152.dp) {
        QuestionCardHeader(question = question, onTypeChange = onTypeChange)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(start = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "단답형 대답",
                color = Color.Black,
                fontFamily = QuestionInter,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        QuestionCardControls(
            question = question,
            onAddOption = onAddQuestion,
            onRemoveQuestion = onRemove
        )
    }
}

@Composable
private fun QuestionCardHeader(
    question: SurveyQuestionDraft,
    onTypeChange: (SurveyQuestionType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(start = 20.dp, end = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = question.title,
            onValueChange = { question.title = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(
                color = Color.Black,
                fontFamily = QuestionInter,
                fontSize = 16.sp
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        QuestionTypeMenu(
            selectedType = question.type,
            onTypeChange = onTypeChange
        )
    }
}

@Composable
private fun QuestionTypeMenu(
    selectedType: SurveyQuestionType,
    onTypeChange: (SurveyQuestionType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (selectedType == SurveyQuestionType.MULTIPLE_CHOICE) "객관식 질문" else "단답형 질문"

    Box {
        Box(
            modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFFFEFA))
                .border(1.dp, QuestionBorder, RoundedCornerShape(20.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color.Black,
                fontFamily = QuestionInter,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("객관식 질문") },
                onClick = {
                    expanded = false
                    onTypeChange(SurveyQuestionType.MULTIPLE_CHOICE)
                }
            )
            DropdownMenuItem(
                text = { Text("단답형 질문") },
                onClick = {
                    expanded = false
                    onTypeChange(SurveyQuestionType.SHORT_ANSWER)
                }
            )
        }
    }
}

@Composable
private fun QuestionCardControls(
    question: SurveyQuestionDraft,
    onAddOption: () -> Unit,
    onRemoveQuestion: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().height(50.dp)) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "필수 질문",
                color = Color.Black,
                fontFamily = QuestionInter,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(10.dp))
            RequiredToggle(
                checked = question.required,
                onCheckedChange = { question.required = it }
            )
        }
        Icon(
            imageVector = Icons.Outlined.AddCircleOutline,
            contentDescription = if (question.type == SurveyQuestionType.MULTIPLE_CHOICE) "선택지 추가" else "항목 추가",
            tint = QuestionGreen,
            modifier = Modifier
                .align(Alignment.Center)
                .size(28.dp)
                .clickable(onClick = onAddOption)
        )
        Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = "질문 삭제",
            tint = QuestionGreen,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp)
                .size(30.dp)
                .clickable(onClick = onRemoveQuestion)
        )
    }
}

@Composable
private fun RequiredToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .width(52.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFFEFA))
            .border(1.dp, if (checked) QuestionGreen else QuestionMuted, RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = if (checked) "ON" else "OFF",
            modifier = Modifier.padding(start = 6.dp),
            color = Color.Black,
            fontFamily = QuestionInter,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 2.dp)
                .size(22.dp)
                .clip(RoundedCornerShape(50))
                .background(if (checked) QuestionGreen else QuestionMuted)
        )
    }
}

@Composable
private fun QuestionCardContainer(
    height: Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .background(QuestionSurface)
            .border(1.dp, QuestionBorder, RoundedCornerShape(20.dp)),
        content = content
    )
}

@Composable
private fun NewQuestionButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(QuestionBackground)
            .border(1.dp, QuestionBorder, RoundedCornerShape(20.dp))
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
            fontFamily = QuestionInter,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun QuestionBottomActions(
    onCreatePageClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(94.dp)
                .padding(horizontal = 20.dp)
        ) {
            Box(modifier = Modifier.width(182.dp).height(94.dp)) {
                ActionButton(
                    text = "다음 페이지 생성",
                    outlined = true,
                    onClick = onCreatePageClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 20.dp)
                        .fillMaxWidth()
                        .height(59.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.width(170.dp).height(94.dp)) {
                ActionButton(
                    text = "다음 단계로",
                    outlined = false,
                    onClick = onNextClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 22.dp)
                        .fillMaxWidth()
                        .height(55.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun ActionButton(
    text: String,
    outlined: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(40.dp))
            .background(if (outlined) QuestionBackground else QuestionGreen)
            .border(1.dp, QuestionGreen, RoundedCornerShape(40.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (outlined) QuestionGreen else Color.White,
            fontFamily = QuestionInter,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SurveyCreationStepTwoPreview() {
    SurveyCreationStepTwoScreen()
}
