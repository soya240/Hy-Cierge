package com.taxiconcierge

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * ──────────────────────────────────────────────────────────────────────────
 *  LightStage — 승객 화면 뒤에 항상 깔려 있는 배경·책상·오브
 *  (1600×900 Figma 프레임과 같은 좌표계. PassengerApp 이 고정 비율 프레임으로 감싸서 글자·버튼과 정확히 겹침)
 *
 *  등장: P01 책상 위 작은 오브 → "숑" 위로 튀어오른 뒤 → 제자리로 "두웅" 떠서 안착, 동시에 책상이 아래로 → P03A
 *
 *  오브 5단계 (OrbFx)
 *    Idle       : 하늘·흰·연보라·옅은 분홍 빛 덩어리 5개가 서로 다른 속도·방향으로 흘러다님 (방향이 천천히 바뀌는 사인 합성)
 *                 4.5초 주기로 밝기·크기 아주 미세하게 호흡, 바깥 글로우는 아주 약하게
 *    Activation : Tap to speak → 밝기↑, 흐름 1.6배, 가운데 빛이 바깥으로 퍼짐, 표면 미세 일렁임, 글로우 살짝 확장
 *    Listening  : 마이크 소리 → 저음(느린 성분)은 파랑·라벤더 층을 키우고, 고음(빠른 변화)은 흰빛·분홍 층을 반짝이게
 *    Processing : 빛 덩어리들이 가운데로 모이고 밝기가 일정한 리듬으로 아주 약하게
 *    Result     : 흐름이 느려지며 Idle 로 복귀
 *  ※ 오브는 끝까지 둥근 오브. 하단 음파로 변하지 않음 (음파는 P03C·P03 화면이 따로 그림)
 * ──────────────────────────────────────────────────────────────────────────
 */

val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

/** 조절용 파라미터 (한 곳에서 세기·속도 조절) */
object OrbTuning {
    var flowSpeed = 0.22f        // Idle 내부 흐름 속도
    var activationBoost = 0.35f  // Activation 밝기 증가
    var voiceGain = 1.0f         // 음성 반응 강도
    var glow = 0.30f             // 바깥 글로우 세기 (아주 약하게)
    var breathe = 0.022f         // 호흡 크기 변화 (2.2%)
    var ripple = 0.012f          // 표면 일렁임 (1.2%)
}

enum class OrbFx { IDLE, ACTIVATION, LISTENING, PROCESSING, RESULT }

/** res/drawable-nodpi 에 Figma 이미지가 있으면 쓰고, 없으면 null (코드로 대체해서 그림) */
@Composable
fun figmaImage(name: String): ImageBitmap? {
    val ctx = LocalContext.current
    return remember(name) {
        val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        if (id == 0) null else runCatching { ImageBitmap.imageResource(ctx.resources, id) }.getOrNull()
    }
}

/** 배경: Figma 배경 이미지(또는 같은 구성의 그라데이션) + 그 위를 천천히 흘러가는 빛. 화면 전체를 채움 */
@Composable
fun PassengerBackdrop(s: AppState) {
    val bg = figmaImage("fig_bg")
    val time by rememberSeconds()
    val dim by animateFloatAsState(if (s.p.ordinal >= P.WAITING.ordinal) 0.18f else 0f, tween(900, easing = EaseInOutCubic), label = "dim")
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Color(0xFF131313))
        if (bg != null) {
            drawImage(bg, dstSize = IntSize(w.roundToInt(), h.roundToInt()))
        } else {
            drawRect(Brush.radialGradient(listOf(Color(0xFF1E5BA6), Color(0x001E5BA6)), Offset(w * 0.1f, h * 1.05f), w * 0.62f))
            drawRect(Brush.radialGradient(listOf(Color(0xFF3E5470), Color(0x003E5470)), Offset(w, 0f), w * 0.45f))
        }
        val t = time
        fun light(c: Color, a: Float, x: Float, y: Float, r: Float) = drawRect(
            Brush.radialGradient(listOf(c.copy(alpha = a), c.copy(alpha = a * 0.35f), c.copy(alpha = 0f)), Offset(x * w, y * h), r * w),
            blendMode = BlendMode.Screen,
        )
        light(Color(0xFF2B6CC4), 0.34f, 0.18f + 0.16f * sin(t * 0.13f), 0.88f + 0.08f * cos(t * 0.11f), 0.42f)
        light(Color(0xFF6A7FB8), 0.22f, 0.84f + 0.10f * cos(t * 0.09f), 0.12f + 0.10f * sin(t * 0.15f), 0.34f)
        light(Color(0xFF3A8DB0), 0.15f, 0.55f + 0.25f * sin(t * 0.07f + 1.3f), 0.70f + 0.12f * cos(t * 0.12f), 0.30f)
        light(Color(0xFF7466C8), 0.10f, 0.35f + 0.25f * cos(t * 0.08f + 2.1f), 0.30f + 0.12f * sin(t * 0.1f), 0.26f)
        if (dim > 0f) drawRect(Color.Black.copy(alpha = dim))
    }
}

