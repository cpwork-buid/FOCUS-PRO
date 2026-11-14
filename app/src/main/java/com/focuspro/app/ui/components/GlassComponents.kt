package com.focuspro.app.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.focuspro.app.ui.theme.DarkCardSurface
import com.focuspro.app.ui.theme.DeepNavyBlue
import com.focuspro.app.ui.theme.ElectricPurple
import com.focuspro.app.ui.theme.LightBlue
import com.focuspro.app.ui.theme.LightCardSurface
import com.focuspro.app.ui.theme.MidnightBlack
import com.focuspro.app.ui.theme.PremiumWhite
import com.focuspro.app.ui.theme.SoftCyan
import com.focuspro.app.ui.theme.SoftLavender
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FocusGradientBackground(
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    val brush = if (isDark) {
        Brush.verticalGradient(listOf(MidnightBlack, DeepNavyBlue, Color(0xFF13112B)))
    } else {
        Brush.verticalGradient(listOf(PremiumWhite, LightBlue.copy(alpha = 0.45f), SoftLavender.copy(alpha = 0.55f)))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
    ) {
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color = ElectricPurple,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == MidnightBlack
    val containerColor = if (isDark) DarkCardSurface else LightCardSurface
    val borderBrush = Brush.linearGradient(
        listOf(
            accentColor.copy(alpha = 0.45f),
            SoftCyan.copy(alpha = 0.25f)
        )
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderBrush),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun AnimatedCircularProgress(
    progress: Float,
    centerTitle: String,
    centerSubtitle: String,
    size: Dp = 148.dp,
    strokeWidth: Dp = 12.dp,
    accentStart: Color = ElectricPurple,
    accentEnd: Color = SoftCyan,
    animate: Boolean = true
) {
    val target = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = if (animate) 800 else 0, easing = FastOutSlowInEasing),
        label = "circle_progress"
    )
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(accentStart, accentEnd, accentStart)),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = centerSubtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AppIconImage(
    drawable: Drawable?,
    contentDescription: String,
    size: Dp = 40.dp
) {
    val bitmap = remember(drawable) {
        runCatching { drawable?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
                .background(ElectricPurple.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contentDescription.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                color = ElectricPurple
            )
        }
    }
}

@Composable
fun ModuleHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = accent)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ConfettiCelebrationOverlay(
    visible: Boolean,
    onFinished: () -> Unit
) {
    if (!visible) return
    val progress = remember { Animatable(0f) }
    val particles = remember {
        List(36) {
            Triple(
                Random.nextFloat() * 360f,
                Random.nextFloat() * 0.7f + 0.3f,
                listOf(ElectricPurple, SoftCyan, Color(0xFFEC4899), Color(0xFF10B981), Color(0xFFF59E0B)).random()
            )
        }
    }

    LaunchedEffect(visible) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(1100, easing = FastOutSlowInEasing))
        onFinished()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2.5f)
        val maxRadius = size.minDimension * 0.45f
        particles.forEach { (angleDeg, speed, color) ->
            val rad = Math.toRadians(angleDeg.toDouble())
            val dist = maxRadius * speed * progress.value
            val x = center.x + (cos(rad) * dist).toFloat()
            val y = center.y + (sin(rad) * dist).toFloat() + (progress.value * progress.value * 160f)
            drawCircle(
                color = color.copy(alpha = (1f - progress.value).coerceIn(0f, 1f)),
                radius = 10f * (1f - progress.value * 0.4f),
                center = Offset(x, y)
            )
        }
    }
}
