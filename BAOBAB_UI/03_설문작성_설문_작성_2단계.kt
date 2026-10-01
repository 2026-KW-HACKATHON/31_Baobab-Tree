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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val StepTwoBackground = Color(0xFFFDF9F1)
private val StepTwoCardBackground = Color(0xFFF4F1E9)
private val StepTwoGreen = Color(0xFF2F5539)
private val StepTwoBorder = Color(0x80545454)
private val StepTwoMuted = Color(0xFF545454)
private val StepTwoInter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Bold)
)

private val StepTwoAnswers = listOf(
    "매우그렇다",
    "그렇다",
    "보통이다",
    "그렇지 않다",
    "전혀 그렇지 않다"
)

@Composable
fun SurveyParticipationStep2Screen(
    onBackClick: () -> Unit = {},
    onCompleteClick: () -> Unit = {}
) {
    var selectedAnswer by remember { mutableStateOf("매우그렇다") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StepTwoBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(55.dp))
            StepTwoHeader(onBackClick = onBackClick)
            StepTwoProgress()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(603.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StepTwoQuestionCard(
                    selectedAnswer = selectedAnswer,
                    onAnswerSelected = { selectedAnswer = it }
                )
            }
        }
        StepTwoCompleteButton(
            onClick = onCompleteClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun StepTwoHeader(onBackClick: () -> Unit) {
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
            StepTwoSvg(
                assetName = "survey_step2_back.svg",
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            text = "설문 참여하기",
            modifier = Modifier.align(Alignment.Center),
            color = Color.Black,
            fontFamily = StepTwoInter,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 28.sp
        )
    }
}

@Composable
private fun StepTwoProgress() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(81.dp)
            .padding(horizontal = 42.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepTwoNumber(number = "1")
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(StepTwoGreen)
        )
        StepTwoNumber(number = "2")
    }
}

@Composable
private fun StepTwoNumber(number: String) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(50))
            .background(StepTwoGreen),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            color = Color.White,
            fontFamily = StepTwoInter,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 28.sp
        )
    }
}

@Composable
private fun StepTwoQuestionCard(
    selectedAnswer: String,
    onAnswerSelected: (String) -> Unit
) {
    StepTwoCard {
        Text(
            text = "위와 같은 개선 사항이 반영된다면 바오밥식당을 방문할 의향이 있으신가요?",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            color = Color.Black,
            fontFamily = StepTwoInter,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 19.sp
        )
        StepTwoAnswers.forEach { answer ->
            val selected = selectedAnswer == answer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clickable { onAnswerSelected(answer) }
                    .padding(start = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepTwoSvg(
                    assetName = if (selected) {
                        "survey_step2_checked.svg"
                    } else {
                        "survey_step2_unchecked.svg"
                    },
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    text = answer,
                    modifier = Modifier.padding(start = 10.dp),
                    color = if (selected) StepTwoGreen else StepTwoMuted,
                    fontFamily = StepTwoInter,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 19.sp
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(start = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepTwoSvg(
                assetName = "survey_step2_required.svg",
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = "필수 질문 입니다",
                modifier = Modifier.padding(start = 4.dp),
                color = Color.Red,
                fontFamily = StepTwoInter,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun StepTwoCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(StepTwoCardBackground)
            .border(1.dp, StepTwoBorder, RoundedCornerShape(20.dp)),
        content = content
    )
}

@Composable
private fun StepTwoCompleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(85.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(StepTwoGreen)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "설문 참여 완료하기",
                    color = Color.White,
                    fontFamily = StepTwoInter,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StepTwoSvg(
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
private fun SurveyParticipationStep2Preview() {
    SurveyParticipationStep2Screen()
}
