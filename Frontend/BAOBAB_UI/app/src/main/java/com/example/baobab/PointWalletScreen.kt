package com.example.baobab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WalletGreen = Color(0xFF2F5539)
private val WalletMuted = Color(0xFF697369)

private data class ExchangeItem(val shop: String, val title: String, val cost: Int, val category: String,
    val icon: ImageVector)
private val ExchangeItems = listOf(
    ExchangeItem("월계동 바오밥 카페", "아메리카노 500원 할인", 500, "카페", Icons.Outlined.Coffee),
    ExchangeItem("월계 골목 분식", "분식 메뉴 1,000원 할인", 1000, "먹거리", Icons.Outlined.Restaurant),
    ExchangeItem("동네 책방", "도서 구매 1,000원 할인", 1000, "문화", Icons.Outlined.MenuBook),
    ExchangeItem("월계 문화센터", "문화 프로그램 우선 신청권", 1500, "문화", Icons.Outlined.TheaterComedy),
    ExchangeItem("초록 동네 공방", "원데이 클래스 2,000원 할인", 2000, "문화", Icons.Outlined.LocalFlorist),
    ExchangeItem("바오밥 베이커리", "베이커리 1,000원 할인", 1000, "먹거리", Icons.Outlined.Restaurant)
)

class DemoWalletState {
    var balance by mutableIntStateOf(3000)
    val coupons = mutableStateListOf<Int>()
}

