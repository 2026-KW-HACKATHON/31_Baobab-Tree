package com.example.baobab

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class DeviceCoordinates(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val measuredAtMillis: Long
)

@SuppressLint("MissingPermission")
suspend fun getCurrentDeviceCoordinates(
    context: Context
): DeviceCoordinates {
    val appContext = context.applicationContext

    val preciseLocationAllowed = ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    require(preciseLocationAllowed) {
        "지역 인증을 위해 정확한 위치 권한을 허용해주세요."
    }

    return withTimeout(25_000L) {
        suspendCancellableCoroutine<DeviceCoordinates> { continuation ->
            val cancellation = CancellationTokenSource()

            continuation.invokeOnCancellation {
                cancellation.cancel()
            }

            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(10_000L)
                .setDurationMillis(20_000L)
                .build()

            LocationServices.getFusedLocationProviderClient(appContext)
                .getCurrentLocation(request, cancellation.token)
                .addOnSuccessListener { location ->
                    if (!continuation.isActive) {
                        return@addOnSuccessListener
                    }

                    when {
                        location == null -> {
                            continuation.resumeWithException(
                                IllegalStateException(
                                    "현재 위치를 확인하지 못했습니다. 위치 기능을 켜고 다시 시도해주세요."
                                )
                            )
                        }

                        androidx.core.location.LocationCompat.isMock(location) -> {
                            continuation.resumeWithException(
                                IllegalStateException(
                                    "모의 위치로는 지역 인증을 진행할 수 없습니다."
                                )
                            )
                        }

                        !location.hasAccuracy() ||
                                location.accuracy <= 0f ||
                                location.accuracy > 100f -> {
                            continuation.resumeWithException(
                                IllegalStateException(
                                    "위치 오차가 큽니다. 창가나 실외에서 다시 시도해주세요."
                                )
                            )
                        }

                        SystemClock.elapsedRealtimeNanos() -
                                location.elapsedRealtimeNanos !in
                                0L..30_000_000_000L -> {
                            continuation.resumeWithException(
                                IllegalStateException(
                                    "위치 정보가 오래되었습니다. 다시 시도해주세요."
                                )
                            )
                        }

                        else -> {
                            continuation.resume(
                                DeviceCoordinates(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    accuracyMeters = location.accuracy,
                                    measuredAtMillis = location.time
                                )
                            )
                        }
                    }
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
                .addOnCanceledListener {
                    continuation.cancel()
                }
        }
    }
}