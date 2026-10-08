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
    onHome: () -> Unit, onSearch: () -> Unit, onMy: () -> Unit, onPointHistory: () -> Unit
) {
    val repository = remember { HttpSurveyRepository(BuildConfig.SURVEY_API_BASE_URL) }
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf("LIST") }
    var previousPage by rememberSaveable { mutableStateOf("LIST") }
    var detailId by rememberSaveable { mutableIntStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }
    var submittedSearch by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("") }
    var applicationFilter by rememberSaveable { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf(emptyList<RecruitmentItem>()) }
    var applications by remember { mutableStateOf(emptyList<RecruitmentApplicationItem>()) }
    var detail by remember { mutableStateOf<RecruitmentItem?>(null) }
    var ownDetail by remember { mutableStateOf(false) }
    var cancelId by remember { mutableStateOf<Int?>(null) }
    fun navigate(next: String) {
        if (!busy) { loading = true; error = null; notice = null; page = next }
    }
    fun back() {
        if (!busy) { if (page == "LIST") onBack() else navigate(if (page == "DETAIL") previousPage else "LIST") }
    }
    fun openDetail(id: Int) { previousPage = page; detailId = id; navigate("DETAIL") }
    BackHandler { back() }
    if (page == "MANAGE" && token != null) {
        RecruitmentManageScreen(token, onBack = { navigate("LIST"); refresh++ })
        return
    }
    if (page == "CREATE" && token != null) {
        RecruitmentCreateScreen(token, onBack = { navigate("LIST") }, onCreated = {
            previousPage = "LIST"; detailId = it.id; navigate("DETAIL")
            notice = "모집을 등록했습니다."; refresh++
        })
        return
    }
    LaunchedEffect(page, detailId, submittedSearch, type, refresh, token) {
        loading = true; error = null
        try {
            when (page) {
                "LIST" -> items = withContext(Dispatchers.IO) {
                    repository.getRecruitments(activityType = type.takeIf { it.isNotEmpty() }, search = submittedSearch.takeIf { it.isNotBlank() })
                }
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
    ProvideTextStyle(TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp, color = RInk)) {
        Scaffold(modifier = Modifier.fillMaxSize().safeDrawingPadding(), containerColor = RCream,
            bottomBar = {
                if (page != "DETAIL") Column {
                    if (page == "LIST") Button(
                        onClick = { if (token == null) onLogin() else navigate("CREATE") }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = RGreen)
                    ) {Text(
                        text = "＋ 참여자 모집하기",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ) }
                    RBottomNavigation(!busy, onHome, onSearch, onMy)
                }
            }
        ) { padding ->
            key(page, detailId) {
                Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (page == "LIST") {
                        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                            BaobabLogo(Modifier.weight(1f))
                            IconButton(onClick = { loading = true; refresh++ }, enabled = !loading && !busy) {
                                Icon(Icons.Outlined.Refresh, "새로고침", tint = RGreen)
                            }
                            IconButton(onClick = onMy, enabled = !busy) {
                                Icon(Icons.Outlined.Person, "마이페이지", tint = RGreen, modifier = Modifier.size(28.dp))
                            }
                        }
                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF1EEE7)) {
                            Row(Modifier.fillMaxWidth()) {
                                TextButton(onClick = onHome, modifier = Modifier.weight(1f)) { Text("설문", color = RInk, fontSize = 14.sp) }
                                Surface(Modifier.weight(1f), color = RSage, shape = RoundedCornerShape(14.dp)) {
                                    Text("참여자 모집", Modifier.padding(12.dp), color = RGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }
                        OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            placeholder = { Text("참여할 연구를 찾아보세요", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = RGreen) },
                            trailingIcon = { IconButton(onClick = { submittedSearch = search.trim(); loading = true; refresh++ }) {
                                Icon(Icons.Outlined.ChevronRight, "검색", tint = RGreen)
                            } }, shape = RoundedCornerShape(10.dp), colors = RFieldColors(), textStyle = TextStyle(fontSize = 14.sp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { submittedSearch = search.trim(); loading = true; refresh++ }))
                        RFilters(listOf("" to "전체", "EXPERIMENT" to "실험", "INTERVIEW" to "인터뷰", "USABILITY" to "사용성 테스트", "OTHER" to "기타"), type) {
                            if (type != it) { type = it; loading = true }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { navigate("APPLICATIONS") }) { Text("내 신청 내역", color = RGreen) }
                            TextButton(onClick = { if (token == null) onLogin() else navigate("MANAGE") }) { Text("내 모집글 관리", color = RGreen) }
                        }
                    } else RHeader(if (page == "DETAIL") "모집 상세" else "내 신청 내역", !busy,
                        onRefresh = { loading = true; refresh++ }, refreshEnabled = !loading && !busy) { back() }
                    if (page == "APPLICATIONS") RFilters(listOf("" to "전체", "APPLIED" to "신청 중", "ACCEPTED" to "선정", "COMPLETED" to "완료", "CANCELLED" to "취소", "REJECTED" to "미선정"), applicationFilter) { applicationFilter = it }
                    notice?.let { Text(it, color = RGreen) }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (loading || busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = RGreen)
                    if (!loading && error == null) when (page) {
                        "LIST" -> {
                            if (items.isEmpty()) Text("현재 모집 중인 글이 없습니다.", color = RInk)
                            items.forEach { item -> RRecruitmentCard(item) { openDetail(item.id) } }
                        }
                        "DETAIL" -> detail?.let { item ->
                            RDetail(item, busy, token != null, ownDetail, onLogin, onManage = { navigate("MANAGE") }, onApply = { slotId, message ->
                                val currentToken = token
                                if (currentToken != null) action {
                                    withContext(Dispatchers.IO) { repository.applyRecruitment(currentToken, item.id, slotId, message, true) }
                                    applicationFilter = ""
                                    page = "APPLICATIONS"
                                    notice = "신청을 처리했습니다. 아래에서 상태를 확인해주세요."
                                }
                            })
                        }
                        "APPLICATIONS" -> {
                            if (token == null) RPrimaryButton("로그인하고 신청 내역 보기", true, onLogin)
                            else {
                                val visible = applications.filter { applicationFilter.isEmpty() || it.status == applicationFilter }
                                if (visible.isEmpty()) Text("해당 신청 내역이 없습니다.", color = RInk)
                                visible.forEach { application ->
                                    RApplicationCard(application, !busy, onDetail = { openDetail(application.recruitment.id) },
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
private fun RDetail(item: RecruitmentItem, busy: Boolean, loggedIn: Boolean, owner: Boolean, onLogin: () -> Unit, onManage: () -> Unit, onApply: (Int, String) -> Unit) {
    var slotId by rememberSaveable(item.id) { mutableIntStateOf(0) }
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    var agreed by rememberSaveable(item.id) { mutableStateOf(false) }
    var message by rememberSaveable(item.id) { mutableStateOf("") }
    val selected = item.slots.firstOrNull { it.id == slotId }
    val accepting = item.status == "OPEN" && runCatching { OffsetDateTime.parse(item.applicationDeadline).isAfter(OffsetDateTime.now()) }.getOrDefault(false)
    RBadge("${rType(item.activityType)} · ${if (item.participationMode == "ONLINE") "온라인" else "오프라인"}")
    Text(item.title, color = RGreen, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Text(item.organization, color = RInk, fontSize = 14.sp)
    RPanel {
        RInfo("참여 보상", "${rNumber(item.rewardPoint)} P", true)
        RInfo("소요 시간", "약 ${item.durationMinutes}분")
        RInfo("참여 장소", item.location)
        RInfo("모집 인원", "${item.acceptedCount} / ${item.targetCount}명")
        RInfo("신청 마감", rDate(item.applicationDeadline))
    }
    Text("연구 소개", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RGreen)
    RPanel { Text(item.description, color = RInk, lineHeight = 22.sp) }
    Text("신청 조건", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RGreen)
    RPanel { Text(item.eligibility, color = RInk) }
    if (owner) RPrimaryButton("내 모집글 · 신청자 관리", !busy, onManage)
    else if (!accepting) Text("신청이 마감된 모집입니다.", color = RGreen)
    else {
        Text("가능한 일정 선택", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RGreen)
        OutlinedButton(onClick = { expanded = !expanded }, enabled = !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, RBorder), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = RInk)) {
            Text(selected?.let { rDate(it.startsAt) } ?: "일정을 선택해주세요", Modifier.weight(1f).padding(vertical = 6.dp))
            Icon(Icons.Outlined.KeyboardArrowDown, "일정 펼치기", tint = RGreen)
        }
        if (expanded) RPanel {
            item.slots.forEach { slot ->
                val future = runCatching { OffsetDateTime.parse(slot.startsAt).isAfter(OffsetDateTime.now()) }.getOrDefault(false)
                TextButton(onClick = { slotId = slot.id; expanded = false }, enabled = !busy && future, modifier = Modifier.fillMaxWidth()) {
                    Text("${if (slotId == slot.id) "✓ " else ""}${rDate(slot.startsAt)} ~ ${rDate(slot.endsAt, "a h시 mm분")}", color = RGreen)
                }
            }
        }
        OutlinedTextField(message, { if (it.length <= 2000) message = it }, modifier = Modifier.fillMaxWidth(), label = { Text("신청 메시지 (선택)") }, enabled = !busy,
            shape = RoundedCornerShape(12.dp), colors = RFieldColors())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(agreed, { agreed = it }, enabled = !busy, colors = CheckboxDefaults.colors(checkedColor = RGreen))
            Text("모집자가 신청자 이름·프로필과 신청 메시지를 확인하는 데 동의합니다.", Modifier.weight(1f), color = RInk, fontSize = 13.sp)
        }
        if (!loggedIn) RPrimaryButton("로그인하고 신청하기", !busy, onLogin)
        else RPrimaryButton("참가 신청하기", !busy && agreed && selected != null) { onApply(slotId, message.trim()) }
    }
}
@Composable
private fun RInfo(label: String, value: String, emphasis: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.width(84.dp), color = RInk, fontSize = 14.sp)
        Text(value, Modifier.weight(1f), color = if (emphasis) RGreen else RInk, fontSize = if (emphasis) 17.sp else 14.sp,
            fontWeight = if (emphasis) FontWeight.Bold else FontWeight.Normal)
    }
}
@Composable
private fun RApplicationCard(item: RecruitmentApplicationItem, enabled: Boolean, onDetail: () -> Unit, onCancel: () -> Unit, onPointHistory: () -> Unit) {
    RPanel {
        RBadge(rStatus(item.status), item.status in listOf("APPLIED", "REJECTED", "CANCELLED"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.recruitment.title, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RGreen)
            IconButton(onClick = onDetail, enabled = enabled) { Icon(Icons.Outlined.ChevronRight, "모집 상세", tint = RInk) }
        }
        Text(item.recruitment.organization, color = RInk)
        Text(if (item.status == "APPLIED") "신청일 ${rDate(item.createdAt, "M월 d일")}" else "${rDate(item.slot.startsAt)} · ${item.recruitment.location}", color = RInk)
        when (item.status) {
            "APPLIED" -> Text("선정 결과를 기다리고 있어요.", color = Color(0xFF7657AF))
            "ACCEPTED" -> Text("참여 예정 · ${rNumber(item.recruitment.rewardPoint)} P", color = RGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            "COMPLETED" -> {
                Text(if (item.paidAt != null) "+${rNumber(item.recruitment.rewardPoint)} P 지급 완료" else "참여 완료 · 보상 0P", color = RGreen, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onPointHistory, enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) { Text("포인트 내역 보기", color = RInk) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDetail, enabled = enabled, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("상세 보기", color = RInk) }
            if (item.status in listOf("APPLIED", "ACCEPTED")) OutlinedButton(onClick = onCancel, enabled = enabled, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("신청 취소", color = RInk) }
        }
    }
}
@Composable
private fun RPrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled, shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = RGreen)) { Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
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
