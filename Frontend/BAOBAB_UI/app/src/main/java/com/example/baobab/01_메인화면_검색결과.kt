package com.example.baobab

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SearchScreenBackground = Color(0xFFFDF9F1)
private val SearchFieldBackground = Color(0xFFF4F1E9)
private val SearchGreen = Color(0xFF2F5539)
private val SearchMuted = Color(0xFF697369)
//private val SearchInterBold = FontFamily(Font(R.font.inter_variable, weight = FontWeight.Bold))
private val SearchInterBold = FontFamily.SansSerif
private data class SearchCategoryOption(
    val label: String
)

private val SearchCategories = listOf(
    SearchCategoryOption("전체"),
    SearchCategoryOption("생활·편의"),
    SearchCategoryOption("지역·사회"),
    SearchCategoryOption("교육·학습"),
    SearchCategoryOption("문화·스포츠"),
    SearchCategoryOption("경제·상권"),
    SearchCategoryOption("건강·의료")
)

@Composable
fun SearchResultsScreen(
    searchTerm: String = "플라스틱",
    onSurveyClick: (SurveyItem) -> Unit = {},
    onCreateSurveyClick: () -> Unit = {},
    onMyClick: () -> Unit = {},
    onSearchTermChange: (String) -> Unit = {},
    category: String = "전체",
    onCategoryChange: (String) -> Unit = {},
    surveys: List<SurveyItem> = emptyList(),
    loading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var searchQuery by remember(searchTerm) { mutableStateOf(searchTerm) }
    var selectedCategory by remember(category) { mutableStateOf(category) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val visibleSurveys = surveys.filter { survey ->
        (selectedCategory == "전체" || survey.category == selectedCategory) &&
            (searchQuery.isBlank() || survey.title.contains(searchQuery, ignoreCase = true) ||
                survey.author.contains(searchQuery, ignoreCase = true))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SearchScreenBackground)
            .safeDrawingPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Row(Modifier.fillMaxWidth().padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.Outlined.ChevronLeft, "뒤로가기", tint = SearchGreen) }
                Box(Modifier.weight(1f)) {
                    SearchField(searchQuery, { searchQuery = it; onSearchTermChange(it) }, searchFocusRequester)
                }
            }
            SearchCategoryBar(
                selectedCategory = selectedCategory,
                onCategorySelected = {
                    selectedCategory = it
                    onCategoryChange(it)
                }
            )
            SurveyRequestContent(
                loading = loading, error = error, data = visibleSurveys, onRetry = onRetry,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                emptyMessage = "검색 결과가 없습니다."
            ) { items ->
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(156.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 124.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { survey ->
                        SurveyFeedCard(
                            survey = survey,
                            onClick = {
                                onSurveyClick(survey)
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(19.dp))
        }

        SearchFloatingActions(
            onSearchClick = {
                searchFocusRequester.requestFocus()
                keyboardController?.show()
            },
            onCreateSurveyClick = onCreateSurveyClick,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth()
                .height(40.dp),
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0x96000000),
                fontFamily = SearchInterBold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            ),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SearchFieldBackground)
                        .padding(start = 15.5.dp, end = 14.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = SearchMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Box(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        innerTextField()
                    }
                }
            }
        )
    }
}

@Composable
private fun SearchCategoryBar(
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
                .padding(start = 16.dp, top = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchCategories.forEach { category ->
                val selected = selectedCategory == category.label
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) SearchGreen else Color(0x0D545454))
                        .border(
                            width = if (selected) 1.dp else 0.dp,
                            color = if (selected) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onCategorySelected(category.label) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.label,
                        color = if (selected) Color.White else SearchMuted,
                        fontFamily = SearchInterBold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 14.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchFloatingActions(
    onSearchClick: () -> Unit,
    onCreateSurveyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SearchGreen)
                .clickable(onClick = onSearchClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "설문 검색",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SearchGreen)
                .clickable(onClick = onCreateSurveyClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = "설문 만들기",
                tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SearchResultsScreenPreview() {
    SearchResultsScreen(surveys = SampleSurveys)
}
