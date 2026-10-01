import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private val CreationCompleteBackground = Color(0xFFFDF9F1)
private val CreationCompleteGreen = Color(0xFF2F5539)
private val CreationCompleteGray = Color(0xCC545454)
private val CreationCompleteInter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Bold)
)

@Composable
fun SurveyCreationCompleteScreen(
    onHomeClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreationCompleteBackground)
    ) {
        Image(
            painter = painterResource(R.drawable.survey_creation_complete),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 108.dp)
                .size(370.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 478.dp)
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "설문이 등록되었어요!",
                color = Color.Black,
                fontFamily = CreationCompleteInter,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 558.dp)
                .fillMaxWidth()
                .height(90.dp)
                .padding(top = 10.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Text(
                text = "다양한 사람들의 소중한 의견이",
                color = CreationCompleteGray,
                fontFamily = CreationCompleteInter,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "모이길 기다려주세요.",
                color = CreationCompleteGray,
                fontFamily = CreationCompleteInter,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 45.dp)
                .height(55.dp)
                .background(CreationCompleteGreen, RoundedCornerShape(40.dp))
                .clickable(onClick = onHomeClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "홈 화면으로 돌아가기",
                color = Color.White,
                fontFamily = CreationCompleteInter,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SurveyCreationCompletePreview() {
    SurveyCreationCompleteScreen()
}
