package com.nh.magiceraser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private data class EmberSpark(val x: Float, val speed: Float, val radius: Float, val phase: Float, val alpha: Float)

@Composable
fun NHBackground(content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "amoled_bg")
    val orbit by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orbit"
    )
    val drift by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )
    val pulse by t.animateFloat(
        0.65f, 1f,
        infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val sparks = remember {
        val rng = Random(42)
        List(22) {
            EmberSpark(
                x = rng.nextFloat(),
                speed = 0.4f + rng.nextFloat() * 0.8f,
                radius = 1.8f + rng.nextFloat() * 3.5f,
                phase = rng.nextFloat(),
                alpha = 0.16f + rng.nextFloat() * 0.40f
            )
        }
    }

    Box(
        Modifier.fillMaxSize().background(NH.Bg).drawBehind {
            val w = size.width
            val h = size.height

            val step = 68f
            val shiftY = (drift * step) % step
            val gridColor = NH.Red.copy(alpha = 0.04f)
            var gx = 0f
            while (gx < w) {
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), 1f)
                gx += step
            }
            var gy = shiftY
            while (gy < h) {
                drawLine(gridColor, Offset(0f, gy), Offset(w, gy), 1f)
                gy += step
            }

            val r1 = w * 0.85f
            val c1 = Offset(w * (0.12f + 0.25f * orbit), h * (0.06f + 0.05f * orbit))
            drawCircle(
                Brush.radialGradient(
                    listOf(NH.Red.copy(alpha = 0.24f * pulse), NH.DarkRed.copy(alpha = 0.08f), Color.Transparent),
                    c1, r1
                ),
                r1, c1
            )

            val r2 = w * 0.82f
            val c2 = Offset(w * (0.88f - 0.22f * orbit), h * (0.92f - 0.06f * orbit))
            drawCircle(
                Brush.radialGradient(
                    listOf(NH.Scarlet.copy(alpha = 0.18f * pulse), NH.DarkRed.copy(alpha = 0.06f), Color.Transparent),
                    c2, r2
                ),
                r2, c2
            )

            sparks.forEach { s ->
                val prog = (drift * s.speed + s.phase) % 1f
                val sy = h * (1.05f - prog * 1.1f)
                val sx = w * s.x + sin((prog * 2f * PI).toFloat()) * 14f
                val fade = sin((prog * PI).toFloat()).coerceIn(0f, 1f)
                drawCircle(
                    NH.Red.copy(alpha = s.alpha * fade * 0.35f),
                    radius = s.radius * 2.4f,
                    center = Offset(sx, sy)
                )
                drawCircle(
                    NH.Scarlet.copy(alpha = s.alpha * fade),
                    radius = s.radius,
                    center = Offset(sx, sy)
                )
            }
        },
        content = content
    )
}

fun Modifier.glass(shape: Shape = RoundedCornerShape(20.dp), alpha: Float = 0.08f): Modifier =
    this.clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF1A090E).copy(alpha = (alpha * 8.5f).coerceIn(0.48f, 0.92f)),
                    Color(0xFF080406).copy(alpha = (alpha * 9.5f).coerceIn(0.65f, 0.96f))
                )
            )
        )
        .border(
            1.dp,
            Brush.linearGradient(
                listOf(
                    NH.Red.copy(alpha = 0.42f),
                    Color.White.copy(alpha = 0.07f),
                    NH.Scarlet.copy(alpha = 0.20f)
                )
            ),
            shape
        )

@Composable
fun CyberFade(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 0.94f),
        exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.94f)
    ) {
        content()
    }
}

@Composable
fun GradientText(text: String, size: Int, brush: Brush = NH.Accent, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            brush = brush,
            fontSize = size.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = (size * 1.08f).sp,
            letterSpacing = (-0.4).sp
        )
    )
}

