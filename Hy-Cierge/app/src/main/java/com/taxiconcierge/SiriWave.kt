package com.taxiconcierge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/*
 * ──────────────────────────────────────────────────────────────────────────
 *  SiriWave (kopiro/siriwave) iOS9 스타일을 Kotlin으로 옮긴 것
 *  원본: https://github.com/kopiro/siriwave  src/ios9-curve.ts, src/index.ts
 *
 *  ■ 원본 구조 (그대로)
 *    - 색깔 하나 = "곡선 묶음(iOS9Curve)". 묶음 안에 작은 곡선 2~4개가 랜덤으로 생긴다(noOfCurves [2,5)).
 *    - 곡선마다 랜덤: amplitude 0.3~1, offset -3~3, width 1~3, speed 0.5~1, verse -1~1, despawnTimeout 0.5~2초
 *    - 태어나면 +0.02/프레임으로 올라오고, 시간이 지나면 -0.02/프레임으로 가라앉고, 거의 0이 되면 새로 태어남
 *    - y = Σ |amp · sin(verse·x − phase) · att(x)| / 곡선수,  att(x) = (4 / (4 + x²))⁴, 양끝은 att(i/25·2) 로 한 번 더 감쇠
 *    - 합성: globalAlpha 0.7 + "lighter"(빛 더하기)
 *    - 전체 amplitude·speed 는 lerpSpeed 0.1 로 따라감
 *
 *  ■ 이 앱에서 더한 것
 *    - 색 묶음마다 반응 속도를 다르게(lag) → 소리가 커지면 일부 파형이 먼저 솟고 나머지가 뒤따름
 *    - morph(0→1): 화면 가운데 오브 자리에서 시작해 아래로 내려가며 가로로 펼쳐짐 (LightStage 에서 사용)
 *    - 가장자리는 흐리게, 가운데는 또렷하게 (가로 알파 마스크) + 크게 흐린 한 장을 깔아 빛 번짐
 *
 *  ■ 색: Figma "Voice waveform"(701:16714) SVG 에서 추출
 *    support #5D8190(0.55) / #354D80(0.83) / #657BAA(0.66) / #75B8EC(0.85) / #AFE7F6(0.85) / #F2F7FF(0.82)
 *    + 요청한 연보라·옅은 분홍을 은은하게 한 겹씩
 * ──────────────────────────────────────────────────────────────────────────
 */

private const val GRAPH_X = 25f
private const val AMPLITUDE_FACTOR = 0.8f
private const val SPEED_FACTOR = 1f
private const val ATT_FACTOR = 4f
private const val DESPAWN_FACTOR = 0.02f
private const val DEAD_REL = 0.006f
private const val PIXEL_DEPTH = 0.12f
private const val LERP_SPEED = 0.1f

private fun att(x: Float): Float = (ATT_FACTOR / (ATT_FACTOR + x * x)).pow(ATT_FACTOR)
private fun rnd(a: Float, b: Float) = a + Random.nextFloat() * (b - a)

/** 원본 iOS9Curve 한 색깔 묶음 (+ 묶음별 반응 속도 lag) */
class Ios9Curve(val color: Color, val alpha: Float, val lag: Float) {
    var spawnAt = 0L
    var n = 0
    var prevMax = 0f
    var amp = 0.3f          // 이 묶음이 지금 따라가고 있는 전체 진폭 (lag 로 늦게/빠르게)
    private var phases = FloatArray(0)
    private var amps = FloatArray(0)
    private var despawn = LongArray(0)
    private var offsets = FloatArray(0)
    private var speeds = FloatArray(0)
    private var finals = FloatArray(0)
    private var widths = FloatArray(0)
    private var verses = FloatArray(0)

    private fun spawn(now: Long) {
        spawnAt = now
        n = floor(rnd(2f, 5f)).toInt()
        phases = FloatArray(n); amps = FloatArray(n); despawn = LongArray(n)
        offsets = FloatArray(n); speeds = FloatArray(n); finals = FloatArray(n); widths = FloatArray(n); verses = FloatArray(n)
        for (ci in 0 until n) {
            despawn[ci] = rnd(500f, 2000f).toLong()
            offsets[ci] = rnd(-3f, 3f)
            speeds[ci] = rnd(0.5f, 1f)
            finals[ci] = rnd(0.3f, 1f)
            widths[ci] = rnd(1f, 3f)
            verses[ci] = rnd(-1f, 1f)
        }
    }

