package com.example.baobab

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.net.URI

private const val LocalNetworkPermission = "android.permission.ACCESS_LOCAL_NETWORK"

fun usesLocalNetwork(baseUrl: String): Boolean {
    val host = URI(baseUrl).host?.lowercase()?.removeSurrounding("[", "]") ?: return false
    val parts = host.split('.').mapNotNull { it.toIntOrNull() }
    return host == "localhost" || host.endsWith(".local") || host == "::1" ||
        host.startsWith("fc") && host.contains(':') || host.startsWith("fd") && host.contains(':') ||
        host.startsWith("fe80:") || (parts.size == 4 && parts.all { it in 0..255 } &&
        (parts[0] == 10 || parts[0] == 127 || parts[0] == 192 && parts[1] == 168 ||
            parts[0] == 172 && parts[1] in 16..31 || parts[0] == 169 && parts[1] == 254))
}

@Composable
fun LocalNetworkPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val needsPermission = Build.VERSION.SDK_INT >= 37 && usesLocalNetwork(BuildConfig.SURVEY_API_BASE_URL)
    fun hasPermission() = !needsPermission || ContextCompat.checkSelfPermission(
        context, LocalNetworkPermission
    ) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(hasPermission()) }
    var requested by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = hasPermission()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(needsPermission) {
        if (!granted && !requested) {
            requested = true
            launcher.launch(LocalNetworkPermission)
        }
    }
    if (granted) {
        content()
    } else {
        SurveyRequestContent<Unit>(
            loading = false,
            error = "로컬 서버에 연결하려면 주변 기기 접근 권한을 허용해주세요.",
            data = null,
            onRetry = {
                granted = hasPermission()
                if (!granted) launcher.launch(LocalNetworkPermission)
            },
            modifier = Modifier.fillMaxSize()
        ) {}
    }
}
