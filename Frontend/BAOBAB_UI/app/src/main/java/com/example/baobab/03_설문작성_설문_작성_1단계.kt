package com.example.baobab

import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.outlined.ExpandMore
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
private val SurveyDateFormat = DateTimeFormatter.ofPattern("uuuu. MM. dd.")
    .withResolverStyle(java.time.format.ResolverStyle.STRICT)
internal fun parseSurveyDeadline(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim(), SurveyDateFormat) }.getOrNull()

internal fun isSurveyBasicsComplete(draft: SurveyDraftStepOne, today: LocalDate = LocalDate.now()): Boolean =
    draft.title.isNotBlank() && draft.category in SurveyCategories && draft.introduction.isNotBlank() &&
        draft.audience.isNotBlank() && parseSurveyDeadline(draft.deadline)?.let { !it.isBefore(today) } == true

@Composable
fun SurveyCreationStepOneScreen(
    onBackClick: () -> Unit = {},
    onNextClick: (SurveyDraftStepOne) -> Unit = {},
    state: SurveyCreationState = remember { SurveyCreationState() }, editing: Boolean = false
) {
    var title by state::title
    var category by state::category
    var introduction by state::introduction
    var audience by state::audience
    var deadline by state::deadline
    val draft = SurveyDraftStepOne(title, category, introduction, audience, deadline)
    val ready = isSurveyBasicsComplete(draft)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurveyFormBackground)
            .safeDrawingPadding().imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SurveyFormHeader(onBackClick, if (editing) "설문 수정하기" else "설문 작성하기")
            SurveyFormProgress(currentStep = 1)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 2.dp, bottom = 110.dp)
            ) {
                SurveyFieldSection(
                    number = 1,
                    label = "설문 제목",
                    value = title,
                    placeholder = "설문 제목을 입력하세요. (필수)",
                    onValueChange = { title = it }
                )
                Spacer(modifier = Modifier.height(24.dp))
                SurveyFormChoiceField("2. 모집 목적 (필수)", category,
                    SurveyCategories.map { it to it }) { category = it }
                Spacer(modifier = Modifier.height(24.dp))
                SurveyFieldSection(
                    number = 3,
                    label = "설문 소개 (필수)",
                    value = introduction,
                    placeholder = "예) 주민들이 원하는 동네 시설을 알아보기 위한 설문입니다. 설문의 목적과 내용을 소개해주세요.",
                    onValueChange = { introduction = it },
                    multiline = true
                )
                Spacer(modifier = Modifier.height(24.dp))
                AudienceField(audience, { audience = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(24.dp))
                SurveyDeadlineSection(
                    value = deadline,
                    onValueChange = { deadline = it }
                )
            }
        }

        Button(
            onClick = { if (isSurveyBasicsComplete(draft)) onNextClick(draft) },
            enabled = ready,
            colors = ButtonDefaults.buttonColors(containerColor = SurveyFormGreen),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 16.dp)
                .height(55.dp)
        ) {
            Text(
                text = if (ready) "다음 단계로" else "필수 정보를 모두 입력해주세요",
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
internal fun SurveyInput(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    multiline: Boolean = false,
    enabled: Boolean = true,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = if (multiline) 120.dp else 52.dp),
        singleLine = !multiline,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
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
internal fun SurveySelectionField(label: String, value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, enabled: Boolean = true) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(label, Modifier.padding(start = 4.dp, bottom = 11.dp), color = SurveyFormGreen,
            fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .background(SurveyFormField, RoundedCornerShape(10.dp))
            .border(1.dp, SurveyFormBorder, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(value, Modifier.weight(1f), fontSize = 14.sp)
            Icon(icon, label, tint = SurveyFormGreen)
        }
    }
}

@Composable
private fun SurveyDeadlineSection(value: String, onValueChange: (String) -> Unit) {
    val context = LocalContext.current
    SurveySelectionField("5. 설문 마감일 (필수)", value.ifBlank { "달력에서 날짜를 선택해주세요" },
        Icons.Outlined.EventAvailable, {
            val today = LocalDate.now()
            val initial = parseSurveyDeadline(value)?.takeIf { !it.isBefore(today) } ?: today
            DatePickerDialog(context, { _, year, month, day ->
                onValueChange(LocalDate.of(year, month + 1, day).format(SurveyDateFormat))
            }, initial.year, initial.monthValue - 1, initial.dayOfMonth).apply {
                datePicker.minDate = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.show()
        })
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SurveyCreationStepOnePreview() {
    SurveyCreationStepOneScreen()
}