@Composable
fun PulsingDot(color: Color = NH.Red, size: Dp = 6.dp) {
    val t = rememberInfiniteTransition(label = "dot")
    val sc by t.animateFloat(
        0.85f, 1.45f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dsc"
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size * 1.8f)) {
        Box(
            Modifier.size(size)
                .graphicsLayer { scaleX = sc; scaleY = sc; alpha = 1.55f - sc }
                .clip(CircleShape)
                .background(color.copy(alpha = 0.45f))
        )
        Box(Modifier.size(size).clip(CircleShape).background(color))
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    brush: Brush = NH.Accent
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "gb"
    )
    val t = rememberInfiniteTransition(label = "btn_sheen")
    val sheen by t.animateFloat(
        -0.4f, 1.4f,
        infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "sheen"
    )

    Box(
        modifier
            .graphicsLayer { scaleX = sc; scaleY = sc; alpha = if (enabled) 1f else 0.35f }
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(Color.White.copy(alpha = 0.35f), NH.Scarlet.copy(alpha = 0.25f), Color.White.copy(alpha = 0.12f))
                ),
                RoundedCornerShape(16.dp)
            )
            .drawBehind {
                if (enabled) {
                    val x = sheen * size.width
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.20f), Color.Transparent),
                            Offset(x - size.width * 0.2f, 0f),
                            Offset(x + size.width * 0.2f, size.height)
                        )
                    )
                }
            }
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(
                text,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun IconAction(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Color.White,
    size: Dp = 36.dp
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.88f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "ia")
    Box(
        modifier
            .size(size)
            .graphicsLayer { scaleX = sc; scaleY = sc; alpha = if (enabled) 1f else 0.32f }
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF17090E))
            .border(
                1.dp,
                Brush.linearGradient(listOf(NH.Red.copy(alpha = 0.5f), Color.White.copy(alpha = 0.08f))),
                RoundedCornerShape(12.dp)
            )
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = if (enabled) tint else NH.TextLo, modifier = Modifier.size(size * 0.5f)) }
}

@Composable
fun ToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.9f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "tb")
    Column(
        modifier
            .graphicsLayer { scaleX = sc; scaleY = sc; alpha = if (enabled) 1f else 0.32f }
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            Modifier.size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    if (selected) NH.Accent
                    else Brush.verticalGradient(listOf(Color(0xFF1F0C12), Color(0xFF11070A)))
                )
                .border(
                    1.dp,
                    if (selected) Color.White.copy(alpha = 0.55f) else NH.Red.copy(alpha = 0.28f),
                    RoundedCornerShape(13.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (selected) Color.White else NH.TextMid, modifier = Modifier.size(19.dp))
        }
        Text(
            label,
            fontSize = 10.sp,
            color = if (selected) NH.Scarlet else NH.TextLo,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.94f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "pill")
    Box(
        modifier
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) NH.Accent
                else Brush.horizontalGradient(listOf(Color(0xFF1A0B10), Color(0xFF12070B)))
            )
            .border(
                1.dp,
                if (selected) Color.White.copy(alpha = 0.45f) else NH.Red.copy(alpha = 0.28f),
                RoundedCornerShape(50)
            )
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color.White else NH.TextMid,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onFinished: (() -> Unit)? = null
) {
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 11.sp, color = NH.TextMid, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .background(NH.Red.copy(alpha = 0.16f))
                    .border(1.dp, NH.Red.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 1.dp)
            ) {
                Text(valueText, fontSize = 10.sp, color = NH.Scarlet, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }
        }
        Slider(
            value, onChange,
            modifier = Modifier.height(32.dp),
            valueRange = range,
            enabled = enabled,
            onValueChangeFinished = onFinished,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = NH.Red,
                inactiveTrackColor = Color(0xFF2A1219),
                disabledThumbColor = Color.White.copy(alpha = 0.35f),
                disabledActiveTrackColor = NH.Red.copy(alpha = 0.25f),
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.08f)
            )
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .glass(RoundedCornerShape(16.dp), 0.09f)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconAction(NHIcons.Back, onBack, size = 34.dp)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PulsingDot(NH.Red, 6.dp)
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = 10.sp,
                    color = NH.TextLo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
        trailing()
    }
}

