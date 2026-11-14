package com.taxiconcierge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min

/**
 * Figma 픽셀값을 그대로 쓰기 위한 배율.
 * 화면 크기가 달라도 Figma 프레임(기사 1350×946, 승객 1600×900) 비율대로 커지고 작아진다.
 *   예) 60.u  = Figma 60px,  32.t = Figma 32px 글자
 */
val LocalScale = staticCompositionLocalOf { 1f }

val Int.u: Dp
    @Composable get() = (this * LocalScale.current).dp

val Int.t: TextUnit
    @Composable get() = (this * LocalScale.current / LocalDensity.current.fontScale).sp

@Composable
fun Scaled(w: Float, h: Float, bg: Color, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(bg)) {
        val s = min(maxWidth.value / w, maxHeight.value / h)
        CompositionLocalProvider(LocalScale provides s) { content() }
    }
}

@Composable
fun Tx(
    text: String,
    size: Int,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    lh: Int? = null,
    align: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        modifier,
        color = color,
        fontSize = size.t,
        fontWeight = weight,
        lineHeight = (lh ?: (size * 13 / 10)).t,
        textAlign = align,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

// ───────── 돈 표기 ─────────
private val won = NumberFormat.getNumberInstance(Locale.KOREA)
fun krw(v: Int) = "₩" + won.format(v)

/** ₩20,000 → "$14.49" (승객 언어 통화) */
fun money(l: Lang, krwValue: Int): String? {
    val c = Content.currencyOf(l) ?: return null
    val v = krwValue / c.krwPerUnit
    val num = if (c.decimals == 0) won.format(Math.round(v)) else String.format(Locale.US, "%,.${c.decimals}f", v)
    return c.symbol + num
}

/** 헤더 칩: "1 USD ≈ ₩1,380" */
fun rateLine(l: Lang): String? {
    val c = Content.currencyOf(l) ?: return null
    return "${c.per} ${c.code} ≈ ${krw(Math.round(c.per * c.krwPerUnit).toInt())}"
}

// ───────── 아이콘 (Figma 아이콘을 선으로 직접 그림: 외부 이미지 없이) ─────────
enum class Ic { BACK, ARROW, CHEV, CHEV_L, CHECK, PIN, PHONE, FORK, SWAP, GLOBE, WAVE, CHAT, MIC, RETRY, WARN }

@Composable
fun Ico(kind: Ic, color: Color, size: Dp, stroke: Float = 2f) {
    Canvas(Modifier.size(size)) {
        val k = this.size.minDimension / 24f
        val st = Stroke(width = stroke * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun line(vararg pts: Float) {
            val path = Path()
            path.moveTo(pts[0] * k, pts[1] * k)
            var i = 2
            while (i < pts.size) {
                path.lineTo(pts[i] * k, pts[i + 1] * k); i += 2
            }
            drawPath(path, color, style = st)
        }
        when (kind) {
            Ic.BACK -> { line(19f, 12f, 5f, 12f); line(11f, 6f, 5f, 12f, 11f, 18f) }
            Ic.ARROW -> { line(5f, 12f, 19f, 12f); line(13f, 6f, 19f, 12f, 13f, 18f) }
            Ic.CHEV -> line(9f, 6f, 15f, 12f, 9f, 18f)
            Ic.CHEV_L -> line(15f, 6f, 9f, 12f, 15f, 18f)
            Ic.CHECK -> line(5f, 12.5f, 10f, 17.5f, 19f, 7f)
            Ic.PIN -> {
                val path = Path()
                path.moveTo(12f * k, 21f * k)
                path.lineTo(5.7f * k, 13.1f * k)
                path.arcTo(Rect(5f * k, 3f * k, 19f * k, 17f * k), 154f, 232f, false)
                path.close()
                drawPath(path, color, style = st)
                drawCircle(color, 2.6f * k, Offset(12f * k, 10f * k), style = st)
            }
            Ic.PHONE -> {
                drawRoundRect(color, Offset(7f * k, 3f * k), Size(10f * k, 18f * k), CornerRadius(2.5f * k), style = st)
                line(11f, 17.5f, 13f, 17.5f)
            }
            Ic.FORK -> {
                line(6f, 3f, 6f, 8.5f, 8.5f, 11f, 11f, 8.5f, 11f, 3f); line(8.5f, 11f, 8.5f, 21f)
                line(17.5f, 21f, 17.5f, 3f, 14.5f, 6.5f, 14.5f, 13f, 17.5f, 13f)
            }
            Ic.SWAP -> { line(4f, 8f, 19f, 8f); line(15f, 4f, 19f, 8f, 15f, 12f); line(20f, 16f, 5f, 16f); line(9f, 12f, 5f, 16f, 9f, 20f) }
            Ic.GLOBE -> {
                drawCircle(color, 9f * k, Offset(12f * k, 12f * k), style = st)
                drawOval(color, Offset(8.3f * k, 3f * k), Size(7.4f * k, 18f * k), style = st)
                line(3f, 12f, 21f, 12f)
            }
            Ic.WAVE -> {
                val hs = floatArrayOf(4f, 9f, 14f, 8f, 16f, 10f, 5f)
                for ((i, h) in hs.withIndex()) {
                    val x = 3f + i * 3f
                    line(x, 12f - h / 2, x, 12f + h / 2)
                }
            }
            Ic.MIC -> {
                drawRoundRect(color, Offset(9f * k, 3f * k), Size(6f * k, 11f * k), CornerRadius(3f * k), style = st)
                val u = Path()
                u.arcTo(Rect(5.5f * k, 5f * k, 18.5f * k, 17.5f * k), 0f, 180f, true)
                drawPath(u, color, style = st)
                line(12f, 17.5f, 12f, 21f)
            }
            Ic.RETRY -> {
                val a = Path()
                a.arcTo(Rect(4.5f * k, 4.5f * k, 19.5f * k, 19.5f * k), -60f, 300f, true)
                drawPath(a, color, style = st)
                line(16.2f, 2.8f, 16.2f, 6.9f, 12.2f, 6.9f)
            }
            Ic.WARN -> {
                line(12f, 3.5f, 21.5f, 20f, 2.5f, 20f, 12f, 3.5f)
                line(12f, 10f, 12f, 14f); line(12f, 17f, 12f, 17.2f)
            }
            Ic.CHAT -> {
                drawRoundRect(color, Offset(3f * k, 4f * k), Size(18f * k, 13f * k), CornerRadius(3f * k), style = st)
                line(8f, 17f, 8f, 21f, 12.5f, 17f)
                line(7.5f, 9f, 16.5f, 9f); line(7.5f, 12.5f, 13.5f, 12.5f)
            }
        }
    }
}

// ───────── 음파 효과 (Figma "Voice waveform") ─────────

/** 끊김 없이 계속 흐르는 시간(초). 반복 애니메이션처럼 되감기는 순간이 없어서 움직임이 자연스럽다. */
@Composable
fun rememberSeconds(): androidx.compose.runtime.State<Float> = androidx.compose.runtime.produceState(0f) {
    val start = androidx.compose.runtime.withFrameNanos { it }
    while (true) androidx.compose.runtime.withFrameNanos { value = (it - start) / 1_000_000_000f }
}

/** 바닥 음파 = SiriWave iOS9 파형 (SiriWave.kt) */
@Composable
fun VoiceGlow(level: Float, active: Boolean, modifier: Modifier, light: Boolean = false) =
    SiriWave(level, active, modifier, light)

/** 크게 번지게(blur). 안드로이드 12 이상에서만 적용, 그 아래는 그냥 그려짐 */
fun Modifier.graphicsLayerBlur(radius: Dp): Modifier =
    this.blur(radius, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
