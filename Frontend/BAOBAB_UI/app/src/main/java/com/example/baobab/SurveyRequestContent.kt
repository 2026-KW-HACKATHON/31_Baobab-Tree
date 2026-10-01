package com.example.baobab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun <T> SurveyRequestContent(
    loading: Boolean,
    error: String?,
    data: T?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = "등록된 설문이 없습니다.",
    content: @Composable (T) -> Unit
) {
    Box(modifier = modifier.background(Color(0xFFFDF9F1)), contentAlignment = Alignment.Center) {
        when {
            loading -> CircularProgressIndicator(color = Color(0xFF2F5539))
            error != null -> Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(error, textAlign = TextAlign.Center)
                Button(onClick = onRetry) { Text("다시 시도") }
            }
            data == null || (data is Collection<*> && data.isEmpty()) -> Text(
                emptyMessage, modifier = Modifier.padding(24.dp), textAlign = TextAlign.Center
            )
            else -> content(data)
        }
    }
}