/** 화면별 오브 자리 (Figma 좌표). r=0 이면 숨김 */
private fun orbSpot(p: P): Triple<Float, Float, Float> = when (p) {
    P.WELCOME -> Triple(800f, 490f, 18f)         // 책상 위 작은 오브 (Figma P01)
    P.PROMPT, P.ASK -> Triple(800f, 460f, 110f)  // P03A
    P.LISTEN -> Triple(800f, 300f, 46f)          // P03C: 마이크 자리에서 목소리에 반응
    else -> Triple(800f, 460f, 0f)
}

@Composable
fun LightStage(s: AppState) {
    val desk = figmaImage("fig_desk")
    val tex = figmaImage("fig_orb")
    val time by rememberSeconds()

    // ── 위치·크기: 등장(숑→두웅)은 직접 시퀀스로, 나머지는 부드럽게
    val ox = remember { Animatable(800f) }
    val oy = remember { Animatable(490f) }
    val orr = remember { Animatable(18f) }
    val oa = remember { Animatable(1f) }
    val deskY = remember { Animatable(441f) }
    val deskA = remember { Animatable(1f) }
    val rings = remember { Animatable(0f) }
    var prev by remember { mutableStateOf(P.WELCOME) }
    LaunchedEffect(s.p) {
        val (x, y, r) = orbSpot(s.p)
        if (prev == P.WELCOME && (s.p == P.PROMPT || s.p == P.ASK)) {
            // 책상은 아래로 내려가며 사라지고
            launch { deskY.animateTo(980f, tween(1500, easing = EaseInOutCubic)) }
            launch { deskA.animateTo(0f, tween(1300, delayMillis = 200)) }
            // 오브는 숑! 위로 튀어오르며 커졌다가
            launch { orr.animateTo(118f, tween(520, easing = EaseOutCubic)); orr.animateTo(110f, spring(0.55f, 160f)) }
            oy.animateTo(330f, tween(520, easing = EaseOutCubic))
            // 제자리로 두웅 (살짝 튕기며 안착)
            launch { rings.animateTo(1f, tween(1200, delayMillis = 300)) }
            oy.animateTo(460f, spring(dampingRatio = 0.42f, stiffness = 90f))
        } else {
            launch { ox.animateTo(x, tween(900, easing = EaseInOutCubic)) }
            launch { if (r > 0f) orr.animateTo(r, tween(900, easing = EaseInOutCubic)) }
            launch { oa.animateTo(if (r > 0f) 1f else 0f, tween(if (r > 0f) 700 else 450)) }
            launch { rings.animateTo(if (s.p == P.ASK || s.p == P.PROMPT) 1f else 0f, tween(if (s.p == P.ASK) 900 else 380)) }
            launch { deskA.animateTo(if (s.p == P.WELCOME) 1f else 0f, tween(600)) }
            launch { deskY.animateTo(if (s.p == P.WELCOME) 441f else 980f, tween(900, easing = EaseInOutCubic)) }
            oy.animateTo(y, tween(900, easing = EaseInOutCubic))
        }
        prev = s.p
    }

    // ── 오브 상태 (5단계)
    val fx = when {
        s.p == P.LISTEN && s.listening -> OrbFx.LISTENING
        s.p == P.LISTEN && !s.listening -> OrbFx.PROCESSING
        s.orbActivating -> OrbFx.ACTIVATION
        else -> OrbFx.IDLE
    }
    val energy by animateFloatAsState(
        when (fx) { OrbFx.ACTIVATION -> 1f; OrbFx.LISTENING -> 0.85f; OrbFx.PROCESSING -> 0.45f; else -> 0f },
        tween(if (fx == OrbFx.IDLE || fx == OrbFx.RESULT) 1600 else 600, easing = EaseInOutCubic), label = "energy",
    )
    val gather by animateFloatAsState(if (fx == OrbFx.PROCESSING) 1f else 0f, tween(1200, easing = EaseInOutCubic), label = "gather")

    // ── 음성 → 저음(느린)·고음(빠른) 성분으로 나눠서 부드럽게
    val level by rememberUpdatedState(if (fx == OrbFx.LISTENING || fx == OrbFx.ACTIVATION) s.level else 0f)
    val low = remember { mutableFloatStateOf(0f) }
    val high = remember { mutableFloatStateOf(0f) }
    val flow = remember { mutableFloatStateOf(0f) }        // 내부 흐름 위상 (속도가 바뀌어도 끊김 없이 누적)
    val energyNow by rememberUpdatedState(energy)
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) withFrameNanos { now ->
            val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceIn(0f, 0.05f)
            last = now
            val lv = (level * OrbTuning.voiceGain).coerceIn(0f, 1f)
            low.floatValue += (lv - low.floatValue) * (1f - exp(-3.5f * dt))                    // 느리게 따라감 = 저음
            val fast = (lv - low.floatValue).coerceAtLeast(0f)
            high.floatValue += (fast * 2.2f - high.floatValue) * (1f - exp(-12f * dt))          // 빠른 변화 = 고음
            flow.floatValue += dt * OrbTuning.flowSpeed * (1f + 0.6f * energyNow + 1.2f * low.floatValue)
        }
    }

    Box(Modifier.fillMaxSize()) {
        // ① 책상 일러스트 (P01): Figma 처럼 screen 합성
        if (desk != null && deskA.value > 0.01f) {
            Canvas(Modifier.fillMaxSize()) {
                val k = size.width / 1600f
                drawImage(
                    desk,
                    dstOffset = IntOffset(0, (deskY.value * k).roundToInt()),
                    dstSize = IntSize(size.width.roundToInt(), (800f * k).roundToInt()),
                    alpha = deskA.value,
                    blendMode = BlendMode.Screen,
                )
            }
        }
        // ② 궤도 링 (P03A) — 거의 정지, 아주 미세하게만
        if (rings.value > 0.01f) {
            Canvas(Modifier.fillMaxSize()) { drawRings(ox.value, oy.value, rings.value * (1f - 0.7f * energy), time) }
        }
        // ③ 오브
        if (oa.value > 0.01f) {
            Canvas(Modifier.fillMaxSize()) {
                drawOrb(tex, ox.value, oy.value, orr.value, oa.value, energy, gather, low.floatValue, high.floatValue, flow.floatValue, time)
            }
        }
    }
}

