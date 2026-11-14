package com.taxiconcierge

import androidx.compose.foundation.Image
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.draw.drawWithContent
import kotlin.math.sin
import kotlin.math.roundToInt
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.core.calculateTargetValue

/** 승객 화면 색 (Figma P01~P25, 다크 1600×900) */
object PC {
    val bg = Color(0xFF131313)
    val header = Color(0xFF191919)
    val btn = Color(0xFF2B2B2B)
    val border = Color(0xFF3E3E3E)
    val card = Color(0xFF222222)
    val text = Color(0xFFF5F5F5)
    val sub = Color(0xFFC5C5C5)
    val mute = Color(0xFF8D8D8D)
    val accent = Color(0xFFBDE6F6)
    val onAccent = Color(0xFF131313)
    val green = Color(0xFF8BD6A8)
    val orange = Color(0xFFE9B26B)
    val cyan = Color(0xFFA6DFF4)
    val pillBorder = Color(0xFF8297A0)
}

/** 오른쪽 아래 통번역 아이콘과 안 겹치게 비워 두는 폭 */
private val IconGap: androidx.compose.ui.unit.Dp
    @Composable get() = 100.u

private val withTranslate = setOf(
    P.HOME, P.KIT, P.NEAR, P.PLACE, P.PLACE_INFO, P.SPOT, P.TOOLS, P.FOOD, P.FOOD_PREF, P.STREET, P.BOOK,
    P.DROP, P.DROP_CONFIRM, P.CONVERT, P.ALMOST, P.LIVE, P.PLAZA,
)

/** 화면 흐름 메모: P01 → P03A 로 막 넘어왔는지 (그땐 오브가 안착한 뒤에 버튼이 올라옴) */
object PFlow { var fromWelcome = false }

@Composable
fun PassengerApp(s: AppState) {
    Scaled(1600f, 900f, PC.bg) {
        Box(Modifier.fillMaxSize()) {
            // ① 배경(Figma 배경 + 천천히 흐르는 빛)은 화면 전체
            PassengerBackdrop(s)
            // ② 1600×900 고정 프레임: 오브·링·책상과 글자·버튼이 같은 좌표계라서 어떤 화면 비율에서도 정확히 겹친다
            Box(Modifier.align(Alignment.Center).size(1600.u, 900.u)) {
                LightStage(s)
                AnimatedContent(
                    targetState = s.p,
                    contentKey = { if (it == P.PROMPT) P.ASK else it },
                    transitionSpec = {
                        (fadeIn(tween(560, delayMillis = 140, easing = EaseOutQuint)) +
                            slideInVertically(tween(700, delayMillis = 140, easing = EaseOutQuint)) { it / 40 }) togetherWith
                            (fadeOut(tween(280)) + slideOutVertically(tween(320)) { -it / 60 }) using SizeTransform(clip = false)
                    },
                    label = "screen",
                    modifier = Modifier.fillMaxSize(),
                ) { screen ->
                    when (screen) {
                        P.WELCOME -> Welcome(s)
                        P.PROMPT, P.ASK -> PAsk(s)
                        P.LANGUAGE -> PLanguage(s)
                        P.LISTEN -> PListen(s)
                        P.REVIEW -> PReview(s)
                        P.UNCLEAR -> PUnclear(s)
                        P.TYPE -> PType(s)
                        P.EDIT -> PEdit(s)
                        P.WAITING -> PWaiting(s)
                        P.CONFIRMED -> PConfirmed(s)
                        P.HOME -> PHome(s)
                        P.KIT -> PKit(s)
                        P.NEAR -> PNear(s)
                        P.PLACE -> PPlace(s)
                        P.PLACE_INFO -> PPlaceInfo(s)
                        P.SEND -> PSend(s)
                        P.SPOT -> PSpot(s)
                        P.TALK -> PTalk(s)
                        P.TOOLS -> PTools(s)
                        P.FOOD -> PFood(s)
                        P.FOOD_PREF -> PFoodPref(s)
                        P.STREET -> PStreet(s)
                        P.BOOK -> PBook(s)
                        P.DROP -> PDrop(s)
                        P.DROP_CONFIRM -> PDropConfirm(s)
                        P.DROP_PENDING -> PDropPending(s)
                        P.DROP_DONE -> PDropDone(s)
                        P.CONVERT -> PConvert(s)
                        P.ALMOST -> PAlmost(s)
                        P.LIVE -> PLive(s)
                        P.PLAZA -> PPlaza(s)
                    }
                }
                if (s.p in withTranslate) TranslatePill(s, Modifier.align(Alignment.BottomEnd).padding(end = 40.u, bottom = 40.u))
                s.toast?.let { Toast(it, Modifier.align(Alignment.BottomCenter).padding(bottom = 56.u)) }
                // 지역 큐레이션 알림: 새 동네에 들어오면 위에 잠깐
                val rb = s.regionBanner
                if (rb != null && s.p.ordinal >= P.HOME.ordinal && s.p != P.LIVE) RegionBanner(s, rb, Modifier.align(Alignment.TopCenter).padding(top = 122.u))
            }
            s.sheet?.let { PSheet(s, it) }
            DemoPanel(s, Modifier.align(Alignment.BottomEnd).padding(14.u))
        }
    }

    // 화면 자동 넘김 (시연 흐름)
    LaunchedEffect(s.p) {
        // 홈 3단계 중 들어가 본 곳 기록 (체크 표시·다음 단계 안내용)
        if (s.p in listOf(P.KIT, P.NEAR, P.TOOLS) && s.p !in s.visited) s.visited.add(s.p)
        when (s.p) {
            // P01: 책상 이미지 → 2.6초 뒤 오브가 숑 튀어나와 두웅 안착 (LightStage) → P03A
            P.WELCOME -> { delay(2600); if (s.p == P.WELCOME) { PFlow.fromWelcome = true; s.p = P.ASK } }
            P.PROMPT -> s.p = P.ASK
            P.WAITING -> {
                s.waitDone = false
                delay(2600); s.waitDone = true
                delay(1400); if (s.p == P.WAITING) s.pConfirmed()
            }
            // P24: 기사님이 확인하면 → P24A
            P.DROP_PENDING -> { delay(3400); if (s.p == P.DROP_PENDING) s.pDropConfirmed() }
            else -> {}
        }
    }
    // Processing: 다 들은 뒤 오브가 가운데로 모이는 걸 잠깐 보여주고 결과 화면으로
    LaunchedEffect(s.awaitingResult) {
        if (s.awaitingResult) {
            delay(950)
            val go = s.awaitingResult && s.p == P.LISTEN
            s.awaitingResult = false
            if (go) s.pHeard(s.heard)
        }
    }
    LaunchedEffect(s.regionBanner) {
        if (s.regionBanner != null) { delay(7000); s.regionBanner = null }
    }
    LaunchedEffect(s.toast) {
        if (s.toast != null) { delay(2600); s.toast = null }
    }
}

// ───────────────────────── 공통 부품 ─────────────────────────

@Composable
fun PHeader(s: AppState, back: Boolean = true, rate: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().height(106.u).background(Color(0xCC191919)).padding(horizontal = 36.u),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (back) {
            Box(
                Modifier.pressable { s.pBack() }.size(72.u).clip(RoundedCornerShape(16.u)).background(PC.btn).border(2.u, PC.border, RoundedCornerShape(16.u)),
                contentAlignment = Alignment.Center,
            ) { Ico(Ic.BACK, PC.text, 36.u) }
            Spacer(Modifier.width(24.u))
        }
        Tx("Hy-Cierge", 30, PC.sub, FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        if (rate) rateLine(s.lang)?.let {
            Row(
                Modifier.pressable { s.p = P.CONVERT }.height(72.u).clip(RoundedCornerShape(16.u)).background(PC.card).border(2.u, PC.border, RoundedCornerShape(16.u))
                    .padding(horizontal = 28.u),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Ico(Ic.SWAP, PC.text, 34.u)
                Spacer(Modifier.width(18.u))
                Tx("$it  ·  ${(if (Rates.live) LT.liveRate else PT.sample)[s.lang]}", 26, PC.text, FontWeight.SemiBold)
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(2.u).background(PC.btn))
}

/** 헤더 + 본문 틀 */
@Composable
fun PPage(s: AppState, back: Boolean = true, rate: Boolean = true, title: String? = null, sub: String? = null, body: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().glow(0.32f)) {
        PHeader(s, back, rate)
        Column(Modifier.weight(1f).fillMaxWidth().padding(start = 56.u, end = 56.u, top = 44.u, bottom = 44.u)) {
            if (title != null) Tx(title, 58, PC.text, FontWeight.Medium, lh = 72, modifier = Modifier.enter(0))
            if (sub != null) {
                Spacer(Modifier.height(8.u))
                Tx(sub, 25, PC.sub, modifier = Modifier.enter(1))
            }
            if (title != null) Spacer(Modifier.height(40.u))
            Column(Modifier.weight(1f).fillMaxWidth().enter(2)) { body() }
        }
    }
}

@Composable
fun PCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: Int = 32, content: @Composable ColumnScope.() -> Unit) {
    val m = (if (onClick != null) modifier.pressable(onClick = onClick) else modifier)
        .clip(RoundedCornerShape(30.u)).background(PC.card).border(2.u, PC.border, RoundedCornerShape(30.u))
    Column(m.padding(padding.u), content = content)
}

@Composable
fun PBtn(text: String, onClick: () -> Unit, primary: Boolean = false, modifier: Modifier = Modifier, h: Int = 80) {
    Box(
        modifier.pressable(onClick = onClick).height(h.u).widthIn(min = 200.u).clip(RoundedCornerShape(16.u))
            .background(if (primary) PC.accent else PC.btn)
            .border(2.u, if (primary) PC.accent else PC.border, RoundedCornerShape(16.u))
            .padding(horizontal = 40.u),
        contentAlignment = Alignment.Center,
    ) { Tx(text, 28, if (primary) PC.onAccent else PC.text, FontWeight.SemiBold, maxLines = 1) }
}

@Composable
fun PTile(icon: Ic, size: Int = 80, bg: Color = PC.btn, fg: Color = PC.text) {
    Box(Modifier.size(size.u).clip(RoundedCornerShape((size / 5).u)).background(bg), contentAlignment = Alignment.Center) {
        Ico(icon, fg, (size / 2).u)
    }
}

/** 사진 자리 (Figma 사진은 PC에서 res/drawable에 넣으면 교체 가능) */
@Composable
fun Photo(seed: Int, modifier: Modifier) {
    val pairs = listOf(
        Color(0xFF6B3A2A) to Color(0xFF2B1A14),
        Color(0xFF2F5D4A) to Color(0xFF15231C),
        Color(0xFF5A4A2E) to Color(0xFF221C12),
        Color(0xFF34495E) to Color(0xFF151C24),
    )
    val (a, b) = pairs[Math.floorMod(seed, pairs.size)]
    Box(modifier.background(Brush.linearGradient(listOf(a, b))), contentAlignment = Alignment.Center) {
        Ico(Ic.PIN, Color(0x55FFFFFF), 64.u)
    }
}

@Composable
fun FeatureCard(icon: Ic, title: String, sub: String, onClick: () -> Unit, modifier: Modifier) {
    PCard(modifier.height(310.u), onClick) {
        Row {
            PTile(icon)
            Spacer(Modifier.weight(1f))
            Ico(Ic.ARROW, PC.text, 36.u)
        }
        Spacer(Modifier.weight(1f))
        Tx(title, 34, PC.text, FontWeight.SemiBold)
        Spacer(Modifier.height(14.u))
        Tx(sub, 24, PC.sub, lh = 34)
    }
}

/** 통번역 아이콘: 목적지 확정 후 화면 오른쪽 아래에 고정 (보고서 4번 시나리오) */
@Composable
fun TranslatePill(s: AppState, modifier: Modifier) {
    Box(
        modifier.enter(4).pressable { s.talkReturn = s.p; s.p = P.TALK }.size(92.u).clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Ico(Ic.CHAT, Color.Black, 38.u, stroke = 1.8f)
            Tx(PT.translateCaps[s.lang], 11, Color.Black, FontWeight.Bold, maxLines = 1)
        }
    }
}


@Composable
fun Toast(text: String, modifier: Modifier) {
    Box(
        modifier.enter().clip(RoundedCornerShape(40.u)).background(PC.accent).padding(horizontal = 40.u, vertical = 22.u),
    ) { Tx(text, 26, PC.onAccent, FontWeight.SemiBold) }
}

// ───────────────────────── P01 환영 ─────────────────────────

@Composable
fun Welcome(s: AppState) {
    Box(Modifier.fillMaxSize().clickable { PFlow.fromWelcome = true; s.p = P.ASK }) {
        Column(Modifier.fillMaxWidth().padding(top = 200.u), horizontalAlignment = Alignment.CenterHorizontally) {
            Tx("Welcome", 60, PC.text, FontWeight.Bold, lh = 78, align = TextAlign.Center, modifier = Modifier.enter(0))
            Tx("I'm Hy-Cierge", 60, PC.text, FontWeight.Bold, lh = 78, align = TextAlign.Center, modifier = Modifier.enter(3))
        }
    }
}

// ───────────────────────── P02 언어 선택 (아이폰식 휠) ─────────────────────────

private val wheelLangs = listOf(Lang.KO, Lang.ZH, Lang.JA, Lang.EN)

/**
 * 아이폰 시계 앱처럼 위아래로 굴리는 언어 휠.
 *  - 손가락으로 끌면 따라오고, 튕기면 관성으로 굴러가다 가장 가까운 칸에 '착' 멈춤
 *  - 가운데에서 멀어질수록 원통처럼 기울고(rotationX) 작아지고 흐려짐
 *  - 가운데 줄 양옆에 ▸ ◂ 표시
 */
@Composable
fun PLanguage(s: AppState) {
    val density = LocalDensity.current
    val itemDp = 116.u
    val itemPx = with(density) { itemDp.toPx() }
    val pos = remember { Animatable(wheelLangs.indexOf(s.lang).coerceAtLeast(0).toFloat()) }
    val scope = rememberCoroutineScope()
    val decay = remember(density) { androidx.compose.animation.splineBasedDecay<Float>(density) }
    val maxI = (wheelLangs.size - 1).toFloat()
    fun settle(velocityItems: Float) = scope.launch {
        val target = decay.calculateTargetValue(pos.value, velocityItems).coerceIn(0f, maxI).let { kotlin.math.round(it) }
        pos.animateTo(target, spring(dampingRatio = 0.82f, stiffness = 220f), initialVelocity = velocityItems)
    }
    val sel = kotlin.math.round(pos.value).toInt().coerceIn(0, wheelLangs.lastIndex)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) { CenterHeader(s) }
        Column(Modifier.fillMaxWidth().padding(top = 170.u), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.enter(0).height(56.u).clip(RoundedCornerShape(28.u)).background(Color(0x1AFFFFFF))
                    .border(2.u, Color(0x66FFFFFF), RoundedCornerShape(28.u)).padding(horizontal = 30.u),
                contentAlignment = Alignment.Center,
            ) { Tx(PT.changeLanguageCaps[s.lang], 22, PC.text, FontWeight.Bold) }
        }
        // 휠 (가운데 y=480)
        Box(
            Modifier.fillMaxWidth().padding(top = 270.u).height(420.u)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { d ->
                        scope.launch {
                            val v = pos.value - d / itemPx
                            // 끝을 넘어가면 고무줄처럼 덜 따라옴
                            val over = if (v < 0f) v else if (v > maxI) v - maxI else 0f
                            pos.snapTo(v - over * 0.6f)
                        }
                    },
                    onDragStopped = { v -> settle(-v / itemPx) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            // 선택 띠
            Box(Modifier.width(720.u).height(itemDp).clip(RoundedCornerShape(26.u)).background(Color(0x14FFFFFF)))
            Row(Modifier.width(820.u), verticalAlignment = Alignment.CenterVertically) {
                Tx("▸", 40, PC.accent, FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Tx("◂", 40, PC.accent, FontWeight.Bold)
            }
            for ((i, l) in wheelLangs.withIndex()) {
                val d = i - pos.value
                val ad = kotlin.math.abs(d)
                if (ad > 2.4f) continue
                Box(
                    Modifier.width(680.u).height(itemDp)
                        .graphicsLayer {
                            translationY = d * itemPx * (1f - 0.08f * ad)
                            rotationX = (-d * 24f).coerceIn(-70f, 70f)
                            cameraDistance = 10f * density.density
                            val sc = 1f - 0.12f * ad.coerceAtMost(2f)
                            scaleX = sc; scaleY = sc
                            alpha = (1f - 0.42f * ad).coerceIn(0f, 1f)
                        }
                        .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                            scope.launch { pos.animateTo(i.toFloat(), spring(dampingRatio = 0.82f, stiffness = 200f)) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Tx(l.native, 50, if (i == sel) Color.White else PC.sub, FontWeight.Bold)
                        Spacer(Modifier.width(18.u))
                        Tx(l.enName, 26, if (i == sel) PC.accent else PC.mute, FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.u))
                    }
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 90.u).enter(delayMs = 300)
                .pressable { s.pPickLang(wheelLangs[sel]) }
                .width(300.u).height(80.u).clip(RoundedCornerShape(50.u)).background(Color(0xFFBCE8F5)),
            contentAlignment = Alignment.Center,
        ) { Tx(PT.done[wheelLangs[sel]], 30, Color(0xFF152A31), FontWeight.Bold) }
    }
}

