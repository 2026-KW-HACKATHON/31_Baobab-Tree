package com.example.baobab

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.baobab.ui.theme.BAOBABTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class RecruitmentLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val slot = RecruitmentSlot(9, 42, "2099-12-02T01:00:00Z", "2099-12-02T03:00:00Z")
    private val item = RecruitmentItem(42, 1, RecruitmentAuthor(1, "광운대학교 연구실"),
        "광운대학교 학생과 월계1동 주민을 위한 실험 참여자 모집 안내", "광운대학교 사용자 경험 연구실",
        "EXPERIMENT", "OFFLINE", recruitmentDescription("학술·연구", "일상 속 경험을 알아보는 연구입니다.\n긴 안내문도 줄바꿈하여 읽을 수 있습니다."),
        "광운대학교 학생 또는 월계1동 주민 누구나 신청할 수 있습니다.", "서울시 노원구 광운대학교 연구실, 자세한 장소는 선정 후 안내드립니다.",
        60, 10, 2, 1000, "2099-12-01T00:00:00Z", "OPEN", "2026-10-08T00:00:00Z", listOf(slot))

    @Test fun detailKeepsLongTitleAndFixedActionSeparateAtLargeFont() {
        var managed = false
        compose.setContent {
            BAOBABTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.4f)) {
                    Box(Modifier.width(320.dp).fillMaxHeight()) {
                        RecruitmentDetailScreen(item, false, false, null, "모집을 등록했습니다.", true, true,
                            {}, {}, {}, { managed = true }, { _, _ -> })
                    }
                }
            }
        }
        compose.onNodeWithText(item.organization).performScrollTo()
        val title = compose.onNodeWithText(item.title, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val organization = compose.onNodeWithText(item.organization, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("Title must not overlap organization", title.bottom <= organization.top)
        compose.onNodeWithText("신청자 관리").assertIsDisplayed()
        screenshot("recruitment-detail.png")
        compose.onNodeWithText(item.location).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("신청자 관리").performClick()
        assertTrue(managed)
    }

    @Test fun ownedAndApplicationCardsStayReadableAndActionsWorkAtLargeFont() {
        var managed = false
        var cancelled = false
        compose.setContent {
            BAOBABTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.4f)) {
                    Column(Modifier.width(320.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        MyOwnedRecruitmentCard(item) { managed = true }
                        RecruitmentApplicationCard(RecruitmentApplicationItem(7, "ACCEPTED", null, item.createdAt, null, null, null, slot, item),
                            true, {}, { cancelled = true }, {})
                    }
                }
            }
        }
        compose.onNodeWithText("신청자 관리 →").performClick()
        assertTrue(managed)
        compose.onNodeWithText("신청 취소").performScrollTo().assertIsDisplayed()
        screenshot("recruitment-activity.png")
        compose.onNodeWithText("신청 취소").performClick()
        assertTrue(cancelled)
    }

    private fun screenshot(name: String) {
        val folder = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)!!
        File(folder, name).outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
