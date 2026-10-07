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
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val HomeBackground = Color(0xFFFDF9F1)
private val HomeGreen = Color(0xFF2F5539)
private val HomeLogoGreen = Color(0xFF2D4F37)
private val HomeLogoBrown = Color(0xFF66472E)
private val HomeMuted = Color(0x80545454)
private val HomeInter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Bold)
)
private val HomeJaro = FontFamily(Font(R.font.jaro_variable))

private data class HomeCategory(
    val label: String
)

private data class HomeSurvey(
    val category: String,
    val title: String,
    val author: String,
    val points: String,
    val imageRes: Int,
    val categoryBackground: Color,
    val categoryForeground: Color,
    val categoryFontSize: Int = 11
)

data class HomeSurveyItem(
    val category: String,
    val title: String,
    val author: String,
    val points: String
)

private val HomeCategories = listOf(
    HomeCategory("전체"),
    HomeCategory("생활·편의"),
    HomeCategory("지역·사회"),
    HomeCategory("교육·학습"),
    HomeCategory("문화·스포츠"),
    HomeCategory("경제·상권"),
    HomeCategory("건강·의료")
)

private val HomeSurveys = listOf(
    HomeSurvey("생활·편의", "월계동 소상공인 이용 만족도 조사", "월계동 상인회", "500P", R.drawable.participated_survey_01, Color(0xFFF6E2E3), Color(0xFF800000)),
    HomeSurvey("교육·학습", "월계동 영유아 돌봄교실 운영 수요조사", "월계 엄마", "100P", R.drawable.participated_survey_02, Color(0xFFE3F0E4), Color(0xFF1F6035), 12),
    HomeSurvey("지역·사회", "월계 OO빌라 근처 불법 주차 관련 인식 조사", "월계1동전문가", "500P", R.drawable.participated_survey_03, Color(0xFFE1ECF4), Color(0xFF173F6B), 12),
    HomeSurvey("문화·스포츠", "월계동 자취생 야구 단관 구인 (20명)", "두산 이겨라", "100P", R.drawable.participated_survey_04, Color(0xFFF6F0D9), Color(0xFF806A00)),
    HomeSurvey("경제·상권", "oo 24시 무인 카페 사용도 조사", "OO 24시 카페 운영자", "500P", R.drawable.participated_survey_05, Color(0xFFF6E8DC), Color(0xFF803C00)),
    HomeSurvey("건강·의료", "야간 소음으로 인한 OO빌라 수면 만족도 조사", "컴공은 피곤하다", "500P", R.drawable.participated_survey_06, Color(0xFFEBE3F2), Color(0xFF502060)),
    HomeSurvey("생활·편의", "배X, 요X요 등 배달 어플 수요 조사", "월계동 상인회", "500P", R.drawable.participated_survey_07, Color(0xFFF6E2E3), Color(0xFF800000)),
    HomeSurvey("지역·사회", "광운대 앞 지쿠터 마구잡이 주차로 인한 불편성", "월계동 상인회", "500P", R.drawable.participated_survey_08, Color(0xFFE1ECF4), Color(0xFF173F6B), 11)
)

@Composable
fun HomeScreen(
    onSurveyClick: (HomeSurveyItem) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onCreateSurveyClick: () -> Unit = {},
    onMyClick: () -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf("전체") }
    val visibleSurveys = if (selectedCategory == "전체") {
        HomeSurveys
    } else {
        HomeSurveys.filter { it.category == selectedCategory }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(44.dp))
            HomeHeader(onSearchClick = onSearchClick)
            HomeCategoryBar(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(713.dp)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleSurveys, key = { it.title }) { survey ->
                    HomeSurveyCard(
                        survey = survey,
                        onClick = {
                            onSurveyClick(
                                HomeSurveyItem(
                                    category = survey.category,
                                    title = survey.title,
                                    author = survey.author,
                                    points = survey.points
                                )
                            )
                        }
                    )
                }
            }
        }

        HomeFloatingActions(
            onMyClick = onMyClick,
            onCreateSurveyClick = onCreateSurveyClick,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun HomeHeader(onSearchClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(start = 16.dp, end = 16.dp, top = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val logo = buildAnnotatedString {
            withStyle(SpanStyle(color = HomeLogoGreen)) { append("BA") }
            withStyle(SpanStyle(color = HomeLogoBrown)) { append("OB") }
            withStyle(SpanStyle(color = HomeLogoGreen)) { append("AB") }
        }
        Text(
            text = logo,
            modifier = Modifier.width(119.dp),
            fontFamily = HomeJaro,
            fontSize = 35.sp,
            lineHeight = 35.sp
        )
        Box(
            modifier = Modifier
                .size(30.dp)
                .clickable(onClick = onSearchClick),
            contentAlignment = Alignment.Center
        ) {
            SvgAsset("search_icon.svg", Modifier.fillMaxSize())
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
            .height(48.dp)
            .clipToBounds()
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .horizontalScroll(rememberScrollState())
                .padding(start = 15.dp, end = 17.dp, top = 5.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                        fontFamily = HomeInter,
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
private fun HomeSurveyCard(
    survey: HomeSurvey,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(180.dp)
            .shadow(2.dp, shape)
            .clip(shape)
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
            fontFamily = HomeInter,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Clip
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, end = 10.dp, top = 157.dp)
                .width(160.dp)
                .height(13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = survey.author,
                modifier = Modifier.width(75.dp),
                color = Color(0xFF545454),
                fontFamily = HomeInter,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = survey.points,
                modifier = Modifier.width(75.dp),
                color = HomeGreen,
                fontFamily = HomeInter,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 75.dp)
                .clip(CircleShape)
                .background(survey.categoryBackground)
                .border(2.dp, Color.White, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = survey.category,
                color = survey.categoryForeground,
                fontFamily = HomeInter,
                fontSize = survey.categoryFontSize.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HomeFloatingActions(
    onMyClick: () -> Unit,
    onCreateSurveyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(HomeGreen)
                .clickable(onClick = onMyClick),
            contentAlignment = Alignment.Center
        ) {
            SvgAsset("search_profile_icon.svg", Modifier.size(30.dp))
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(HomeGreen)
                .clickable(onClick = onCreateSurveyClick),
            contentAlignment = Alignment.Center
        ) {
            SvgAsset("search_add_icon.svg", Modifier.size(24.dp))
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
                    """<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\"></head><body style=\"margin:0;background:transparent;width:100%;height:100%;overflow:hidden\"><img src=\"$assetName\" style=\"display:block;width:100%;height:100%;object-fit:contain\"></body></html>""",
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
private fun HomeScreenPreview() {
    HomeScreen()
}