// ───────────────────────── 음성 화면 공통 ─────────────────────────

/** P03 계열 헤더: 투명 + 가운데 제목 */
@Composable
fun GlowHeader(s: AppState) {
    Box(Modifier.fillMaxWidth().height(106.u).padding(horizontal = 20.u)) {
        Box(
            Modifier.align(Alignment.CenterStart).size(72.u).clip(RoundedCornerShape(16.u)).background(Color(0x332B2B2B))
                .clickable { s.pBack() },
            contentAlignment = Alignment.Center,
        ) { Ico(Ic.BACK, PC.text, 36.u) }
        Tx("Hy-Cierge", 24, PC.text, FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
    }
    Box(Modifier.fillMaxWidth().height(2.u).background(Color(0x33FFFFFF)))
}

/** 말하는 문장: 마지막 몇 글자는 보라빛 + 깜빡이는 커서 (Figma P03) */
@Composable
fun LiveSentence(text: String, typing: Boolean, size: Int = 60) {
    if (typing) {
        val blink = rememberInfiniteTransition(label = "caret")
        val a by blink.animateFloat(1f, 0f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "caretAlpha")
        val tail = minOf(5, text.length)
        val str = buildAnnotatedString {
            append(text.dropLast(tail))
            withStyle(SpanStyle(color = Color(0xFF9C8CFF))) { append(text.takeLast(tail)) }
            withStyle(SpanStyle(color = PC.text.copy(alpha = a))) { append("|") }
        }
        Text(str, color = PC.text, fontSize = size.t, fontWeight = FontWeight.Medium, lineHeight = (size * 7 / 5).t, textAlign = TextAlign.Center)
        return
    }
    val sweep = remember(text) { Animatable(0f) }
    LaunchedEffect(text) { sweep.animateTo(1.25f, tween(1700, easing = EaseInOutCubic)) }
    val p = sweep.value
    // 그라데이션 띠: 흰색(지나감) → 하늘 → 연보라 → 아직 안 보임
    val stops = listOf(
        (p - 0.22f) to PC.text,
        (p - 0.12f) to Color(0xFF9FDCF6),
        (p - 0.04f) to Color(0xFFB9A8FF),
        p to Color(0x00B9A8FF),
    ).map { (f, c) -> f.coerceIn(0f, 1f) to c }
    val clean = mutableListOf<Pair<Float, Color>>()
    for ((f, c) in stops) {
        val ff = if (clean.isNotEmpty() && f <= clean.last().first) (clean.last().first + 0.0001f).coerceAtMost(1f) else f
        clean += ff to c
    }
    val brush = if (p >= 1.24f) androidx.compose.ui.graphics.SolidColor(PC.text) else Brush.horizontalGradient(*clean.toTypedArray())
    Text(
        text,
        style = TextStyle(brush = brush, fontSize = size.t, fontWeight = FontWeight.Medium, lineHeight = (size * 7 / 5).t, textAlign = TextAlign.Center),
    )
}

@Composable
fun RequestCard(s: AppState, text: String, typing: Boolean, modifier: Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(26.u)).background(Color(0x0FFFFFFF)).border(2.u, Color(0x2EFFFFFF), RoundedCornerShape(26.u))
            .clickable(enabled = !typing) { s.p = P.EDIT }.padding(36.u),
    ) {
        Tx(PT.yourRequestCap[s.lang], 22, Color(0xFF8FC7FF), FontWeight.SemiBold, modifier = Modifier.align(Alignment.TopStart))
        Box(Modifier.align(Alignment.Center).padding(horizontal = 40.u)) { LiveSentence(text, typing) }
    }
}

// ───────────────────────── P03A 말하기 / 입력 ─────────────────────────

@Composable
fun PAsk(s: AppState) {
    // 오브·궤도 링·책상은 LightStage 가 그린다. 여기는 Figma P03A 자리 그대로 글자·버튼만.
    // P01 에서 막 넘어왔으면 오브가 '숑 → 두웅' 안착할 때까지 기다렸다가 버튼이 올라온다.
    val delayIn = remember { if (PFlow.fromWelcome) 1150L else 0L }
    val show = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        PFlow.fromWelcome = false
        if (delayIn > 0) {
            delay(delayIn)
            s.speak(Content.whereTo[s.lang])
        }
        show.animateTo(1f, tween(900, easing = EaseOutQuint))
    }
    val sh = show.value
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.graphicsLayer { alpha = sh }) { CenterHeader(s, back = false) }
        Tx(
            Content.whereTo[s.lang], 60, PC.text, FontWeight.Bold, lh = 76, align = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 200.u).enter(delayMs = (delayIn * 0.55f).toLong() + 100L),
        )
        // Tap to speak (y 655) + 뒤에서 숨 쉬는 빛
        Box(
            Modifier.fillMaxWidth().padding(top = 575.u).height(240.u).graphicsLayer { alpha = sh; translationY = (1f - sh) * 70f },
            contentAlignment = Alignment.Center,
        ) {
            ButtonHalo(Modifier.size(760.u, 240.u))
            Box(
                Modifier.pressable(enabled = sh > 0.5f && !s.orbActivating) {
                    // Activation: 오브가 밝아지며 빛이 바깥으로 퍼지는 걸 0.65초 보여준 뒤 듣기 시작
                    s.orbActivating = true
                    scope.launch {
                        try {
                            delay(650)
                            if (s.p == P.ASK || s.p == P.PROMPT) s.pListen()
                        } finally {
                            s.orbActivating = false
                        }
                    }
                }.width(500.u).height(80.u).clip(RoundedCornerShape(50.u)).background(Color(0xFFBCE8F5)),
                contentAlignment = Alignment.Center,
            ) { Tx(PT.tapToSpeak[s.lang], 30, Color(0xFF152A31), FontWeight.Bold) }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 763.u).graphicsLayer { alpha = sh },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(250.u).height(60.u).pressable { s.heard = ""; s.p = P.TYPE }, contentAlignment = Alignment.Center) {
                Tx(PT.typeInstead[s.lang], 24, Color(0x80D2E3F1), FontWeight.Bold)
            }
            Spacer(Modifier.width(10.u))
            Tx("|", 30, Color(0x80F5F5F5), FontWeight.Bold)
            Spacer(Modifier.width(10.u))
            Box(Modifier.width(250.u).height(60.u).pressable { s.langReturn = P.ASK; s.p = P.LANGUAGE }, contentAlignment = Alignment.Center) {
                Tx(PT.chooseLanguage[s.lang], 24, Color(0x80D2E3F1), FontWeight.Bold)
            }
        }
    }
}

/** Figma 헤더: 가운데 제목 + 아래 가는 선 (뒤로 버튼은 선택) */
@Composable
fun CenterHeader(s: AppState, back: Boolean = true) {
    Box(Modifier.fillMaxWidth().height(106.u).padding(horizontal = 20.u)) {
        if (back) Box(
            Modifier.align(Alignment.CenterStart).pressable { s.pBack() }.size(72.u).clip(RoundedCornerShape(16.u)).background(Color(0x332B2B2B)),
            contentAlignment = Alignment.Center,
        ) { Ico(Ic.BACK, PC.text, 36.u) }
        Tx("Hy-Cierge", 24, Color.White, FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
    }
    Box(Modifier.fillMaxWidth().height(1.u).background(Color(0x40FFFFFF)))
}

// ───────────────────────── P03C 듣는 중 ─────────────────────────

@Composable
fun PListen(s: AppState) {
    // 오브는 마이크 자리(800,300)로 올라가 목소리에 반응 (LightStage). 바닥엔 Siri 파형이 따로 목소리에 반응.
    val ui by animateFloatAsState(if (s.listening) 1f else 0f, tween(500), label = "listenUi")
    Box(Modifier.fillMaxSize()) {
        SiriWave(
            s.level, s.listening,
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(320.u).graphicsLayer { alpha = 0.3f + 0.7f * ui },
        )
        Column(Modifier.fillMaxSize()) { CenterHeader(s) }
        // 오브 위 마이크 글리프
        Box(Modifier.fillMaxWidth().padding(top = 274.u), contentAlignment = Alignment.TopCenter) {
            Ico(Ic.MIC, Color(0xFF14304A), 52.u, stroke = 2.4f)
        }
        Column(Modifier.fillMaxWidth().padding(top = 382.u), horizontalAlignment = Alignment.CenterHorizontally) {
            Tx(PT.speakNaturally[s.lang], 56, PC.text, FontWeight.Bold, lh = 72, align = TextAlign.Center, modifier = Modifier.enter(1))
            Spacer(Modifier.height(22.u))
            val pill by animateColorAsState(if (s.listening) Color(0xFFE5393B) else Color(0x33FFFFFF), tween(400), label = "pill")
            Box(Modifier.enter(2).clip(RoundedCornerShape(30.u)).background(pill).padding(horizontal = 26.u, vertical = 6.u)) {
                Tx(if (s.listening) PT.listeningBadge[s.lang] else "· · ·", 22, Color.White, FontWeight.Bold)
            }
            Spacer(Modifier.height(30.u))
            if (s.liveText.isNotBlank()) {
                Box(Modifier.padding(horizontal = 180.u).heightIn(max = 130.u).clipToBounds(), contentAlignment = Alignment.TopCenter) {
                    LiveSentence(s.liveText, s.listening, 40)
                }
            }
        }
        // Finish: 지금까지 들린 말로 끝내기
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 105.u).graphicsLayer { alpha = ui }
                .pressable(enabled = s.listening) { s.finishListening() }
                .width(260.u).height(80.u).clip(RoundedCornerShape(50.u)).background(Color(0xFFBCE8F5)),
            contentAlignment = Alignment.Center,
        ) { Tx(PT.finish[s.lang], 30, Color(0xFF152A31), FontWeight.Bold) }
        Tx("MIC · ${s.engine}", 16, Color(0x66FFFFFF), modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.u, bottom = 18.u))
    }
}

// ───────────────────────── P03 요청 확인 ─────────────────────────

@Composable
fun PReview(s: AppState) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CenterHeader(s)
            Column(Modifier.weight(1f).fillMaxWidth().padding(start = 56.u, end = 56.u, top = 56.u, bottom = 46.u)) {
                RequestCard(s, s.requestText, false, Modifier.fillMaxWidth().weight(1f))
                Spacer(Modifier.height(34.u))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.height(80.u).clip(RoundedCornerShape(40.u)).background(Color.White).clickable { s.langReturn = P.REVIEW; s.p = P.LANGUAGE }
                            .padding(horizontal = 30.u),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Ico(Ic.GLOBE, PC.onAccent, 34.u)
                        Spacer(Modifier.width(14.u))
                        Tx("LANG : ${s.lang.code}", 24, PC.onAccent, FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(20.u))
                    Box(
                        Modifier.size(80.u).clip(CircleShape).background(PC.btn).border(2.u, PC.border, CircleShape).clickable { s.pListen() },
                        contentAlignment = Alignment.Center,
                    ) { Ico(Ic.RETRY, PC.text, 34.u) }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.height(84.u).widthIn(min = 300.u).clip(RoundedCornerShape(42.u)).background(PC.accent)
                            .clickable { s.pSend() }.padding(horizontal = 44.u),
                        contentAlignment = Alignment.Center,
                    ) { Tx(PT.sendToDriver[s.lang], 28, PC.onAccent, FontWeight.SemiBold) }
                }
            }
        }
    }
}

// ───────────────────────── P03D 언어를 못 알아들음 ─────────────────────────

@Composable
fun PUnclear(s: AppState) = PPage(s, rate = false, title = PT.unclearTitle[s.lang], sub = PT.unclearSub[s.lang]) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.u)).background(Color(0xFF2A2420))
            .border(2.u, Color(0xFF5A4A3A), RoundedCornerShape(24.u)).padding(32.u),
    ) {
        Tx(PT.whatWeHeard[s.lang], 22, PC.orange)
        Spacer(Modifier.height(14.u))
        Tx(s.heard.ifBlank { PT.nothingHeard[s.lang] }, 36, PC.text, FontWeight.SemiBold, lh = 48)
    }
    Spacer(Modifier.height(50.u))
    Row(horizontalArrangement = Arrangement.spacedBy(24.u)) {
        PBtn(PT.tryAgainSpeak[s.lang], { s.pListen() }, primary = true, h = 86)
        PBtn(PT.chooseLanguage[s.lang], { s.langReturn = P.ASK; s.p = P.LANGUAGE }, h = 86)
        PBtn(PT.reviewRequest[s.lang], { s.p = P.TYPE }, h = 86)
    }
}

// ───────────────────────── 입력칸 ─────────────────────────

@Composable
fun InputBox(value: String, onChange: (String) -> Unit, hint: String) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = TextStyle(color = PC.text, fontSize = 32.t),
        cursorBrush = SolidColor(PC.text),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().heightIn(min = 112.u).clip(RoundedCornerShape(18.u)).background(PC.card)
                    .border(2.u, PC.border, RoundedCornerShape(18.u)).padding(horizontal = 26.u, vertical = 26.u),
            ) {
                if (value.isEmpty()) Tx(hint, 32, PC.mute)
                inner()
            }
        },
    )
}

// ───────────────────────── P03B 직접 입력 ─────────────────────────

@Composable
fun PType(s: AppState) = PPage(s, rate = false, title = PT.typeTitle[s.lang], sub = PT.typeSub[s.lang]) {
    var text by remember { mutableStateOf(s.heard) }
    PCard(Modifier.fillMaxWidth(), padding = 30) {
        Tx(PT.destinationCap[s.lang], 20, PC.sub, FontWeight.SemiBold)
        Spacer(Modifier.height(16.u))
        InputBox(text, { text = it }, PT.placeholder[s.lang])
    }
    Spacer(Modifier.height(30.u))
    Row(verticalAlignment = Alignment.CenterVertically) {
        PBtn(PT.reviewRequest[s.lang], { s.pHeard(text) }, primary = true, h = 84)
        Spacer(Modifier.width(40.u))
        Tx(PT.orPick[s.lang], 22, PC.mute)
        Spacer(Modifier.width(18.u))
        Row(horizontalArrangement = Arrangement.spacedBy(12.u)) {
            for (d in Content.dests) Box(
                Modifier.height(64.u).clip(RoundedCornerShape(32.u)).background(PC.btn).border(2.u, PC.border, RoundedCornerShape(32.u))
                    .clickable { s.pPickDest(d, 0) }.padding(horizontal = 24.u),
                contentAlignment = Alignment.Center,
            ) { Tx(d.name[s.lang], 22, PC.text, FontWeight.SemiBold, maxLines = 1) }
        }
    }
}

// ───────────────────────── P27 문장 수정 ─────────────────────────

@Composable
fun PEdit(s: AppState) = PPage(s, rate = false, title = PT.editTitle[s.lang], sub = PT.editSub[s.lang]) {
    var text by remember { mutableStateOf(s.requestText) }
    PCard(Modifier.fillMaxWidth(), padding = 30) {
        Tx(PT.requestIn.fmt(s.lang, if (s.lang == Lang.EN) "ENGLISH" else s.lang.native), 20, PC.sub, FontWeight.SemiBold)
        Spacer(Modifier.height(16.u))
        InputBox(text, { text = it }, PT.placeholder[s.lang])
        Spacer(Modifier.height(14.u))
        Tx(PT.keyboardNote[s.lang], 20, PC.mute)
    }
    Spacer(Modifier.height(30.u))
    PBtn(PT.saveChanges[s.lang], {
        val m = Content.match(text)
        if (m != null) s.pHeard(text) else { s.heard = text; s.p = P.REVIEW }
    }, primary = true, h = 84)
}

// ───────────────────────── P04 기사님 대기 (돌다가 체크) ─────────────────────────

@Composable
fun PWaiting(s: AppState) {
    Box(Modifier.fillMaxSize().glow()) {
        Column(Modifier.fillMaxSize()) {
            GlowHeader(s)
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(130.u))
                Tx(PT.waiting[s.lang], 50, PC.text, FontWeight.Medium)
                Spacer(Modifier.height(44.u))
                Row(
                    Modifier.width(1210.u).height(190.u).clip(RoundedCornerShape(24.u)).background(Color(0xCC1C1F24))
                        .border(2.u, PC.border, RoundedCornerShape(24.u)).padding(horizontal = 36.u),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Tx(PT.requestSent[s.lang], 22, Color(0xFF8FC7FF))
                        Spacer(Modifier.height(10.u))
                        Tx("${s.dest.name[s.lang]}  ·  ${s.entr.name[s.lang]}", 38, PC.text, FontWeight.Medium)
                    }
                    SpinCheck(s.waitDone, 96)
                }
            }
        }
    }
}

