package com.example.baobab

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChartOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SearchBackground = Color(0xFFFDF9F1)
private val SearchBottomSurface = Color(0xFFFEFBF8)
private val SearchInputSurface = Color(0xFFF4F1E9)
private val SearchBaobabGreen = Color(0xFF2D4F37)
private val SearchMutedText = Color(0x80000000)

private data class SearchCategory(
    val label: String,
    val color: Color,
    val icon: ImageVector
)

data class SearchResultSurvey(
    val category: String = "지역·사회",
    val title: String = "설문 제목",
    val author: String = "설문 작성자"
)

@Composable
fun SearchResultsScreen(
    searchTerm: String = "플라스틱",
    onSurveyClick: (SearchResultSurvey) -> Unit = {},
    onHomeClick: () -> Unit = {},
    onCreateSurveyClick: () -> Unit = {},
    onMyClick: () -> Unit = {}
) {
    var selectedCategory by remember { mutableIntStateOf(0) }
    var searchQuery by remember(searchTerm) { mutableStateOf(searchTerm) }
    val categories = remember {
        listOf(
            SearchCategory("전체", Color(0xFFDFC27D), Icons.Outlined.PieChartOutline),
            SearchCategory("생활·편의", Color(0xFFCEE0D0), Icons.Outlined.ShoppingCart),
            SearchCategory("지역·사회", Color(0xFFC9D7ED), Icons.Outlined.Groups),
            SearchCategory("교육", Color(0xFFFCDCC5), Icons.Outlined.School),
            SearchCategory("창업·사업", Color(0xFFCEB8EC), Icons.Outlined.Business),
            SearchCategory("스포츠·문화", Color(0xFFB9B6B6), Icons.Outlined.SportsSoccer),
            SearchCategory("건강", Color(0xFFE196CE), Icons.Outlined.Person)
        )
    }
    val surveys = remember { List(4) { SearchResultSurvey() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SearchBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(44.dp))
            SearchResultsHeader(onMyClick = onMyClick)
            SearchResultsSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it }
            )
            SearchResultsCategories(
                categories = categories,
                selectedIndex = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            SearchResultsList(
                surveys = surveys,
                onSurveyClick = onSurveyClick,
                modifier = Modifier.weight(1f)
            )
        }

        SearchResultsBottomBar(
            onHomeClick = onHomeClick,
            onCreateSurveyClick = onCreateSurveyClick,
            onMyClick = onMyClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SearchResultsHeader(onMyClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .padding(horizontal = 15.dp)
    ) {
        SearchResultsLogo(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 1.dp, y = (-1).dp)
        )
        Icon(
            imageVector = Icons.Outlined.NotificationsNone,
            contentDescription = "알림",
            tint = Color.Black,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-50).dp)
                .size(35.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFE3E3E3))
                .border(1.dp, Color(0xFF545454), RoundedCornerShape(50))
                .clickable(onClick = onMyClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = "내 프로필",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun SearchResultsLogo(modifier: Modifier = Modifier) {
    val logo = buildAnnotatedString {
        withStyle(SpanStyle(color = SearchBaobabGreen)) { append("BA") }
        withStyle(SpanStyle(color = Color(0xFF563C28))) { append("OB") }
        withStyle(SpanStyle(color = SearchBaobabGreen)) { append("AB") }
    }

    androidx.compose.material3.Text(
        text = logo,
        modifier = modifier.width(150.dp),
        fontFamily = FontFamily(Font(R.font.jaro_regular)),
        fontSize = 35.sp,
        lineHeight = 35.sp,
        textAlign = TextAlign.Start
    )
}

@Composable
private fun SearchResultsSearchBar(
    value: String,
    onValueChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 17.5.dp, end = 14.5.dp)
            .height(60.dp),
        singleLine = true,
        textStyle = TextStyle(
            color = Color.Black,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .offset(y = 9.5.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SearchInputSurface)
                    .padding(horizontal = 15.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "검색",
                    tint = Color(0x4D000000),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.6.dp))
                innerTextField()
            }
        }
    )
}

@Composable
private fun SearchResultsCategories(
    categories: List<SearchCategory>,
    selectedIndex: Int,
    onCategorySelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(125.dp)
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(15.9.dp)
    ) {
        categories.forEachIndexed { index, category ->
            SearchCategoryItem(
                category = category,
                selected = index == selectedIndex,
                onClick = { onCategorySelected(index) }
            )
        }
    }
}

@Composable
private fun SearchCategoryItem(
    category: SearchCategory,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(70.dp)
            .height(100.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(50))
                .background(category.color)
                .border(1.dp, Color(0xFF545454), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.label,
                tint = Color(0xFF737373),
                modifier = Modifier.size(30.dp)
            )
        }
        Text(
            text = category.label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp),
            color = if (selected) Color.Black else SearchMutedText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun SearchResultsList(
    surveys: List<SearchResultSurvey>,
    onSurveyClick: (SearchResultSurvey) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 0.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(surveys) { survey ->
            SearchResultCard(
                survey = survey,
                onClick = { onSurveyClick(survey) }
            )
        }
    }
}

@Composable
private fun SearchResultCard(
    survey: SearchResultSurvey,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(195.dp)
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(start = 9.dp, top = 7.5.dp, bottom = 7.5.dp, end = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .fillMaxSize()
                .background(Color(0xFFE9E9E9)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "NO IMAGE",
                color = Color(0xFFAAAAAA),
                fontSize = 20.sp,
                maxLines = 1
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(start = 14.dp, top = 11.dp, end = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = survey.category,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFC9D7ED))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                color = Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = survey.title,
                modifier = Modifier.padding(top = 8.dp),
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = survey.author,
                modifier = Modifier.padding(top = 41.dp),
                color = Color.Black,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .width(100.dp)
                    .height(35.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE9DBB8)),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "포인트",
                    modifier = Modifier.padding(start = 12.5.dp),
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SearchResultsBottomBar(
    onHomeClick: () -> Unit,
    onCreateSurveyClick: () -> Unit,
    onMyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(93.dp)
            .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
            .background(SearchBottomSurface),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ) {
        SearchBottomItem(
            label = "홈",
            icon = Icons.Outlined.Home,
            onClick = onHomeClick
        )
        SearchBottomItem(
            label = "설문 찾기",
            icon = Icons.Outlined.Search,
            selected = true,
            onClick = {}
        )
        SearchBottomItem(
            label = "설문 만들기",
            icon = Icons.Outlined.PieChartOutline,
            onClick = onCreateSurveyClick
        )
        SearchBottomItem(
            label = "MY",
            icon = Icons.Outlined.Person,
            onClick = onMyClick
        )
    }
}

@Composable
private fun SearchBottomItem(
    label: String,
    icon: ImageVector,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .height(90.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) SearchBaobabGreen else Color.Black,
            modifier = Modifier
                .padding(top = if (selected) 15.33.dp else 16.33.dp)
                .size(30.dp)
        )
        Text(
            text = label,
            modifier = Modifier
                .width(80.dp)
                .padding(top = if (selected) 3.7.dp else 3.dp),
            color = if (selected) SearchBaobabGreen else Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
