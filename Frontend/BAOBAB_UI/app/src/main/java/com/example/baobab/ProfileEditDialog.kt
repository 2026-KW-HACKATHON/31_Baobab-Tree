package com.example.baobab

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileEditDialog(user: UserProfile, saving: Boolean, error: String?, onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit) {
    var name by rememberSaveable(user.id) { mutableStateOf(user.name) }
    var email by rememberSaveable(user.id) { mutableStateOf(user.email) }
    var region by rememberSaveable(user.id) { mutableStateOf(user.region.orEmpty()) }
    var ageGroup by rememberSaveable(user.id) { mutableStateOf(user.ageGroup.orEmpty()) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("계정 정보 수정") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("아이디: ${user.loginId}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(name, { name = it }, label = { Text("이름") }, singleLine = true,
                    enabled = !saving, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(email, { email = it }, label = { Text("이메일") }, singleLine = true,
                    enabled = !saving, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(region, { region = it }, label = { Text("지역 (선택)") }, singleLine = true,
                    enabled = !saving, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(ageGroup, { ageGroup = it }, label = { Text("연령대 (선택)") },
                    placeholder = { Text("예: 20대") }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, email, region, ageGroup) },
                enabled = !saving && name.isNotBlank() && email.isNotBlank()) {
                Text(if (saving) "저장 중…" else "저장")
            }
        },
        dismissButton = { TextButton(onDismiss, enabled = !saving) { Text("취소") } }
    )
}
