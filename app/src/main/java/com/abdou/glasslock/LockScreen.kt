@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.abdou.glasslock

import android.content.Context
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun rememberTilt(): State<Offset> {
    val ctx = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        val r = FloatArray(9)
        val o = FloatArray(3)
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(r, e.values)
                SensorManager.getOrientation(r, o)
                val x = (o[2] / 0.5f).coerceIn(-1f, 1f)
                val y = ((o[1] + 1.0f) / 0.5f).coerceIn(-1f, 1f)
                val p = tilt.value
                tilt.value = Offset(p.x + (x - p.x) * 0.12f, p.y + (y - p.y) * 0.12f)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(l) }
    }
    return tilt
}

@Composable
fun LockScreen(onUnlock: () -> Unit, onTorch: (Boolean) -> Unit, onCamera: () -> Unit) {
    val tilt by rememberTilt()
    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    var now by remember { mutableStateOf(Date()) }
    var torch by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(5000) } }
    val hh = SimpleDateFormat("HH", Locale.getDefault()).format(now)
    val mm = SimpleDateFormat("mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(now)

    Box(
        Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragEnd = {
                    scope.launch {
                        if (drag.value < -size.height * 0.25f) onUnlock()
                        drag.animateTo(0f, spring(0.6f, 300f))
                    }
                },
                onVerticalDrag = { c, d ->
                    c.consume()
                    scope.launch { drag.snapTo((drag.value + d).coerceAtMost(0f)) }
                }
            )
        }
    ) {
        SceneBack(Modifier.graphicsLayer {
            translationX = -tilt.x * 10.dp.toPx(); translationY = -tilt.y * 8.dp.toPx()
            scaleX = 1.08f; scaleY = 1.08f
        })
        Column(
            Modifier.fillMaxSize().padding(top = 64.dp).graphicsLayer {
                translationX = tilt.x * 14.dp.toPx(); translationY = drag.value * 0.4f
                alpha = (1f + drag.value / (size.height * 0.6f)).coerceIn(0f, 1f)
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicText(date, style = TextStyle(Color.White, 20.sp))
            Spacer(Modifier.height(8.dp))
            GlassClock(hh, mm)
        }
        SceneFront(Modifier.graphicsLayer {
            translationX = tilt.x * 26.dp.toPx(); translationY = tilt.y * 14.dp.toPx() + drag.value * 0.4f
            scaleX = 1.1f; scaleY = 1.1f
        })
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 40.dp, end = 40.dp, bottom = 40.dp)
                .graphicsLayer { alpha = (1f + drag.value / (size.height * 0.4f)).coerceIn(0f, 1f) },
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GlassButton(torch, { torch = !torch; onTorch(torch) }) { drawTorch() }
            GlassButton(false, onCamera) { drawCamera() }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).size(134.dp, 5.dp)
                .clip(RoundedCornerShape(3.dp)).background(Color.White.copy(0.9f))
        )
    }
}

@Composable
fun GlassClock(h: String, m: String) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        val density = LocalDensity.current
        val fs = with(density) { (maxWidth * 0.8f).toSp() }
        val ff = remember { FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD)) }
        val base = TextStyle(
            fontFamily = ff, fontSize = fs, lineHeight = fs * 0.82f, textAlign = TextAlign.Center,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )
        val fill = Brush.verticalGradient(
            listOf(Color.White.copy(0.7f), Color(0xFFBEE1FF).copy(0.3f)), 0f, with(density) { fs.toPx() * 1.7f }
        )
        val txt = "$h\n$m"
        Box {
            BasicText(txt, style = base.copy(brush = fill))
            BasicText(txt, style = base.copy(color = Color.White.copy(0.8f), drawStyle = Stroke(width = 4f)))
        }
    }
}

@Composable
fun GlassButton(on: Boolean, onClick: () -> Unit, icon: DrawScope.() -> Unit) {
    Box(
        Modifier.size(56.dp).clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color.White.copy(if (on) 0.6f else 0.3f), Color.White.copy(0.08f))))
            .border(1.dp, Color.White.copy(0.45f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Canvas(Modifier.size(26.dp), onDraw = icon) }
}

private fun DrawScope.drawTorch() {
    val p = Path().apply {
        moveTo(size.width * 0.25f, 0f); lineTo(size.width * 0.75f, 0f)
        lineTo(size.width * 0.65f, size.height * 0.3f); lineTo(size.width * 0.35f, size.height * 0.3f); close()
    }
    drawPath(p, Color.White)
    drawRoundRect(Color.White, Offset(size.width * 0.35f, size.height * 0.36f), Size(size.width * 0.3f, size.height * 0.64f), CornerRadius(6f))
}

private fun DrawScope.drawCamera() {
    drawRoundRect(Color.White, Offset(0f, size.height * 0.2f), Size(size.width, size.height * 0.7f), CornerRadius(8f), style = Stroke(3f))
    drawCircle(Color.White, size.width * 0.2f, Offset(size.width / 2, size.height * 0.55f), style = Stroke(3f))
}

@Composable
fun SceneBack(modifier: Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF1260DC), Color(0xFF3A9AF2), Color(0xFFA9D8FF)), 0f, h * 0.6f))
        drawOval(Color.White.copy(0.75f), Offset(-w * 0.1f, h * 0.2f), Size(w * 0.5f, h * 0.07f))
        drawOval(Color.White.copy(0.7f), Offset(w * 0.55f, h * 0.35f), Size(w * 0.6f, h * 0.08f))
        val m = Path().apply {
            moveTo(0f, h * 0.56f); lineTo(w * 0.18f, h * 0.47f); lineTo(w * 0.36f, h * 0.53f)
            lineTo(w * 0.59f, h * 0.455f); lineTo(w * 0.82f, h * 0.52f); lineTo(w, h * 0.485f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(m, Color(0xFF2C7A6C))
        val g = Path().apply {
            moveTo(0f, h * 0.59f); cubicTo(w * 0.3f, h * 0.52f, w * 0.65f, h * 0.53f, w, h * 0.58f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(g, Color(0xFF2A8A38))
    }
}

@Composable
fun SceneFront(modifier: Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val p = Path().apply {
            moveTo(0f, h * 0.635f); cubicTo(w * 0.28f, h * 0.56f, w * 0.64f, h * 0.51f, w, h * 0.54f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(p, Brush.verticalGradient(listOf(Color(0xFF66CC4E), Color(0xFF2A8A38)), h * 0.5f, h))
    }
}
