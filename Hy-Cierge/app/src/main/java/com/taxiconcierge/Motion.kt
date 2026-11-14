package com.taxiconcierge

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.exp
import kotlin.math.cos
import kotlin.math.sin

/** 요즘 앱들이 쓰는 부드러운 감속 곡선 (easeOutQuint) */
val EaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/** 화면 어디를 만지거나(태블릿) 마우스를 올렸는지 — 배경 빛이 따라온다 (monopo.london 느낌) */
object TouchField {
    var target by mutableStateOf(Offset(0.5f, 0.65f))   // 화면 대비 비율 0~1
    var lastTouch by mutableLongStateOf(0L)
    var pressed by mutableStateOf(false)
}

/** 최상위 화면에 붙이면 손가락·마우스 위치를 TouchField 에 기록 (버튼 터치는 그대로 통과) */
fun Modifier.trackTouch(): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val e = awaitPointerEvent(PointerEventPass.Initial)
            val c = e.changes.firstOrNull() ?: continue
            if (size.width > 0 && size.height > 0) {
                TouchField.target = Offset(c.position.x / size.width, c.position.y / size.height)
            }
            TouchField.lastTouch = System.currentTimeMillis()
            TouchField.pressed = c.pressed
        }
    }
}

/**
 * 살아 움직이는 오로라 배경
 *  - 빛 덩어리 4개가 서로 다른 느린 사인 곡선을 따라 계속 흘러다닌다 (끊김 없는 연속 시간)
 *  - 손가락·마우스를 따라오는 빛 덩어리: 액체처럼 살짝 늦게 따라오고, 누르면 더 밝게 퍼진다.
 *    아무도 안 만지면 2~3초에 걸쳐 원래 자리로 돌아가 혼자 떠다닌다.
 * @param strength 1 = 음성 화면(진하게), 0.3 = 일반 화면(은은하게)
 */
fun Modifier.glow(@Suppress("UNUSED_PARAMETER") strength: Float = 1f): Modifier = this
// ↑ 배경은 이제 PassengerBackdrop(LightStage.kt) 하나가 모든 승객 화면 뒤에 깔려서 끊기지 않게 이어짐.
//   예전 화면별 오로라는 끔 (화면마다 따로 그리면 전환할 때 배경이 뚝 끊겼음)

/**
 * 화면에 들어올 때 살짝 아래에서 떠오르며 또렷해지기.
 * @param index 순서 (0.09초씩 늦게) / delayMs 를 주면 그 시간만큼 기다렸다가
 * @param key 값이 바뀔 때마다 다시 재생 / dx 가로로 들어오기
 */
fun Modifier.enter(index: Int = 0, key: Any? = Unit, dx: Float = 0f, delayMs: Long = -1L): Modifier = composed {
    val v = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        delay(if (delayMs >= 0) delayMs else 90L * index)
        v.animateTo(1f, tween(760, easing = EaseOutQuint))
    }
    this.graphicsLayer {
        val p = v.value
        alpha = p
        translationY = if (dx == 0f) (1f - p) * 56f else 0f
        translationX = (1f - p) * dx
        val sc = 0.985f + 0.015f * p
        scaleX = sc; scaleY = sc
    }
}

/** 누르면 살짝 눌리는(작아지는) 버튼 */
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.955f else 1f, spring(dampingRatio = 0.55f, stiffness = 700f), label = "press")
    this
        .graphicsLayer { scaleX = sc; scaleY = sc }
        .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}

/** 숨 쉬듯 살짝 커졌다 작아지기 (Tap to speak 버튼 등) */
fun Modifier.breathe(amount: Float = 0.03f): Modifier = composed {
    val t = rememberInfiniteTransition(label = "breathe")
    val s by t.animateFloat(1f, 1f + amount, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
    this.graphicsLayer { scaleX = s; scaleY = s }
}

/** 마이크 뒤에서 은은하게 숨 쉬는 빛 (소리가 크면 부드럽게 커짐) */
@Composable
fun MicPulse(level: Float, size: Int, color: Color = Color(0xFF8FD3FF)) {
    val time by rememberSeconds()
    val lv by animateFloatAsState(level, spring(dampingRatio = 0.8f, stiffness = 120f), label = "lv")
    Canvas(Modifier.size(size.u).graphicsLayerBlur(24.u)) {
        val base = this.size.minDimension * 0.22f
        val breathe = 1f + 0.06f * sin(time * 1.6f)
        val r = base * breathe * (1f + 0.9f * lv)
        drawCircle(
            Brush.radialGradient(
                listOf(color.copy(alpha = 0.55f + 0.35f * lv), Color(0xFF6E6FE6).copy(alpha = 0.25f + 0.3f * lv), Color.Transparent),
                center = center,
                radius = r * 1.8f,
            ),
            radius = r * 1.8f,
        )
    }
}
