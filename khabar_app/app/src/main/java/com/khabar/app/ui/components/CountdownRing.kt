package com.khabar.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.khabar.app.ui.theme.SystemBlue

@Composable
fun CountdownRing(
    progress: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    strokeWidth: Dp = 4.dp,
    highlightColor: Color = SystemBlue,
    trackColor: Color = Color(0xFFE5E5EA),
    content: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)

            // Background circle
            drawCircle(
                color = trackColor,
                style = stroke
            )

            // Progress arc (-90 degrees is top)
            val sweep = (progress.coerceIn(0f, 1f)) * 360f
            drawArc(
                color = highlightColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = stroke
            )
        }

        if (content != null) {
            content()
        } else {
            Icon(
                imageVector = Icons.Rounded.Alarm,
                contentDescription = "Countdown",
                tint = highlightColor,
                modifier = Modifier.size(size * 0.4f)
            )
        }
    }
}