@Composable
fun PointWalletScreen(point: Int?, loggedIn: Boolean, loading: Boolean, error: String?, wallet: DemoWalletState,
    onRetry: () -> Unit, onBack: () -> Unit, onLogin: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedAmount by rememberSaveable { mutableIntStateOf(1000) }
    var category by rememberSaveable { mutableStateOf("전체") }
    var exchanging by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirming by rememberSaveable { mutableStateOf(false) }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Color(0xFFFDF9F1)).safeDrawingPadding()) {
        Box(Modifier.fillMaxWidth().height(64.dp)) {
            IconButton(onBack, Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) {
                Icon(Icons.Outlined.ChevronLeft, "뒤로가기", tint = WalletGreen)
            }
            Text("포인트 & 쿠폰", Modifier.align(Alignment.Center), fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Surface(color = WalletGreen, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("현재 보유 포인트", color = Color(0xFFDCE7D8))
                    Text(if (!loggedIn) "로그인 후 확인" else point?.let { "%,d P".format(it) } ?: "불러오는 중…",
                        color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("설문에 참여하고 포인트를 모아보세요", color = Color(0xFFDCE7D8), fontSize = 13.sp)
                }
            }
            if (!loggedIn) Button(onLogin, Modifier.fillMaxWidth()) { Text("로그인하기") }
            if (loggedIn && loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (loggedIn && error != null) {
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onRetry) { Text("다시 시도") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("교환소", "쿠폰함", "충전").forEachIndexed { index, title ->
                    FilterChip(selected = tab == index, onClick = { tab = index }, label = { Text(title) },
                        modifier = Modifier.weight(1f))
                }
            }
            if (tab == 0) {
                Text("동네 포인트 교환소", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Surface(color = Color(0xFFF0E5CD), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("체험용 잔액 %,d P".format(wallet.balance), fontWeight = FontWeight.Bold, color = WalletGreen)
                        Text("3,000P로 교환을 체험해보세요. 실제 보유 포인트는 차감되지 않아요.", color = WalletMuted, fontSize = 13.sp)
                        TextButton(onClick = { tab = 2 }) { Text("테스트 포인트 충전 →") }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("전체", "카페", "먹거리", "문화").forEach { label ->
                        FilterChip(category == label, onClick = { category = label }, label = { Text(label) })
                    }
                }
                ExchangeItems.forEachIndexed { index, item ->
                    if (category == "전체" || category == item.category) {
                        Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(item.icon, null, tint = WalletGreen, modifier = Modifier.size(32.dp))
                                    Column {
                                        Text(item.shop, color = WalletMuted, fontSize = 13.sp)
                                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("%,d P".format(item.cost), color = WalletGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Button(onClick = { exchanging = index }, enabled = loggedIn) { Text("교환하기") }
                                }
                            }
                        }
                    }
                }
                Text("상품과 매장은 임시 예시입니다. 교환한 쿠폰도 실제로 사용할 수 없어요.", color = WalletMuted, fontSize = 13.sp)
            } else if (tab == 1) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("내 쿠폰함", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("예시 ${wallet.coupons.size + 1}장", color = WalletMuted)
                }
                Surface(color = Color.White, shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(color = Color(0xFFEAF0E4), shape = RoundedCornerShape(16.dp)) {
                            Icon(Icons.Outlined.Coffee, null, Modifier.padding(16.dp).size(32.dp), tint = WalletGreen)
                        }
                        Text("월계동 바오밥 카페", color = WalletMuted, fontSize = 14.sp)
                        Text("아메리카노 500원 할인", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = WalletGreen)
                        Text("카페에서 사용할 수 있는 할인 쿠폰이에요.", color = WalletMuted, fontSize = 14.sp)
                        HorizontalDivider(color = Color(0xFFEAEDE5))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.ConfirmationNumber, null, tint = WalletGreen)
                            Text("미리보기 쿠폰", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { notice = "예시 쿠폰이에요. 실제 교환·사용 기능은 준비 중입니다." },
                            modifier = Modifier.fillMaxWidth()) { Text("쿠폰 보기") }
                    }
                }
                Text("현재는 예시 쿠폰입니다. 실제 매장에서 사용할 수 없어요.", color = WalletMuted, fontSize = 13.sp)
                wallet.coupons.forEach { index ->
                    val item = ExchangeItems[index]
                    Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(item.shop, color = WalletMuted, fontSize = 13.sp)
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = WalletGreen)
                            Text("체험 교환 쿠폰 · %,d P".format(item.cost), color = WalletMuted)
                            OutlinedButton(onClick = { notice = "${item.title}\n체험용 쿠폰입니다. 실제 사용 기능은 준비 중이에요." },
                                modifier = Modifier.fillMaxWidth()) { Text("쿠폰 보기") }
                        }
                    }
                }
            } else {
                Text("포인트 충전", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("충전할 포인트를 선택해주세요", color = WalletMuted)
                listOf(1000, 3000, 5000).forEach { amount ->
                    Surface(onClick = { selectedAmount = amount }, shape = RoundedCornerShape(16.dp),
                        color = if (selectedAmount == amount) Color(0xFFEAF0E4) else Color.White) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selectedAmount == amount, onClick = { selectedAmount = amount })
                            Text("%,d P".format(amount), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
                Button(onClick = { confirming = true }, enabled = loggedIn, modifier = Modifier.fillMaxWidth()) {
                    Text("%,d P 테스트 충전".format(selectedAmount))
                }
                Surface(color = Color(0xFFF0E5CD), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("체험용 잔액: %,d P".format(wallet.balance), fontWeight = FontWeight.Bold)
                        TextButton(onClick = { wallet.balance = 3000; wallet.coupons.clear() }) { Text("교환 체험 초기화") }
                    }
                }
                Text("실제 결제 없이 충전 흐름만 체험합니다. 테스트 잔액은 보유 포인트와 별도이며 서버에 저장되지 않습니다.",
                    color = WalletMuted, fontSize = 13.sp)
            }
        }
    }
    if (confirming) AlertDialog(onDismissRequest = { confirming = false },
        title = { Text("테스트 충전") },
        text = { Text("%,d P를 테스트 잔액에 추가할까요? 실제 결제는 발생하지 않습니다.".format(selectedAmount)) },
        confirmButton = { TextButton(onClick = {
            wallet.balance = (wallet.balance.toLong() + selectedAmount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            confirming = false
            notice = "테스트 충전이 완료되었습니다."
        }) { Text("충전하기") } },
        dismissButton = { TextButton(onClick = { confirming = false }) { Text("취소") } })
    exchanging?.let { index ->
        val item = ExchangeItems[index]
        val enough = wallet.balance >= item.cost
        AlertDialog(onDismissRequest = { exchanging = null }, title = { Text(if (enough) "쿠폰으로 교환하기" else "체험 포인트가 부족해요") },
            text = { Text(if (enough) "${item.title}\n체험용 %,d P를 차감하고 쿠폰함에 추가합니다.".format(item.cost)
                else "필요 %,d P · 체험 잔액 %,d P\n충전 화면에서 테스트 포인트를 추가해주세요.".format(item.cost, wallet.balance)) },
            confirmButton = { TextButton(onClick = {
                if (enough && loggedIn) {
                    wallet.balance -= item.cost
                    wallet.coupons.add(index)
                    tab = 1
                    notice = "쿠폰함에 체험 쿠폰을 추가했어요."
                } else tab = 2
                exchanging = null
            }) { Text(if (enough) "교환하기" else "충전하기") } },
            dismissButton = { TextButton(onClick = { exchanging = null }) { Text("취소") } })
    }
    notice?.let { message -> AlertDialog(onDismissRequest = { notice = null },
        title = { Text("안내") }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { notice = null }) { Text("확인") } }) }
}
