package com.nh.magiceraser

import android.graphics.Bitmap
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private enum class VStage { Pick, Edit, Exporting, Done }

private fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier) {
    Column(
        modifier
            .glass(RoundedCornerShape(16.dp), 0.1f)
            .padding(vertical = 9.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            value,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = NH.Scarlet,
            maxLines = 1,
            softWrap = false
        )
        Text(
            label,
            fontSize = 10.sp,
            color = NH.TextLo,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun VideoEraserScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var frame by remember { mutableStateOf<Bitmap?>(null) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var pos by remember { mutableFloatStateOf(0f) }
    val rects = remember { mutableStateListOf<RectF>() }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCur by remember { mutableStateOf<Offset?>(null) }
    var expand by remember { mutableFloatStateOf(1f) }

    var exporting by remember { mutableStateOf(false) }
    var done by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(1) }
    var live by remember { mutableStateOf<Bitmap?>(null) }
    var startMs by remember { mutableLongStateOf(0L) }
    var nowMs by remember { mutableLongStateOf(0L) }
    var tookMs by remember { mutableLongStateOf(0L) }
    var job by remember { mutableStateOf<Job?>(null) }
    var resultUri by remember { mutableStateOf<Uri?>(null) }

    DisposableEffect(exporting) {
        view.keepScreenOn = exporting
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(exporting) {
        while (exporting) { nowMs = System.currentTimeMillis(); delay(400) }
    }
    BackHandler(enabled = exporting) { job?.cancel() }

    suspend fun loadFrame(uri: Uri, tMs: Long): Pair<Bitmap?, Long> = withContext(Dispatchers.IO) {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(ctx, uri)
            val d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            Pair(r.getFrameAtTime(tMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC), d)
        } catch (e: Exception) {
            Pair(null, 0L)
        } finally {
            r.release()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val (bmp, d) = loadFrame(uri, 0)
            if (bmp == null) {
                Toast.makeText(ctx, "Could not open this video", Toast.LENGTH_SHORT).show()
            } else {
                videoUri = uri; frame = bmp; durationMs = d; pos = 0f
                rects.clear(); resultUri = null; live = null
            }
        }
    }
    val pick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }

    val stage = when {
        exporting -> VStage.Exporting
        resultUri != null -> VStage.Done
        videoUri == null -> VStage.Pick
        else -> VStage.Edit
    }

    AnimatedContent(
        targetState = stage,
        transitionSpec = { (fadeIn(tween(380)) + scaleIn(tween(380), initialScale = 0.95f)) togetherWith fadeOut(tween(200)) },
        label = "vstage"
    ) { st ->
        when (st) {
            VStage.Pick -> Column(Modifier.fillMaxSize().systemBarsPadding()) {
                ScreenHeader("Video Studio", "AMOLED Multi-Box Frame Cleaner", onBack)
                Column(
                    Modifier.weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .glass(RoundedCornerShape(22.dp), 0.08f)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier.size(80.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(NH.Red.copy(alpha = 0.45f), NH.DarkRed.copy(alpha = 0.15f), Color.Transparent)))
                            .border(1.5.dp, NH.Red.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.size(54.dp).clip(CircleShape).background(NH.Cool),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(NHIcons.Movie, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }
                    Text(
                        "Choose a Video",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                    Text(
                        "Draw boxes over static watermarks or logos.\nEvery frame is cleaned with audio preserved.",
                        color = NH.TextLo,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 18.dp)
                    ) {
                        Pill("H.264 MP4", false, {})
                        Pill("Audio Kept", false, {})
                        Pill("Multi-Box", false, {})
                    }
                    GradientButton("Select Video", { pick() }, icon = NHIcons.Add, brush = NH.Cool)
                }
            }

            VStage.Edit -> Column(Modifier.fillMaxSize().systemBarsPadding()) {
                ScreenHeader("Target Regions", "Drag on frame to box logos • ${rects.size} active", onBack) {
                    if (rects.isNotEmpty()) {
                        IconAction(NHIcons.Close, { rects.clear() }, tint = NH.Scarlet)
                    }
                    IconAction(NHIcons.Movie, { pick() })
                }
                BoxWithConstraints(
                    Modifier.weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .glass(RoundedCornerShape(22.dp), 0.08f)
                ) {
                    val bmp = frame
                    if (bmp != null) {
                        val wPx = constraints.maxWidth.toFloat()
                        val hPx = constraints.maxHeight.toFloat()
                        val scale = minOf((wPx - 16f) / bmp.width, (hPx - 16f) / bmp.height)
                        val dw = bmp.width * scale
                        val dh = bmp.height * scale
                        val ox = (wPx - dw) / 2f
                        val oy = (hPx - dh) / 2f
                        val img = remember(bmp) { bmp.asImageBitmap() }
                        val t = rememberInfiniteTransition(label = "dash")
                        val phase by t.animateFloat(
                            0f, 30f, infiniteRepeatable(tween(750, easing = LinearEasing), RepeatMode.Restart), label = "ph"
                        )
                        fun norm(o: Offset) = Offset(((o.x - ox) / dw).coerceIn(0f, 1f), ((o.y - oy) / dh).coerceIn(0f, 1f))

                        Canvas(
                            Modifier.fillMaxSize().pointerInput(bmp, scale) {
                                detectDragGestures(
                                    onDragStart = { o -> dragStart = norm(o); dragCur = norm(o) },
                                    onDrag = { change, _ -> change.consume(); dragCur = norm(change.position) },
                                    onDragEnd = {
                                        val s = dragStart
                                        val c = dragCur
                                        if (s != null && c != null) {
                                            val r = RectF(minOf(s.x, c.x), minOf(s.y, c.y), maxOf(s.x, c.x), maxOf(s.y, c.y))
                                            if (r.width() > 0.01f && r.height() > 0.01f) rects.add(r)
                                        }
                                        dragStart = null; dragCur = null
                                    },
                                    onDragCancel = { dragStart = null; dragCur = null }
                                )
                            }
                        ) {
                            drawImage(
                                img,
                                dstOffset = IntOffset(ox.roundToInt(), oy.roundToInt()),
                                dstSize = IntSize(dw.roundToInt(), dh.roundToInt())
                            )
                            fun drawR(l: Float, tp: Float, r: Float, b: Float) {
                                val tl = Offset(ox + l * dw, oy + tp * dh)
                                val sz = Size((r - l) * dw, (b - tp) * dh)
                                val cr = CornerRadius(10f, 10f)
                                drawRoundRect(NH.Red.copy(alpha = 0.28f), tl, sz, cr)
                                drawRoundRect(
                                    NH.Scarlet, tl, sz, cr,
                                    style = Stroke(3.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), phase))
                                )
                                for (cx in listOf(tl.x, tl.x + sz.width)) for (cy in listOf(tl.y, tl.y + sz.height)) {
                                    drawCircle(Color.White, 8f, Offset(cx, cy))
                                    drawCircle(NH.Red, 5f, Offset(cx, cy))
                                }
                            }
                            for (r in rects) drawR(r.left, r.top, r.right, r.bottom)
                            val s = dragStart
                            val c = dragCur
                            if (s != null && c != null) drawR(minOf(s.x, c.x), minOf(s.y, c.y), maxOf(s.x, c.x), maxOf(s.y, c.y))
                        }
                        if (rects.isEmpty() && dragStart == null) {
                            Box(
                                Modifier.align(Alignment.BottomCenter)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF14070B).copy(alpha = 0.88f))
                                    .border(1.dp, NH.Red.copy(alpha = 0.5f), RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    "Drag across logo to add target box",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Column(
                    Modifier.fillMaxWidth()
                        .padding(top = 4.dp)
                        .glass(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), 0.11f)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    LabeledSlider(
                        "Preview Frame", "${fmt((pos * durationMs).toLong())} / ${fmt(durationMs)}", pos, { pos = it }, 0f..1f,
                        onFinished = {
                            val u = videoUri
                            if (u != null) scope.launch {
                                val (b, _) = loadFrame(u, (pos * durationMs).toLong())
                                if (b != null) frame = b
                            }
                        }
                    )
                    if (rects.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(rects.toList()) { i, _ ->
                                Row(
                                    Modifier.clip(RoundedCornerShape(50))
                                        .background(Color(0xFF1F0A10))
                                        .border(1.dp, NH.Red.copy(alpha = 0.45f), RoundedCornerShape(50))
                                        .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text("Box #${i + 1}", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
                                    Box(
                                        Modifier.size(18.dp)
                                            .clip(CircleShape)
                                            .background(NH.Red.copy(alpha = 0.35f))
                                            .clickable { if (i < rects.size) rects.removeAt(i) },
                                        contentAlignment = Alignment.Center
                                    ) { Icon(NHIcons.Close, null, tint = Color.White, modifier = Modifier.size(11.dp)) }
                                }
                            }
                        }
                    }
                    LabeledSlider("Border Expand", "${expand.roundToInt()}%", expand, { expand = it }, 0f..5f)
                    GradientButton(
                        if (rects.isEmpty()) "Drag on Video to Select Area" else "Clean Video (${rects.size})",
                        {
                            val u = videoUri ?: return@GradientButton
                            job = scope.launch {
                                exporting = true; done = 0; total = 1; live = null
                                startMs = System.currentTimeMillis(); nowMs = startMs; resultUri = null
                                try {
                                    val out = VideoProcessor.process(
                                        ctx, u, rects.toList(), expand,
                                        { d, t -> done = d; total = t },
                                        { b -> live = b }
                                    )
                                    tookMs = System.currentTimeMillis() - startMs
                                    resultUri = out
                                    if (out == null) Toast.makeText(ctx, "Export failed", Toast.LENGTH_LONG).show()
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Toast.makeText(ctx, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    exporting = false
                                }
                            }
                        },
                        Modifier.fillMaxWidth(), enabled = rects.isNotEmpty(), icon = NHIcons.Sparkle, brush = NH.Cool
                    )
                }
            }

            VStage.Exporting -> {
                val progress = if (total > 0) done.toFloat() / total else 0f
                val smooth by animateFloatAsState(progress, tween(450), label = "smooth")
                val elapsed = (nowMs - startMs).coerceAtLeast(0)
                val eta = if (done > 0) elapsed * (total - done) / done else 0L
                val glow = rememberInfiniteTransition(label = "pg")
                val ga by glow.animateFloat(
                    0.35f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "ga"
                )
                Column(
                    Modifier.fillMaxSize()
                        .systemBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PulsingDot(NH.Red, 7.dp)
                        Text("AI Frame Reactor", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("Frame $done of $total • Keep screen open", fontSize = 11.sp, color = NH.TextLo)

                    Box(
                        Modifier.fillMaxWidth()
                            .height(156.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF0C0507))
                            .border(
                                1.5.dp,
                                Brush.linearGradient(listOf(NH.Red.copy(ga), NH.Scarlet.copy(ga), NH.Ember.copy(ga))),
                                RoundedCornerShape(20.dp)
                            )
                    ) {
                        val lv = live
                        if (lv != null) {
                            val li = remember(lv) { lv.asImageBitmap() }
                            Canvas(Modifier.fillMaxSize()) {
                                val s = minOf(size.width / lv.width, size.height / lv.height)
                                val w = (lv.width * s).roundToInt()
                                val h = (lv.height * s).roundToInt()
                                drawImage(
                                    li,
                                    dstOffset = IntOffset(((size.width - w) / 2f).roundToInt(), ((size.height - h) / 2f).roundToInt()),
                                    dstSize = IntSize(w, h)
                                )
                            }
                        }
                        ScanOverlay(Modifier.fillMaxSize())
                    }

                    Box(Modifier.size(136.dp), contentAlignment = Alignment.Center) {
                        ProgressRing(smooth, Modifier.fillMaxSize(), stroke = 13f)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${(smooth * 100).roundToInt()}%", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("DONE", fontSize = 9.sp, color = NH.Scarlet, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox("Elapsed", fmt(elapsed), Modifier.weight(1f))
                        StatBox("Remaining", if (done > 2) fmt(eta) else "--:--", Modifier.weight(1f))
                        StatBox("Boxes", "${rects.size}", Modifier.weight(1f))
                    }
                    GradientButton(
                        "Cancel Export", { job?.cancel() }, Modifier.fillMaxWidth(),
                        brush = Brush.horizontalGradient(listOf(Color(0xFF280E14), Color(0xFF18080C)))
                    )
                }
            }

            VStage.Done -> Box(Modifier.fillMaxSize().systemBarsPadding()) {
                Confetti()
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedCheck(Modifier.size(92.dp))
                    Spacer(Modifier.height(12.dp))
                    GradientText("Video Cleaned!", 26, NH.Accent)
                    Text(
                        "Saved to Movies/NH Magic Eraser",
                        color = NH.TextLo, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox("Boxes", "${rects.size}", Modifier.weight(1f))
                        StatBox("Time", fmt(tookMs), Modifier.weight(1f))
                        StatBox("Frames", "$total", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradientButton(
                            "Play Video", { resultUri?.let { MediaUtils.view(ctx, it, "video/mp4") } },
                            Modifier.weight(1f), icon = NHIcons.Play
                        )
                        GradientButton(
                            "Share", { resultUri?.let { MediaUtils.share(ctx, it, "video/mp4") } },
                            Modifier.weight(1f), icon = NHIcons.Share,
                            brush = Brush.horizontalGradient(listOf(Color(0xFF2B0E15), Color(0xFF19080D)))
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    GradientButton(
                        "Clean Another Video", { resultUri = null; videoUri = null; frame = null; rects.clear(); live = null },
                        Modifier.fillMaxWidth(),
                        brush = Brush.horizontalGradient(listOf(Color(0xFF220B11), Color(0xFF14060A)))
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Back to Studio Home",
                        color = NH.TextLo,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onBack() }.padding(10.dp)
                    )
                }
            }
        }
    }
}
