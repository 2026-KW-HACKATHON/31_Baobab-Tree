package com.example.baobab

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check

@Composable
fun LocationCheckSection(
    enabled: Boolean,
    onBusyChanged: (Boolean) -> Unit,
    onVerified: (DeviceCoordinates?) -> Unit = {},
    verificationToken: String? = null,
    onSaved: () -> Unit = {},
    initiallyVerified: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentBusyChanged by rememberUpdatedState(onBusyChanged)
    val currentVerified by rememberUpdatedState(onVerified)
    val currentSaved by rememberUpdatedState(onSaved)
    val repository = remember { HttpSurveyRepository(BuildConfig.SURVEY_API_BASE_URL) }
    var saved by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<LocationCheckResult?>(null) }
    var progressText by remember { mutableStateOf("GPS 위치 확인 중…") }
    var busy by remember { mutableStateOf(false) }
    var coordinates by remember { mutableStateOf<DeviceCoordinates?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var skipped by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var requestingPermission by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { currentBusyChanged(false) } }

    fun readLocation() {
        if (busy || !enabled) return
        progressText = "GPS 위치 확인 중…"
        busy = true
        currentBusyChanged(true)
        saved = false
        currentVerified(null)
        result = null
        coordinates = null
        error = null
        skipped = false
        permissionDenied = false
        scope.launch {
            try {
                val point = getCurrentDeviceCoordinates(context)
                coordinates = point
                progressText = "월계1동 경계 확인 중…"
                val checked = withContext(Dispatchers.IO) { repository.checkLocation(point) }
                if (checked.eligible && verificationToken != null) {
                    progressText = "지역 인증 저장 중…"
                    withContext(Dispatchers.IO) { repository.verifyRegion(point, verificationToken) }
                    saved = true
                }
                result = checked
                currentVerified(point.takeIf { checked.eligible })
                if (saved) currentSaved()
            } catch (e: TimeoutCancellationException) {
                error = "위치 확인 시간이 초과되었습니다. 위치 기능을 켜고 창가나 실외에서 다시 시도해주세요."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "현재 위치를 확인하지 못했습니다."
            } finally {
                busy = false
                currentBusyChanged(false)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        requestingPermission = false
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) readLocation()
        else {
            permissionDenied = true
            error = "정확한 위치 권한이 필요합니다. 권한을 허용하거나 다음에 진행해주세요."
        }
    }

    val verified = initiallyVerified || result?.eligible == true

    Surface(
        modifier = Modifier.fillMaxWidth(), color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFD9E0D5))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("월계1동 지역 인증", fontWeight = FontWeight.Bold, color = Color(0xFF2F5539))
            Text(if (verificationToken == null) "위치 인증은 선택 사항입니다. 다음에 진행해도 가입할 수 있습니다." else "현재 위치를 확인하고 계정에 지역 인증을 저장합니다.")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    if (verified) "월계1동 위치 확인 완료" else "월계1동 위치 확인",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2F5539)
                )
                if (busy || requestingPermission) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF2F5539)
                    )
                } else if (verified) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "위치 인증 성공",
                        tint = Color(0xFF2F5539),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Button(
                        onClick = {
                            error = null
                            permissionDenied = false
                            requestingPermission = true
                            permissionLauncher.launch(arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ))
                        },
                        enabled = enabled && !busy && !requestingPermission,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2F5539), contentColor = Color.White
                        )
                    ) {
                        Text("확인")
                    }
                }
            }
            val failureMessage = error ?: result?.takeIf { !it.eligible }?.message
            failureMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (permissionDenied) TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            }, enabled = enabled && !busy && !requestingPermission) { Text("앱 설정에서 위치 권한 허용") }
            if (!verified) TextButton(onClick = { skipped = true; saved = false; currentVerified(null); result = null; coordinates = null; error = null; permissionDenied = false },
                enabled = enabled && !busy && !requestingPermission) { Text("다음에 하기") }
        }
    }
}
