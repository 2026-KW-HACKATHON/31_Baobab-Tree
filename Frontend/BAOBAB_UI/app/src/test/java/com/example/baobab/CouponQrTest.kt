package com.example.baobab

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Test

class CouponQrTest {
    @Test
    fun qrDecodesToCouponRedemptionUrl() {
        val couponId = "550e8400-e29b-41d4-a716-446655440000"
        val url = couponRedemptionUrl(couponId, "https://baobab.example/api/")
        assertEquals("https://baobab.example/api/coupons/$couponId/redeem", url)
        assertEquals(url, couponRedemptionUrl(couponId, "https://baobab.example/api"))
        val matrix = couponQrMatrix(url)
        val scale = 8
        val width = matrix.width * scale
        val height = matrix.height * scale
        val pixels = IntArray(width * height) { index ->
            if (matrix[(index % width) / scale, (index / width) / scale]) {
                0xFF000000.toInt()
            } else {
                0xFFFFFFFF.toInt()
            }
        }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels)))
        assertEquals(url, QRCodeReader().decode(bitmap).text)
    }
}