@Composable
fun Spinner(size: Int) {
    // 시간이 계속 흐르는 방식으로 회전 → 한 바퀴 돌 때 툭 끊기는 순간이 없다. 꼬리가 흐려지는 혜성 모양.
    val time by rememberSeconds()
    Canvas(Modifier.size(size.u)) {
        val sw = this.size.minDimension * 0.10f
        val inset = sw / 2
        val arc = androidx.compose.ui.geometry.Size(this.size.width - sw, this.size.height - sw)
        drawArc(Color(0x1FFFFFFF), 0f, 360f, false, topLeft = Offset(inset, inset), size = arc, style = Stroke(sw))
        rotate((time * 320f) % 360f) {
            drawArc(
                Brush.sweepGradient(0f to Color(0x00BDE6F6), 0.72f to Color(0xFFBDE6F6), 0.75f to Color.White, 0.76f to Color(0x00FFFFFF), center = center),
                startAngle = 0f, sweepAngle = 270f, useCenter = false,
                topLeft = Offset(inset, inset), size = arc, style = Stroke(sw),
            )
            val r = (this.size.minDimension - sw) / 2
            drawCircle(Color.White, sw / 2, Offset(center.x, center.y - r))
        }
    }
}

// ───────────────────────── 목적지 확정 ─────────────────────────

@Composable
fun PConfirmed(s: AppState) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) { CenterHeader(s) }
        Tx(
            PT.comfort[s.lang], 50, PC.text, FontWeight.Bold, lh = 70, align = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 120.u).enter(1),
        )
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 70.u).enter(delayMs = 700).pressable { s.p = P.HOME }
                .height(80.u).clip(RoundedCornerShape(50.u)).background(Color(0xFFBCE8F5)).padding(horizontal = 34.u),
            contentAlignment = Alignment.Center,
        ) { Tx(PT.exploreOnTheWay[s.lang], 28, Color(0xFF152A31), FontWeight.Bold) }
    }
}

// ───────────────────────── P06 홈 ─────────────────────────

@Composable
fun PHome(s: AppState) {
    val steps = listOf(P.KIT, P.NEAR, P.TOOLS)
    val nextStep = steps.firstOrNull { it !in s.visited }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CenterHeader(s, back = false)
            Spacer(Modifier.height(54.u))
            Tx(PT.exploreOnTheWay[s.lang], 50, PC.text, FontWeight.Bold, lh = 80, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().enter(0))
            Spacer(Modifier.height(14.u))
            Row(
                Modifier.align(Alignment.CenterHorizontally).enter(1).pressable { s.p = P.PLACE },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Ico(Ic.PIN, PC.sub, 34.u)
                Spacer(Modifier.width(10.u))
                Tx("${s.dest.name[s.lang]} · ${s.entr.name[s.lang]}", 28, PC.sub)
            }
            Spacer(Modifier.height(36.u))
            Row(Modifier.fillMaxWidth().padding(horizontal = 56.u), horizontalArrangement = Arrangement.spacedBy(24.u)) {
                GlassCard(Ic.PHONE, PT.cardKit[s.lang], P.KIT in s.visited, nextStep == P.KIT, Modifier.weight(1f).enter(3)) { s.p = P.KIT }
                GlassCard(Ic.PIN, PT.cardNear[s.lang], P.NEAR in s.visited, nextStep == P.NEAR, Modifier.weight(1f).enter(4)) { s.p = P.NEAR }
                GlassCard(Ic.FORK, PT.cardTools[s.lang], P.TOOLS in s.visited, nextStep == P.TOOLS, Modifier.weight(1f).enter(5)) { s.p = P.TOOLS }
            }
            Spacer(Modifier.height(24.u))
            // 실시간: GPS 내 주변 큐레이션 · 라이더 광장
            Row(Modifier.fillMaxWidth().padding(horizontal = 56.u), horizontalArrangement = Arrangement.spacedBy(24.u)) {
                WideBanner(Ic.PIN, LT.aroundYou[s.lang], s.region?.name?.get(s.lang) ?: LT.waitingGps[s.lang], s.carLat != null, Modifier.weight(1f).enter(6)) { s.p = P.LIVE }
                WideBanner(Ic.CHAT, LT.plazaCard[s.lang], LT.plazaSub[s.lang], true, Modifier.weight(1f).enter(7)) { s.p = P.PLAZA }
            }
        }
    }
}

/** Figma P06 유리 카드: 반투명 흰 바탕 + 흰 50% 테두리, 지금 차례면 하늘색 테두리·NEXT, 다녀오면 체크 */
@Composable
fun GlassCard(icon: Ic, title: String, done: Boolean, next: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bd by animateColorAsState(if (next) Color(0xFFBCE8F5) else Color(0x80FFFFFF), tween(400), label = "glassBd")
    Box(
        modifier.pressable(onClick = onClick).height(227.u).clip(RoundedCornerShape(20.u)).background(Color(0x1AD9D9D9))
            .border(2.u, bd, RoundedCornerShape(20.u)).padding(start = 31.u, end = 24.u, top = 29.u),
    ) {
        Column {
            Ico(icon, PC.text, 48.u)
            Spacer(Modifier.height(30.u))
            Tx(title, 36, PC.text, FontWeight.Bold, lh = 44)
        }
        Box(Modifier.align(Alignment.TopEnd)) {
            when {
                done -> Box(Modifier.size(36.u).clip(CircleShape).background(PC.green), contentAlignment = Alignment.Center) {
                    Ico(Ic.CHECK, PC.onAccent, 22.u, stroke = 3f)
                }
                next -> Box(Modifier.clip(RoundedCornerShape(20.u)).background(Color(0xFFBCE8F5)).padding(horizontal = 12.u, vertical = 4.u)) {
                    Tx("NEXT", 16, Color(0xFF152A31), FontWeight.Bold)
                }
            }
        }
    }
}

// ───────────────────────── P07 디지털 키트 ─────────────────────────

@Composable
fun AppTile(app: KitApp, size: Int) {
    Box(Modifier.size(size.u).clip(RoundedCornerShape((size / 5).u)).background(Color(app.tile)), contentAlignment = Alignment.Center) {
        if (app.glyph == "●") Ico(Ic.PIN, Color(app.glyphColor), (size * 6 / 10).u, stroke = 3f)
        else Tx(app.glyph, size * 6 / 10, Color(app.glyphColor), FontWeight.Black)
    }
}

@Composable
fun QrTile(url: String, label: String, size: Int = 174) {
    val img = remember(url) { qrImage(url, 360) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size.u).clip(RoundedCornerShape(16.u)).background(Color.White).padding((size / 14).u)) {
            Image(img, contentDescription = label, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(20.u))
        Tx(label, 24, PC.text, FontWeight.SemiBold)
    }
}

@Composable
fun PKit(s: AppState) = PPage(s, title = PT.digitalKit[s.lang], sub = PT.kitSub[s.lang]) {
    val apps = Content.kitApps
    val pager = rememberPagerState(initialPage = s.kitPage.coerceIn(0, apps.lastIndex)) { apps.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.currentPage) { s.kitPage = pager.currentPage }
    val page = pager.currentPage
    val last = page == apps.lastIndex

    // ① 키트 진행표: 앱 4개 칩 (누르면 그 카드로, 받은 앱은 체크)
    Row(horizontalArrangement = Arrangement.spacedBy(14.u), verticalAlignment = Alignment.CenterVertically) {
        for ((i, app) in apps.withIndex()) {
            val on = i == page
            val done = i in s.kitAdded
            val bg by animateColorAsState(if (on) Color(0xFF2C3A42) else PC.card, tween(300), label = "chipBg")
            val bd by animateColorAsState(if (on) PC.cyan else PC.border, tween(300), label = "chipBd")
            Row(
                Modifier.pressable { scope.launch { pager.animateScrollToPage(i) } }.height(64.u).clip(RoundedCornerShape(32.u))
                    .background(bg).border(2.u, bd, RoundedCornerShape(32.u)).padding(start = 10.u, end = 22.u),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTile(app, 44)
                Spacer(Modifier.width(12.u))
                Tx(app.name, 22, if (on) PC.text else PC.sub, FontWeight.SemiBold, maxLines = 1)
                if (done) {
                    Spacer(Modifier.width(10.u))
                    Box(Modifier.size(28.u).clip(CircleShape).background(PC.green), contentAlignment = Alignment.Center) {
                        Ico(Ic.CHECK, PC.onAccent, 18.u, stroke = 3f)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Tx("${s.kitAdded.size} / ${apps.size}  ${PT.kitAdded[s.lang]}", 22, PC.sub)
    }
    Spacer(Modifier.height(26.u))

    // ② 손가락·마우스로 드래그해서 넘기는 카드 (가운데 카드는 크고 또렷, 옆 카드는 작고 흐리게)
    HorizontalPager(
        state = pager,
        // 세게 밀면 여러 장이 쭈루룩 넘어가고, 끝에서 살짝 튕기며 멈춘다
        flingBehavior = PagerDefaults.flingBehavior(
            state = pager,
            pagerSnapDistance = PagerSnapDistance.atMost(apps.size),
            snapAnimationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        ),
        contentPadding = PaddingValues(end = 230.u),
        pageSpacing = 26.u,
        modifier = Modifier.fillMaxWidth().height(380.u),
    ) { i ->
        val app = apps[i]
        val off = ((pager.currentPage - i) + pager.currentPageOffsetFraction).let { if (it < 0) -it else it }.coerceIn(0f, 1f)
        val done = i in s.kitAdded
        Row(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    val sc = 1f - 0.08f * off
                    scaleX = sc; scaleY = sc
                    alpha = 1f - 0.5f * off
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                }
                .clip(RoundedCornerShape(30.u))
                .background(Brush.linearGradient(listOf(Color(0xFF262A30), Color(0xFF1C1E22))))
                .border(2.u, if (off < 0.5f) Color(0xFF4A5560) else PC.border, RoundedCornerShape(30.u))
                .padding(36.u),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                AppTile(app, 104)
                Spacer(Modifier.weight(1f))
                Tx(app.category[s.lang], 22, PC.sub)
                Spacer(Modifier.height(6.u))
                Tx(app.name, 48, PC.text, FontWeight.SemiBold, maxLines = 1)
                Spacer(Modifier.height(14.u))
                Tx(app.desc[s.lang], 25, PC.sub, lh = 34, maxLines = 2)
            }
            Spacer(Modifier.width(36.u))
            Box(Modifier.width(2.u).fillMaxHeight().padding(vertical = 20.u).background(PC.border))
            Spacer(Modifier.width(40.u))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(30.u)) {
                    QrTile(app.iosUrl, "iPhone", 160)
                    QrTile(app.androidUrl, "Android", 160)
                }
                Spacer(Modifier.height(22.u))
                Row(
                    Modifier.pressable {
                        if (i !in s.kitAdded) s.kitAdded.add(i)
                        val todo = ((i + 1)..apps.lastIndex).firstOrNull { it !in s.kitAdded }
                            ?: (0 until i).firstOrNull { it !in s.kitAdded }
                        if (todo != null) scope.launch { pager.animateScrollToPage(todo) }
                        else s.toast = PT.kitAllDone[s.lang]
                    }.height(60.u).clip(RoundedCornerShape(30.u))
                        .background(if (done) Color(0xFF203028) else PC.btn)
                        .border(2.u, if (done) PC.green else PC.border, RoundedCornerShape(30.u)).padding(horizontal = 26.u),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (done) { Ico(Ic.CHECK, PC.green, 24.u, stroke = 2.6f); Spacer(Modifier.width(10.u)) }
                    Tx((if (done) PT.kitAdded else PT.kitGotIt)[s.lang], 22, if (done) PC.green else PC.text, FontWeight.SemiBold)
                }
            }
        }
    }
    Spacer(Modifier.weight(1f))

    // ③ 아래: 진행 점 + 이전 / 다음 (마지막엔 다음 단계 '목적지 주변'으로)
    Row(Modifier.fillMaxWidth().padding(start = 300.u, end = IconGap), verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.u), verticalAlignment = Alignment.CenterVertically) {
            for (i in apps.indices) {
                val w by animateIntAsState(if (i == page) 44 else 12, tween(380, easing = EaseOutQuint), label = "dot")
                Box(Modifier.width(w.u).height(12.u).clip(RoundedCornerShape(6.u)).background(if (i == page) PC.text else Color(0x55FFFFFF)))
            }
        }
        Spacer(Modifier.weight(1f))
        PagerBtn(PT.previous[s.lang], Ic.CHEV_L, true, page > 0) { scope.launch { pager.animateScrollToPage(page - 1) } }
        Spacer(Modifier.width(14.u))
        if (last || s.kitAdded.size == apps.size) {
            Row(
                Modifier.pressable { s.p = P.NEAR }.height(80.u).clip(RoundedCornerShape(16.u)).background(PC.accent).padding(horizontal = 30.u),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Tx(PT.nextNear[s.lang], 24, PC.onAccent, FontWeight.SemiBold)
                Spacer(Modifier.width(12.u))
                Ico(Ic.ARROW, PC.onAccent, 28.u)
            }
        } else {
            PagerBtn(PT.next[s.lang], Ic.CHEV, false, true) { scope.launch { pager.animateScrollToPage(page + 1) } }
        }
    }
}

// ───────────────────────── P08 목적지 주변 (홀로그램 지도 + 관성 스크롤 목록) ─────────────────────────