    private fun yRel(i: Float): Float {
        var y = 0f
        for (ci in 0 until n) {
            val t = 4f * (-1f + (ci.toFloat() / (n - 1)) * 2f) + offsets[ci]
            val x = i * (1f / widths[ci]) - t
            y += abs(amps[ci] * sin(verses[ci] * x - phases[ci]) * att(x))
        }
        return if (n == 0) 0f else y / n
    }

    /** 원본 yPos 를 높이 대비 비율로 */
    fun yPos(i: Float): Float = AMPLITUDE_FACTOR * amp * yRel(i) * att((i / GRAPH_X) * 2f)

    fun step(nowMs: Long, speed: Float, targetAmp: Float, frames: Float) {
        if (spawnAt == 0L) spawn(nowMs)
        amp += (targetAmp - amp) * (1f - (1f - lag).pow(frames))
        for (ci in 0 until n) {
            amps[ci] += if (spawnAt + despawn[ci] <= nowMs) -DESPAWN_FACTOR * frames else DESPAWN_FACTOR * frames
            amps[ci] = amps[ci].coerceIn(0f, finals[ci])
            phases[ci] = (phases[ci] + speed * speeds[ci] * SPEED_FACTOR * frames) % (2f * PI.toFloat())
        }
        var maxY = 0f
        var i = -GRAPH_X
        while (i <= GRAPH_X) { maxY = maxOf(maxY, yPos(i)); i += 0.5f }
        if (maxY < DEAD_REL && prevMax > maxY) spawnAt = 0L
        prevMax = maxY
    }
}

/** 원본 SiriWave 컨트롤러 */
class SiriEngine(light: Boolean) {
    val curves: List<Ios9Curve> = if (light) listOf(
        Ios9Curve(Color(0xFF1A4FA0), 0.45f, 0.10f),
        Ios9Curve(Color(0xFF4A6FD0), 0.40f, 0.08f),
        Ios9Curve(Color(0xFF3D8BE8), 0.55f, 0.14f),
        Ios9Curve(Color(0xFF6CC4F2), 0.55f, 0.11f),
        Ios9Curve(Color(0xFFB9E3FF), 0.70f, 0.06f),
    ) else listOf(
        Ios9Curve(Color(0xFF354D80), 0.83f, 0.07f),   // Figma
        Ios9Curve(Color(0xFF657BAA), 0.66f, 0.09f),   // Figma
        Ios9Curve(Color(0xFFB9A8FF), 0.35f, 0.05f),   // 연보라 (은은하게)
        Ios9Curve(Color(0xFF75B8EC), 0.85f, 0.16f),   // Figma 연하늘 — 먼저 솟음
        Ios9Curve(Color(0xFFF3C6E8), 0.22f, 0.04f),   // 옅은 분홍 (아주 은은하게)
        Ios9Curve(Color(0xFFAFE7F6), 0.85f, 0.12f),   // Figma
        Ios9Curve(Color(0xFFF2F7FF), 0.82f, 0.09f),   // Figma 청백
    )
    val support = if (light) Color(0xFF3D8BE8) to 0.5f else Color(0xFF5D8190) to 0.55f
    var speed = 0.06f

    fun step(nowMs: Long, frames: Float, targetAmp: Float, targetSpeed: Float) {
        speed += (targetSpeed - speed) * (1f - (1f - LERP_SPEED).pow(frames))
        for (c in curves) c.step(nowMs, speed, targetAmp, frames)
    }
}

/**
 * 파형 그리기.
 * @param morph   0 = 오브 자리에 모여 있음, 1 = 화면 하단 전체에 펼쳐짐 (층마다 조금씩 늦게 펼쳐짐)
 * @param origin  모여 있을 때 위치 (화면 대비 비율 0~1). 지정 안 하면 처음부터 하단
 * @param fade    전체 투명도
 */
