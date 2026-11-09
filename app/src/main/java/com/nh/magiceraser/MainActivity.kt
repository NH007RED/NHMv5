package com.nh.magiceraser

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class Screen { Splash, Home, Image, Video }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT)
        )
        setContent {
            NHTheme {
                NHBackground { App() }
            }
        }
    }
}

@Composable
fun App() {
    var screen by remember { mutableStateOf(Screen.Splash) }
    LaunchedEffect(Unit) {
        delay(1750)
        screen = Screen.Home
    }
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            (fadeIn(tween(380)) + scaleIn(tween(380), initialScale = 0.95f)) togetherWith fadeOut(tween(220))
        },
        label = "screens"
    ) { s ->
        when (s) {
            Screen.Splash -> SplashScreen()
            Screen.Home -> HomeScreen { screen = it }
            Screen.Image -> {
                BackHandler { screen = Screen.Home }
                ImageEraserScreen { screen = Screen.Home }
            }
            Screen.Video -> {
                BackHandler { screen = Screen.Home }
                VideoEraserScreen { screen = Screen.Home }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    val t = rememberInfiniteTransition(label = "splash")
    val pulse by t.animateFloat(
        0.90f, 1.14f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val rot by t.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot"
    )
    val load by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1050, easing = LinearEasing), RepeatMode.Restart),
        label = "load"
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(148.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        Brush.radialGradient(listOf(NH.Red.copy(alpha = 0.45f * pulse), Color.Transparent), c, size.width * 0.55f),
                        radius = size.width * 0.55f,
                        center = c
                    )
                    rotate(rot, c) {
                        drawCircle(
                            NH.Red.copy(alpha = 0.65f),
                            radius = size.width * 0.42f,
                            center = c,
                            style = Stroke(3.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f))
                        )
                    }
                    rotate(-rot * 1.3f, c) {
                        drawCircle(
                            NH.Scarlet.copy(alpha = 0.4f),
                            radius = size.width * 0.33f,
                            center = c,
                            style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), 0f))
                        )
                    }
                }
                Box(
                    Modifier.size(76.dp)
                        .graphicsLayer { scaleX = pulse * 0.95f; scaleY = pulse * 0.95f }
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(NH.Red, NH.DarkRed)))
                        .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(NHIcons.Sparkle, null, tint = Color.White, modifier = Modifier.size(38.dp))
                }
            }

            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .background(NH.Red.copy(alpha = 0.16f))
                    .border(1.dp, NH.Red.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                Text("TRUE AMOLED • STUDIO v5.0", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = NH.Scarlet, letterSpacing = 1.sp)
            }

            GradientText("NH Magic Eraser", 26)
            Text("RED EDITION • BY NH DEVELOPER", color = NH.TextLo, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)

            Box(
                Modifier.padding(top = 8.dp)
                    .size(width = 130.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF240B11))
                    .drawBehind {
                        val w = size.width * 0.45f
                        val x = (size.width + w) * load - w
                        drawRect(
                            NH.Accent,
                            Offset(x.coerceAtLeast(0f), 0f),
                            Size((x + w).coerceAtMost(size.width) - x.coerceAtLeast(0f), size.height)
                        )
                    }
            )
        }
    }
}

@Composable
private fun Reveal(index: Int, content: @Composable () -> Unit) {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }
    AnimatedVisibility(
        show,
        enter = fadeIn(tween(460, delayMillis = index * 75)) +
            slideInVertically(tween(460, delayMillis = index * 75)) { it / 5 }
    ) { content() }
}