@Composable
fun HoloMap(s: AppState, focus: Int, modifier: Modifier) {
    val timeS = rememberSeconds()
    val d = s.dest
    val cyan = Color(0xFF7FE3FF)
    val density = LocalDensity.current
    val labelPx = with(density) { 20.u.toPx() }
    val paint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD; setShadowLayer(7f, 0f, 0f, android.graphics.Color.BLACK)
        }
    }
    val spots = d.nearby.filter { it.lat != 0.0 }
    val fit = listOf(d.lat to d.lng) + spots.map { it.lat to it.lng }
    val figMap = figmaImage("fig_map")
    Box(
        modifier.clip(RoundedCornerShape(28.u))
            .background(Brush.verticalGradient(listOf(Color(0xFF061522), Color(0xFF0B2536))))
            .border(2.u, Color(0x5576D8F5), RoundedCornerShape(28.u)),
    ) {
        // 진짜 지도(OpenStreetMap)를 홀로그램 색으로 + 바닥에 비스듬히 눕힘
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                // Figma 지도는 FigmaHoloMap 이 직접 원근을 그림 (글자는 똑바로 서 있게)
                if (figMap == null) {
                    rotationX = 28f
                    cameraDistance = 9f * density.density
                    scaleX = 1.14f; scaleY = 1.14f
                }
            },
        ) {
            // Figma P08 의 지도 이미지(fig_map)가 있으면 그걸 홀로그램으로, 없으면 앱 내장 지도
            if (figMap != null) FigmaHoloMap(s, figMap, focus, timeS, labelPx, paint)
            else RealMap(d.lat, d.lng, 16.0, Modifier.fillMaxSize(), fit = fit, minZoom = 14.0, maxZoom = 16.4, lang = s.lang) { proj ->
                val t = timeS.value
                val w = size.width
                val h = size.height
                val flick = 0.93f + 0.07f * sin(t * 11f) * sin(t * 3.7f)
                fun c(a: Float) = cyan.copy(alpha = (a * flick).coerceIn(0f, 1f))
                // 격자
                val n = 16
                for (i in 0..n) {
                    drawLine(c(0.06f), Offset(w * i / n, 0f), Offset(w * i / n, h), strokeWidth = 1f)
                    drawLine(c(0.06f), Offset(0f, h * i / n), Offset(w, h * i / n), strokeWidth = 1f)
                }
                // 목적지 구역 (숨 쉬듯) + 레이더 고리
                val dc = proj.at(d.lat, d.lng)
                val m = proj.pxPerMeter(d.lat)
                val pulse = 0.5f + 0.5f * sin(t * 2.2f)
                drawCircle(c(0.14f + 0.12f * pulse), 90f * m, dc)
                drawCircle(c(0.9f), 90f * m, dc, style = Stroke(2.6f))
                for (i in 0..2) {
                    val ph = ((t * 0.45f + i / 3f) % 1f)
                    drawCircle(c((1f - ph) * 0.55f), 260f * m * ph, dc, style = Stroke(2f))
                }
                // 하차 지점들
                for (e in d.entrances) if (e.lat != 0.0) {
                    val p = proj.at(e.lat, e.lng)
                    drawCircle(Color.White.copy(alpha = 0.9f), 6f, p)
                }
                // 주변 장소 (번호)
                for ((i, sp) in spots.withIndex()) {
                    val p = proj.at(sp.lat, sp.lng)
                    val on = i == focus
                    val bl = 0.5f + 0.5f * sin(t * 3f + i * 1.7f)
                    drawLine(c(0.35f), dc, p, strokeWidth = 1.6f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), -t * 30f))
                    drawCircle(c(if (on) 0.4f else 0.18f + 0.12f * bl), if (on) 30f else 22f, p)
                    drawCircle(c(1f), if (on) 10f else 7f, p)
                    paint.color = android.graphics.Color.argb((235 * flick).toInt().coerceIn(0, 255), 210, 248, 255)
                    paint.textSize = labelPx
                    drawContext.canvas.nativeCanvas.drawText("${i + 1}  ${sp.name[s.lang]}", p.x, p.y - 34f, paint)
                }
                // 내 택시 (GPS 가 있으면)
                val la = s.carLat
                val ln = s.carLng
                if (la != null && ln != null) {
                    val p = proj.at(la, ln)
                    if (p.x in -50f..w + 50f && p.y in -50f..h + 50f) {
                        drawCircle(Color(0x55FFFFFF), 22f, p)
                        drawCircle(Color.White, 9f, p)
                    }
                }
                // 핀
                drawCircle(Color.White, 10f, dc)
                paint.color = android.graphics.Color.WHITE
                paint.textSize = labelPx * 1.1f
                drawContext.canvas.nativeCanvas.drawText(d.name[s.lang], dc.x, dc.y - 40f, paint)
                // 스캔 빛: 위 → 아래
                val sy = ((t * 0.16f) % 1.2f - 0.1f) * h
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, c(0.22f), c(0.05f), Color.Transparent), startY = sy - h * 0.06f, endY = sy + h * 0.04f),
                    Offset(0f, sy - h * 0.06f), androidx.compose.ui.geometry.Size(w, h * 0.10f), blendMode = BlendMode.Screen,
                )
            }
        }
        // 가는 가로 주사선 (홀로그램 질감, 내장 지도일 때만)
        if (figMap == null) Canvas(Modifier.fillMaxSize()) {
            var y = 0f
            while (y < size.height) { drawLine(Color(0x0D9FE8FF), Offset(0f, y), Offset(size.width, y), 1f); y += 6f }
        }
        Row(Modifier.align(Alignment.TopStart).padding(22.u), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(RoundedCornerShape(10.u)).background(Color(0x337FE3FF)).padding(horizontal = 12.u, vertical = 4.u)) {
                Tx("HOLO MAP", 16, cyan, FontWeight.Bold)
            }
            Spacer(Modifier.width(12.u))
            Tx("${d.name[s.lang]} · ${s.entr.name[s.lang]}", 20, Color(0xFFCDEFFC), FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/** Figma P08 지도 이미지 위에서 핀 · 목적지 고리 · 주변 장소 점이 놓인 자리 (Figma 1600×1006 좌표) */
private const val FIG_PIN_X = 800f
private const val FIG_PIN_Y = 490f
private val figDots = listOf(1042f to 406f, 536f to 517f, 906f to 707f)

@Suppress("unused")
private val figHoloFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(
    androidx.compose.ui.graphics.ColorMatrix(
        floatArrayOf(
            0.299f * 0.6f, 0.587f * 0.6f, 0.114f * 0.6f, 0f, 0f,
            0.299f * 1.25f, 0.587f * 1.25f, 0.114f * 1.25f, 0f, 0f,
            0.299f * 1.45f, 0.587f * 1.45f, 0.114f * 1.45f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

/**
 * Figma P08 지도로 그리는 홀로그램 테이블
 *  - 지도를 바닥에 눕힌 사다리꼴로 원근 변환(멀수록 좁고 흐림)해서 진짜 홀로그램 테이블처럼
 *  - 글자·핀은 원근을 따라 눕지 않고 똑바로 서서 또렷하게
 *  - 목적지: 바닥에 퍼지는 고리 + 위로 솟는 빛기둥 + 떠 있는 핀 / 주변 장소: 작은 빛기둥 + 이름표
 */
@Composable
private fun FigmaHoloMap(
    s: AppState, img: androidx.compose.ui.graphics.ImageBitmap, focus: Int,
    timeS: androidx.compose.runtime.State<Float>, labelPx: Float, paint: android.graphics.Paint,
) {
    val d = s.dest
    val names = d.nearby.map { it.name[s.lang] } + listOf(d.storyTitle[s.lang])
    val bmp = remember(img) { img.asAndroidBitmap() }
    val imgPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(
                floatArrayOf(
                    0.30f * 0.55f, 0.59f * 0.55f, 0.11f * 0.55f, 0f, 0f,
                    0.30f * 1.30f, 0.59f * 1.30f, 0.11f * 1.30f, 0f, 4f,
                    0.30f * 1.55f, 0.59f * 1.55f, 0.11f * 1.55f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
        }
    }
    val chipPaint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) }
    val txt = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val t = timeS.value
        val w = size.width
        val h = size.height
        val cyan = Color(0xFF7FE3FF)
        // ① 바닥판: 지도에서 핀 주변 1200×760 을 잘라 사다리꼴로 (아래쪽이 가깝고 넓음)
        val cw = 1200f; val ch = 760f
        val sx0 = FIG_PIN_X - cw / 2; val sy0 = FIG_PIN_Y - ch * 0.55f
        val src = floatArrayOf(sx0, sy0, sx0 + cw, sy0, sx0 + cw, sy0 + ch, sx0, sy0 + ch)
        val farW = w * 0.62f; val nearW = w * 1.25f
        val top = h * 0.16f; val bottom = h * 1.02f
        val dst = floatArrayOf(w / 2 - farW / 2, top, w / 2 + farW / 2, top, w / 2 + nearW / 2, bottom, w / 2 - nearW / 2, bottom)
        val m = android.graphics.Matrix().apply { setPolyToPoly(src, 0, dst, 0, 4) }
        fun at(ix: Float, iy: Float): Offset {
            val p = floatArrayOf(ix, iy); m.mapPoints(p); return Offset(p[0], p[1])
        }
        val nc = drawContext.canvas.nativeCanvas
        nc.save()
        val clip = android.graphics.Path().apply { moveTo(dst[0], dst[1]); lineTo(dst[2], dst[3]); lineTo(dst[4], dst[5]); lineTo(dst[6], dst[7]); close() }
        nc.clipPath(clip)
        nc.drawBitmap(bmp, m, imgPaint)
        nc.restore()
        // 바닥 격자 (원근)
        for (k in 0..10) {
            val gx = sx0 + cw * k / 10f
            drawLine(cyan.copy(alpha = 0.07f), at(gx, sy0), at(gx, sy0 + ch), 1f)
            val gy = sy0 + ch * k / 10f
            drawLine(cyan.copy(alpha = 0.07f), at(sx0, gy), at(sx0 + cw, gy), 1f)
        }
        // 먼 쪽은 안개처럼 흐리게, 가장자리는 어둡게
        drawRect(Brush.verticalGradient(0f to Color(0xFF061522), 0.30f to Color(0xEE061522), 0.55f to Color(0x00061522)))
        drawRect(Brush.horizontalGradient(0f to Color(0xCC061522), 0.16f to Color(0x00061522), 0.84f to Color(0x00061522), 1f to Color(0xCC061522)))

        // 바닥에 그리는 원 (원근 따라 납작한 타원)
        fun groundCircle(cx: Float, cy: Float, r: Float, col: Color, stroke: Float) {
            val path = Path()
            for (k in 0..48) {
                val a = k / 48f * 2f * Math.PI.toFloat()
                val p = at(cx + kotlin.math.cos(a) * r, cy + kotlin.math.sin(a) * r)
                if (k == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            if (stroke > 0f) drawPath(path, col, style = Stroke(stroke)) else drawPath(path, col)
        }
        // 빛기둥
        fun beam(p: Offset, height: Float, width: Float, a: Float) {
            drawRect(
                Brush.verticalGradient(listOf(cyan.copy(alpha = 0f), cyan.copy(alpha = a)), startY = p.y - height, endY = p.y),
                Offset(p.x - width / 2, p.y - height), androidx.compose.ui.geometry.Size(width, height), blendMode = BlendMode.Screen,
            )
        }
        // 이름표 (똑바로 선 유리 칩)
        fun chip(p: Offset, text: String, size: Float, strong: Boolean) {
            txt.textSize = size
            val tw = txt.measureText(text)
            val ph = size * 0.55f; val pv = size * 0.38f
            val rect = android.graphics.RectF(p.x - tw / 2 - ph, p.y - size - pv * 2, p.x + tw / 2 + ph, p.y)
            chipPaint.color = if (strong) android.graphics.Color.argb(235, 245, 251, 255) else android.graphics.Color.argb(170, 10, 30, 44)
            nc.drawRoundRect(rect, rect.height() / 2, rect.height() / 2, chipPaint)
            if (!strong) {
                chipPaint.style = android.graphics.Paint.Style.STROKE; chipPaint.strokeWidth = 1.5f
                chipPaint.color = android.graphics.Color.argb(150, 127, 227, 255)
                nc.drawRoundRect(rect, rect.height() / 2, rect.height() / 2, chipPaint)
                chipPaint.style = android.graphics.Paint.Style.FILL
            }
            txt.color = if (strong) android.graphics.Color.rgb(17, 26, 31) else android.graphics.Color.WHITE
            nc.drawText(text, p.x, p.y - pv - size * 0.18f, txt)
        }

        // ② 목적지: 바닥 고리 + 퍼지는 파동
        val pc = at(FIG_PIN_X, FIG_PIN_Y)
        val pulse = 0.5f + 0.5f * sin(t * 2.2f)
        groundCircle(FIG_PIN_X, FIG_PIN_Y, 120f, cyan.copy(alpha = 0.10f + 0.08f * pulse), 0f)
        groundCircle(FIG_PIN_X, FIG_PIN_Y, 120f, cyan.copy(alpha = 0.7f), 2f)
        for (k in 0..1) {
            val ph = ((t * 0.35f + k / 2f) % 1f)
            groundCircle(FIG_PIN_X, FIG_PIN_Y, 120f + 360f * ph, cyan.copy(alpha = (1f - ph) * 0.45f), 2f)
        }
        // ③ 주변 장소: 바닥 점선 + 작은 빛기둥 + 이름표
        for ((i, xy) in figDots.withIndex()) {
            if (i >= names.size) break
            val p = at(xy.first, xy.second)
            val on = i == focus
            drawLine(cyan.copy(alpha = 0.45f), pc, p, strokeWidth = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), -t * 30f))
            groundCircle(xy.first, xy.second, 26f, Color(0xFF9CC9FF).copy(alpha = if (on) 0.6f else 0.35f), 0f)
            val bh = h * (if (on) 0.20f else 0.13f) * (0.9f + 0.1f * sin(t * 2f + i))
            beam(p, bh, if (on) 10f else 6f, if (on) 0.75f else 0.5f)
            drawCircle(Color.White, if (on) 7f else 5f, p)
            chip(Offset(p.x, p.y - bh - 6f), "${i + 1}  ${names[i]}", labelPx * 0.85f, on)
        }
        // ④ 목적지 빛기둥 + 떠 있는 핀
        val bob = 6f * sin(t * 1.6f)
        val beamH = h * 0.30f
        beam(pc, beamH, 18f, 0.85f)
        drawCircle(Color.White, 8f, pc)
        val tip = Offset(pc.x, pc.y - beamH - 8f + bob)
        val pr = 22f
        val pin = Path().apply {
            moveTo(tip.x, tip.y)
            cubicTo(tip.x - pr * 0.4f, tip.y - pr * 0.9f, tip.x - pr, tip.y - pr * 1.3f, tip.x - pr, tip.y - pr * 1.9f)
            cubicTo(tip.x - pr, tip.y - pr * 2.5f, tip.x - pr * 0.55f, tip.y - pr * 2.95f, tip.x, tip.y - pr * 2.95f)
            cubicTo(tip.x + pr * 0.55f, tip.y - pr * 2.95f, tip.x + pr, tip.y - pr * 2.5f, tip.x + pr, tip.y - pr * 1.9f)
            cubicTo(tip.x + pr, tip.y - pr * 1.3f, tip.x + pr * 0.4f, tip.y - pr * 0.9f, tip.x, tip.y)
            close()
        }
        drawCircle(Brush.radialGradient(listOf(cyan.copy(alpha = 0.55f), Color.Transparent), Offset(tip.x, tip.y - pr * 1.9f), pr * 3f), pr * 3f, Offset(tip.x, tip.y - pr * 1.9f), blendMode = BlendMode.Screen)
        drawPath(pin, Color.White)
        drawCircle(Color(0xFF0B2536), pr * 0.38f, Offset(tip.x, tip.y - pr * 1.9f))
        chip(Offset(tip.x, tip.y - pr * 3.3f), d.name[s.lang], labelPx * 1.05f, true)
    }
}

// ───────────────────────── 정밀 하차: GPS 실시간 지도 ─────────────────────────

fun distText(m: Float): String = if (m < 1000f) "${(m / 10f).roundToInt() * 10} m" else String.format(java.util.Locale.US, "%.1f km", m / 1000f)

/**
 * 하차 지점 실시간 지도 (P12A · P24 · P24A · P15)
 *  - 진짜 지도 위에 하차 지점(입구)들을 정확한 좌표로 표시, 고른 곳은 빛나며 숨 쉼
 *  - 내 택시 위치(GPS)와 진행 방향 화살표, 택시 → 하차 지점 점선
 *  - 남은 거리·예상 시간이 GPS 가 들어올 때마다 바뀜. 지도도 택시와 하차 지점이 다 보이게 자동 줌
 */
@Composable
fun LiveDropMap(s: AppState, highlight: Int, modifier: Modifier) {
    val timeS = rememberSeconds()
    val d = s.dest
    val density = LocalDensity.current
    val labelPx = with(density) { 19.u.toPx() }
    val paint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD; setShadowLayer(7f, 0f, 0f, android.graphics.Color.BLACK)
        }
    }
    val ents = d.entrances.map { e -> (if (e.lat != 0.0) e.lat else d.lat) to (if (e.lng != 0.0) e.lng else d.lng) }
    val hi = highlight.coerceIn(0, ents.lastIndex)
    val target = ents[hi]
    val la = s.carLat
    val ln = s.carLng
    val dist = if (la != null && ln != null) distanceM(la, ln, target.first, target.second) else null
    val fit = ents + (if (la != null && ln != null && (dist ?: 9e9f) < 2500f) listOf(la to ln) else emptyList())
    val names = d.entrances.map { it.name[s.lang] }
    Box(modifier.clip(RoundedCornerShape(26.u)).border(2.u, Color(0x5576D8F5), RoundedCornerShape(26.u))) {
        RealMap(d.lat, d.lng, 17.0, Modifier.fillMaxSize(), fit = fit, minZoom = 13.5, maxZoom = 18.0, lang = s.lang) { proj ->
            val t = timeS.value
            val cyan = Color(0xFF7FE3FF)
            val tp = proj.at(target.first, target.second)
            // 택시 → 하차 지점 점선
            val cla = s.carLat
            val cln = s.carLng
            if (cla != null && cln != null) {
                val cp = proj.at(cla, cln)
                drawLine(cyan.copy(alpha = 0.85f), cp, tp, strokeWidth = 4f, cap = StrokeCap.Round,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 12f), -t * 40f))
            }
            // 하차 지점들
            for ((i, e) in ents.withIndex()) {
                val p = proj.at(e.first, e.second)
                val on = i == hi
                if (on) {
                    for (k in 0..1) {
                        val ph = ((t * 0.6f + k / 2f) % 1f)
                        drawCircle(cyan.copy(alpha = (1f - ph) * 0.6f), 16f + 50f * ph, p, style = Stroke(3f))
                    }
                    drawCircle(cyan, 16f, p)
                    drawCircle(Color.White, 7f, p)
                } else {
                    drawCircle(Color.White.copy(alpha = 0.85f), 10f, p)
                    drawCircle(Color(0xFF06131D), 5f, p)
                }
                paint.color = if (on) android.graphics.Color.WHITE else android.graphics.Color.argb(200, 220, 240, 250)
                paint.textSize = if (on) labelPx * 1.1f else labelPx
                drawContext.canvas.nativeCanvas.drawText(names[i], p.x, p.y - 30f, paint)
            }
            // 내 택시: 진행 방향 화살표
            if (cla != null && cln != null) {
                val cp = proj.at(cla, cln)
                drawCircle(Color(0x44FFFFFF), 30f, cp)
                rotate(s.carBearing, cp) {
                    val arrow = Path().apply {
                        moveTo(cp.x, cp.y - 20f); lineTo(cp.x + 14f, cp.y + 14f); lineTo(cp.x, cp.y + 6f); lineTo(cp.x - 14f, cp.y + 14f); close()
                    }
                    drawPath(arrow, Color.White)
                }
            }
        }
        // 위: GPS 상태
        val src = s.posSource
        val live = la != null
        Row(
            Modifier.align(Alignment.TopStart).padding(16.u).clip(RoundedCornerShape(20.u)).background(Color(0xCC0B1A24)).padding(horizontal = 14.u, vertical = 6.u),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val blink by rememberInfiniteTransition(label = "gpsBlink").animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "gpsA")
            Box(Modifier.size(12.u).clip(CircleShape).background((if (live) PC.green else PC.orange).copy(alpha = blink)))
            Spacer(Modifier.width(10.u))
            Tx(if (!live) LT.waitingGps[s.lang] else if (src == "GPS") LT.liveGps[s.lang] else LT.simGps[s.lang], 18, PC.text, FontWeight.Bold)
        }
        // 오른쪽 위: 남은 거리
        if (dist != null) Column(
            Modifier.align(Alignment.TopEnd).padding(16.u).clip(RoundedCornerShape(18.u)).background(Color(0xE6F5FBFF)).padding(horizontal = 20.u, vertical = 12.u),
            horizontalAlignment = Alignment.End,
        ) {
            Tx(LT.toDrop[s.lang], 16, Color(0xFF4F6B78), FontWeight.SemiBold)
            if (dist < 25f) Tx(LT.arrivedAt[s.lang], 28, Color(0xFF0E7FA8), FontWeight.Bold)
            else {
                Tx(distText(dist), 34, Color(0xFF111A1F), FontWeight.Bold)
                Tx(LT.byCar.fmt(s.lang, "${maxOf(1, kotlin.math.ceil(dist / 300f).toInt())}"), 16, Color(0xFF4F6B78))
            }
        }
    }
}


