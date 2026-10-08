package com.example.baobab
import androidx.compose.foundation.layout.offset

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


private val HomeBackground = Color(0xFFFDF9F1)
private val HomeGreen = Color(0xFF2F5539)
private val HomeMuted = Color(0xFF454545)
private val HomeFont = FontFamily.SansSerif

private data class HomeCategory(
    val label: String
)

private val HomeCategories = (listOf("전체") + ParticipationPurposes).map { HomeCategory(it) }

@Composable
fun HomeScreen(
    onSurveyClick: (SurveyItem) -> Unit = {},
    onSearchClick: (String) -> Unit = {},
    onCreateSurveyClick: () -> Unit = {},
    onMyClick: () -> Unit = {},
    onPointClick: () -> Unit = {},
    searchTerm: String = "",
    onSearchTermChange: (String) -> Unit = {},
    surveys: List<SurveyItem> = emptyList(),
    loading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    currentPoint: Int? = null,
    loggedIn: Boolean = false
) {
    var selectedCategory by rememberSaveable { mutableStateOf("전체") }
    val visibleSurveys = if (selectedCategory == "전체") {
        surveys
    } else {
        surveys.filter { participationPurpose(it.category) == selectedCategory }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
            .safeDrawingPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeHeader(onMyClick = onMyClick, onPointClick = onPointClick, currentPoint = currentPoint, loggedIn = loggedIn)
            HomeCategoryBar(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            PullToRefreshBox(isRefreshing = loading, onRefresh = onRetry,
                modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyVerticalGrid(columns = GridCells.Adaptive(156.dp), modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 124.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (error != null) item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(error)
                            TextButton(onRetry) { Text("다시 시도") }
                        }
                    }
                    if (!loading && visibleSurveys.isEmpty() && error == null) item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("아직 등록된 참여 모집이 없어요. 아래로 당겨 새로고침할 수 있어요.", Modifier.padding(24.dp))
                    }
                    items(visibleSurveys, key = { it.id }) { survey ->
                        SurveyFeedCard(survey, onClick = { onSurveyClick(survey) })
                    }
                }
            }
        }
        HomeFloatingActions(
            onSearchClick = { onSearchClick(searchTerm) },
            onCreateSurveyClick = onCreateSurveyClick,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun HomeHeader(onMyClick: () -> Unit, onPointClick: () -> Unit, currentPoint: Int?, loggedIn: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 16.dp, end = 16.dp, top = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BaobabLogo(modifier = Modifier.weight(1f))
        Text(
            text = if (!loggedIn) "로그인" else currentPoint?.let { "%,d P".format(it) } ?: "— P",
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE9DBB8)).clickable(onClick = onPointClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = HomeGreen,
            maxLines = 1
        )
        IconButton(onClick = onMyClick, modifier = Modifier.size(48.dp)
            .padding(start = 4.dp))
        {
            Icon(Icons.Outlined.Person, contentDescription = "마이페이지", modifier = Modifier.size(28.dp), tint = HomeGreen)
        }
    }
}

@Composable
private fun HomeCategoryBar(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()

    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .horizontalScroll(rememberScrollState())
                .padding(start = 15.dp, end = 17.dp, top = 5.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeCategories.forEach { category ->
                val selected = category.label == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) HomeGreen else Color(0x0D545454))
                        .then(
                            if (selected) Modifier.border(1.dp, Color.White, CircleShape)
                            else Modifier
                        )
                        .clickable { onCategorySelected(category.label) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.label,
                        color = if (selected) Color.White else HomeMuted,
                        fontFamily = HomeFont,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun SurveyFeedCard(
    survey: SurveyItem,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(18.dp)
    val badgeShape = RoundedCornerShape(10.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, cardShape)
            .clip(cardShape)
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {

        // 이미지
        // 이미지 + 카테고리 배지
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            SurveyImage(
                survey,
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.55f)
                    .clip(RoundedCornerShape(13.dp)),
                ContentScale.Crop
            )

            Text(
                text = participationPurpose(survey.category),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 7.dp)
                    .offset(y = 13  .dp)
                    .clip(badgeShape)
                    .background(Color(0xFFF6E2E3))
                    .border(
                        width = 3.dp,
                        color = Color.White,
                        shape = badgeShape
                    )
                    .padding(
                        horizontal = 11.dp,
                        vertical = 8.dp
                    ),
                color = Color(0xFF800000),
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        Text(
            text = survey.participationLabel,
            modifier = Modifier.padding(start = 4.dp, top = 10.dp),
            color = HomeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold
        )
        // 제목
        Text(
            text = survey.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 4.dp,
                    end = 4.dp,
                    top = 7.dp
                ),
            color = Color.Black,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Black,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // 작성자 + 포인트
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 4.dp,
                    end = 4.dp,
                    top = 12.dp,
                    bottom = 4.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = survey.author,
                modifier = Modifier.weight(1f),
                color = Color(0xFF666666),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = survey.points,
                color = HomeGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
@Composable
private fun HomeFloatingActions(
    onSearchClick: () -> Unit,
    onCreateSurveyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape)
                .background(HomeGreen).clickable(onClick = onSearchClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Search, contentDescription = "참여 모집 검색", tint = Color.White,
                modifier = Modifier.size(26.dp))
        }
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape)
                .background(HomeGreen).clickable(onClick = onCreateSurveyClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = "참여 모집하기", tint = Color.White,
                modifier = Modifier.size(24.dp))
        }
    }
}
@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(surveys = SampleSurveys)
}