/** 얇은 궤도 링 (Figma P03A shape): 거의 정지, 아주 천천히 기울어지고 빛점 하나만 링을 따라 돈다 */
private fun DrawScope.drawRings(ox: Float, oy: Float, a: Float, t: Float) {
    val k = size.width / 1600f
    val c = Offset(ox * k, oy * k)
    val line = Color.White.copy(alpha = 0.18f * a)
    val sw = 1.4f * k
    rotate(1.2f * sin(t * 0.1f), c) {
        drawCircle(line, 240f * k, c, style = Stroke(sw))
        drawCircle(line, 390f * k, c, style = Stroke(sw))
        drawLine(line, Offset(c.x + 110f * k, c.y), Offset(c.x + 390f * k, c.y + 6f * k), strokeWidth = sw)
        drawLine(line, Offset(c.x, c.y + 110f * k), Offset(c.x - 4f * k, c.y + 240f * k), strokeWidth = sw)
        val p1 = Path().apply {
            moveTo(c.x + 60f * k, c.y - 388f * k)
            cubicTo(c.x + 260f * k, c.y - 380f * k, c.x + 300f * k, c.y - 250f * k, c.x + 190f * k, c.y - 255f * k)
            cubicTo(c.x + 100f * k, c.y - 260f * k, c.x + 150f * k, c.y - 330f * k, c.x + 230f * k, c.y - 300f * k)
        }
        drawPath(p1, line, style = Stroke(sw))
        val p2 = Path().apply {
            moveTo(c.x - 300f * k, c.y + 250f * k)
            cubicTo(c.x - 330f * k, c.y + 160f * k, c.x - 180f * k, c.y + 140f * k, c.x - 200f * k, c.y + 230f * k)
            cubicTo(c.x - 215f * k, c.y + 300f * k, c.x - 150f * k, c.y + 330f * k, c.x - 90f * k, c.y + 330f * k)
        }
        drawPath(p2, line, style = Stroke(sw))
        val ang = t * 0.18f
        drawCircle(Color.White.copy(alpha = 0.5f * a), 3f * k, Offset(c.x + cos(ang) * 390f * k, c.y + sin(ang) * 390f * k))
    }
}

