package com.example.baobab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MyGreen = Color(0xFF2F5539)
private val MyMuted = Color(0xFF697369)

@Composable
fun MyPageScreen(model: MyPageViewModel, token: String?, onBack: () -> Unit,
    onLogin: () -> Unit, onLogout: () -> Unit, onCreate: () -> Unit,
    onDelete: (SurveyItem) -> Unit = {}, onProfileUpdated: () -> Unit = {},
    onOpenSurvey: (String) -> Unit = {}, onPointClick: () -> Unit = {}, onEdit: (SurveyItem) -> Unit = {}) {
    val context = LocalContext.current
    var editingProfile by remember(token) { mutableStateOf(false) }
    var confirmingPassword by remember(token) { mutableStateOf(false) }
    var profilePassword by remember(token) { mutableStateOf("") }
    var showParticipations by rememberSaveable { mutableStateOf(false) }
    if (confirmingPassword && token != null) AlertDialog(
        onDismissRequest = { if (!model.savingProfile) { confirmingPassword = false; profilePassword = ""; model.clearProfileError() } },
        title = { Text("비밀번호 확인") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("계정 정보를 수정하려면 현재 비밀번호를 입력해주세요.")
                OutlinedTextField(profilePassword, { profilePassword = it; model.clearProfileError() },
                    singleLine = true, enabled = !model.savingProfile,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), label = { Text("현재 비밀번호") })
                model.profileError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = {
            model.verifyPassword(token, profilePassword) { confirmingPassword = false; editingProfile = true }
        }, enabled = !model.savingProfile && profilePassword.isNotBlank()) { Text(if (model.savingProfile) "확인 중…" else "확인") } },
        dismissButton = { TextButton({ confirmingPassword = false; profilePassword = ""; model.clearProfileError() },
            enabled = !model.savingProfile) { Text("취소") } })
    var expandedHistory by rememberSaveable { mutableStateOf<Int?>(null) }
    if (editingProfile && token != null) model.profile?.let { user ->
        ProfileEditDialog(user, model.savingProfile, model.profileError,
            onDismiss = { editingProfile = false; profilePassword = ""; model.clearProfileError() },
            onSave = { name, email, region, ageGroup ->
                model.saveProfile(token, name, email, region, ageGroup, profilePassword) {
                    editingProfile = false; profilePassword = ""; onProfileUpdated()
                }
            })
    }
    LaunchedEffect(token) { model.load(token) }
    val selected = model.selectedSurvey
    BackHandler(enabled = selected != null) { model.closeResults() }
    Column(Modifier.fillMaxSize().background(Color(0xFFFDF9F1)).safeDrawingPadding()) {
        Box(Modifier.fillMaxWidth().height(64.dp)) {
            IconButton(onClick = { if (selected != null) model.closeResults() else onBack() },
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) {
                Icon(Icons.Outlined.ChevronLeft, "뒤로가기", tint = MyGreen)
            }
            Text(if (selected == null) "마이페이지" else "답변 통계",
                Modifier.align(Alignment.Center), fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
        when {
            token == null -> Column(Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Person, null, tint = MyGreen, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(20.dp))
                Text("내 설문과 포인트를 한곳에서", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("로그인하면 내 정보와 설문 통계를 볼 수 있어요.", color = MyMuted,
                    modifier = Modifier.padding(vertical = 16.dp))
                Button(onLogin) { Text("로그인하기") }
            }
            selected != null -> SurveyRequestContent(model.resultsLoading, model.resultsError, model.results,
                { model.openResults(selected, token) }, Modifier.weight(1f).fillMaxWidth()) { results ->
                ResultsContent(selected, results, onDelete, { shareSurvey(context, it) }, onEdit)
            }
            else -> SurveyRequestContent(model.loading, model.error, model.profile,
                { model.load(token) }, Modifier.weight(1f).fillMaxWidth()) { user ->
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        Surface(onClick = onPointClick, color = MyGreen, shape = RoundedCornerShape(24.dp)) {
                            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${user.name}님", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text("@${user.loginId}", color = Color(0xFFDCE7D8), fontSize = 14.sp)
                                Spacer(Modifier.height(12.dp))
                                Text("보유 포인트", color = Color(0xFFDCE7D8), fontSize = 13.sp)
                                Text("%,d P".format(user.point), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                Text("쿠폰함 · 포인트 충전 →", color = Color(0xFFDCE7D8), fontSize = 13.sp)
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MyMetric("만든 설문", "${model.surveys.size}개", Modifier.weight(1f))
                            MyMetric("참여한 설문", "${model.participationCount}개", Modifier.weight(1f))
                        }
                    }
                    item {
                        MyPanel {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("계정 정보", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                TextButton(onClick = { model.clearProfileError(); profilePassword = ""; confirmingPassword = true }) { Text("수정") }
                            }
                            user.memberType?.let { type ->
                                Text(if (type == "OTHER") "기타 · ${user.memberDetail.orEmpty()}" else MemberTypes[type].orEmpty(), color = MyGreen)
                            }
                            Text(user.email, color = MyMuted, fontSize = 14.sp)
                            if (!user.region.isNullOrBlank()) Text(user.region, color = MyMuted, fontSize = 14.sp)
                            if (!user.ageGroup.isNullOrBlank()) Text(user.ageGroup, color = MyMuted, fontSize = 14.sp)
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("내가 만든 설문", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            TextButton(onCreate) { Text("새 설문 만들기") }
                        }
                    }
                    if (model.surveys.isEmpty()) item {
                        MyPanel {
                            Text("아직 만든 설문이 없어요", fontWeight = FontWeight.Bold)
                            Text("첫 설문을 만들고 사람들의 의견을 모아보세요.", color = MyMuted, fontSize = 14.sp)
                        }
                    }
                    items(model.surveys, key = { it.id }) { survey ->
                        Card(onClick = { model.openResults(survey, token) },
                            shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(survey.category, color = MyGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(if (survey.status == "OPEN") "진행 중" else "마감", color = MyMuted, fontSize = 12.sp)
                                }
                                Text(survey.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text("${survey.participantCount ?: 0}명 참여 · ${survey.questionCount ?: 0}개 문항", color = MyMuted, fontSize = 13.sp)
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("답변 통계 보기 →", color = MyGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { onEdit(survey) }) { Text("수정") }
                                    TextButton(onClick = { shareSurvey(context, survey) }) { Text("공유") }
                                    TextButton(onClick = { onDelete(survey) }) {
                                        Text("삭제", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        OutlinedButton(onClick = { showParticipations = !showParticipations }, modifier = Modifier.fillMaxWidth()) {
                            Text("내가 참여한 설문 (${model.participationCount}) ${if (showParticipations) "▴" else "▾"}")
                        }
                    }
                    if (showParticipations && model.participations.isEmpty()) item {
                        MyPanel { Text("아직 참여한 설문이 없어요.", color = MyMuted) }
                    }
                    if (showParticipations) items(model.participations, key = { "history-${it.id}" }) { history ->
                        Surface(onClick = { onOpenSurvey(history.surveyId) }, shape = RoundedCornerShape(20.dp), color = Color.White) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(history.category, color = MyGreen, fontSize = 12.sp)
                                Text(history.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text("${history.createdAt.take(10)} 참여 · ${history.rewardPoint}P", color = MyMuted, fontSize = 13.sp)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TextButton(onClick = { expandedHistory = if (expandedHistory == history.id) null else history.id }) {
                                        Text(if (expandedHistory == history.id) "내 답변 접기" else "내 답변 보기")
                                    }
                                    Text("설문 열기 →", color = MyGreen, modifier = Modifier.align(Alignment.CenterVertically))
                                }
                                if (expandedHistory == history.id) {
                                    if (history.answers.isEmpty()) Text("선택 문항에 답변하지 않았어요.", color = MyMuted)
                                    history.answers.forEach { answer ->
                                        Text(answer.question, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(answer.answer, color = MyMuted, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                    item { TextButton(onLogout, enabled = !model.savingProfile,
                        modifier = Modifier.fillMaxWidth()) { Text("로그아웃", color = MaterialTheme.colorScheme.error) } }
                }
            }
        }
    }
}

@Composable
private fun MyPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun MyMetric(label: String, value: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = Color(0xFFEAF0E5)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = MyMuted, fontSize = 13.sp)
            Text(value, color = MyGreen, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun ResultsContent(survey: SurveyItem, results: SurveyResults, onDelete: (SurveyItem) -> Unit,
    onShare: (SurveyItem) -> Unit, onEdit: (SurveyItem) -> Unit) {
    var byResponse by rememberSaveable(survey.id) { mutableStateOf(false) }
    var expandedQuestions by rememberSaveable(survey.id) { mutableStateOf(listOf<Int>()) }
    var expandedResponses by rememberSaveable(survey.id) { mutableStateOf(listOf<Int>()) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(survey.title, fontSize = 23.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            MyMetric("전체 응답", "${results.totalResponses}명", Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton({ onShare(survey) }) { Text("공유") }
                TextButton({ onEdit(survey) }) { Text("설문 수정") }
                TextButton({ onDelete(survey) }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!byResponse, { byResponse = false }, label = { Text("문항별 답변") })
                FilterChip(byResponse, { byResponse = true }, label = { Text("응답별 전체 답변") })
            }
        }
        if (results.totalResponses == 0) item { Text("아직 응답이 없어요.", color = MyMuted) }
        if (byResponse) {
            if (results.responses == null) item { Text("응답별 보기를 사용하려면 서버를 업데이트해주세요.", color = MyMuted) }
            items(results.responses.orEmpty(), key = { it.responseId }) { response ->
                val expanded = response.responseId in expandedResponses
                MyPanel {
                    Text("응답 ${results.responses.orEmpty().indexOf(response) + 1}", fontWeight = FontWeight.Bold)
                    Text(response.createdAt.take(10), color = MyMuted, fontSize = 12.sp)
                    TextButton({ expandedResponses = if (expanded) expandedResponses - response.responseId else expandedResponses + response.responseId }) {
                        Text(if (expanded) "답변 접기 ▴" else "전체 답변 펼치기 ▾")
                    }
                    if (expanded) response.answers.forEachIndexed { index, answer ->
                        Text("${index + 1}. ${answer.question}", fontWeight = FontWeight.Bold)
                        Text(answer.answer ?: "미응답 (선택 문항)", color = MyMuted)
                        HorizontalDivider()
                    }
                }
            }
        } else items(survey.questions, key = { it.id }) { question ->
            val questionResults = results.questions.find { it.questionId == question.id }
            val expanded = question.id in expandedQuestions
            MyPanel {
                Text("${survey.questions.indexOf(question) + 1}. ${question.question}",
                    fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
                Text("${if (question.questionType == "single") "객관식" else "주관식"} · ${questionResults?.responseCount ?: 0}명 응답 · ${if (question.required) "필수" else "선택"}",
                    color = MyMuted, fontSize = 12.sp)
                TextButton({ expandedQuestions = if (expanded) expandedQuestions - question.id else expandedQuestions + question.id }) {
                    Text(if (expanded) "답변 접기 ▴" else "전체 답변 펼치기 ▾")
                }
                if (expanded) {
                    if (questionResults?.results.isNullOrEmpty()) Text("아직 답변이 없습니다.", color = MyMuted)
                    questionResults?.results.orEmpty().forEach { answer ->
                        Text(answer.option, fontSize = 15.sp, lineHeight = 22.sp)
                        Text("${answer.count}명 · ${answer.percentage}%", color = MyGreen, fontSize = 13.sp)
                        if (question.questionType == "single") LinearProgressIndicator(
                            progress = { (answer.percentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp), color = MyGreen, trackColor = Color(0xFFEAF0E5))
                    }
                }
            }
        }
    }
}
