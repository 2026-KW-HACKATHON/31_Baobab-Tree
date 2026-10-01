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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.EventAvailable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SurveyFormBackground = Color(0xFFFDF9F1)
private val SurveyFormGreen = Color(0xFF2F5539)
private val SurveyFormField = Color(0xFFF4F1E9)
private val SurveyFormBorder = Color(0x80545454)
private val SurveyFormPlaceholder = Color(0x80545454)
private val SurveyFormInter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Bold)
)

@Composable
fun SurveyCreationStepOneScreen(
    onBackClick: () -> Unit = {},
    onNextClick: (SurveyDraftStepOne) -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var introduction by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("2026. 10. 08.") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurveyFormBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(54.dp))
            SurveyCreationHeader(onBackClick = onBackClick)
            SurveyCreationProgress()
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
                Spacer(modifier = Modifier.height(45.dp))
                SurveyFieldSection(
                    number = 2,
                    label = "카테고리",
                    value = category,
                    placeholder = "카테고리를 선택하십시오. (필수)",
                    onValueChange = { category = it }
                )
                Spacer(modifier = Modifier.height(45.dp))
                SurveyFieldSection(
                    number = 3,
                    label = "설문 소개",
                    value = introduction,
                    placeholder = "카테고리를 선택하십시오. (필수)",
                    onValueChange = { introduction = it },
                    multiline = true
                )
                Spacer(modifier = Modifier.height(45.dp))
                SurveyFieldSection(
                    number = 4,
                    label = "설문 대상",
                    value = audience,
                    placeholder = "예) 월계 1동 주민, 광운대학생 등",
                    onValueChange = { audience = it }
                )
                Spacer(modifier = Modifier.height(31.dp))
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
                .padding(bottom = 45.dp)
                .height(55.dp)
                .background(SurveyFormGreen, RoundedCornerShape(40.dp))
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
                fontSize = 20.sp,
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
private fun SurveyCreationHeader(onBackClick: () -> Unit) {
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
            fontFamily = SurveyFormInter,
            fontSize = 23.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SurveyCreationProgress() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(81.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 57.dp)
                .fillMaxWidth()
                .padding(top = 34.dp)
                .height(2.dp)
                .background(Color(0x80545454))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 27.dp, top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ProgressStep(number = "1", label = "기본 정보", active = true)
            ProgressStep(number = "2", label = "문항 작성")
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
                .background(
                    color = if (active) SurveyFormGreen else Color(0xFFA8A8A8),
                    shape = RoundedCornerShape(50)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontFamily = SurveyFormInter,
                fontSize = 23.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = label,
            modifier = Modifier.padding(top = 4.dp),
            color = if (active) SurveyFormGreen else Color(0xFF9E9E9E),
            fontFamily = SurveyFormInter,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

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
            .height(if (multiline) 100.dp else 40.dp),
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
                    .padding(horizontal = 9.dp, vertical = if (multiline) 9.dp else 0.dp),
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
