import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val StepThreeBackground = Color(0xFFFDF9F1)
private val StepThreeSurface = Color(0xFFF4F1E9)
private val StepThreeGreen = Color(0xFF2F5539)
private val StepThreeBorder = Color(0x80545454)

@Composable
fun SurveyCreationStepThreeScreen(
    onBackClick: () -> Unit = {},
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
                    .padding(horizontal = 16.dp, top = 20.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SectionLabel(number = 1, text = "리워드 지급")
                RewardSettingsCard(
                    rewardPerPerson = rewardPerPerson,
                    rewardRecipients = rewardRecipients,
                    onRewardPerPersonChange = { rewardPerPerson = it },
                    onRewardRecipientsChange = { rewardRecipients = it }
                )
                SectionLabel(number = 2, text = "소요 시간")
                DurationSelector(
                    selectedDuration = selectedDuration,
                    onDurationSelected = { selectedDuration = it }
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 30.dp)
                .height(55.dp)
                .background(StepThreeGreen, RoundedCornerShape(40.dp))
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
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
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
            fontSize = 23.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StepThreeProgress() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(81.dp)
            .padding(horizontal = 42.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProgressStepCircle(number = "1")
        ProgressLine()
        ProgressStepCircle(number = "2")
        ProgressLine()
        ProgressStepCircle(number = "3")
    }
}

@Composable
private fun ProgressStepCircle(number: String) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(StepThreeGreen, RoundedCornerShape(50)),
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
private fun ProgressLine() {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(2.dp)
            .background(StepThreeGreen)
    )
}

@Composable
private fun SectionLabel(number: Int, text: String) {
    Text(
        text = "$number. $text",
        modifier = Modifier.padding(start = 20.dp),
        color = StepThreeGreen,
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .border(1.dp, StepThreeBorder, RoundedCornerShape(20.dp))
            .background(StepThreeSurface, RoundedCornerShape(20.dp))
            .padding(vertical = 0.dp),
        verticalArrangement = Arrangement.Center
    ) {
        RewardRow(
            label = "1인당 지급할 리워드",
            value = rewardPerPerson,
            onValueChange = onRewardPerPersonChange
        )
        RewardRow(
            label = "리워드 지급 인원 수",
            value = rewardRecipients,
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(150.dp)
            )
            Text(
                text = "1000P",
                color = StepThreeGreen,
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(150.dp)
        )
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .width(if (value.length > 3) 84.dp else 75.dp)
                .height(40.dp),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
            .border(1.dp, StepThreeBorder, RoundedCornerShape(20.dp))
            .background(StepThreeSurface, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
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
private fun DurationOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(37.dp)
            .width(if (text.length > 4) 92.dp else 71.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) StepThreeGreen else StepThreeBorder,
                shape = RoundedCornerShape(40.dp)
            )
            .background(Color.White, RoundedCornerShape(40.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