private data class NearItem(val num: Int, val label: String, val title: String, val sub: String, val photo: Int, val key: String, val go: () -> Unit)

@Composable
fun PNear(s: AppState) = PPage(s, title = PT.nearDest.fmt(s.lang, s.entr.name[s.lang]), sub = PT.nearSpotsSub[s.lang]) {
    val d = s.dest
    var focus by remember { mutableStateOf(-1) }
    val eLa = if (s.entr.lat != 0.0) s.entr.lat else d.lat
    val eLn = if (s.entr.lng != 0.0) s.entr.lng else d.lng
    fun far(la: Double, ln: Double) = LT.fromDrop.fmt(s.lang, distText(distanceM(eLa, eLn, la, ln)))
    fun lbl(key: String, base: String) = if (Content.category[key]?.let { c -> c in s.interests } == true) "${LT.forYou[s.lang]} · $base" else base
    val items = buildList {
        add(NearItem(0, PT.yourDropOff[s.lang], d.name[s.lang], d.cardSub[s.lang], 0, d.img) { s.p = P.PLACE })
        for ((i, sp) in d.nearby.withIndex()) add(NearItem(i + 1, lbl(sp.img, PT.near[s.lang]), sp.name[s.lang], if (sp.lat != 0.0) "${far(sp.lat, sp.lng)} · ${sp.sub[s.lang]}" else sp.sub[s.lang], i + 1, sp.img) { s.spotIdx = i; s.p = P.SPOT })
        add(NearItem(d.nearby.size + 1, PT.localStory[s.lang], d.storyTitle[s.lang], d.story[s.lang], 3, d.img) {
            s.sheet = Sheet.Story(PT.localStory[s.lang], d.storyTitle[s.lang], d.story[s.lang]); s.speak(d.story[s.lang])
        })
        for ((i, poi) in Content.allPois.sortedBy { distanceM(d.lat, d.lng, it.lat, it.lng) }.take(5).withIndex()) add(NearItem(-1, lbl(poi.id, PT.localStory[s.lang]), poi.name[s.lang], "${far(poi.lat, poi.lng)} · ${poi.story[s.lang]}", i + 4, poi.id) {
            s.sheet = Sheet.Story(PT.localStory[s.lang], poi.name[s.lang], poi.story[s.lang]); s.speak(poi.story[s.lang])
        })
    }
    Row(Modifier.fillMaxWidth().height(420.u), horizontalArrangement = Arrangement.spacedBy(26.u)) {
        HoloMap(s, focus, Modifier.weight(1.12f).fillMaxHeight())
        // 손가락으로 쭉 밀면 관성으로 미끄러지는 목록 (위아래 가장자리는 부드럽게 흐려짐)
        val list = rememberLazyListState()
        LazyColumn(
            state = list,
            verticalArrangement = Arrangement.spacedBy(16.u),
            contentPadding = PaddingValues(vertical = 14.u),
            modifier = Modifier.weight(1f).fillMaxHeight()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0f to Color.Transparent, 0.06f to Color.Black, 0.92f to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            itemsIndexed(items) { idx, it ->
                Row(
                    Modifier.fillMaxWidth().height(148.u).enter(idx.coerceAtMost(6))
                        .pressable { focus = if (it.num >= 1) it.num - 1 else -1; it.go() }
                        .clip(RoundedCornerShape(22.u)).background(Color(0xF5FFFFFF)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        PlacePhoto(it.key, it.photo, Modifier.width(170.u).fillMaxHeight(), credit = false)
                        if (it.num >= 0) Box(
                            Modifier.padding(10.u).size(36.u).clip(CircleShape).background(if (it.num == 0) Color(0xFF152A31) else Color(0xFF0E7FA8)),
                            contentAlignment = Alignment.Center,
                        ) { Tx(if (it.num == 0) "★" else "${it.num}", 18, Color.White, FontWeight.Bold) }
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 24.u)) {
                        Tx(it.label, 18, Color(0xFF4F6B78), FontWeight.SemiBold, maxLines = 1)
                        Spacer(Modifier.height(4.u))
                        Tx(it.title, 28, Color(0xFF111A1F), FontWeight.Bold, maxLines = 1)
                        Spacer(Modifier.height(4.u))
                        Tx(it.sub, 20, Color(0xFF55636B), maxLines = 2, lh = 27)
                    }
                    Ico(Ic.ARROW, Color(0xFF152A31), 30.u)
                    Spacer(Modifier.width(22.u))
                }
            }
        }
    }
    Spacer(Modifier.weight(1f))
    Row(Modifier.fillMaxWidth().padding(end = IconGap), horizontalArrangement = Arrangement.End) {
        NextStepBtn(PT.nextTools[s.lang]) { s.p = P.TOOLS }
    }
}

// ───────────────────────── P09 장소 상세 ─────────────────────────

@Composable
fun PPlace(s: AppState) = PPage(s) {
    val d = s.dest
    Row(Modifier.fillMaxWidth().weight(1f).padding(bottom = 110.u), horizontalArrangement = Arrangement.spacedBy(40.u)) {
        PlacePhoto(d.img, 0, Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.u)))
        Column(Modifier.weight(0.82f).fillMaxHeight()) {
            Tx(d.name[s.lang], 56, PC.text, FontWeight.SemiBold, lh = 68, modifier = Modifier.enter(0, dx = 40f))
            Spacer(Modifier.height(16.u))
            Tx(d.blurb[s.lang], 27, PC.sub, lh = 38, modifier = Modifier.enter(1, dx = 40f))
            Spacer(Modifier.height(30.u))
            PCard(Modifier.fillMaxWidth().enter(2, dx = 40f), padding = 28) {
                Tx(PT.yourDropOff[s.lang], 22, PC.sub)
                Spacer(Modifier.height(14.u))
                Tx(s.entr.name[s.lang], 32, PC.text, FontWeight.SemiBold)
                Spacer(Modifier.height(16.u))
                Tx(d.address[s.lang], 21, PC.sub)
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(20.u), modifier = Modifier.enter(3)) {
                PBtn(PT.sendToPhone[s.lang], { s.sendIdx = -1; s.sendReturn = P.PLACE; s.p = P.SEND }, primary = true, h = 80)
                PBtn(PT.visitingInfo[s.lang], { s.p = P.PLACE_INFO }, h = 80)
            }
        }
    }
}

// ───────────────────────── P09A 방문 정보 ─────────────────────────

@Composable
fun PPlaceInfo(s: AppState) = PPage(s, title = PT.visiting.fmt(s.lang, s.dest.name[s.lang]), sub = PT.visitingSub[s.lang]) {
    val d = s.dest
    Row(Modifier.fillMaxWidth().height(330.u), horizontalArrangement = Arrangement.spacedBy(26.u)) {
        InfoCard(Ic.PIN, PT.address[s.lang], d.address[s.lang], "${PT.yourDropOff[s.lang]} · ${s.entr.name[s.lang]}", Modifier.weight(1f).enter(3))
        InfoCard(Ic.SWAP, PT.hoursPay[s.lang], PT.checkStall[s.lang], PT.hoursDiffer[s.lang], Modifier.weight(1f).enter(4))
        InfoCard(Ic.WARN, PT.goodToKnow[s.lang], PT.goodToKnowTitle[s.lang], PT.goodToKnowSub[s.lang], Modifier.weight(1f).enter(5))
    }
    Spacer(Modifier.weight(1f))
    Row(Modifier.fillMaxWidth().padding(end = IconGap), horizontalArrangement = Arrangement.End) {
        NextStepBtn(PT.sendToPhoneTitle[s.lang]) { s.sendIdx = -1; s.sendReturn = P.PLACE_INFO; s.p = P.SEND }
    }
}

@Composable
fun InfoCard(icon: Ic, label: String, title: String, sub: String, modifier: Modifier) {
    PCard(modifier.fillMaxHeight(), padding = 32) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PTile(icon, 64)
            Spacer(Modifier.width(18.u))
            Tx(label, 22, PC.cyan, FontWeight.SemiBold)
        }
        Spacer(Modifier.weight(1f))
        Tx(title, 32, PC.text, FontWeight.SemiBold, lh = 42, maxLines = 2)
        Spacer(Modifier.height(14.u))
        Tx(sub, 22, PC.sub, lh = 31, maxLines = 3)
    }
}

// ───────────────────────── P14 · P22A 휴대폰으로 보내기 (QR) ─────────────────────────

fun guideUrl(name: T): String =
    "https://www.google.com/search?q=" + java.net.URLEncoder.encode("VISITKOREA ${name.en}", "UTF-8")

