package com.example.baobab

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

val MemberTypes = linkedMapOf("KW_STUDENT" to "광운대학교 학생", "LOCAL_GOVERNMENT" to "지자체",
    "WOLGYE_RESIDENT" to "월계1동 주민", "OTHER" to "기타")
val AudienceChoices = listOf("광운대학교 학생", "월계1동 주민", "기타")

@Composable
fun ChoiceField(label: String, value: String, choices: List<String>, onChange: (String) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(value.ifBlank { label }, Modifier.weight(1f))
            Text("▾")
        }
        DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            choices.forEach { option -> DropdownMenuItem(text = { Text(option) },
                onClick = { onChange(option); expanded = false }) }
        }
    }
}

@Composable
fun AudienceField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var otherSelected by rememberSaveable { mutableStateOf(value.isNotBlank() && value !in AudienceChoices.take(2)) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("설문 대상 (필수)")
        ChoiceField("대상을 선택해주세요", if (otherSelected) "기타" else value, AudienceChoices, {
            otherSelected = it == "기타"
            onChange(if (otherSelected) "" else it)
        })
        if (otherSelected) OutlinedTextField(value, onChange, label = { Text("기타 대상 직접 입력 (필수)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
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
