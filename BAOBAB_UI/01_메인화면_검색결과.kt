import android.view.View
import android.webkit.WebView
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.viewinterop.AndroidView

private val SearchScreenBackground = Color(0xFFFDF9F1)
private val SearchFieldBackground = Color(0xFFF4F1E9)
private val SearchGreen = Color(0xFF2F5539)
private val SearchMuted = Color(0x80545454)
private val SearchInterBold = FontFamily(Font(R.font.inter_variable, weight = FontWeight.Bold))

private data class SearchCategoryOption(
    val label: String
)

private data class FigmaSearchSurvey(
    val category: String,
    val title: String,
    val author: String,
    val points: String,
    val imageRes: Int,
    val badgeColor: Color,
    val badgeTextColor: Color,
    val badgeFontSize: Int = 11
)

data class SearchResultSurvey(
    val category: String = "지역·사회",
    val title: String = "설문 제목",
    val author: String = "설문 작성자"
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

private val FigmaSearchSurveys = listOf(
    FigmaSearchSurvey(
        category = "생활·편의",
        title = "일상 속 플라스틱 제품 사용 실태 조사",
        author = "자취3년차",
        points = "500P",
        imageRes = R.drawable.search_card_01,
        badgeColor = Color(0xFFF6E2E3),
        badgeTextColor = Color(0xFF800000)
    ),
    FigmaSearchSurvey(
        category = "생활·편의",
        title = "플라스틱 빨대 VS 종이빨대 무엇이 불편하신가요?",
        author = "1일1아아",
        points = "100P",
        imageRes = R.drawable.search_card_01,
        badgeColor = Color(0xFFF6E2E3),
        badgeTextColor = Color(0xFF800000)
    ),
    FigmaSearchSurvey(
        category = "문화·스포츠",
        title = "플라스틱 다회용 그릇 재활용 참여 조사",
        author = "잠실 가고 싶다",
        points = "100P",
        imageRes = R.drawable.search_card_02,
        badgeColor = Color(0xFFF6F0D9),
        badgeTextColor = Color(0xFF806A00)
    ),
    FigmaSearchSurvey(
        category = "경제·상권",
        title = "플라스틱 빨대 vs 종이 빨대 고객 수요조사",
        author = "OO 24시 카페 운영자",
        points = "500P",
        imageRes = R.drawable.search_card_03,
        badgeColor = Color(0xFFF6E8DC),
        badgeTextColor = Color(0xFF803C00)
    ),
    FigmaSearchSurvey(
        category = "건강·의료",
        title = "미세플라스틱 관련 인식조사",
        author = "보건동아리 BOGEON",
        points = "500P",
        imageRes = R.drawable.search_card_04,
        badgeColor = Color(0xFFEBE3F2),
        badgeTextColor = Color(0xFF502060)
    ),
    FigmaSearchSurvey(
        category = "지역·사회",
        title = "분리수거 플라스틱&유리 구분 문의",
        author = "골목 한바퀴",
        points = "400P",
        imageRes = R.drawable.search_card_05,
        badgeColor = Color(0xFFE1ECF4),
        badgeTextColor = Color(0xFF173F6B),
        badgeFontSize = 12
    )
)

@Composable
fun SearchResultsScreen(
    searchTerm: String = "플라스틱",
    onSurveyClick: (SearchResultSurvey) -> Unit = {},
    onCreateSurveyClick: () -> Unit = {},
    onMyClick: () -> Unit = {},
    onSearchTermChange: (String) -> Unit = {}
) {
    var searchQuery by remember(searchTerm) { mutableStateOf(searchTerm) }
    var selectedCategory by remember { mutableStateOf("전체") }
    val visibleSurveys = FigmaSearchSurveys.filter { survey ->
        (selectedCategory == "전체" || survey.category == selectedCategory) &&
            (searchQuery.isBlank() || survey.title.contains(searchQuery, ignoreCase = true) ||
                survey.author.contains(searchQuery, ignoreCase = true))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SearchScreenBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(44.dp))
            SearchField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onSearchTermChange(it)
                }
            )
            SearchCategoryBar(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleSurveys, key = { it.title }) { survey ->
                    SurveyResultCard(
                        survey = survey,
                        onClick = {
                            onSurveyClick(
                                SearchResultSurvey(
                                    category = survey.category,
                                    title = survey.title,
                                    author = survey.author
                                )
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(19.dp))
        }

        SearchFloatingActions(
            onMyClick = onMyClick,
            onCreateSurveyClick = onCreateSurveyClick,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit
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
                .width(370.dp)
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
                    SvgAsset(
                        assetName = "search_icon.svg",
                        modifier = Modifier
                            .size(width = 20.dp, height = 22.dp)
                            .alpha(0.18f)
                    )
                    Box(modifier = Modifier.offset(x = (-2.5).dp)) {
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
            .height(48.dp)
            .clipToBounds()
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .horizontalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
private fun SurveyResultCard(
    survey: FigmaSearchSurvey,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .size(180.dp)
            .shadow(2.dp, cardShape)
            .clip(cardShape)
            .background(Color.White)
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(survey.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 5.dp)
                .size(width = 170.dp, height = 85.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Text(
            text = survey.title,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 105.dp)
                .size(width = 160.dp, height = 36.dp),
            color = Color.Black,
            fontFamily = SearchInterBold,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Clip
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 157.dp)
                .width(160.dp)
                .height(13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = survey.author,
                modifier = Modifier.width(75.dp),
                color = Color(0xFF545454),
                fontFamily = SearchInterBold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = survey.points,
                modifier = Modifier.width(75.dp),
                color = SearchGreen,
                fontFamily = SearchInterBold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 13.sp,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 75.dp)
                .clip(CircleShape)
                .background(survey.badgeColor)
                .border(2.dp, Color.White, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = survey.category,
                color = survey.badgeTextColor,
                fontFamily = SearchInterBold,
                fontSize = survey.badgeFontSize.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SearchFloatingActions(
    onMyClick: () -> Unit,
    onCreateSurveyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(end = 3.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SearchGreen)
                .clickable(onClick = onMyClick),
            contentAlignment = Alignment.Center
        ) {
            SvgAsset(
                assetName = "search_profile_icon.svg",
                modifier = Modifier.size(30.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SearchGreen)
                .clickable(onClick = onCreateSurveyClick),
            contentAlignment = Alignment.Center
        ) {
            SvgAsset(
                assetName = "search_add_icon.svg",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SvgAsset(
    assetName: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                settings.javaScriptEnabled = false
                loadDataWithBaseURL(
                    "file:///android_asset/",
                    """<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"></head><body style=\"margin:0;background:transparent;width:100vw;height:100vh;overflow:hidden\"><img src=\"$assetName\" style=\"display:block;width:100%;height:100%;object-fit:contain\"></body></html>""",
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun SearchResultsScreenPreview() {
    SearchResultsScreen()
}
