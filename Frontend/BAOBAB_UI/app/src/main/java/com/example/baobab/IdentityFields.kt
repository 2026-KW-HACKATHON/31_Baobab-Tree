package com.example.baobab

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.ui.unit.sp

val MemberTypes = linkedMapOf("KW_STUDENT" to "광운대학교 학생", "LOCAL_GOVERNMENT" to "지자체",
    "WOLGYE_RESIDENT" to "월계1동 주민", "OTHER" to "기타")
val AudienceChoices = listOf("광운대학교 학생", "월계1동 주민", "기타")

@Composable
fun ChoiceField(
    label: String,
    value: String,
    choices: List<String>,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    val green = androidx.compose.ui.graphics.Color(0xFF2F5539)
    val borderColor = androidx.compose.ui.graphics.Color(0xFFD9E0D5)
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { expanded = !expanded },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = shape,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                borderColor
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = androidx.compose.ui.graphics.Color.White,
                contentColor = androidx.compose.ui.graphics.Color.Black,
                disabledContainerColor = androidx.compose.ui.graphics.Color.White
            ),
            contentPadding = PaddingValues(14.dp)
        ) {
            Text(
                text = value.ifBlank { label },
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
            )

            Icon(
                imageVector = if (expanded) {
                    Icons.Outlined.KeyboardArrowUp
                } else {
                    Icons.Outlined.KeyboardArrowDown
                },
                contentDescription = if (expanded) {
                    "목록 접기"
                } else {
                    "목록 펼치기"
                },
                tint = green
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = expanded && enabled,
            enter = androidx.compose.animation.expandVertically(
                expandFrom = Alignment.Top
            ),
            exit = androidx.compose.animation.shrinkVertically(
                shrinkTowards = Alignment.Top
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = shape,
                color = androidx.compose.ui.graphics.Color.White,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    borderColor
                )
            ) {
                Column(Modifier.fillMaxWidth()) {
                    choices.forEachIndexed { index, option ->
                        TextButton(
                            onClick = {
                                onChange(option)
                                expanded = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                0.dp
                            ),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = green
                            )
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (index < choices.lastIndex) {
                            HorizontalDivider(color = borderColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudienceField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var otherSelected by rememberSaveable { mutableStateOf(value.isNotBlank() && value !in AudienceChoices.take(2)) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "4. 설문 대상 (필수)",
            modifier = Modifier.padding(start = 4.dp, bottom = 3.dp),
            color = androidx.compose.ui.graphics.Color(0xFF2F5539),
            fontSize = 16.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        ChoiceField("대상을 선택해주세요", if (otherSelected) "기타" else value, AudienceChoices, {
            otherSelected = it == "기타"
            onChange(if (otherSelected) "" else it)
        })
        if (otherSelected) {
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                placeholder = {
                    Text(
                        text = "기타 대상을 입력해주세요. (필수)",
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = androidx.compose.ui.graphics.Color.White,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.White,
                    focusedBorderColor = androidx.compose.ui.graphics.Color(0xFF2F5539),
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFFD9E0D5),
                    focusedTextColor = androidx.compose.ui.graphics.Color.Black,
                    unfocusedTextColor = androidx.compose.ui.graphics.Color.Black,
                    focusedPlaceholderColor = androidx.compose.ui.graphics.Color(0xFF6F786E),
                    unfocusedPlaceholderColor = androidx.compose.ui.graphics.Color(0xFF6F786E)
                )
            )
        }
    }
}

@Composable
fun EmailField(local: String, onLocal: (String) -> Unit, domain: String, onDomain: (String) -> Unit,
    enabled: Boolean) {
    var selected by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("이메일 (필수)")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(local, { onLocal(it.filterNot { char -> char.isWhitespace() || char == '@' }) },
                label = { Text("이메일 아이디") }, singleLine = true, enabled = enabled, modifier = Modifier.weight(1f))
            Text("@")
            ChoiceField("도메인 선택", selected, listOf("kw.ac.kr", "naver.com", "gmail.com", "daum.net", "kakao.com", "직접 입력"), {
                selected = it; onDomain(if (it == "직접 입력") "" else it)
            }, Modifier.weight(1f), enabled)
        }
        if (selected == "직접 입력") OutlinedTextField(domain,
            { onDomain(it.filterNot { char -> char.isWhitespace() || char == '@' }) }, label = { Text("도메인 직접 입력") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = enabled)
    }
}
