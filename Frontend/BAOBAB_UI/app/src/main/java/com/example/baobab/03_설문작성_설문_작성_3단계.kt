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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.TextButton
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
private val StepThreeSurface = Color.White
private val StepThreeGreen = Color(0xFF2F5539)
private val StepThreeBorder = Color(0xFFD9E0D5)
private val StepThreeInter = FontFamily.SansSerif

@Composable
fun SurveyCreationStepThreeScreen(
    onBackClick: () -> Unit = {},
    onImageAttachClick: () -> Unit = {},
    onCompleteClick: (SurveySettingsDraft) -> Unit = {},
    submitting: Boolean = false,
    error: String? = null,
    currentPoint: Int? = null,
    state: SurveyCreationState = remember { SurveyCreationState() }
) {
    var rewardPerPerson by state::rewardPerPerson
    var rewardRecipients by state::rewardRecipients
    var selectedDuration by state::selectedDuration
    val context = LocalContext.current
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var imageLoading by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { imageLoading = true; selectedImage = uri }
    }
    LaunchedEffect(selectedImage) {
        val uri = selectedImage ?: return@LaunchedEffect
        imageError = null
        val result = withContext(Dispatchers.IO) { runCatching { readSurveyImage(context, uri) } }
        result.fold({ state.imageData = it }, { imageError = it.message ?: "이미지를 불러오지 못했습니다." })
        imageLoading = false
        selectedImage = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StepThreeBackground)
            .safeDrawingPadding().imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SurveyFormHeader(onBackClick = onBackClick)
            SurveyFormProgress(currentStep = 3)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 20.dp,
                        bottom = 120.dp
                    )
            ) {
                error?.let { Text(it, color = Color.Red) }
                SectionLabel(number = 1, text = "리워드 지급")
                Spacer(modifier = Modifier.height(14.dp))
                RewardSettingsCard(
                    rewardPerPerson = rewardPerPerson,
                    rewardRecipients = rewardRecipients,
                    onRewardPerPersonChange = { rewardPerPerson = it },
                    onRewardRecipientsChange = { rewardRecipients = it }
                )
                Text("보유 포인트: ${currentPoint?.let { "%,d P".format(it) } ?: "확인 중"}",
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp), color = StepThreeGreen)
                Text("등록 시 1인당 리워드 × 지급 인원만큼 포인트가 차감됩니다.",
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp), fontSize = 13.sp, color = Color(0xFF697369))
                Spacer(modifier = Modifier.height(14.dp))
                TimeSectionLabel()
                Spacer(modifier = Modifier.height(14.dp))
                DurationSelector(
                    selectedDuration = selectedDuration,
                    onDurationSelected = { selectedDuration = it }
                )
                Spacer(modifier = Modifier.height(14.dp))
                ImageAttachmentSection(onImageAttachClick = {
                    if (!imageLoading && !submitting) {
                        onImageAttachClick()
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                })
                if (imageLoading) Text("사진을 준비하고 있어요…", color = StepThreeGreen)
                imageError?.let { Text(it, color = Color.Red) }
                state.imageData?.let {
                    Spacer(Modifier.height(12.dp))
                    SurveyImage(SurveyItem(category = state.category, imageData = it),
                        Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)))
                    TextButton(onClick = { state.imageData = null }, enabled = !submitting && !imageLoading) { Text("사진 제거") }
                }
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
                    .clip(RoundedCornerShape(16.dp))
                    .background(StepThreeGreen)
                    .clickable(enabled = !submitting && !imageLoading) {
                        onCompleteClick(
                            SurveySettingsDraft(
                                rewardPerPerson = rewardPerPerson,
                                rewardRecipients = rewardRecipients,
                                duration = selectedDuration,
                                imageData = state.imageData
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (submitting) "등록 중..." else "설문 작성 완료",
                    color = Color.White,
                    fontFamily = StepThreeInter,
                    fontSize = 17.sp,
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
    val duration: String,
    val imageData: String? = null
)

@Composable
private fun SectionLabel(number: Int, text: String) {
    Text(
        text = "$number. $text",
        modifier = Modifier
            .heightIn(min = 24.dp)
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
    val points = (rewardPerPerson.filter(Char::isDigit).toLongOrNull() ?: 0L) *
        (rewardRecipients.filter(Char::isDigit).toLongOrNull() ?: 0L)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(StepThreeSurface)
            .border(1.dp, StepThreeBorder, RoundedCornerShape(20.dp))
    ) {
        RewardRow(
            label = "1인당 지급할 리워드",
            value = rewardPerPerson,
            unit = "P",
            onValueChange = onRewardPerPersonChange
        )
        RewardRow(
            label = "리워드 지급 인원 수",
            value = rewardRecipients,
            unit = "명",
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
                modifier = Modifier.weight(1f)
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
    unit: String,
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
            modifier = Modifier.weight(1f)
        )
        BasicTextField(
            value = value.filter(Char::isDigit),
            onValueChange = { entered -> onValueChange(entered.filter { it in '0'..'9' }.take(9)) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            modifier = Modifier.width(92.dp)
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
        Text(unit, modifier = Modifier.width(28.dp).padding(start = 8.dp),
            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StepThreeGreen)
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
    SectionLabel(number = 2, text = "소요 시간")
}

@Composable
private fun ImageAttachmentSection(onImageAttachClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(number = 3, text = "이미지 첨부")
        Box(
            modifier = Modifier.fillMaxWidth().height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, StepThreeBorder, RoundedCornerShape(12.dp))
                .clickable(onClick = onImageAttachClick),
            contentAlignment = Alignment.Center
        ) {
            Text("파일 첨부", color = StepThreeGreen, fontSize = 15.sp,
                fontFamily = StepThreeInter, fontWeight = FontWeight.Bold)
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
                shape = RoundedCornerShape(16.dp)
            )
            .background(if (selected) StepThreeGreen else Color.White, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else StepThreeGreen,
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