/**
 * 오브: 유리 같은 구 안에 빛나는 유체가 흐르는 느낌
 *  - 바탕: 파스텔 블루 구 (Figma 텍스처가 있으면 그 위에 은은하게 깔고 천천히 흘림)
 *  - 빛 덩어리 5개(파스텔블루·아이스블루·청백·라벤더·옅은 분홍): 각자 다른 속도·방향의 사인 합성 궤도로 떠다니며 Screen 합성
 *    → 줄무늬 없이 반투명하게 겹치고 섞임. 궤도 속도 자체도 천천히 변해서 반복 패턴으로 안 보임
 *  - 가운데 은은한 발광, 가장자리 림 라이트, 왼쪽 위 유리 광택
 *  - 외곽은 원형 유지, 표면만 아주 미세하게 일렁임(ripple)
 */
private fun DrawScope.drawOrb(
    tex: ImageBitmap?, oxD: Float, oyD: Float, rD: Float, alpha: Float,
    energy: Float, gather: Float, low: Float, high: Float, flow: Float, t: Float,
) {
    val k = size.width / 1600f
    val c = Offset(oxD * k, oyD * k)
    // 4.5초 주기 호흡 (크기·밝기), Processing 땐 일정한 리듬으로 아주 약하게
    val br = sin(t * (2f * PI.toFloat() / 4.5f))
    val r = rD * k * (1f + OrbTuning.breathe * br + 0.02f * low)
    val bright = 1f + 0.05f * br + OrbTuning.activationBoost * energy + 0.25f * low + 0.15f * gather * sin(t * 2.4f)

    // 바깥 글로우: 아주 약하게, 활성화·음성 때만 살짝 넓어짐
    val gR = r * (1.55f + 0.25f * energy + 0.2f * low)
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFFAFE7F6).copy(alpha = OrbTuning.glow * (0.6f + 0.6f * energy) * alpha), Color(0xFFB9A8FF).copy(alpha = 0.10f * alpha), Color.Transparent),
            c, gR,
        ),
        gR, c, blendMode = BlendMode.Screen,
    )

    // 외곽: 원형 유지 + 아주 미세한 일렁임 (활성화·음성일 때만)
    val rip = OrbTuning.ripple * (energy * 0.6f + low * 0.8f + high * 0.6f)
    val outline = Path()
    val steps = 72
    for (i in 0..steps) {
        val a = (i.toFloat() / steps) * 2f * PI.toFloat()
        val rr = r * (1f + rip * (0.6f * sin(3f * a + t * 1.7f) + 0.4f * sin(5f * a - t * 2.3f)))
        val p = Offset(c.x + cos(a) * rr, c.y + sin(a) * rr)
        if (i == 0) outline.moveTo(p.x, p.y) else outline.lineTo(p.x, p.y)
    }
    outline.close()

    clipPath(outline) {
        // 바탕: 파스텔 블루
        drawCircle(Brush.radialGradient(listOf(Color(0xFFB8DAF5), Color(0xFF7FB2E8), Color(0xFF6E9EDB)), Offset(c.x - r * 0.25f, c.y - r * 0.3f), r * 1.5f), r * 1.1f, c, alpha = alpha)
        // Figma 텍스처가 있으면 그 결을 은은하게 (천천히 흘러감)
        if (tex != null) {
            val dw = r * 2f * 1.2072f * 1.2f
            val dh = r * 2f * 1.5114f * 1.2f
            val dx = c.x - r * 1.1f + sin(flow * 0.7f) * r * 0.15f
            val dy = c.y - r * 1.25f + cos(flow * 0.55f) * r * 0.18f
            drawImage(tex, dstOffset = IntOffset(dx.roundToInt(), dy.roundToInt()), dstSize = IntSize(dw.roundToInt(), dh.roundToInt()), alpha = 0.55f * alpha)
        }
        // 빛 덩어리 5개: (색, 기본 세기, 궤도 반지름, 속도들, 위상, 저음/고음 반응)
        data class Blob(val col: Color, val a: Float, val orbit: Float, val fx: Float, val fy: Float, val ph: Float, val size: Float, val lowR: Float, val highR: Float)
        val blobs = listOf(
            Blob(Color(0xFF3E7FE0), 0.85f, 0.42f, 0.83f, 0.61f, 0.0f, 0.78f, 0.35f, 0.0f),   // 진한 파스텔 블루 (저음)
            Blob(Color(0xFFBFE9F7), 0.70f, 0.48f, 0.57f, 0.92f, 2.1f, 0.62f, 0.1f, 0.2f),    // 아이스 블루
            Blob(Color(0xFFF4F9FF), 0.90f, 0.40f, 1.07f, 0.71f, 4.0f, 0.55f, 0.0f, 0.55f),   // 청백 (고음)
            Blob(Color(0xFFB9A8FF), 0.70f, 0.50f, 0.66f, 1.13f, 1.2f, 0.60f, 0.40f, 0.1f),   // 라벤더 (저음)
            Blob(Color(0xFFF6D2EC), 0.55f, 0.52f, 0.93f, 0.49f, 5.3f, 0.50f, 0.0f, 0.45f),   // 옅은 분홍 (고음)
        )
        for (b in blobs) {
            // 방향이 천천히 바뀌는 궤도: 서로 다른 주파수의 사인 두 개를 섞음
            val f = flow
            val dist = b.orbit * (1f - 0.65f * gather) * (1f + 0.25f * energy)          // Processing 때 가운데로 모임 / Activation 때 바깥으로 퍼짐
            val px = c.x + r * dist * (0.7f * sin(f * b.fx + b.ph) + 0.3f * sin(f * b.fy * 1.9f + b.ph * 0.5f))
            val py = c.y + r * dist * (0.7f * cos(f * b.fy + b.ph * 1.3f) + 0.3f * cos(f * b.fx * 1.6f + b.ph))
            val sz = r * b.size * (1f + b.lowR * low + 0.3f * energy)
            val aa = (b.a * (1f + b.highR * high * 1.5f)).coerceAtMost(1f) * alpha
            drawCircle(
                Brush.radialGradient(listOf(b.col.copy(alpha = aa), b.col.copy(alpha = aa * 0.45f), b.col.copy(alpha = 0f)), Offset(px, py), sz),
                sz, Offset(px, py), blendMode = BlendMode.Screen,
            )
        }
        // 가운데 은은한 발광 (Activation 때 중심→바깥으로 퍼짐)
        val coreR = r * (0.45f + 0.45f * energy)
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = (0.18f + 0.22f * energy + 0.2f * high) * alpha), Color.Transparent), c, coreR),
            coreR, c, blendMode = BlendMode.Screen,
        )
        // 림 라이트 (Figma inset white 62px)
        drawCircle(
            Brush.radialGradient(0.62f to Color.Transparent, 0.9f to Color.White.copy(alpha = 0.35f), 1f to Color.White.copy(alpha = 0.85f), center = c, radius = r * 1.02f),
            r * 1.05f, c, alpha = alpha,
        )
        // 유리 광택 (왼쪽 위)
        val hl = Offset(c.x - r * 0.38f, c.y - r * 0.45f)
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f * alpha), Color.Transparent), hl, r * 0.42f), r * 0.42f, hl)
        // 전체 밝기 (Screen 으로 살짝 들어올림)
        val lift = (bright - 1f).coerceIn(0f, 0.6f)
        if (lift > 0f) drawCircle(Color.White, r * 1.1f, c, alpha = 0.22f * lift * alpha, blendMode = BlendMode.Screen)
    }
}

/** 화면 전환 타이밍용: 오브 활성화(Tap to speak) 짧게 보여주고 다음 화면으로 */
suspend fun activationPause() = delay(650)

@Suppress("unused")
private val springSpec = spring<Float>(stiffness = Spring.StiffnessLow)

@Suppress("unused")
private val sizeZero = Size.Zero