@Composable
fun PSend(s: AppState) {
    val d = s.dest
    val name = if (s.sendIdx in d.nearby.indices) d.nearby[s.sendIdx].name else d.name
    PPage(s, title = PT.takePlans[s.lang], sub = PT.takePlansSub.fmt(s.lang, name[s.lang])) {
        Row(Modifier.fillMaxWidth().height(400.u), horizontalArrangement = Arrangement.spacedBy(30.u)) {
            Row(
                Modifier.weight(1.3f).fillMaxHeight().enter(3).clip(RoundedCornerShape(30.u))
                    .background(Brush.linearGradient(listOf(Color(0xFF233440), Color(0xFF1A2128))))
                    .border(2.u, Color(0x667FE3FF), RoundedCornerShape(30.u)).padding(36.u),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QrTile(guideUrl(name), PT.scanGuide[s.lang], 250)
                Spacer(Modifier.width(40.u))
                Column(Modifier.weight(1f)) {
                    Tx("VISITKOREA", 22, PC.cyan, FontWeight.Bold)
                    Spacer(Modifier.height(10.u))
                    Tx(name[s.lang], 40, PC.text, FontWeight.SemiBold, lh = 50, maxLines = 2)
                    Spacer(Modifier.height(16.u))
                    Tx(PT.officialGuide[s.lang], 23, PC.sub, lh = 33)
                }
            }
            PCard(Modifier.weight(0.7f).fillMaxHeight().enter(4), padding = 32) {
                Tx(PT.openInMaps[s.lang], 22, PC.cyan, FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Box(Modifier.align(Alignment.CenterHorizontally)) {
                    QrTile(Content.mapsSearch("${name.ko}${if (s.sendIdx < 0) " " + s.entr.name.ko else ""}"), "Google Maps", 200)
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ───────────────────────── P22 · P23 주변 장소 상세 ─────────────────────────

@Composable
fun PSpot(s: AppState) = PPage(s) {
    val d = s.dest
    val sp = d.nearby[s.spotIdx.coerceIn(0, d.nearby.lastIndex)]
    Row(Modifier.fillMaxWidth().weight(1f).padding(bottom = 110.u), horizontalArrangement = Arrangement.spacedBy(40.u)) {
        Box(Modifier.weight(1f).fillMaxHeight()) {
            PlacePhoto(sp.img, s.spotIdx + 1, Modifier.fillMaxSize().clip(RoundedCornerShape(20.u)))
            Box(Modifier.padding(20.u).clip(RoundedCornerShape(14.u)).background(Color(0xCC0E7FA8)).padding(horizontal = 14.u, vertical = 6.u)) {
                Tx("${s.spotIdx + 1} · ${PT.near[s.lang]}", 18, Color.White, FontWeight.Bold)
            }
        }
        Column(Modifier.weight(0.82f).fillMaxHeight()) {
            Tx(sp.name[s.lang], 54, PC.text, FontWeight.SemiBold, lh = 66, modifier = Modifier.enter(0, dx = 40f))
            Spacer(Modifier.height(14.u))
            Tx(sp.sub[s.lang], 27, PC.sub, lh = 38, modifier = Modifier.enter(1, dx = 40f))
            Spacer(Modifier.height(28.u))
            PCard(Modifier.fillMaxWidth().enter(2, dx = 40f), padding = 28) {
                Tx(PT.goodToKnow[s.lang], 22, PC.cyan, FontWeight.SemiBold)
                Spacer(Modifier.height(12.u))
                Tx(PT.goodToKnowTitle[s.lang], 30, PC.text, FontWeight.SemiBold)
                Spacer(Modifier.height(10.u))
                Tx(PT.goodToKnowSub[s.lang], 22, PC.sub, lh = 31)
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(20.u), modifier = Modifier.enter(3)) {
                PBtn(PT.sendToPhone[s.lang], { s.sendIdx = s.spotIdx; s.sendReturn = P.SPOT; s.p = P.SEND }, primary = true, h = 80)
                PBtn(PT.near[s.lang], { s.p = P.NEAR }, h = 80)
            }
        }
    }
}

// ───────────────────────── P10 여행 도구 ─────────────────────────

@Composable
fun PTools(s: AppState) = PPage(s, title = PT.travelTools[s.lang], sub = PT.toolsSub[s.lang]) {
    Row(horizontalArrangement = Arrangement.spacedBy(26.u)) {
        FeatureCard(Ic.FORK, PT.findFood[s.lang], PT.findFoodSub[s.lang], { s.p = P.FOOD }, Modifier.weight(1f).enter(3))
        FeatureCard(Ic.PIN, PT.changeDrop[s.lang], PT.changeDropSub[s.lang], { s.pOpenDrop() }, Modifier.weight(1f).enter(4))
        FeatureCard(
            Ic.SWAP, PT.convertCurrency[s.lang],
            PT.convertCurrencySub.fmt(s.lang, PT.currencyName[s.lang]), { s.p = P.CONVERT }, Modifier.weight(1f).enter(5),
        )
    }
    Spacer(Modifier.weight(1f))
    Row(Modifier.fillMaxWidth().padding(end = IconGap), horizontalArrangement = Arrangement.End) {
        NextStepBtn(PT.backOverview[s.lang]) { s.p = P.HOME }
    }
}

// ───────────────────────── P11 식당 ─────────────────────────

@Composable
fun PFood(s: AppState) = PPage(s, title = PT.findFoodNearby[s.lang], sub = PT.findFoodNearbySub.fmt(s.lang, s.dest.name[s.lang])) {
    Row(Modifier.fillMaxWidth().height(430.u), horizontalArrangement = Arrangement.spacedBy(30.u)) {
        Column(
            Modifier.weight(1f).fillMaxHeight().enter(3).pressable { s.p = P.STREET }.clip(RoundedCornerShape(24.u)).background(PC.card)
                .border(2.u, PC.border, RoundedCornerShape(24.u)),
        ) {
            PlacePhoto("bindaetteok", 2, Modifier.fillMaxWidth().weight(1f))
            Row(Modifier.fillMaxWidth().padding(28.u), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Tx(PT.streetFood[s.lang], 34, PC.text, FontWeight.SemiBold)
                    Spacer(Modifier.height(10.u))
                    Tx(PT.streetFoodSub[s.lang], 23, PC.sub)
                }
                Ico(Ic.ARROW, PC.text, 36.u)
            }
        }
        PCard(Modifier.weight(0.85f).fillMaxHeight().enter(4), onClick = { s.p = P.BOOK }, padding = 34) {
            Row {
                Ico(Ic.PHONE, PC.text, 44.u)
                Spacer(Modifier.weight(1f))
                Ico(Ic.ARROW, PC.text, 36.u)
            }
            Spacer(Modifier.weight(1f))
            Tx(PT.bookTable[s.lang], 44, PC.text, FontWeight.Medium)
            Spacer(Modifier.height(14.u))
            Tx(PT.bookTableSub[s.lang], 27, PC.sub, lh = 38)
            Spacer(Modifier.height(30.u))
            PBtn(PT.seeBooking[s.lang], { s.p = P.BOOK }, primary = true, h = 80)
        }
    }
    Spacer(Modifier.weight(1f))
    Row(Modifier.fillMaxWidth().padding(end = IconGap), horizontalArrangement = Arrangement.End) {
        val tag = listOf(if (s.cuisineKorean) PT.koreanFood[s.lang] else PT.anyCuisine[s.lang], if (s.budgetLow) PT.under30k[s.lang] else PT.anyBudget[s.lang]).joinToString(" · ")
        Row(
            Modifier.enter(delayMs = 500).pressable { s.p = P.FOOD_PREF }.height(80.u).clip(RoundedCornerShape(40.u)).background(PC.btn)
                .border(2.u, PC.border, RoundedCornerShape(40.u)).padding(horizontal = 30.u),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Ico(Ic.FORK, PC.text, 30.u)
            Spacer(Modifier.width(12.u))
            Tx("${PT.foodPrefs[s.lang]}  ·  $tag", 24, PC.text, FontWeight.SemiBold)
        }
    }
}

// ───────────────────────── P26 음식 취향 ─────────────────────────

@Composable
fun PFoodPref(s: AppState) = PPage(s, title = PT.foodPrefs[s.lang], sub = PT.foodPrefsSub[s.lang]) {
    @Composable
    fun Choice(text: String, on: Boolean, onClick: () -> Unit, modifier: Modifier) {
        val bg by animateColorAsState(if (on) Color(0xFF243540) else PC.card, tween(300), label = "chBg")
        val bd by animateColorAsState(if (on) PC.cyan else PC.border, tween(300), label = "chBd")
        Row(
            modifier.pressable(onClick = onClick).height(110.u).clip(RoundedCornerShape(24.u)).background(bg).border(2.u, bd, RoundedCornerShape(24.u))
                .padding(horizontal = 30.u),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tx(text, 30, PC.text, FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Box(Modifier.size(40.u).clip(CircleShape).background(if (on) PC.cyan else Color.Transparent).border(2.u, if (on) PC.cyan else PC.border, CircleShape), contentAlignment = Alignment.Center) {
                if (on) Ico(Ic.CHECK, PC.onAccent, 24.u, stroke = 3f)
            }
        }
    }
    Tx(PT.cuisine[s.lang], 24, PC.sub, FontWeight.SemiBold, modifier = Modifier.enter(3))
    Spacer(Modifier.height(16.u))
    Row(Modifier.enter(3), horizontalArrangement = Arrangement.spacedBy(24.u)) {
        Choice(PT.anyCuisine[s.lang], !s.cuisineKorean, { s.cuisineKorean = false }, Modifier.weight(1f))
        Choice(PT.koreanFood[s.lang], s.cuisineKorean, { s.cuisineKorean = true }, Modifier.weight(1f))
    }
    Spacer(Modifier.height(36.u))
    Tx(PT.budget[s.lang], 24, PC.sub, FontWeight.SemiBold, modifier = Modifier.enter(4))
    Spacer(Modifier.height(16.u))
    Row(Modifier.enter(4), horizontalArrangement = Arrangement.spacedBy(24.u)) {
        Choice(PT.anyBudget[s.lang], !s.budgetLow, { s.budgetLow = false }, Modifier.weight(1f))
        Choice(PT.under30k[s.lang], s.budgetLow, { s.budgetLow = true }, Modifier.weight(1f))
    }
    Spacer(Modifier.weight(1f))
    Row(Modifier.fillMaxWidth().padding(end = IconGap), horizontalArrangement = Arrangement.End) {
        NextStepBtn(PT.showRestaurants[s.lang]) { s.p = P.FOOD }
    }
}

// ───────────────────────── P11A 시장 길거리 음식 ─────────────────────────

@Composable
fun PStreet(s: AppState) = PPage(s, title = PT.streetTitle[s.lang], sub = PT.streetDesc[s.lang]) {
    val foods = Content.foods.filter { !s.budgetLow || it.priceKrw <= 30000 }
    Row(Modifier.fillMaxWidth().weight(1f).padding(bottom = 100.u), horizontalArrangement = Arrangement.spacedBy(30.u)) {
        Column(Modifier.weight(1.1f).fillMaxHeight().enter(3), verticalArrangement = Arrangement.spacedBy(14.u)) {
            for (f in foods.take(3)) Row(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.u)).background(PC.card).border(2.u, PC.border, RoundedCornerShape(20.u))
                    .padding(end = 28.u),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlacePhoto(f.img, 2, Modifier.width(150.u).fillMaxHeight(), credit = false)
                Spacer(Modifier.width(22.u))
                Column(Modifier.weight(1f)) {
                    Tx(f.tag[s.lang], 19, PC.cyan, FontWeight.SemiBold)
                    Spacer(Modifier.height(4.u))
                    Tx(f.name[s.lang], 30, PC.text, FontWeight.SemiBold, maxLines = 1)
                    Tx(f.note[s.lang], 20, PC.sub, maxLines = 1)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Tx(krw(f.priceKrw), 30, PC.text, FontWeight.SemiBold)
                    money(s.lang, f.priceKrw)?.let { Tx("≈ $it", 20, PC.sub) }
                }
            }
        }
        PCard(Modifier.weight(0.8f).fillMaxHeight().enter(4), padding = 32) {
            Tx(PT.beforeOrder[s.lang], 22, PC.orange, FontWeight.SemiBold)
            Spacer(Modifier.height(16.u))
            Tx(PT.walkIn[s.lang], 36, PC.text, FontWeight.SemiBold)
            Spacer(Modifier.height(12.u))
            Tx(PT.confirmPrice[s.lang], 24, PC.sub, lh = 34)
            Spacer(Modifier.weight(1f))
            PBtn(PT.marketDetails[s.lang], { s.sheet = Sheet.Foods }, h = 76)
        }
    }
}

// ───────────────────────── P11B 휴대폰으로 식당 예약 ─────────────────────────

@Composable
fun PBook(s: AppState) = PPage(s, title = PT.bookOnPhone[s.lang], sub = PT.bookOnPhoneSub[s.lang]) {
    val app = Content.kitApps.firstOrNull { it.name.startsWith("CATCHTABLE") } ?: Content.kitApps.first()
    Row(Modifier.fillMaxWidth().height(420.u), horizontalArrangement = Arrangement.spacedBy(30.u)) {
        Row(
            Modifier.weight(1.1f).fillMaxHeight().enter(3).clip(RoundedCornerShape(30.u))
                .background(Brush.linearGradient(listOf(Color(0xFF262A30), Color(0xFF1C1E22))))
                .border(2.u, Color(0xFF4A5560), RoundedCornerShape(30.u)).padding(36.u),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column {
                AppTile(app, 96)
                Spacer(Modifier.height(20.u))
                Tx(app.name, 34, PC.text, FontWeight.SemiBold)
            }
            QrTile(app.iosUrl, "iPhone", 200)
            QrTile(app.androidUrl, "Android", 200)
        }
        PCard(Modifier.weight(0.75f).fillMaxHeight().enter(4), padding = 34) {
            Tx(PT.searchToReserve[s.lang], 34, PC.text, FontWeight.SemiBold)
            Spacer(Modifier.height(14.u))
            Tx(PT.searchToReserveSub[s.lang], 24, PC.sub, lh = 34)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ico(Ic.CHECK, PC.green, 30.u, stroke = 2.6f)
                Spacer(Modifier.width(12.u))
                Tx(PT.checkConfirm[s.lang], 22, PC.green, FontWeight.SemiBold)
            }
            Spacer(Modifier.height(14.u))
            Row(verticalAlignment = Alignment.Top) {
                Ico(Ic.WARN, PC.orange, 28.u)
                Spacer(Modifier.width(12.u))
                Tx(LT.bookAlt[s.lang], 20, PC.sub, lh = 28)
            }
        }
    }
}

// ───────────────────────── P12A 내릴 곳 고르기 (실시간 지도) ─────────────────────────

@Composable
fun PDrop(s: AppState) = PPage(s, title = PT.chooseDrop[s.lang], sub = PT.chooseDropSub[s.lang]) {
    val d = s.dest
    var look by remember { mutableStateOf(if (s.pendingEntrance in d.entrances.indices && s.pendingEntrance != s.entrance) s.pendingEntrance else s.entrance) }
    Row(Modifier.fillMaxWidth().height(410.u), horizontalArrangement = Arrangement.spacedBy(26.u)) {
        LiveDropMap(s, look, Modifier.weight(1.1f).fillMaxHeight().enter(3))
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.u)) {
            for ((i, e) in d.entrances.withIndex()) {
                val cur = i == s.entrance
                val on = i == look
                val bd by animateColorAsState(if (on) PC.cyan else if (cur) Color(0xFF3D5A48) else PC.border, tween(300), label = "dropBd")
                val la = s.carLat
                val ln = s.carLng
                val dm = if (la != null && ln != null && e.lat != 0.0) distanceM(la, ln, e.lat, e.lng) else null
                Row(
                    Modifier.fillMaxWidth().weight(1f).enter(4 + i).pressable { look = i }.clip(RoundedCornerShape(24.u))
                        .background(if (cur) Color(0xFF1C2620) else PC.card).border(2.u, bd, RoundedCornerShape(24.u)).padding(horizontal = 26.u),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Tx(if (cur) PT.currentDrop[s.lang] else PT.alternative[s.lang], 18, if (cur) PC.green else PC.sub, FontWeight.SemiBold)
                            if (dm != null) {
                                Spacer(Modifier.width(12.u))
                                Tx("· ${distText(dm)}", 18, PC.cyan, FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.height(4.u))
                        Tx(e.name[s.lang], 30, PC.text, FontWeight.SemiBold, maxLines = 1)
                        Tx(e.side[s.lang], 19, PC.sub, maxLines = 1)
                    }
                    if (!cur) {
                        Spacer(Modifier.width(14.u))
                        PBtn(LT.select[s.lang], { s.pendingEntrance = i; s.p = P.DROP_CONFIRM }, primary = on, h = 64, modifier = Modifier.widthIn(min = 150.u))
                    } else {
                        Ico(Ic.CHECK, PC.green, 34.u, stroke = 2.6f)
                    }
                }
            }
        }
    }
}


// ───────────────────────── P12 변경 요청 확인 ─────────────────────────

@Composable
fun PDropConfirm(s: AppState) = PPage(s, title = PT.confirmRequest[s.lang], sub = PT.confirmRequestSub[s.lang]) {
    val d = s.dest
    val cur = s.entr
    val sel = d.entrances[s.pendingEntrance.coerceIn(0, d.entrances.lastIndex)]
    Row(Modifier.fillMaxWidth().height(340.u), verticalAlignment = Alignment.CenterVertically) {
        PCard(Modifier.weight(1f).fillMaxHeight().enter(3), padding = 30) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PTile(Ic.CHECK, 78, bg = Color(0xFF203028), fg = PC.green)
                Spacer(Modifier.width(22.u))
                Tx(PT.currentDrop[s.lang], 24, PC.green, FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Tx(cur.name[s.lang], 46, PC.text, FontWeight.SemiBold)
            Spacer(Modifier.height(20.u))
            Tx(cur.side[s.lang], 26, PC.sub)
        }
        Box(Modifier.width(90.u), contentAlignment = Alignment.Center) { StepLink(true, Modifier) }
        Column(
            Modifier.weight(1f).fillMaxHeight().enter(4).clip(RoundedCornerShape(30.u))
                .background(Brush.linearGradient(listOf(Color(0xFF26343B), Color(0xFF1B2328))))
                .border(2.u, PC.cyan, RoundedCornerShape(30.u)).padding(30.u),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PTile(Ic.PIN, 78, bg = Color(0xFF2A2F33))
                Spacer(Modifier.width(22.u))
                Tx(PT.selected[s.lang], 24, PC.text, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(Modifier.size(40.u).clip(CircleShape).background(PC.cyan), contentAlignment = Alignment.Center) {
                    Ico(Ic.CHECK, PC.onAccent, 26.u, stroke = 3f)
                }
            }
            Spacer(Modifier.weight(1f))
            Tx(sel.name[s.lang], 46, PC.text, FontWeight.SemiBold)
            Spacer(Modifier.height(20.u))
            Tx(sel.side[s.lang], 26, PC.sub)
        }
    }
    Spacer(Modifier.height(44.u))
    Row(Modifier.fillMaxWidth().padding(end = IconGap).enter(5), horizontalArrangement = Arrangement.End) {
        PBtn(PT.changeSelection[s.lang], { s.p = P.DROP }, h = 82)
        Spacer(Modifier.width(26.u))
        PBtn(PT.sendChange[s.lang], { s.pSendDrop() }, primary = true, h = 82, modifier = Modifier.widthIn(min = 400.u))
    }
}

// ───────────────────────── P24 기사님 확인 대기 · P24A 변경 완료 ─────────────────────────

@Composable
private fun DropStatus(s: AppState, done: Boolean) {
    val d = s.dest
    val target = d.entrances[(if (done) s.entrance else s.pendingEntrance).coerceIn(0, d.entrances.lastIndex)]
    Row(
        Modifier.fillMaxWidth().height(200.u).enter(3).clip(RoundedCornerShape(30.u)).background(Color(0xCC1C1F24))
            .border(2.u, if (done) Color(0xFF3D5A48) else PC.border, RoundedCornerShape(30.u)).padding(horizontal = 44.u),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            val badgeBg = if (done) Color(0xFF203028) else Color(0xFF3A2E1E)
            Box(Modifier.clip(RoundedCornerShape(20.u)).background(badgeBg).padding(horizontal = 16.u, vertical = 6.u)) {
                Tx((if (done) PT.confirmed else PT.pending)[s.lang], 20, if (done) PC.green else PC.orange, FontWeight.Bold)
            }
            Spacer(Modifier.height(18.u))
            Tx("${d.name[s.lang]} · ${target.name[s.lang]}", 42, PC.text, FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(12.u))
            Tx(if (done) PT.useXFor.fmt(s.lang, target.name[s.lang]) else PT.changeRequested[s.lang], 24, PC.sub)
        }
        SpinCheck(done, 100)
    }
}

@Composable
fun PDropPending(s: AppState) = PPage(s, rate = false, title = PT.waitingDriver[s.lang], sub = PT.stillCurrent.fmt(s.lang, s.entr.name[s.lang])) {
    DropStatus(s, false)
    Spacer(Modifier.height(20.u))
    LiveDropMap(s, s.pendingEntrance, Modifier.fillMaxWidth().weight(1f).padding(end = IconGap).enter(4))
}

@Composable
fun PDropDone(s: AppState) = PPage(s, rate = false, title = PT.dropUpdated[s.lang], sub = PT.driverConfirmed[s.lang]) {
    DropStatus(s, true)
    Spacer(Modifier.height(20.u))
    Row(Modifier.fillMaxWidth().weight(1f).padding(end = IconGap), verticalAlignment = Alignment.Bottom) {
        LiveDropMap(s, s.entrance, Modifier.weight(1f).fillMaxHeight().enter(4))
        Spacer(Modifier.width(26.u))
        NextStepBtn(PT.backTools[s.lang]) { s.p = P.TOOLS }
    }
}

// ───────────────────────── P13 환율 ─────────────────────────

@Composable
fun PConvert(s: AppState) = PPage(s, title = PT.convertTitle[s.lang], sub = PT.convertSub[s.lang]) {
    val c = Content.currencyOf(s.lang)
    Row(Modifier.fillMaxWidth().height(400.u), horizontalArrangement = Arrangement.spacedBy(28.u)) {
        PCard(Modifier.weight(1f).fillMaxHeight(), padding = 34) {
            Tx(PT.amountKrw[s.lang], 26, PC.sub)
            Spacer(Modifier.height(30.u))
            Tx(krw(s.krwAmount), 70, PC.text, FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(22.u)) {
                for (v in listOf(10000, 20000, 50000)) {
                    val on = v == s.krwAmount
                    Box(
                        Modifier.weight(1f).height(80.u).clip(RoundedCornerShape(14.u)).background(PC.btn)
                            .border(2.u, if (on) PC.cyan else PC.border, RoundedCornerShape(14.u))
                            .clickable { s.krwAmount = v },
                        contentAlignment = Alignment.Center,
                    ) { Tx(krw(v), 26, PC.text, FontWeight.SemiBold) }
                }
            }
        }
        PCard(Modifier.weight(1f).fillMaxHeight(), padding = 34) {
            Tx(PT.estimatedIn.fmt(s.lang, c?.code ?: ""), 26, PC.sub)
            Spacer(Modifier.height(30.u))
            Tx(money(s.lang, s.krwAmount) ?: "-", 70, PC.cyan, FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Tx("${(if (Rates.live) LT.liveRate else PT.sampleRate)[s.lang]}  ·  ${rateLine(s.lang) ?: ""}", 28, PC.text, FontWeight.SemiBold)
            Spacer(Modifier.height(22.u))
            Tx(if (Rates.live) LT.rateSource.fmt(s.lang, Rates.updated ?: "-") else PT.disclaimer[s.lang], 21, PC.sub)
            Spacer(Modifier.height(10.u))
        }
    }
}


// ───────────────────────── 시트 (QR · 이야기 · 통역 · 음식) ─────────────────────────

@Composable
fun PSheet(s: AppState, sheet: Sheet) {
    Box(
        Modifier.fillMaxSize().background(Color(0xB3000000)).clickable { s.sheet = null },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.enter().width(1180.u).clip(RoundedCornerShape(32.u)).background(Color(0xFF1C1C1C))
                .border(2.u, PC.border, RoundedCornerShape(32.u)).clickable { }.padding(48.u),
        ) {
            when (sheet) {
                is Sheet.Qr -> {
                    Tx(sheet.title, 44, PC.text, FontWeight.SemiBold)
                    Spacer(Modifier.height(10.u))
                    Tx(sheet.sub, 25, PC.sub)
                    Spacer(Modifier.height(40.u))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(70.u, Alignment.CenterHorizontally)) {
                        for ((label, url) in sheet.codes) QrTile(url, label, 300)
                    }
                }
                is Sheet.Story -> {
                    Tx(sheet.label, 24, PC.cyan, FontWeight.SemiBold)
                    Spacer(Modifier.height(12.u))
                    Tx(sheet.title, 48, PC.text, FontWeight.SemiBold)
                    Spacer(Modifier.height(24.u))
                    Tx(sheet.text, 32, PC.sub, lh = 48)
                }
                is Sheet.Info -> {
                    Tx(sheet.label, 24, PC.cyan, FontWeight.SemiBold)
                    Spacer(Modifier.height(12.u))
                    Tx(sheet.title, 44, PC.text, FontWeight.SemiBold, lh = 58)
                    Spacer(Modifier.height(20.u))
                    Tx(sheet.text, 30, PC.sub, lh = 44)
                }
                Sheet.Foods -> {
                    Tx(PT.streetFood[s.lang], 44, PC.text, FontWeight.SemiBold)
                    Spacer(Modifier.height(28.u))
                    Column(verticalArrangement = Arrangement.spacedBy(16.u)) {
                        for (f in Content.foods) Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.u)).background(PC.btn).padding(horizontal = 28.u, vertical = 22.u),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Tx(f.tag[s.lang], 20, PC.cyan, FontWeight.SemiBold)
                                Spacer(Modifier.height(4.u))
                                Tx(f.name[s.lang], 32, PC.text, FontWeight.SemiBold)
                                Tx(f.note[s.lang], 21, PC.sub)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Tx(krw(f.priceKrw), 32, PC.text, FontWeight.SemiBold)
                                money(s.lang, f.priceKrw)?.let { Tx("≈ $it", 22, PC.sub) }
                            }
                        }
                    }
                }
                Sheet.Translate -> {
                    Tx(PT.translate[s.lang], 44, PC.text, FontWeight.SemiBold)
                    Spacer(Modifier.height(8.u))
                    Tx(PT.translateSub[s.lang], 25, PC.sub)
                    s.shown?.let {
                        Spacer(Modifier.height(24.u))
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.u)).background(Color(0xFF1D2A30)).padding(28.u)) {
                            Tx(PT.driverHears[s.lang], 21, PC.cyan)
                            Spacer(Modifier.height(8.u))
                            Tx(it.target, 44, PC.text, FontWeight.SemiBold, lh = 56)
                        }
                    }
                    Spacer(Modifier.height(24.u))
                    Column(verticalArrangement = Arrangement.spacedBy(14.u)) {
                        for (r in Content.passengerPhrases.chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(14.u)) {
                            for (ph in r) Box(
                                Modifier.weight(1f).height(84.u).clip(RoundedCornerShape(16.u)).background(PC.btn)
                                    .border(2.u, PC.border, RoundedCornerShape(16.u)).clickable { s.passengerSays(ph) }
                                    .padding(horizontal = 24.u),
                                contentAlignment = Alignment.CenterStart,
                            ) { Tx(ph.text[s.lang], 26, PC.text, FontWeight.SemiBold, maxLines = 1) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(36.u))
            Row(Modifier.fillMaxWidth().padding(end = IconGap), horizontalArrangement = Arrangement.End) {
                PBtn(PT.close[s.lang], { s.sheet = null })
            }
        }
    }
}

