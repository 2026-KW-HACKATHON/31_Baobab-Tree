package com.example.baobab

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SurveyFormChoiceField(label: String, value: String, options: List<Pair<String, String>>,
    busy: Boolean = false, onChange: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        SurveySelectionField(label, options.firstOrNull { it.first == value }?.second
            ?: if (label.contains("모집 목적")) "모집 목적을 선택해주세요" else "선택해주세요",
            Icons.Outlined.ExpandMore, { expanded = !expanded }, enabled = !busy)
        AnimatedVisibility(visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 6.dp)
                .background(Color.White, RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFFD9E0D5), RoundedCornerShape(10.dp))) {
                options.forEachIndexed { index, (key, text) ->
                    Row(Modifier.fillMaxWidth().background(if (value == key) Color(0xFFEAF0E4) else Color.Transparent)
                        .clickable(enabled = !busy) { onChange(key); expanded = false }
                        .padding(horizontal = 14.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text, color = Color(0xFF2F5539), fontSize = 14.sp)
                        if (value == key) Text("✓", color = Color(0xFF2F5539))
                    }
                    if (index < options.lastIndex) HorizontalDivider(color = Color(0xFFD9E0D5))
                }
            }
        }
    }
}
