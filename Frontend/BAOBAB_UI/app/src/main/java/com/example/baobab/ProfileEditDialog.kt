package com.example.baobab

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ProfileEditDialog(user: UserProfile, saving: Boolean, error: String?, onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit) {
    var name by rememberSaveable(user.id) { mutableStateOf(user.name) }
    var email by rememberSaveable(user.id) { mutableStateOf(user.email) }
    var newPassword by remember(user.id) { mutableStateOf("") }
    var passwordConfirmation by remember(user.id) { mutableStateOf("") }
    val passwordError = when {
        newPassword.isEmpty() && passwordConfirmation.isEmpty() -> null
        newPassword.trim().length < 8 -> "새 비밀번호는 8자 이상 입력해주세요."
        newPassword.toByteArray(Charsets.UTF_8).size > 72 -> "새 비밀번호가 너무 깁니다."
        newPassword != passwordConfirmation -> "새 비밀번호가 일치하지 않습니다."
        else -> null
    }
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
                OutlinedTextField(newPassword, { newPassword = it }, label = { Text("새 비밀번호") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(passwordConfirmation, { passwordConfirmation = it }, label = { Text("새 비밀번호 확인") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
                passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(ageGroup, { ageGroup = it }, label = { Text("연령대 (선택)") },
                    placeholder = { Text("예: 20대") }, singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, email, newPassword, ageGroup) },
                enabled = !saving && name.isNotBlank() && email.isNotBlank() && passwordError == null) {
                Text(if (saving) "저장 중…" else "저장")
            }
        },
        dismissButton = { TextButton(onDismiss, enabled = !saving) { Text("취소") } }
    )
}
