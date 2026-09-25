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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CompletionBackground = Color(0xFFFDF9F1)
private val CompletionGreen = Color(0xFF2F5539)
private val CompletionGold = Color(0xFFFDC854)
private val CompletionBrown = Color(0xFF8C510A)
private val CompletionGray = Color(0xCC545454)

@Composable
fun SurveyCompletionScreen(
    onOtherSurveyClick: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CompletionBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(69.dp))
            CompletionImage()
            CompletionTitle()
            CompletionMessage()
            RewardCard()
        }

        CompletionActions(
            onOtherSurveyClick = onOtherSurveyClick,
            onHomeClick = onHomeClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun CompletionImage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(246.dp)
            .background(Color(0xFFE9E9E9)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = Color(0xFFB4B4B4),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "NO IMAGE",
                color = Color(0xFFB4B4B4),
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CompletionTitle() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "설문 참여가",
            color = Color.Black,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "완료되었습니다!",
            color = Color.Black,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CompletionMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "설문 참여 감사멘트",
            color = CompletionGray,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "소중한 의견을 남겨주셔서 감사합니다",
            color = CompletionGray,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RewardCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 43.dp)
            .border(3.dp, CompletionGold, RoundedCornerShape(15.dp))
            .clip(RoundedCornerShape(15.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier.size(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.StarBorder,
                        contentDescription = "Point 리워드",
                        tint = CompletionGold,
                        modifier = Modifier.size(35.dp)
                    )
                }
                Text(
                    text = "Point",
                    color = Color.Black,
                    fontSize = 35.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "리워드가 지급되었어요!",
                modifier = Modifier.padding(top = 1.dp),
                color = CompletionBrown,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CompletionActions(
    onOtherSurveyClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        CompletionButton(
            text = "다른 설문 참여하기",
            background = CompletionGreen,
            textColor = Color.White,
            onClick = onOtherSurveyClick
        )
        Spacer(modifier = Modifier.height(35.dp))
        CompletionButton(
            text = "홈 화면으로 돌아가기",
            background = Color.White,
            textColor = CompletionGreen,
            borderColor = CompletionGreen,
            onClick = onHomeClick
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun CompletionButton(
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
            .clip(RoundedCornerShape(40.dp))
            .then(
                if (borderColor != null) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(40.dp))
                } else {
                    Modifier
                }
            )
            .background(background)
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
