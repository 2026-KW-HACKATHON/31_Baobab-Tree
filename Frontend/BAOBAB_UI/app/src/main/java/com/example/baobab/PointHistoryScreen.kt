package com.example.baobab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PointHistoryItem(
    val id: Int,
    val amount: Int,
    val description: String,
    val createdAt: String
)

data class PointHistoryResponse(
    val point: Int,
    val histories: List<PointHistoryItem>
)

@Composable
fun PointHistoryScreen(
    token: String?,
    onBack: () -> Unit
) {
    val green = Color(0xFF2F5539)
    val muted = Color(0xFF697369)

    val repository = remember {
        HttpSurveyRepository(BuildConfig.SURVEY_API_BASE_URL)
    }

    var response by remember(token) {
        mutableStateOf<PointHistoryResponse?>(null)
    }
    var loading by remember(token) { mutableStateOf(false) }
    var error by remember(token) { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableStateOf("전체") }

    LaunchedEffect(token, refreshKey) {
        response = null
        error = null

        if (token == null) {
            loading = false
            return@LaunchedEffect
        }

        loading = true

        try {
            response = withContext(Dispatchers.IO) {
                repository.getPointHistory(token)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "포인트 내역을 불러오지 못했습니다."
        } finally {
            loading = false
        }
    }

    val histories = response?.histories.orEmpty()
    val visibleHistories = when (filter) {
        "획득" -> histories.filter { it.amount > 0 }
        "사용" -> histories.filter { it.amount < 0 }
        else -> histories
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCF9F0))
            .safeDrawingPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text("뒤로", color = green)
            }

            Text(
                text = "포인트 이용 내역",
                modifier = Modifier.align(Alignment.Center),
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = green
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = green,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("현재 보유 포인트", color = Color.White)

                    Text(
                        text = response?.point?.let {
                            String.format(Locale.KOREA, "%,d P", it)
                        } ?: "—",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("전체", "획득", "사용").forEach { label ->
                    FilterChip(
                        selected = filter == label,
                        onClick = { filter = label },
                        label = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        when {
            token == null -> {
                Text(
                    text = "로그인 후 포인트 내역을 확인할 수 있습니다.",
                    color = muted,
                    modifier = Modifier.padding(20.dp)
                )
            }

            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = green)
                }
            }

            error != null -> {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = error ?: "포인트 내역 조회 실패",
                        color = MaterialTheme.colorScheme.error
                    )

                    OutlinedButton(onClick = { refreshKey++ }) {
                        Text("다시 불러오기")
                    }
                }
            }

            visibleHistories.isEmpty() -> {
                Text(
                    text = when (filter) {
                        "획득" -> "포인트 획득 내역이 없습니다."
                        "사용" -> "포인트 사용 내역이 없습니다."
                        else -> "포인트 이용 내역이 없습니다."
                    },
                    color = muted,
                    modifier = Modifier.padding(20.dp)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = visibleHistories,
                        key = { it.id }
                    ) { item ->
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = pointHistoryDescription(
                                            item.description
                                        ),
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Text(
                                        text = pointHistoryDate(item.createdAt),
                                        color = muted,
                                        fontSize = 12.sp
                                    )
                                }

                                Text(
                                    text = pointHistoryAmount(item.amount),
                                    color = if (item.amount >= 0) {
                                        green
                                    } else {
                                        Color(0xFFB64545)
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun pointHistoryAmount(amount: Int): String {
    val sign = when {
        amount > 0 -> "+"
        amount < 0 -> "-"
        else -> ""
    }

    val number = String.format(
        Locale.KOREA,
        "%,d",
        kotlin.math.abs(amount.toLong())
    )

    return "$sign$number P"
}

private fun pointHistoryDate(value: String): String {
    return runCatching {
        OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
    }.getOrDefault(value)
}

private fun pointHistoryDescription(value: String): String {
    val labels = listOf(
        "Survey reward: " to "설문 참여 보상: ",
        "Survey registration reward budget: " to "설문 등록 리워드 예산: ",
        "Survey unused reward refund: " to "설문 미사용 리워드 환급: "
    )

    val label = labels.firstOrNull { value.startsWith(it.first) }
        ?: return value

    return label.second + value.removePrefix(label.first)
}