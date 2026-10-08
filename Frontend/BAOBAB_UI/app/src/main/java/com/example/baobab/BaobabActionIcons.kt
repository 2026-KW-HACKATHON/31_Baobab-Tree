package com.example.baobab

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal object BaobabActionIcons {
    val Edit = Icons.Outlined.Edit
    val Share = Icons.Outlined.Share
    val Delete = ImageVector.Builder("LinedTrash", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(3f, 6f); lineTo(21f, 6f)
            moveTo(8f, 6f); lineTo(8f, 3f); lineTo(16f, 3f); lineTo(16f, 6f)
            moveTo(5f, 6f); lineTo(6f, 21f); lineTo(18f, 21f); lineTo(19f, 6f)
            moveTo(9f, 10f); lineTo(9f, 17f)
            moveTo(12f, 10f); lineTo(12f, 17f)
            moveTo(15f, 10f); lineTo(15f, 17f)
        }
    }.build()
}

@Composable
internal fun BaobabActionButton(icon: ImageVector, description: String, onClick: () -> Unit,
    enabled: Boolean = true, destructive: Boolean = false) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else Color(0xFF2F5539)
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
        Icon(icon, description, Modifier.size(24.dp), tint = if (enabled) tint else tint.copy(alpha = 0.38f))
    }
}
