package com.example.baobab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.net.URI

internal fun couponRedemptionUrl(couponId: String, baseUrl: String = BuildConfig.SURVEY_API_BASE_URL): String =
    URI(baseUrl.trimEnd('/') + "/").resolve("coupons/$couponId/redeem").toString()

internal fun couponQrMatrix(redemptionUrl: String) = QRCodeWriter().encode(
    redemptionUrl,
    BarcodeFormat.QR_CODE,
    0,
    0,
    mapOf(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to 4)
)

@Composable
internal fun CouponQrDialog(coupon: WalletCoupon, onDismiss: () -> Unit) {
    val qr = remember(coupon.id) {
        runCatching { couponQrMatrix(couponRedemptionUrl(coupon.id)) }.getOrNull()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("쿠폰 QR코드") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(coupon.shop)
                Text(coupon.title, fontWeight = FontWeight.Bold)
                if (coupon.status == "AVAILABLE" && qr != null) {
                    Canvas(
                        Modifier.fillMaxWidth().aspectRatio(1f).semantics {
                            contentDescription = "${coupon.title} 쿠폰 QR코드"
                        }
                    ) {
                        drawRect(Color.White)
                        val moduleSize = kotlin.math.floor(size.minDimension / qr.width)
                        val originX = (size.width - moduleSize * qr.width) / 2f
                        val originY = (size.height - moduleSize * qr.height) / 2f
                        for (y in 0 until qr.height) {
                            for (x in 0 until qr.width) {
                                if (qr[x, y]) drawRect(
                                    Color.Black,
                                    Offset(originX + x * moduleSize, originY + y * moduleSize),
                                    Size(moduleSize, moduleSize)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        when {
                            coupon.status == "USED" -> "이미 사용한 쿠폰입니다."
                            coupon.status != "AVAILABLE" -> "사용할 수 없는 쿠폰입니다."
                            else -> "QR코드를 생성하지 못했습니다. 쿠폰 번호를 확인해주세요."
                        },
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text("쿠폰 번호: ${coupon.id}")
                Text("휴대폰 카메라로 QR코드를 스캔하고 링크를 열면 자동으로 사용 처리됩니다.")
                Text("현재 상품은 테스트 예시이며 실제 매장에서 사용할 수 없습니다.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } }
    )
}