fun DrawScope.drawSiri(e: SiriEngine, blend: BlendMode, glow: Boolean, morph: Float = 1f, origin: Offset = Offset.Unspecified, fade: Float = 1f) {
    if (fade <= 0.001f) return
    val w = size.width
    val h = size.height
    val bottom = h - 2f
    val heightMaxFull = h * if (origin.isUnspecified) 0.80f else 0.36f
    val o = if (origin.isUnspecified) Offset(w / 2, bottom) else Offset(origin.x * w, origin.y * h)

    // 지지선 (원본 drawSupportLine)
    val sm = ease((morph - 0.35f) / 0.65f)
    if (sm > 0f) {
        drawRect(
            Brush.horizontalGradient(
                0f to Color.Transparent,
                0.1f to e.support.first.copy(alpha = e.support.second),
                0.8f to e.support.first.copy(alpha = e.support.second),
                1f to Color.Transparent,
            ),
            topLeft = Offset(0f, bottom - (if (glow) 6f else 1.5f)),
            size = androidx.compose.ui.geometry.Size(w, if (glow) 12f else 3f),
            alpha = 0.7f * sm * fade,
            blendMode = blend,
        )
    }

    for ((gi, c) in e.curves.withIndex()) {
        if (c.n == 0) continue
        // 층마다 펼쳐지는 타이밍·방향을 조금씩 다르게
        val delay = 0.06f * gi
        val m = ease(((morph - delay) / (1f - delay)).coerceIn(0f, 1f))
        if (m <= 0f) continue
        val spread = 0.10f + 0.90f * m                         // 가로로 늘어남
        val drift = (if (gi % 2 == 0) 1f else -1f) * 0.04f * (1f - m) * w   // 서로 다른 방향으로 갈라짐
        val cx = o.x + (w / 2 - o.x) * m + drift
        val base = o.y + (bottom - o.y) * m                    // 아래로 내려감
        val hMax = heightMaxFull * (0.35f + 0.65f * m)
        val half = (w / 2f) * spread * 1.08f
        val path = Path()
        path.moveTo(cx - half, base)
        var i = -GRAPH_X
        while (i <= GRAPH_X) {
            path.lineTo(cx + half * (i / GRAPH_X), base - hMax * c.yPos(i))
            i += PIXEL_DEPTH
        }
        path.lineTo(cx + half, base)
        path.close()
        drawPath(path, c.color, alpha = 0.7f * c.alpha * fade * (0.25f + 0.75f * m), blendMode = blend)
    }
}

private fun ease(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

/** 빛 번짐 레이어 + 또렷한 레이어(가장자리 흐리게) 두 장으로 그리는 공통 래퍼 */
@Composable
fun SiriLayers(
    engine: SiriEngine,
    tick: () -> Long,
    modifier: Modifier,
    light: Boolean,
    morph: () -> Float = { 1f },
    origin: () -> Offset = { Offset.Unspecified },
    fade: () -> Float = { 1f },
) {
    val blend = if (light) BlendMode.SrcOver else BlendMode.Plus     // 원본 "lighter"
    Box(modifier) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = 0.95f }.blur(34.u, BlurredEdgeTreatment.Unbounded)) {
            tick()
            drawSiri(engine, blend, glow = true, morph = morph(), origin = origin(), fade = fade())
        }
        Canvas(
            Modifier.fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // 가장자리는 흐릿하게 사라지고 가운데는 또렷하게
                    drawRect(
                        Brush.horizontalGradient(0f to Color.Transparent, 0.22f to Color.Black, 0.78f to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                }
                .blur(2.u, BlurredEdgeTreatment.Unbounded),
        ) {
            tick()
            drawSiri(engine, blend, glow = false, morph = morph(), origin = origin(), fade = fade())
        }
    }
}

/** 프레임마다 엔진을 움직이는 루프. 반환값을 읽으면 다시 그려짐 */
@Composable
fun rememberSiriTick(engine: SiriEngine, level: () -> Float, active: () -> Boolean, calm: () -> Float = { 0.30f }): () -> Long {
    val tick = remember { mutableLongStateOf(0L) }
    LaunchedEffect(engine) {
        var last = 0L
        while (true) withFrameNanos { now ->
            val frames = if (last == 0L) 1f else ((now - last) / 16_666_667f).coerceIn(0.2f, 3f)
            last = now
            val lv = level().coerceIn(0f, 1f)
            val on = active()
            val amp = if (on) 0.42f + 0.85f * lv else calm()
            val spd = if (on) 0.10f + 0.16f * lv else 0.05f
            engine.step(now / 1_000_000, frames, amp, spd)
            tick.longValue = now
        }
    }
    return { tick.longValue }
}

/** 화면 하단 전체에 깔리는 파형 (기사 화면 등에서 단독으로 사용) */
@Composable
fun SiriWave(level: Float, active: Boolean, modifier: Modifier, light: Boolean = false) {
    val engine = remember(light) { SiriEngine(light) }
    val lv by rememberUpdatedState(level)
    val on by rememberUpdatedState(active)
    val tick = rememberSiriTick(engine, { lv }, { on })
    SiriLayers(engine, tick, modifier, light)
}