@Composable
fun ProgressRing(progress: Float, modifier: Modifier = Modifier, stroke: Float = 14f) {
    val spin = rememberInfiniteTransition(label = "ring")
    val glow by spin.animateFloat(
        0.3f, 0.75f,
        infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val rot by spin.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot"
    )
    Canvas(modifier) {
        val d = min(size.width, size.height) - stroke * 2.4f
        val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
        val sz = Size(d, d)

        val outerD = d + stroke * 1.5f
        val outerTl = Offset((size.width - outerD) / 2f, (size.height - outerD) / 2f)
        rotate(rot) {
            drawArc(
                NH.Red.copy(alpha = 0.35f),
                0f, 360f, false,
                outerTl, Size(outerD, outerD),
                style = Stroke(2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 14f), 0f))
            )
        }

        drawArc(Color(0xFF260E15), 0f, 360f, false, tl, sz, style = Stroke(stroke))

        val sweep = 360f * progress.coerceIn(0f, 1f)
        val brush = Brush.sweepGradient(listOf(NH.Red, NH.Scarlet, NH.Ember, NH.Red))
        rotate(-90f) {
            drawArc(brush, 0f, sweep, false, tl, sz, alpha = glow, style = Stroke(stroke * 1.9f, cap = StrokeCap.Round))
            drawArc(brush, 0f, sweep, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}

@Composable
fun AnimatedCheck(modifier: Modifier = Modifier, color: Color = NH.Red) {
    val p = remember { Animatable(0f) }
    val pop = remember { Animatable(0.55f) }
    val spin = rememberInfiniteTransition(label = "chk_spin")
    val ringRot by spin.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "rrot"
    )
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(950, easing = FastOutSlowInEasing)) }
    Canvas(modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
        val w = size.width
        val stroke = w * 0.075f
        val v = p.value
        drawCircle(color.copy(alpha = 0.18f * v), radius = w / 2f)
        drawCircle(color.copy(alpha = 0.08f * v), radius = w * 0.62f)
        rotate(ringRot) {
            drawCircle(
                NH.Scarlet.copy(alpha = 0.4f * v),
                radius = w * 0.54f,
                style = Stroke(2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f), 0f))
            )
        }
        val arcSize = Size(w - stroke, w - stroke)
        rotate(-90f) {
            drawArc(
                Brush.sweepGradient(listOf(NH.Red, NH.Scarlet, NH.Ember, NH.Red)),
                0f, 360f * min(1f, v / 0.6f), false,
                Offset(stroke / 2, stroke / 2), arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        val t = ((v - 0.55f) / 0.45f).coerceIn(0f, 1f)
        if (t > 0f) {
            val a = Offset(w * 0.30f, w * 0.52f)
            val b = Offset(w * 0.45f, w * 0.67f)
            val c = Offset(w * 0.72f, w * 0.38f)
            if (t <= 0.4f) {
                val f = t / 0.4f
                drawLine(Color.White, a, Offset(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f), stroke, StrokeCap.Round)
            } else {
                val f = (t - 0.4f) / 0.6f
                drawLine(Color.White, a, b, stroke, StrokeCap.Round)
                drawLine(Color.White, b, Offset(b.x + (c.x - b.x) * f, b.y + (c.y - b.y) * f), stroke, StrokeCap.Round)
            }
        }
    }
}

private data class Particle(val vx: Float, val vy: Float, val color: Int, val size: Float, val rot: Float)

@Composable
fun Confetti(modifier: Modifier = Modifier) {
    val parts = remember {
        val r = Random(7)
        List(56) {
            Particle(
                (r.nextFloat() - 0.5f) * 1.35f, 0.35f + r.nextFloat() * 0.65f,
                r.nextInt(4), 7f + r.nextFloat() * 9f, r.nextFloat() * 360f
            )
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(3200, easing = LinearEasing)) }
    val cols = listOf(NH.Red, NH.Scarlet, NH.Ember, Color.White)
    Canvas(modifier.fillMaxSize()) {
        val tt = t.value
        parts.forEach { p ->
            val x = size.width * (0.5f + p.vx * tt)
            val y = size.height * 0.30f - size.height * p.vy * tt * 0.8f + size.height * 1.1f * tt * tt
            val a = (1f - tt * tt).coerceIn(0f, 1f)
            rotate(p.rot + tt * 540f, Offset(x, y)) {
                drawRoundRect(
                    cols[p.color].copy(alpha = a),
                    Offset(x - p.size / 2, y - p.size / 4),
                    Size(p.size, p.size / 2),
                    CornerRadius(3f, 3f)
                )
            }
        }
    }
}

@Composable
fun ScanOverlay(modifier: Modifier = Modifier, color: Color = NH.Red) {
    val t = rememberInfiniteTransition(label = "scan")
    val p by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p"
    )
    Canvas(modifier) {
        val y = size.height * p
        val band = 120f
        drawRect(
            Brush.verticalGradient(
                listOf(Color.Transparent, color.copy(alpha = 0.35f), Color.Transparent),
                startY = y - band, endY = y + band
            ),
            Offset(0f, y - band), Size(size.width, band * 2)
        )
        drawLine(Color.White.copy(alpha = 0.95f), Offset(0f, y), Offset(size.width, y), 3f)
        drawLine(color, Offset(0f, y), Offset(size.width, y), 8f)
    }
}