// ───────────────────────── 이전 / 다음 버튼 ─────────────────────────

@Composable
fun PagerBtn(text: String, icon: Ic, iconFirst: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val fg = if (enabled) PC.text else PC.mute
    Row(
        Modifier.pressable(enabled = enabled, onClick = onClick).height(80.u).width(188.u).clip(RoundedCornerShape(16.u))
            .background(PC.btn).border(2.u, PC.border, RoundedCornerShape(16.u)).padding(horizontal = 22.u),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (iconFirst) Ico(icon, fg, 30.u)
        Tx(text, 24, fg, FontWeight.SemiBold, modifier = Modifier.weight(1f), align = TextAlign.Center)
        if (!iconFirst) Ico(icon, fg, 30.u)
    }
}


// ───────────────────────── 여정 카드 · 연결선 · 다음 단계 버튼 ─────────────────────────

@Composable
fun StepCard(n: Int, icon: Ic, title: String, sub: String, done: Boolean, next: Boolean, s: AppState, onClick: () -> Unit, modifier: Modifier) {
    val bd by animateColorAsState(if (next) PC.cyan else if (done) Color(0xFF3D5A48) else PC.border, tween(400), label = "stepBd")
    Column(
        modifier.pressable(onClick = onClick).height(330.u).clip(RoundedCornerShape(30.u))
            .background(if (next) Brush.linearGradient(listOf(Color(0xFF243038), Color(0xFF1C2226))) else Brush.linearGradient(listOf(PC.card, PC.card)))
            .border(2.u, bd, RoundedCornerShape(30.u)).padding(30.u),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PTile(icon)
            Spacer(Modifier.width(16.u))
            Tx("STEP $n", 20, if (next) PC.cyan else PC.mute, FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            when {
                done -> Box(Modifier.size(40.u).clip(CircleShape).background(PC.green), contentAlignment = Alignment.Center) {
                    Ico(Ic.CHECK, PC.onAccent, 24.u, stroke = 3f)
                }
                next -> Box(Modifier.clip(RoundedCornerShape(20.u)).background(PC.cyan).padding(horizontal = 14.u, vertical = 6.u)) {
                    Tx("NEXT", 18, PC.onAccent, FontWeight.Bold)
                }
                else -> Ico(Ic.ARROW, PC.text, 34.u)
            }
        }
        Spacer(Modifier.weight(1f))
        Tx(title, 34, PC.text, FontWeight.SemiBold)
        Spacer(Modifier.height(12.u))
        Tx(sub, 23, PC.sub, lh = 32)
    }
}

/** 카드 사이 연결선: 앞 단계를 끝내면 빛이 흘러간다 */
@Composable
fun StepLink(active: Boolean, modifier: Modifier) {
    val time by rememberSeconds()
    Canvas(modifier.width(44.u).height(20.u)) {
        val y = size.height / 2
        drawLine(Color(0x33FFFFFF), Offset(0f, y), Offset(size.width, y), strokeWidth = 3f)
        if (active) {
            val p = (time * 0.8f) % 1f
            drawLine(
                Brush.horizontalGradient(listOf(Color.Transparent, PC.cyan, Color.Transparent), startX = size.width * (p - 0.5f), endX = size.width * (p + 0.5f)),
                Offset(0f, y), Offset(size.width, y), strokeWidth = 4f,
            )
        }
        drawCircle(if (active) PC.cyan else Color(0x55FFFFFF), 5f, Offset(size.width, y))
    }
}

@Composable
fun NextStepBtn(text: String, onClick: () -> Unit) {
    Row(
        Modifier.enter(delayMs = 500).pressable(onClick = onClick).height(80.u).clip(RoundedCornerShape(40.u))
            .background(Brush.horizontalGradient(listOf(Color(0xFFCDEFFC), PC.accent))).padding(horizontal = 34.u),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tx(text, 25, PC.onAccent, FontWeight.SemiBold)
        Spacer(Modifier.width(14.u))
        Ico(Ic.ARROW, PC.onAccent, 28.u)
    }
}

/**
 * Tap to speak 버튼 뒤에서 숨 쉬는 빛.
 * 캔버스를 버튼보다 넉넉하게 크게 잡고 그 안쪽에만 그려서, 흐림(blur)이 캔버스 경계에서 잘려 네모가 보이지 않게 한다.
 */
@Composable
fun ButtonHalo(modifier: Modifier) {
    val time by rememberSeconds()
    Canvas(modifier.graphicsLayerBlur(26.u)) {
        val k = 0.5f + 0.5f * kotlin.math.sin(time * 1.9f)
        val bw = size.width * 0.70f + size.width * 0.06f * k     // 버튼(500) 보다 살짝 크게
        val bh = size.height * 0.34f + size.height * 0.08f * k
        val left = (size.width - bw) / 2
        val top = (size.height - bh) / 2
        drawRoundRect(
            Brush.horizontalGradient(
                listOf(Color(0x008FD3FF), Color(0xFF8FD3FF), Color(0xFFB9A8FF), Color(0xFF8FD3FF), Color(0x008FD3FF)),
                startX = left - 40f, endX = left + bw + 40f,
            ),
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(bw, bh),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(bh / 2),
            alpha = 0.30f + 0.30f * k,
        )
    }
}

/**
 * 돌던 스피너가 → 원으로 닫히며 하얗게 차오르고 → 톡 튀면서 → 체크가 그려지는 변신 애니메이션
 */
