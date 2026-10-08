package com.example.baobab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign

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

private val ExchangeItemIds = listOf(
    "cafe-americano",
    "snack-discount",
    "book-discount",
    "culture-priority",
    "workshop-discount",
    "bakery-discount"
)

@Composable
fun PointWalletScreen(point: Int?, loggedIn: Boolean, loading: Boolean, error: String?, coupons: List<WalletCoupon>, exchangeBusy: Boolean, exchangeError: String?, onExchange: (String, (WalletCoupon) -> Unit) -> Unit, paymentBusy: Boolean, paymentError: String?, onCharge: (String, Int) -> Unit, onRetry: () -> Unit, onBack: () -> Unit, onLogin: () -> Unit, onHistoryClick: () -> Unit = {}) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedAmount by rememberSaveable { mutableIntStateOf(1000) }
    var selectedProvider by rememberSaveable { mutableStateOf("KAKAOPAY") }
    var category by rememberSaveable { mutableStateOf("전체") }
    var exchanging by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirming by rememberSaveable { mutableStateOf(false) }
    var selectedCouponId by rememberSaveable { mutableStateOf<String?>(null) }
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
            OutlinedButton(
                onClick = onHistoryClick,
                enabled = loggedIn,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "포인트 획득 · 사용 내역",
                    color = WalletGreen
                )
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
                Surface(
                    color = Color(0xFFF0E5CD),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = point?.let {
                                "교환 가능 잔액 %,d P".format(it)
                            } ?: "잔액을 불러오는 중…",
                            fontWeight = FontWeight.Bold,
                            color = WalletGreen
                        )

                        Text(
                            "쿠폰 교환 시 현재 보유 포인트에서 차감됩니다.",
                            color = WalletMuted,
                            fontSize = 13.sp
                        )

                        TextButton(onClick = { tab = 2 }) {
                            Text("포인트 충전 →")
                        }
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
                                    Button(onClick = { exchanging = index }, enabled = loggedIn && !loading && !exchangeBusy && !paymentBusy && point != null) { Text("교환하기") }
                                }
                            }
                        }
                    }
                }
                Text("상품과 매장은 임시 예시입니다. 교환한 쿠폰도 실제로 사용할 수 없어요.", color = WalletMuted, fontSize = 13.sp)
            } else if (tab == 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "내 쿠폰함",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("${coupons.size}장", color = WalletMuted)
                }

                if (!loggedIn) {
                    Text("로그인하면 보유 쿠폰을 확인할 수 있어요.")
                } else if (!loading && error == null && coupons.isEmpty()) {
                    Text(
                        "보유한 쿠폰이 없습니다. 교환소에서 쿠폰을 교환해보세요.",
                        color = WalletMuted
                    )
                }

                coupons.forEach { coupon ->
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Outlined.ConfirmationNumber,
                                contentDescription = null,
                                tint = WalletGreen
                            )

                            Text(
                                coupon.shop,
                                color = WalletMuted,
                                fontSize = 13.sp
                            )

                            Text(
                                coupon.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = WalletGreen
                            )

                            Text(
                                "교환 포인트: %,d P".format(coupon.cost),
                                color = WalletMuted
                            )

                            Text(
                                if (coupon.status == "AVAILABLE") {
                                    "보유 중"
                                } else if (coupon.status == "USED") {
                                    "사용 완료"
                                } else {
                                    "사용 불가"
                                },
                                color = WalletGreen
                            )

                            OutlinedButton(
                                onClick = {
                                    selectedCouponId = coupon.id
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("쿠폰 보기")
                            }
                        }
                    }
                }

                if (loggedIn) {
                    OutlinedButton(
                        onClick = onRetry,
                        enabled = !loading && !exchangeBusy && !paymentBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("쿠폰함 새로고침")
                    }
                }
            } else {
                Text(
                    "포인트 충전",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text("충전할 포인트를 선택해주세요", color = WalletMuted)

                listOf(1000, 3000, 5000).forEach { amount ->
                    Surface(
                        onClick = {
                            if (!paymentBusy) selectedAmount = amount
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedAmount == amount) {
                            Color(0xFFEAF0E4)
                        } else {
                            Color.White
                        }
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAmount == amount,
                                onClick = { selectedAmount = amount },
                                enabled = !paymentBusy
                            )
                            Text(
                                "%,d P / %,d원".format(amount, amount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                Text("결제 수단", fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PaymentProviderButton(
                        title = "카카오페이",
                        logoRes = R.drawable.kakaopay_logo,
                        brandColor = Color(0xFFFFE500),
                        contentColor = Color(0xFF191919),
                        selected = selectedProvider == "KAKAOPAY",
                        enabled = !paymentBusy,
                        onClick = {
                            selectedProvider = "KAKAOPAY"
                        },
                        modifier = Modifier.weight(1f)
                    )

                    PaymentProviderButton(
                        title = "토스페이먼츠",
                        logoRes = R.drawable.tosspayments_logo,
                        brandColor = Color(0xFF0064FF),
                        contentColor = Color.White,
                        selected = selectedProvider == "TOSS",
                        enabled = !paymentBusy,
                        onClick = {
                            selectedProvider = "TOSS"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (paymentError != null) {
                    Text(
                        paymentError,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = { confirming = true },
                    enabled = loggedIn && !paymentBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (paymentBusy) "결제 준비 중…"
                        else "%,d P 충전하기".format(selectedAmount)
                    )
                }

                OutlinedButton(
                    onClick = onRetry,
                    enabled = loggedIn && !loading && !paymentBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("잔액 새로고침")
                }

                Text(
                    "현재는 테스트 결제입니다. 실제 돈은 차감되지 않으며, " +
                            "승인된 충전 포인트는 서버에 저장됩니다.",
                    color = WalletMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("포인트 충전") },
            text = {
                val providerName = if (selectedProvider == "KAKAOPAY") {
                    "카카오페이"
                } else {
                    "토스페이먼츠"
                }

                Text(
                    "$providerName 결제창으로 이동합니다.\n" +
                            "%,d원으로 %,d포인트를 충전합니다.\n".format(
                                selectedAmount,
                                selectedAmount
                            ) +
                            "현재는 테스트 결제로 실제 출금되지 않습니다."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !paymentBusy,
                    onClick = {
                        confirming = false
                        onCharge(selectedProvider, selectedAmount)
                    }
                ) {
                    Text("결제창 열기")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) {
                    Text("취소")
                }
            }
        )
    }
    exchanging?.let { index ->
        val item = ExchangeItems[index]
        val balance = point
        val enough = balance != null && balance >= item.cost

        AlertDialog(
            onDismissRequest = {
                if (!exchangeBusy) exchanging = null
            },
            title = {
                Text(
                    if (enough) "쿠폰으로 교환하기"
                    else "포인트가 부족해요"
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(item.title)

                    Text(
                        if (enough) {
                            "%,d P가 보유 포인트에서 차감됩니다.".format(item.cost)
                        } else {
                            "필요 %,d P · 보유 %,d P".format(
                                item.cost,
                                balance ?: 0
                            )
                        }
                    )

                    if (exchangeError != null) {
                        Text(
                            exchangeError,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = loggedIn &&
                            !loading &&
                            !exchangeBusy &&
                            !paymentBusy,
                    onClick = {
                        if (enough) {
                            onExchange(ExchangeItemIds[index]) { coupon ->
                                exchanging = null
                                tab = 1
                                selectedCouponId = coupon.id
                            }
                        } else {
                            exchanging = null
                            tab = 2
                        }
                    }
                ) {
                    Text(
                        if (exchangeBusy) "교환 중…"
                        else if (enough) "교환하기"
                        else "충전하기"
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !exchangeBusy,
                    onClick = { exchanging = null }
                ) {
                    Text("취소")
                }
            }
        )
    }
    coupons.firstOrNull { it.id == selectedCouponId }?.let { coupon ->
        CouponQrDialog(coupon = coupon, onDismiss = {
            selectedCouponId = null
            onRetry()
        })
    }
}
@Composable
private fun PaymentProviderButton(
    title: String,
    logoRes: Int,
    brandColor: Color,
    contentColor: Color,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = brandColor.copy(alpha = if (enabled) 1f else 0.5f),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                contentColor
            } else {
                brandColor
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterVertically
            )
        ) {
            Box(Modifier.fillMaxWidth().height(40.dp).clipToBounds(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(logoRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.requiredSize(
                        if (logoRes == R.drawable.tosspayments_logo) 180.dp else 100.dp,
                        if (logoRes == R.drawable.tosspayments_logo) 90.dp else 36.dp
                    ),
                    colorFilter = if (logoRes == R.drawable.tosspayments_logo) ColorFilter.tint(Color.White) else null
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (selected) Icon(Icons.Outlined.CheckCircle, "선택됨", tint = contentColor, modifier = Modifier.size(16.dp))
                Text(
                    text = title,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            }
        }
    }
}
