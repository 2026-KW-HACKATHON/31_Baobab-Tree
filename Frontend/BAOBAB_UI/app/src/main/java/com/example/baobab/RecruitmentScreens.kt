package com.example.baobab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RGreen = Color(0xFF2F5539)
private val RInk = Color(0xFF454545)
private val RCream = Color(0xFFFDF9F1)
private val RBorder = Color(0xFFD9E0D5)
private val RLavender = Color(0xFFEAE1FF)
private val RSage = Color(0xFFE4EFDF)
private fun rNumber(value: Int) = String.format(Locale.KOREA, "%,d", value)
private fun rDate(value: String, pattern: String = "M월 d일 a h시 mm분"): String = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern(pattern, Locale.KOREA))
}.getOrDefault(value)
private fun rType(value: String) = when (value) {
    "EXPERIMENT" -> "실험"
    "INTERVIEW" -> "인터뷰"
    "USABILITY" -> "사용성 테스트"
    else -> "기타"
}
private fun rStatus(value: String) = when (value) {
    "APPLIED" -> "신청 중"
    "ACCEPTED" -> "선정 완료"
    "COMPLETED" -> "참여 완료"
    "CANCELLED" -> "신청 취소"
    "REJECTED" -> "미선정"
    else -> value
}

@Composable
fun RecruitmentHubScreen(
    token: String?, onBack: () -> Unit, onLogin: () -> Unit,
    onHome: () -> Unit, onSearch: () -> Unit, onMy: () -> Unit, onPointHistory: () -> Unit,
    initialPage: String = "APPLICATIONS", initialDetailId: Int = 0
) {
    val repository = remember { HttpSurveyRepository(BuildConfig.SURVEY_API_BASE_URL) }
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf(initialPage) }
    var previousPage by rememberSaveable { mutableStateOf(initialPage) }
    var detailId by rememberSaveable { mutableIntStateOf(initialDetailId) }
    var applicationFilter by rememberSaveable { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var applications by remember { mutableStateOf(emptyList<RecruitmentApplicationItem>()) }
    var detail by remember { mutableStateOf<RecruitmentItem?>(null) }
    var ownDetail by remember { mutableStateOf(false) }
    var cancelId by remember { mutableStateOf<Int?>(null) }
    fun navigate(next: String) {
        if (!busy) {
            if (next == "LIST") { onBack(); return }
            loading = true; error = null; notice = null; page = next
        }
    }
    fun back() {
        if (!busy) {
            if (page == initialPage || page == "LIST") onBack()
            else navigate(if (page == "DETAIL" && previousPage != "DETAIL") previousPage else initialPage)
        }
    }
    fun openDetail(id: Int) { previousPage = page; detailId = id; navigate("DETAIL") }
    BackHandler { back() }
    if (page == "MANAGE" && token != null) {
        RecruitmentManageScreen(token, initialRecruitmentId = detailId.takeIf { it > 0 }, onBack = {
            if (initialPage == "MANAGE") onBack() else navigate("DETAIL")
        })
        return
    }
    if (page == "CREATE" && token != null) {
        RecruitmentCreateScreen(token, onBack = { navigate("LIST") }, onCreated = {
            previousPage = "LIST"; detailId = it.id; navigate("DETAIL")
            notice = "모집을 등록했습니다."; refresh++
        })
        return
    }
    LaunchedEffect(page, detailId, refresh, token) {
        loading = true; error = null
        try {
            when (page) {
                "DETAIL" -> {
                    detail = null
                    val result = withContext(Dispatchers.IO) {
                        val item = repository.getRecruitment(detailId)
                        val userId = if (token != null) repository.getProfile(token).id else null
                        item to (item.authorId == userId)
                    }
                    ownDetail = result.second; detail = result.first
                }
                "APPLICATIONS" -> applications = if (token == null) emptyList() else withContext(Dispatchers.IO) {
                    repository.getMyRecruitmentApplications(token)
                }
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "불러오지 못했습니다." }
        finally { loading = false }
    }
    fun action(block: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null; notice = null
        scope.launch {
            try { block(); loading = true; refresh++ }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "요청에 실패했습니다." }
            finally { busy = false }
        }
    }
    if (page == "DETAIL") {
        RecruitmentDetailScreen(detail, loading, busy, error, notice, token != null, ownDetail,
            onBack = { back() }, onRetry = { loading = true; refresh++ }, onLogin = onLogin,
            onManage = { navigate("MANAGE") }, onApply = { slotId, message ->
                val currentToken = token
                if (currentToken != null) action {
                    withContext(Dispatchers.IO) { repository.applyRecruitment(currentToken, detailId, slotId, message, true) }
                    applicationFilter = ""; page = "APPLICATIONS"
                    notice = "신청을 처리했습니다. 아래에서 상태를 확인해주세요."
                }
            })
        return
    }
    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
        Scaffold(modifier = Modifier.fillMaxSize().safeDrawingPadding(), containerColor = RCream,
            topBar = {
                ActivityScreenHeader(if (page == "APPLICATIONS") "내 신청 내역" else "참여 모집",
                    onBack = { back() }, enabled = !busy,
                    onRefresh = { loading = true; refresh++ }, refreshEnabled = !loading)
            }
        ) { padding ->
            key(page, detailId) {
                Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (page == "APPLICATIONS") RFilters(listOf("" to "전체", "APPLIED" to "신청 중", "ACCEPTED" to "선정", "COMPLETED" to "완료", "CANCELLED" to "취소", "REJECTED" to "미선정"), applicationFilter) { applicationFilter = it }
                    if (token == null && page in listOf("CREATE", "MANAGE")) {
                        RPrimaryButton("로그인하고 계속하기", true, onLogin)
                    }
                    notice?.let { Text(it, color = RGreen) }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (loading || busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = RGreen)
                    if (!loading && error == null) when (page) {
                        "APPLICATIONS" -> {
                            if (token == null) RPrimaryButton("로그인하고 신청 내역 보기", true, onLogin)
                            else {
                                val visible = applications.filter { applicationFilter.isEmpty() || it.status == applicationFilter }
                                if (visible.isEmpty()) Text("해당 신청 내역이 없습니다.", color = RInk)
                                visible.forEach { application ->
                                    RecruitmentApplicationCard(application, !busy, onDetail = { openDetail(application.recruitment.id) },
                                        onCancel = { cancelId = application.id }, onPointHistory = onPointHistory)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    cancelId?.let { id -> AlertDialog(onDismissRequest = { cancelId = null }, title = { Text("신청을 취소할까요?") },
        text = { Text("참여 일정 시작 전까지 취소할 수 있습니다.") },
        dismissButton = { TextButton(onClick = { cancelId = null }) { Text("닫기") } },
        confirmButton = { TextButton(onClick = {
            cancelId = null
            val currentToken = token
            if (currentToken != null) action {
                withContext(Dispatchers.IO) { repository.cancelRecruitmentApplication(currentToken, id) }
                notice = "신청을 취소했습니다."
            }
        }, enabled = !busy) { Text("신청 취소") } }) }
}

@Composable
private fun RHeader(title: String, enabled: Boolean, onRefresh: () -> Unit, refreshEnabled: Boolean, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(60.dp)) {
        IconButton(onClick = onBack, enabled = enabled, modifier = Modifier.align(Alignment.CenterStart)) { Icon(Icons.Outlined.ChevronLeft, "뒤로", tint = RGreen) }
        Text(title, Modifier.align(Alignment.Center), fontSize = 21.sp, fontWeight = FontWeight.Bold, color = RGreen)
        IconButton(onClick = onRefresh, enabled = refreshEnabled, modifier = Modifier.align(Alignment.CenterEnd)) {
            Icon(Icons.Outlined.Refresh, "새로고침", tint = RGreen)
        }
    }
}
@Composable
private fun RFilters(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { (value, label) ->
            Surface(onClick = { onSelect(value) }, shape = RoundedCornerShape(24.dp),
                color = if (value == selected) RGreen else Color(0x0D545454)) {
                Text(label, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 14.sp, lineHeight = 18.sp,
                    color = if (value == selected) Color.White else RInk, fontWeight = FontWeight.Bold)
            }
        }
    }
}
@Composable
private fun RPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(18.dp), shadowElevation = 3.dp) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
@Composable
private fun RBadge(text: String, lavender: Boolean = false) {
    Surface(color = if (lavender) RLavender else RSage, shape = RoundedCornerShape(10.dp)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = if (lavender) Color(0xFF393068) else RGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable
private fun RRecruitmentCard(item: RecruitmentItem, onClick: () -> Unit) {
    RPanel {
        RBadge("${rType(item.activityType)} · ${if (item.participationMode == "ONLINE") "온라인" else "오프라인"}", item.participationMode == "ONLINE")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.title, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RGreen)
            IconButton(onClick = onClick) { Icon(Icons.Outlined.ChevronRight, "상세 보기", tint = RInk) }
        }
        Text(item.organization, color = RInk)
        item.slots.firstOrNull()?.let { slot ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.DateRange, null, modifier = Modifier.size(18.dp), tint = RInk)
                Text("${rDate(slot.startsAt, "M월 d일")} ~ ${rDate(slot.endsAt, "M월 d일")}", color = RInk)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(18.dp), tint = RInk)
            Text("${item.location} · 약 ${item.durationMinutes}분", color = RInk)
        }
        HorizontalDivider(color = RBorder)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("모집 ${item.acceptedCount} / ${item.targetCount}명", color = RInk)
            Text("${rNumber(item.rewardPoint)} P", color = RGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable
private fun RPrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled, shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = RGreen, contentColor = Color.White)) { Text(text, fontSize = 15.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold) }
}
@Composable
private fun RFieldColors() = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
    focusedBorderColor = RGreen, unfocusedBorderColor = RBorder)
@Composable
private fun RBottomNavigation(enabled: Boolean, onHome: () -> Unit, onSearch: () -> Unit, onMy: () -> Unit) {
    Surface(color = RCream, tonalElevation = 0.dp) {
        Column {
            HorizontalDivider(color = RBorder)
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = onHome, enabled = enabled) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.Home, null, tint = RGreen); Text("홈", color = RGreen, fontSize = 12.sp) } }
                TextButton(onClick = onSearch, enabled = enabled) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.Search, null, tint = Color.Gray); Text("검색", color = Color.Gray, fontSize = 12.sp) } }
                TextButton(onClick = onMy, enabled = enabled) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.Person, null, tint = Color.Gray); Text("마이페이지", color = Color.Gray, fontSize = 12.sp) } }
            }
        }
    }
}
