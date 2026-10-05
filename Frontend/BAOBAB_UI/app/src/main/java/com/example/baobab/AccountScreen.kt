package com.example.baobab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AccountScreen(signup: Boolean, account: AccountViewModel, onSuccess: () -> Unit,
    onSwitch: () -> Unit, onGuest: () -> Unit) {
    var id by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var emailLocal by rememberSaveable { mutableStateOf("") }
    var emailDomain by rememberSaveable { mutableStateOf("") }
    var memberType by rememberSaveable { mutableStateOf("") }
    var memberDetail by rememberSaveable { mutableStateOf("") }
    val email = "$emailLocal@$emailDomain"
    val signupReady = name.isNotBlank() && emailLocal.isNotBlank() &&
        Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email) &&
        memberType.isNotBlank() && (memberType != "OTHER" || memberDetail.isNotBlank())
    Column(Modifier.fillMaxSize().background(Color(0xFFFDF9F1)).safeDrawingPadding().imePadding()
        .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
        BaobabLogo()
        Spacer(Modifier.height(24.dp))
        Text(if (signup) "회원가입" else "로그인", style = MaterialTheme.typography.titleLarge)
        if (signup) {
            OutlinedTextField(name, { name = it }, label = { Text("이름") }, singleLine = true, enabled = !account.busy, modifier = Modifier.fillMaxWidth())
            EmailField(emailLocal, { emailLocal = it }, emailDomain, { emailDomain = it }, !account.busy)
            Text("가입 유형 (필수)")
            ChoiceField("가입 유형을 선택해주세요", MemberTypes[memberType].orEmpty(), MemberTypes.values.toList(),
                { label -> memberType = MemberTypes.entries.first { it.value == label }.key }, enabled = !account.busy)
            if (memberType == "OTHER") OutlinedTextField(memberDetail, { memberDetail = it },
                label = { Text("기타 소속 직접 입력 (필수)") }, enabled = !account.busy,
                singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        OutlinedTextField(id, { id = it }, label = { Text("아이디") }, singleLine = true, enabled = !account.busy, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("비밀번호") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), enabled = !account.busy, modifier = Modifier.fillMaxWidth())
        account.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 12.dp)) }
        if (!signup && account.hasSavedSession && account.token == null) {
            TextButton(onClick = { account.restoreSession(force = true) { if (it) onSuccess() } }, enabled = !account.busy) {
                Text("저장된 로그인 다시 확인")
            }
        }
        Button(onClick = {
            if (signup) account.signup(name, email, id, password, memberType, memberDetail, onSuccess)
            else account.login(id, password, onSuccess)
        }, enabled = !account.busy && id.isNotBlank() && password.isNotBlank() && (!signup || signupReady), modifier = Modifier.fillMaxWidth()) {
            Text(if (account.busy) "처리 중…" else if (signup) "가입하기" else "로그인")
        }
        TextButton(onSwitch, enabled = !account.busy) { Text(if (signup) "로그인으로 돌아가기" else "회원가입") }
        if (!signup) TextButton(onGuest, enabled = !account.busy) { Text("Guest로 로그인") }
    }
}