@Composable
fun SpinCheck(done: Boolean, size: Int) {
    val time by rememberSeconds()
    val p = remember { Animatable(0f) }          // 0 = 도는 중, 1 = 체크 완성
    val pop = remember { Animatable(1f) }
    var frozen by remember { mutableStateOf(0f) }
    LaunchedEffect(done) {
        if (done) {
            frozen = (time * 320f) % 360f
            p.animateTo(1f, tween(900, easing = LinearEasing))
        } else p.snapTo(0f)
    }
    LaunchedEffect(done) {
        if (done) {
            delay(330)
            pop.snapTo(0.82f)
            pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 420f))
        }
    }
    Canvas(Modifier.size(size.u).graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
        val v = p.value
        val sw = this.size.minDimension * 0.10f
        val inset = sw / 2
        val arc = androidx.compose.ui.geometry.Size(this.size.width - sw, this.size.height - sw)
        val r = this.size.minDimension / 2
        // ① 원 닫힘 (0~0.35): 270° → 360°
        val close = (v / 0.35f).coerceIn(0f, 1f)
        val angle = if (v > 0f) frozen else (time * 320f) % 360f
        drawArc(Color(0x1FFFFFFF), 0f, 360f, false, topLeft = Offset(inset, inset), size = arc, style = Stroke(sw))
        rotate(angle) {
            drawArc(
                Brush.sweepGradient(0f to Color(0x00BDE6F6).copy(alpha = close), 0.72f to Color(0xFFBDE6F6), 0.75f to Color.White, 0.76f to Color.White.copy(alpha = close), center = center),
                startAngle = 0f, sweepAngle = 270f + 90f * close, useCenter = false,
                topLeft = Offset(inset, inset), size = arc, style = Stroke(sw),
            )
        }
        // ② 하얗게 차오름 (0.25~0.55)
        val fill = ((v - 0.25f) / 0.3f).coerceIn(0f, 1f)
        if (fill > 0f) drawCircle(Color.White, r * fill)
        // ③ 체크 그리기 (0.5~1)
        val chk = ((v - 0.5f) / 0.5f).coerceIn(0f, 1f)
        if (chk > 0f) {
            val path = Path().apply {
                moveTo(r * 0.58f, r * 1.02f)
                lineTo(r * 0.88f, r * 1.32f)
                lineTo(r * 1.44f, r * 0.72f)
            }
            val pm = androidx.compose.ui.graphics.PathMeasure()
            pm.setPath(path, false)
            val seg = Path()
            pm.getSegment(0f, pm.length * FastOutSlowInEasing.transform(chk), seg, true)
            drawPath(seg, PC.onAccent, style = Stroke(sw * 0.9f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}

// ───────────────────────── P16 · P16A 기사님과 대화 (통역) ─────────────────────────

@Composable
fun PTalk(s: AppState) = PPage(s, rate = false, title = PT.talkTitle[s.lang], sub = PT.talkSub[s.lang]) {
    Row(Modifier.fillMaxWidth().weight(1f).padding(bottom = 12.u), horizontalArrangement = Arrangement.spacedBy(28.u)) {
        // ① 대화 카드
        Column(
            Modifier.weight(1.2f).fillMaxHeight().enter(3).clip(RoundedCornerShape(30.u)).background(Color(0xCC1C1F24))
                .border(2.u, PC.border, RoundedCornerShape(30.u)).padding(32.u),
        ) {
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.u)) {
                if (s.talkYou.isBlank() && s.talkDriver.isBlank()) {
                    Spacer(Modifier.weight(1f))
                    Tx(PT.talkEmpty[s.lang], 26, PC.mute, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.weight(1f))
                }
                if (s.talkYou.isNotBlank()) Column(Modifier.fillMaxWidth().enter(key = s.talkYou), horizontalAlignment = Alignment.End) {
                    Tx(PT.you[s.lang], 18, PC.cyan, FontWeight.SemiBold)
                    Spacer(Modifier.height(6.u))
                    Box(Modifier.widthIn(max = 640.u).clip(RoundedCornerShape(24.u, 6.u, 24.u, 24.u)).background(Color(0xFFBCE8F5)).padding(horizontal = 26.u, vertical = 16.u)) {
                        Tx(s.talkYou, 28, Color(0xFF152A31), FontWeight.SemiBold, lh = 38)
                    }
                }
                if (s.talkDriver.isNotBlank()) Column(Modifier.fillMaxWidth().enter(key = s.talkDriver, delayMs = 500)) {
                    Tx(PT.driverTranslated[s.lang], 18, PC.sub, FontWeight.SemiBold)
                    Spacer(Modifier.height(6.u))
                    Box(Modifier.widthIn(max = 640.u).clip(RoundedCornerShape(6.u, 24.u, 24.u, 24.u)).background(Color.White).padding(horizontal = 26.u, vertical = 16.u)) {
                        Tx(s.talkDriver, 28, Color(0xFF111A1F), FontWeight.SemiBold, lh = 38)
                    }
                }
            }
            // 자주 쓰는 말
            Column(verticalArrangement = Arrangement.spacedBy(12.u)) {
                for (r in Content.passengerPhrases.take(4).chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(12.u)) {
                    for (ph in r) Box(
                        Modifier.weight(1f).height(62.u).pressable { s.talkSay(ph) }.clip(RoundedCornerShape(31.u)).background(PC.btn)
                            .border(2.u, PC.border, RoundedCornerShape(31.u)).padding(horizontal = 22.u),
                        contentAlignment = Alignment.Center,
                    ) { Tx(ph.text[s.lang], 21, PC.text, FontWeight.SemiBold, maxLines = 1) }
                }
            }
        }
        // ② Tap to speak 카드: 목소리에 반응하는 파형
        val bd by animateColorAsState(if (s.listening) PC.cyan else PC.border, tween(400), label = "talkBd")
        Box(
            Modifier.weight(0.8f).fillMaxHeight().enter(4).clip(RoundedCornerShape(30.u))
                .background(Brush.verticalGradient(listOf(Color(0xFF1E2C36), Color(0xFF121A20))))
                .border(2.u, bd, RoundedCornerShape(30.u)),
        ) {
            SiriWave(
                s.level, s.listening,
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(230.u).padding(bottom = 96.u)
                    .graphicsLayer { alpha = if (s.listening) 1f else 0.35f },
            )
            Column(Modifier.fillMaxSize().padding(32.u), horizontalAlignment = Alignment.CenterHorizontally) {
                val pill by animateColorAsState(if (s.listening) Color(0xFFE5393B) else Color(0x22FFFFFF), tween(400), label = "talkPill")
                Row(
                    Modifier.clip(RoundedCornerShape(30.u)).background(pill).padding(horizontal = 22.u, vertical = 8.u),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Ico(Ic.MIC, Color.White, 26.u)
                    Spacer(Modifier.width(8.u))
                    Tx(if (s.listening) PT.listeningCaps[s.lang] else PT.tapToSpeak[s.lang], 20, Color.White, FontWeight.Bold)
                }
                Spacer(Modifier.height(26.u))
                Box(Modifier.fillMaxWidth().heightIn(min = 110.u), contentAlignment = Alignment.Center) {
                    if (s.listening && s.liveText.isNotBlank()) LiveSentence(s.liveText, true, 32)
                    else Tx(if (s.listening) PT.speakInYourLang[s.lang] else PT.speakToDriver[s.lang], 30, PC.text, FontWeight.SemiBold, lh = 40, align = TextAlign.Center)
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.pressable { if (s.listening) s.mic?.finishNow() else s.talkListen() }
                        .fillMaxWidth().height(80.u).clip(RoundedCornerShape(40.u))
                        .background(if (s.listening) Color.White else Color(0xFFBCE8F5)),
                    contentAlignment = Alignment.Center,
                ) { Tx((if (s.listening) PT.finishListening else PT.startListening)[s.lang], 26, Color(0xFF152A31), FontWeight.Bold) }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.u), horizontalArrangement = Arrangement.End) {
        PBtn(PT.done[s.lang], { s.stopListening(); s.p = s.talkReturn }, h = 72)
    }
}

// ───────────────────────── P15 곧 도착 (실시간 지도) ─────────────────────────

@Composable
fun PAlmost(s: AppState) = PPage(s, title = PT.almost[s.lang], sub = PT.almostSub[s.lang]) {
    Row(Modifier.fillMaxWidth().height(410.u), horizontalArrangement = Arrangement.spacedBy(26.u)) {
        PCard(Modifier.weight(0.9f).fillMaxHeight().enter(3), padding = 34) {
            PTile(Ic.PIN, 80)
            Spacer(Modifier.weight(1f))
            Tx(s.dest.name[s.lang], 42, PC.text, FontWeight.SemiBold)
            Spacer(Modifier.height(8.u))
            Tx(s.entr.name[s.lang], 30, PC.cyan, FontWeight.SemiBold)
            Tx(s.entr.side[s.lang], 22, PC.sub, maxLines = 2)
            Box(Modifier.fillMaxWidth().padding(vertical = 24.u).height(2.u).background(PC.border))
            Tx(PT.keepLinks[s.lang], 24, PC.text, lh = 34)
        }
        LiveDropMap(s, s.entrance, Modifier.weight(1.2f).fillMaxHeight().enter(4))
    }
    Spacer(Modifier.weight(1f))
    Row(Modifier.fillMaxWidth().padding(end = IconGap), horizontalArrangement = Arrangement.End) {
        PBtn(PT.changeDrop[s.lang], { s.pOpenDrop() }, h = 84)
        Spacer(Modifier.width(20.u))
        PBtn(PT.sendToPhone[s.lang], { s.sheet = s.mapQr() }, primary = true, h = 84, modifier = Modifier.widthIn(min = 320.u))
    }
}

// ───────────────────────── 동네 알림 배너 (지역 큐레이션) ─────────────────────────

@Composable
fun RegionBanner(s: AppState, r: Region, modifier: Modifier) {
    Row(
        modifier.enter(key = r.id).pressable { s.regionBanner = null; s.p = P.LIVE }
            .clip(RoundedCornerShape(40.u)).background(Color(0xEE0D1C26)).border(2.u, Color(0x887FE3FF), RoundedCornerShape(40.u))
            .padding(horizontal = 28.u, vertical = 14.u),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(14.u).clip(CircleShape).background(PC.cyan))
        Spacer(Modifier.width(14.u))
        Column {
            Tx(LT.enter.fmt(s.lang, r.name[s.lang]), 26, PC.text, FontWeight.Bold)
            Tx("${LT.localTip[s.lang]} · ${r.tip[s.lang]}", 19, PC.sub, maxLines = 1)
        }
        Spacer(Modifier.width(18.u))
        Ico(Ic.ARROW, PC.text, 28.u)
    }
}

// ───────────────────────── 지금 내 주변 (GPS 관광지 + 지역 큐레이션) ─────────────────────────

private data class Near2(val key: String, val title: String, val sub: String, val lat: Double, val lng: Double, val open: () -> Unit)

@Composable
fun PLive(s: AppState) = PPage(s, title = LT.aroundYou[s.lang], sub = LT.aroundSub[s.lang]) {
    val timeS = rememberSeconds()
    val density = LocalDensity.current
    val labelPx = with(density) { 18.u.toPx() }
    val paint = remember {
        android.graphics.Paint().apply { isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD; setShadowLayer(7f, 0f, 0f, android.graphics.Color.BLACK) }
    }
    val la = s.carLat
    val ln = s.carLng
    val cLat = la ?: s.dest.lat
    val cLng = ln ?: s.dest.lng
    // 명소 + 목적지 주변 장소를 거리순으로
    val items = buildList {
        for (p in Content.allPois) add(Near2(p.id, p.name[s.lang], p.story[s.lang], p.lat, p.lng) {
            s.sheet = Sheet.Story(PT.localStory[s.lang], p.name[s.lang], p.story[s.lang]); s.speak(p.story[s.lang])
        })
        for (d in Content.dests) for (sp in d.nearby) if (sp.lat != 0.0 && sp.img !in Content.allPois.map { it.id }) add(Near2(sp.img, sp.name[s.lang], sp.sub[s.lang], sp.lat, sp.lng) {
            s.sheet = Sheet.Info(LT.closest[s.lang], sp.name[s.lang], sp.sub[s.lang])
        })
    }.sortedWith(compareBy({ if (Content.category[it.key]?.let { c -> c in s.interests } == true) 0 else 1 }, { distanceM(cLat, cLng, it.lat, it.lng) }))
    Row(Modifier.fillMaxWidth().height(400.u), horizontalArrangement = Arrangement.spacedBy(26.u)) {
        Box(Modifier.weight(1.1f).fillMaxHeight().enter(3).clip(RoundedCornerShape(26.u)).border(2.u, Color(0x5576D8F5), RoundedCornerShape(26.u))) {
            RealMap(cLat, cLng, 15.6, Modifier.fillMaxSize(), lang = s.lang) { proj ->
                val t = timeS.value
                val cyan = Color(0xFF7FE3FF)
                // 동네 범위
                for (r in Content.regions) {
                    val p = proj.at(r.lat, r.lng)
                    val on = r.id == s.region?.id
                    drawCircle(cyan.copy(alpha = if (on) 0.12f else 0.04f), r.radiusM * proj.pxPerMeter(r.lat), p)
                    if (on) drawCircle(cyan.copy(alpha = 0.6f), r.radiusM * proj.pxPerMeter(r.lat), p, style = Stroke(2f))
                }
                // 명소
                for ((i, it) in items.take(8).withIndex()) {
                    val p = proj.at(it.lat, it.lng)
                    val bl = 0.5f + 0.5f * sin(t * 2.6f + i)
                    drawCircle(cyan.copy(alpha = 0.18f + 0.15f * bl), 20f, p)
                    drawCircle(cyan, 7f, p)
                    paint.color = android.graphics.Color.argb(230, 215, 245, 255); paint.textSize = labelPx
                    drawContext.canvas.nativeCanvas.drawText(it.title, p.x, p.y - 26f, paint)
                }
                // 목적지
                val dp = proj.at(s.dest.lat, s.dest.lng)
                drawCircle(Color.White, 9f, dp)
                drawCircle(Color.White, 18f, dp, style = Stroke(2.5f))
                // 내 택시
                val cla = s.carLat
                val cln = s.carLng
                if (cla != null && cln != null) {
                    val cp = proj.at(cla, cln)
                    val ph = (t * 0.7f) % 1f
                    drawCircle(Color.White.copy(alpha = (1f - ph) * 0.5f), 18f + 40f * ph, cp, style = Stroke(2f))
                    rotate(s.carBearing, cp) {
                        drawPath(Path().apply { moveTo(cp.x, cp.y - 20f); lineTo(cp.x + 14f, cp.y + 14f); lineTo(cp.x, cp.y + 6f); lineTo(cp.x - 14f, cp.y + 14f); close() }, Color.White)
                    }
                }
            }
            if (la == null) Column(
                Modifier.align(Alignment.Center).clip(RoundedCornerShape(24.u)).background(Color(0xE60B1A24)).padding(28.u),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Tx(LT.waitingGps[s.lang], 26, PC.text, FontWeight.Bold)
                Spacer(Modifier.height(16.u))
                PBtn(LT.startSim[s.lang], { s.simDriving = true }, primary = true, h = 70)
            }
            Box(Modifier.align(Alignment.TopStart).padding(16.u).clip(RoundedCornerShape(20.u)).background(Color(0xCC0B1A24)).padding(horizontal = 14.u, vertical = 6.u)) {
                Tx(if (la == null) LT.waitingGps[s.lang] else if (s.posSource == "GPS") LT.liveGps[s.lang] else LT.simGps[s.lang], 18, PC.text, FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            // 지역 카드
            val r = s.region
            Column(
                Modifier.fillMaxWidth().enter(4, key = r?.id).clip(RoundedCornerShape(24.u))
                    .background(Brush.linearGradient(listOf(Color(0xFF233440), Color(0xFF1A2128))))
                    .border(2.u, Color(0x667FE3FF), RoundedCornerShape(24.u)).padding(26.u),
            ) {
                Tx(LT.nowIn[s.lang], 18, PC.cyan, FontWeight.SemiBold)
                Spacer(Modifier.height(6.u))
                Tx(r?.name?.get(s.lang) ?: "—", 34, PC.text, FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.height(6.u))
                Tx(r?.intro?.get(s.lang) ?: LT.waitingGps[s.lang], 20, PC.sub, lh = 28, maxLines = 3)
                if (r != null) {
                    Spacer(Modifier.height(10.u))
                    Tx("${LT.localTip[s.lang]} · ${r.tip[s.lang]}", 19, PC.orange, FontWeight.SemiBold, maxLines = 1)
                }
            }
            Spacer(Modifier.height(16.u))
            Tx(LT.closest[s.lang], 20, PC.sub, FontWeight.SemiBold)
            Spacer(Modifier.height(10.u))
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.u)) {
                itemsIndexed(items.take(10)) { idx, it ->
                    Row(
                        Modifier.fillMaxWidth().height(96.u).pressable { it.open() }.clip(RoundedCornerShape(18.u)).background(Color(0xF5FFFFFF)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlacePhoto(it.key, idx + 2, Modifier.width(120.u).fillMaxHeight(), credit = false)
                        Column(Modifier.weight(1f).padding(horizontal = 18.u)) {
                            if (Content.category[it.key]?.let { c -> c in s.interests } == true) Tx(LT.forYou[s.lang], 14, Color(0xFF0E7FA8), FontWeight.Bold)
                            Tx(it.title, 24, Color(0xFF111A1F), FontWeight.Bold, maxLines = 1)
                            Tx(it.sub, 17, Color(0xFF55636B), maxLines = 1)
                        }
                        Tx(distText(distanceM(cLat, cLng, it.lat, it.lng)), 20, Color(0xFF0E7FA8), FontWeight.Bold)
                        Spacer(Modifier.width(18.u))
                    }
                }
            }
        }
    }
}

// ───────────────────────── 라이더 광장 (만남의 광장) ─────────────────────────

@Composable
fun PPlaza(s: AppState) = PPage(s, title = LT.plaza[s.lang], sub = LT.plazaSub[s.lang]) {
    val timeS = rememberSeconds()
    val density = LocalDensity.current
    val labelPx = with(density) { 16.u.toPx() }
    val paint = remember {
        android.graphics.Paint().apply { isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD; setShadowLayer(7f, 0f, 0f, android.graphics.Color.BLACK) }
    }
    // 3초마다 숫자 갱신, 4.5초마다 새 대화
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1500); tick++ } }
    val base = listOf(14, 9, 11, 17)
    val counts = Content.dests.mapIndexed { i, d ->
        base[i % base.size] + ((sin(tick * 0.35 + i * 1.3) * 2.5).roundToInt()) + (if (d.id == s.dest.id) s.plazaStamps + 1 else 0)
    }
    val total = counts.sum() + 6
    val colors = listOf(Color(0xFF7FE3FF), Color(0xFFB9A8FF), Color(0xFFF6D2EC), Color(0xFFAFE7F6), Color(0xFFFFD27F), Color(0xFF9FF0C0))
    Row(Modifier.fillMaxWidth().height(400.u), horizontalArrangement = Arrangement.spacedBy(26.u)) {
        Box(Modifier.weight(1.15f).fillMaxHeight().enter(3).clip(RoundedCornerShape(26.u)).border(2.u, Color(0x5576D8F5), RoundedCornerShape(26.u))) {
            RealMap(37.5690, 126.9855, 14.6, Modifier.fillMaxSize(), fit = Content.dests.map { it.lat to it.lng }, minZoom = 13.8, maxZoom = 15.2, lang = s.lang) { proj ->
                val t = timeS.value
                // 많이 가는 곳: 숫자만큼 커지는 빛
                for ((i, d) in Content.dests.withIndex()) {
                    val p = proj.at(d.lat, d.lng)
                    val r = 22f + counts[i] * 2.4f
                    drawCircle(Brush.radialGradient(listOf(Color(0x667FE3FF), Color(0x007FE3FF)), p, r * 1.8f), r * 1.8f, p)
                    drawCircle(Color(0xAA7FE3FF), r * 0.35f, p)
                    paint.color = android.graphics.Color.WHITE; paint.textSize = labelPx * 1.2f
                    drawContext.canvas.nativeCanvas.drawText("${d.name[s.lang]} · ${counts[i]}", p.x, p.y - r * 0.5f - 10f, paint)
                }
                // 가상 라이더: 두 지점 사이를 천천히 오감
                for ((i, rd) in plazaRiders.withIndex()) {
                    val ph = ((t * rd.speed + rd.phase) % 2f).let { if (it > 1f) 2f - it else it }
                    val e = ph * ph * (3 - 2 * ph)
                    val p = proj.at(rd.aLat + (rd.bLat - rd.aLat) * e, rd.aLng + (rd.bLng - rd.aLng) * e)
                    val col = colors[i % colors.size]
                    drawCircle(col.copy(alpha = 0.25f), 16f, p)
                    drawCircle(col, 6f, p)
                }
                // 나
                // 나도 정확한 위치가 아니라 동네 가운데로만 표시
                val la = s.region?.lat ?: s.dest.lat
                val ln = s.region?.lng ?: s.dest.lng
                val me = proj.at(la, ln)
                val ph = (t * 0.8f) % 1f
                drawCircle(Color.White.copy(alpha = (1f - ph) * 0.6f), 14f + 36f * ph, me, style = Stroke(2.5f))
                drawCircle(Color.White, 10f, me)
                paint.color = android.graphics.Color.WHITE; paint.textSize = labelPx * 1.1f
                drawContext.canvas.nativeCanvas.drawText(LT.you[s.lang], me.x, me.y - 22f, paint)
            }
            Row(
                Modifier.align(Alignment.TopStart).padding(16.u).clip(RoundedCornerShape(20.u)).background(Color(0xCC0B1A24)).padding(horizontal = 14.u, vertical = 6.u),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(12.u).clip(CircleShape).background(PC.green))
                Spacer(Modifier.width(10.u))
                Tx(LT.ridersNow.fmt(s.lang, "$total"), 18, PC.text, FontWeight.Bold)
            }
            Box(Modifier.align(Alignment.BottomEnd).padding(14.u).clip(RoundedCornerShape(12.u)).background(Color(0x99000000)).padding(horizontal = 10.u, vertical = 4.u)) {
                Tx(LT.demoNote[s.lang], 14, Color(0xCCFFFFFF))
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            // 지금 많이 가는 곳
            Tx(LT.trending[s.lang], 20, PC.sub, FontWeight.SemiBold)
            Spacer(Modifier.height(10.u))
            val order = Content.dests.indices.sortedByDescending { counts[it] }
            val maxC = counts.max().toFloat()
            for (i in order.take(3)) {
                val d = Content.dests[i]
                val w by animateFloatAsState(counts[i] / maxC, tween(800), label = "trend")
                Row(Modifier.fillMaxWidth().height(40.u), verticalAlignment = Alignment.CenterVertically) {
                    Tx(d.name[s.lang], 20, PC.text, FontWeight.SemiBold, maxLines = 1, modifier = Modifier.width(280.u))
                    Box(Modifier.weight(1f).height(10.u).clip(RoundedCornerShape(5.u)).background(Color(0x22FFFFFF))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(w).clip(RoundedCornerShape(5.u)).background(PC.cyan))
                    }
                    Spacer(Modifier.width(12.u))
                    Tx(LT.riders.fmt(s.lang, "${counts[i]}"), 19, PC.cyan, FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.u))
            Tx(LT.chat[s.lang], 20, PC.sub, FontWeight.SemiBold)
            Spacer(Modifier.height(10.u))
            // 대화: 내 말이 맨 위, 그 아래 다른 라이더 말이 4.5초마다 하나씩
            val feedIdx = tick / 3
            Column(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.u)).background(Color(0xCC1C1F24))
                    .border(2.u, PC.border, RoundedCornerShape(20.u)).padding(16.u).clipToBounds(),
                verticalArrangement = Arrangement.spacedBy(8.u),
            ) {
                val mine = s.plazaMine.lastOrNull()
                if (mine != null) Column(Modifier.fillMaxWidth().enter(key = s.plazaMine.size)) {
                    Tx(LT.you[s.lang], 14, PC.cyan, FontWeight.Bold)
                    Tx(mine, 19, PC.text, FontWeight.SemiBold, maxLines = 1)
                }
                for (k in 0 until (if (mine != null) 1 else 2)) {
                    val m = plazaFeed[Math.floorMod(feedIdx - k, plazaFeed.size)]
                    Column(Modifier.fillMaxWidth().enter(key = feedIdx - k)) {
                        Tx(LT.from.fmt(s.lang, m.from[s.lang]), 14, PC.mute, FontWeight.SemiBold)
                        Tx(m.text[s.lang], 19, PC.text, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(12.u))
            Row(horizontalArrangement = Arrangement.spacedBy(10.u)) {
                for (txt in listOf(LT.reactHi[s.lang], LT.reactTip[s.lang], LT.reactHere.fmt(s.lang, s.dest.name[s.lang]))) Box(
                    Modifier.weight(1f).height(58.u).pressable { s.plazaMine.add(txt) }.clip(RoundedCornerShape(29.u)).background(PC.btn)
                        .border(2.u, PC.border, RoundedCornerShape(29.u)).padding(horizontal = 12.u),
                    contentAlignment = Alignment.Center,
                ) { Tx(txt, 16, PC.text, FontWeight.SemiBold, maxLines = 1) }
            }
        }
    }
    Spacer(Modifier.weight(1f))
    Row(Modifier.fillMaxWidth().padding(end = IconGap), horizontalArrangement = Arrangement.End) {
        NextStepBtn("${LT.stamp[s.lang]} · ${s.dest.name[s.lang]}") { s.plazaStamps++; s.toast = "📍 ${s.dest.name[s.lang]} +1" }
    }
}

/** 홈 아래 넓은 버튼 (지금 내 주변 · 라이더 광장) */
@Composable
fun WideBanner(icon: Ic, title: String, sub: String, live: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.pressable(onClick = onClick).height(100.u).clip(RoundedCornerShape(20.u)).background(Color(0x1AD9D9D9))
            .border(2.u, Color(0x667FE3FF), RoundedCornerShape(20.u)).padding(horizontal = 28.u),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Ico(icon, PC.text, 40.u)
        Spacer(Modifier.width(20.u))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tx(title, 28, PC.text, FontWeight.Bold)
                if (live) {
                    Spacer(Modifier.width(12.u))
                    Box(Modifier.clip(RoundedCornerShape(10.u)).background(Color(0xFFE5393B)).padding(horizontal = 10.u, vertical = 2.u)) {
                        Tx("LIVE", 14, Color.White, FontWeight.Bold)
                    }
                }
            }
            Tx(sub, 20, PC.sub, maxLines = 1)
        }
        Ico(Ic.ARROW, PC.text, 30.u)
    }
}
