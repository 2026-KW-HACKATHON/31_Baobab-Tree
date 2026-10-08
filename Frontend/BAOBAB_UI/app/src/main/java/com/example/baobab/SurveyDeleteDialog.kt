package com.example.baobab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun SurveyDeleteDialog(survey: SurveyItem, deleting: Boolean, error: String?,
    onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        title = { Text("설문을 삭제할까요?") },
        icon = { Icon(BaobabActionIcons.Delete, null, tint = MaterialTheme.colorScheme.error) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(survey.title)
                Text("설문과 수집된 답변이 함께 삭제됩니다. 삭제 후에는 복구할 수 없어요.")
                Text("등록 시 차감한 보상 중 아직 지급하지 않은 포인트는 돌려드립니다. 참여자에게 이미 지급한 포인트는 유지됩니다.")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !deleting) {
                Text(if (deleting) "삭제 중…" else "삭제", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !deleting) { Text("취소") } }
    )
}