/** Compact animated Before/After AI Demo Showcase Banner on the Home Screen. */
@Composable
private fun LiveDemoBanner() {
    val t = rememberInfiniteTransition(label = "demo")
    val split by t.animateFloat(
        0.20f, 0.82f,
        infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "split"
    )
    val glow by t.animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dglow"
    )

    Box(
        Modifier.fillMaxWidth()
            .height(104.dp)
            .glass(RoundedCornerShape(20.dp), 0.1f)
            .padding(10.dp)
    ) {
        Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))) {
            val w = size.width
            val h = size.height
            val sx = w * split

            drawRect(Brush.horizontalGradient(listOf(Color(0xFF14070B), Color(0xFF220A12), Color(0xFF100508))))

            val step = w / 8f
            for (i in 1..7) {
                drawLine(NH.Red.copy(alpha = 0.09f), Offset(i * step, 0f), Offset(i * step, h), 1f)
            }
            drawLine(NH.Red.copy(alpha = 0.09f), Offset(0f, h * 0.5f), Offset(w, h * 0.5f), 1f)

            drawCircle(NH.Scarlet.copy(alpha = 0.16f), radius = h * 0.28f, center = Offset(w * 0.24f, h * 0.48f))
            drawCircle(NH.Red.copy(alpha = 0.10f), radius = h * 0.42f, center = Offset(w * 0.24f, h * 0.48f))

            clipRect(0f, 0f, sx, h) {
                val objTl = Offset(w * 0.46f, h * 0.20f)
                val objSz = Size(w * 0.30f, h * 0.56f)
                drawRoundRect(NH.Red.copy(alpha = 0.26f), objTl, objSz, CornerRadius(12f, 12f))
                drawRoundRect(
                    NH.Scarlet.copy(alpha = 0.85f),
                    objTl, objSz,
                    CornerRadius(12f, 12f),
                    style = Stroke(2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f))
                )
                val cx = objTl.x + objSz.width / 2f
                val cy = objTl.y + objSz.height / 2f
                drawCircle(Color.White.copy(alpha = 0.8f), 8f, Offset(cx, cy), style = Stroke(2f))
                drawLine(NH.Red, Offset(cx - 14f, cy), Offset(cx + 14f, cy), 2f)
                drawLine(NH.Red, Offset(cx, cy - 14f), Offset(cx, cy + 14f), 2f)
            }

            drawRect(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, NH.Red.copy(alpha = 0.42f * glow), Color.Transparent),
                    startX = sx - 28f, endX = sx + 28f
                ),
                Offset(sx - 28f, 0f), Size(56f, h)
            )
            drawLine(Color.White, Offset(sx, 0f), Offset(sx, h), 2.5f)
            drawCircle(NH.Red, 10f, Offset(sx, h / 2f))
            drawCircle(Color.White, 4.5f, Offset(sx, h / 2f))
        }

        Box(
            Modifier.align(Alignment.TopStart).padding(7.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.72f))
                .border(1.dp, NH.Red.copy(alpha = 0.55f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("MASK TARGET", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = NH.Scarlet, maxLines = 1)
        }

        Box(
            Modifier.align(Alignment.TopEnd).padding(7.dp)
                .clip(RoundedCornerShape(50))
                .background(NH.Red.copy(alpha = 0.85f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("AI CLEANED", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
        }

        Row(
            Modifier.align(Alignment.BottomStart).padding(7.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PulsingDot(NH.Green, 5.dp)
            Text(
                "Real-time On-Device AI Inpainting",
                fontSize = 9.sp,
                color = NH.TextMid,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    desc: String,
    chips: List<String>,
    icon: ImageVector,
    c1: Color,
    c2: Color,
    tag: String,
    onClick: () -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.97f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "fc")
    val t = rememberInfiniteTransition(label = "card_fx")
    val shimmer by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )
    val ringRot by t.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
        label = "ringRot"
    )

    Box(
        Modifier.fillMaxWidth()
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(c1, c2, Color(0xFF080406))))
            .border(
                1.2.dp,
                Brush.linearGradient(
                    listOf(NH.Red.copy(alpha = 0.85f), Color.White.copy(alpha = 0.14f), NH.Scarlet.copy(alpha = 0.55f))
                ),
                RoundedCornerShape(22.dp)
            )
            .drawBehind {
                val c = Offset(size.width * 0.86f, size.height * 0.36f)
                drawCircle(NH.Red.copy(alpha = 0.16f), size.height * 0.62f, c)
                rotate(ringRot, c) {
                    drawCircle(
                        Color.White.copy(alpha = 0.12f),
                        size.height * 0.38f,
                        c,
                        style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), 0f))
                    )
                }
                val x = -size.width * 0.4f + shimmer * size.width * 1.9f
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.12f), Color.Transparent),
                        Offset(x, 0f), Offset(x + size.width * 0.35f, size.height)
                    )
                )
            }
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(NH.Red)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                tag,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            subtitle,
                            fontSize = 10.sp,
                            color = NH.RoseGold,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        desc,
                        fontSize = 11.sp,
                        color = NH.TextMid,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(10.dp))

                Box(
                    Modifier.size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(NH.Red, NH.DarkRed)))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }

            // Single-line guaranteed bottom bar: compact chips on left, non-wrapping OPEN pill on right
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chips.forEach { chip ->
                    Box(
                        Modifier.clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.52f))
                            .border(1.dp, NH.Red.copy(alpha = 0.35f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            chip,
                            fontSize = 9.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(NH.Red.copy(alpha = 0.28f))
                        .border(1.dp, NH.Red.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "OPEN",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                    Icon(NHIcons.Play, null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, title: String, sub: String, tint: Color, modifier: Modifier) {
    Row(
        modifier
            .glass(RoundedCornerShape(16.dp), 0.09f)
            .padding(horizontal = 9.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            Modifier.size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.16f))
                .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                sub,
                fontSize = 9.sp,
                color = NH.TextLo,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StepNode(num: String, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .glass(RoundedCornerShape(14.dp), 0.07f)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier.size(20.dp)
                .clip(CircleShape)
                .background(NH.Accent),
            contentAlignment = Alignment.Center
        ) {
            Text(num, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
        }
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = NH.TextMid,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HomeScreen(onOpen: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("nh", 0) }
    var showNotice by remember { mutableStateOf(!prefs.getBoolean("notice_ok", false)) }

    if (showNotice) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = NH.Surface2,
            titleContentColor = NH.TextHi,
            textContentColor = NH.TextMid,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PulsingDot(NH.Red, 7.dp)
                    Text("Studio Usage Policy", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            },
            text = {
                Text(
                    "Use NH Magic Eraser only on photos and videos you own or have permission to edit. " +
                        "Do not remove ownership, copyright, or AI-origin marks to mislead others.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean("notice_ok", true).apply()
                    showNotice = false
                }) {
                    Text("ENTER STUDIO", color = NH.Red, fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }

    Column(
        Modifier.fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Compact Top Studio Header Bar
        Reveal(0) {
            Row(
                Modifier.fillMaxWidth()
                    .glass(RoundedCornerShape(18.dp), 0.09f)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(NH.Accent)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(NHIcons.Sparkle, null, tint = Color.White, modifier = Modifier.size(19.dp))
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("NH Magic Eraser", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
                        Box(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(NH.Red.copy(alpha = 0.2f))
                                .border(1.dp, NH.Red, RoundedCornerShape(50))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text("PRO v5", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = NH.Scarlet, maxLines = 1)
                        }
                    }
                    Text("AMOLED Red Edition • NH Developer", fontSize = 10.sp, color = NH.TextLo, maxLines = 1)
                }
                IconAction(NHIcons.Bolt, { showNotice = true }, tint = NH.Scarlet, size = 34.dp)
            }
        }

        // 2. Compact Hero Headline
        Reveal(1) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(horizontal = 2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PulsingDot(NH.Red, 6.dp)
                    Text(
                        "ON-DEVICE AI ERASER STUDIO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NH.Scarlet,
                        letterSpacing = 1.1.sp,
                        maxLines = 1
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Erase anything.", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    GradientText("Pure Black.", 24)
                }
                Text(
                    "Remove unwanted objects, text, and static logos from photos and videos offline.",
                    color = NH.TextLo,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2
                )
            }
        }

        // 3. Compact Live Demo Banner
        Reveal(2) {
            LiveDemoBanner()
        }

        // 4. Primary Studio Launch Cards
        Reveal(3) {
            FeatureCard(
                title = "Image Eraser Pro",
                subtitle = "NEURAL AI",
                desc = "Paint over any object or text. Pinch-zoom 8x, compare before & after, and export lossless HD.",
                chips = listOf("8x Zoom", "Feather", "Compare"),
                icon = NHIcons.Image,
                c1 = Color(0xFF3B0711),
                c2 = Color(0xFF170509),
                tag = "PHOTOS"
            ) { onOpen(Screen.Image) }
        }

        Reveal(4) {
            FeatureCard(
                title = "Video Eraser Pro",
                subtitle = "FRAME ENGINE",
                desc = "Box static logos or watermarks across frames automatically while keeping original audio.",
                chips = listOf("Multi-Box", "Audio Kept", "H.264"),
                icon = NHIcons.Movie,
                c1 = Color(0xFF2B0912),
                c2 = Color(0xFF120408),
                tag = "VIDEOS"
            ) { onOpen(Screen.Video) }
        }

        // 5. 3-Step Workflow Strip (Uniform single-line cards)
        Reveal(5) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StepNode("01", "Select", Modifier.weight(1f))
                StepNode("02", "Mask", Modifier.weight(1f))
                StepNode("03", "Erase", Modifier.weight(1f))
            }
        }

        // 6. Compact AMOLED Telemetry Tiles (Single-line text, zero vertical stretching)
        Reveal(6) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(NHIcons.Bolt, "Offline", "100% Private", NH.Red, Modifier.weight(1f))
                StatTile(NHIcons.Check, "AMOLED", "Pure #000", NH.Scarlet, Modifier.weight(1f))
                StatTile(NHIcons.Download, "HD Save", "Full Quality", NH.Ember, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}
