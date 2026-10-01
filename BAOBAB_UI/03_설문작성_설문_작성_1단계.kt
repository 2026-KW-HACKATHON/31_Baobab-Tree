import android.view.View
import android.webkit.WebView
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val SurveyStepBackground = Color(0xFFFDF9F1)
private val SurveyQuestionBackground = Color(0xFFF4F1E9)
private val SurveyStepGreen = Color(0xFF2F5539)
private val SurveyStepMuted = Color(0xFF545454)
private val SurveyStepBorder = Color(0x80545454)
private val SurveyStepInter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Bold)
)

private data class SurveyQuestionOption(
    val label: String,
    val selectedIcon: String = "survey_checkbox_checked.svg",
    val unselectedIcon: String = "survey_checkbox_unchecked.svg"
)

@Composable
fun SurveyParticipationStep1Screen(
    onBackClick: () -> Unit = {},
    onNextClick: () -> Unit = {}
) {
    var studentAnswer by remember { mutableStateOf("예") }
    var restaurantAnswer by remember { mutableStateOf("예") }
    var selectedReasons by remember { mutableStateOf(setOf("기타")) }
    var otherAnswer by remember {
        mutableStateOf("식당의 운영시간이 짧아 이용하고 싶은 시간대에 방문하기 어렵습니다.")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurveyStepBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(55.dp))
            SurveyStepHeader(onBackClick = onBackClick)
            SurveyStepProgress()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(603.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SingleChoiceQuestionCard(
                    title = "광운대 학생이십니까?",
                    selectedAnswer = studentAnswer,
                    onAnswerSelected = { studentAnswer = it }
                )
                SingleChoiceQuestionCard(
                    title = "바오밥식당 방문 경험이 있으십니까?",
                    selectedAnswer = restaurantAnswer,
                    onAnswerSelected = { restaurantAnswer = it }
                )
                MultipleChoiceQuestionCard(
                    selectedOptions = selectedReasons,
                    onOptionsChanged = { selectedReasons = it }
                )
                ShortAnswerQuestionCard(
                    value = otherAnswer,
                    onValueChange = { otherAnswer = it }
                )
            }
        }

        SurveyStepNextButton(
            onClick = onNextClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SurveyStepHeader(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .size(40.dp)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            SvgAsset(
                assetName = "survey_step_back.svg",
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            text = "설문 참여하기",
            modifier = Modifier.align(Alignment.Center),
            color = Color.Black,
            fontFamily = SurveyStepInter,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 28.sp
        )
    }
}

@Composable
private fun SurveyStepProgress() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(81.dp)
            .padding(horizontal = 42.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepNumber(number = "1", active = true)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(SurveyStepBorder)
        )
        StepNumber(number = "2", active = false)
    }
}

@Composable
private fun StepNumber(number: String, active: Boolean) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) SurveyStepGreen else Color(0xFFA8A8A8)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            color = Color.White,
            fontFamily = SurveyStepInter,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 28.sp
        )
    }
}

@Composable
private fun SingleChoiceQuestionCard(
    title: String,
    selectedAnswer: String,
    onAnswerSelected: (String) -> Unit
) {
    QuestionCard {
        QuestionTitle(title)
        SurveyQuestionOption(label = "예").let { option ->
            QuestionOptionRow(
                option = option,
                selected = selectedAnswer == option.label,
                unselectedLabelColor = Color.Black,
                onClick = { onAnswerSelected(option.label) }
            )
        }
        SurveyQuestionOption(label = "아니오").let { option ->
            QuestionOptionRow(
                option = option,
                selected = selectedAnswer == option.label,
                unselectedLabelColor = Color.Black,
                onClick = { onAnswerSelected(option.label) }
            )
        }
        RequiredQuestionNotice(assetName = "survey_required_first.svg")
    }
}

@Composable
private fun MultipleChoiceQuestionCard(
    selectedOptions: Set<String>,
    onOptionsChanged: (Set<String>) -> Unit
) {
    val options = listOf(
        "메뉴의 다양성 및 매력 부족",
        "가격 대비 만족도 부족",
        "식당 위치 및 접근성 불편",
        "홍보 및 식당 정보 부족",
        "기타"
    )

    QuestionCard {
        QuestionTitle("바오밥식당의 방문율이 낮은 주요 원인은 무엇이라고 생각하시나요?")
        options.forEach { label ->
            val option = SurveyQuestionOption(label = label)
            val selected = label in selectedOptions
            QuestionOptionRow(
                option = option,
                selected = selected,
                unselectedLabelColor = SurveyStepMuted,
                onClick = {
                    onOptionsChanged(
                        if (selected) selectedOptions - label else selectedOptions + label
                    )
                }
            )
        }
        RequiredQuestionNotice(assetName = "survey_required.svg")
    }
}

@Composable
private fun QuestionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurveyQuestionBackground)
            .border(1.dp, SurveyStepBorder, RoundedCornerShape(20.dp))
    ) {
        content()
    }
}

@Composable
private fun QuestionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        color = Color.Black,
        fontFamily = SurveyStepInter,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 19.sp
    )
}

@Composable
private fun QuestionOptionRow(
    option: SurveyQuestionOption,
    selected: Boolean,
    unselectedLabelColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(onClick = onClick)
            .padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SvgAsset(
            assetName = if (selected) option.selectedIcon else option.unselectedIcon,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = option.label,
            modifier = Modifier.padding(start = 10.dp),
            color = if (selected) SurveyStepGreen else unselectedLabelColor,
            fontFamily = SurveyStepInter,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun RequiredQuestionNotice(assetName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SvgAsset(
            assetName = assetName,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = "필수 질문 입니다",
            modifier = Modifier.padding(start = 4.dp),
            color = Color.Red,
            fontFamily = SurveyStepInter,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun ShortAnswerQuestionCard(
    value: String,
    onValueChange: (String) -> Unit
) {
    QuestionCard {
        QuestionTitle("‘기타’를 선택하신 경우, 그 이유를 구체적으로 작성해 주세요.")
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            textStyle = TextStyle(
                color = Color.Black,
                fontFamily = SurveyStepInter,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 21.sp
            )
        )
    }
}

@Composable
private fun SurveyStepNextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 7.dp)
            .height(80.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(55.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(SurveyStepGreen)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "다음 페이지",
                color = Color.White,
                fontFamily = SurveyStepInter,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp
            )
        }
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
private fun SurveyParticipationStep1Preview() {
    SurveyParticipationStep1Screen()
}
