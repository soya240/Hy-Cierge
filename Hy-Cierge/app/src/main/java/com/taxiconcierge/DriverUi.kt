package com.taxiconcierge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay

/** 기사 화면 색 (Figma D01~D09, Pleos 라이트) */
object DC {
    val bg = Color(0xFFF7F8FA)
    val white = Color.White
    val text = Color(0xFF131417)
    val sub = Color(0xFF535762)
    val line = Color(0xFFDDE0E6)
    val tile = Color(0xFFE8F1FE)
    val blue = Color(0xFF005CCF)
    val icon = Color(0xFF1A6FE0)
    val primary = Color(0x0D131417)   // rgba(19,20,23,0.05)
    val mapPanel = Color(0xFFE9EBEF)
    val mapBlock = Color(0xFFDDE1E8)
    val river = Color(0xFFA9CBE6)
    val route = Color(0xFF0A6CFF)
}

@Composable
fun DriverApp(s: AppState) {
    Scaled(1350f, 946f, DC.bg) {
        // 주행 중이면 어떤 화면이든 내비만 보여주고 음성으로만 안내 (D05)
        val navOnly = s.d == D.NAV || (s.moving && s.d != D.ARRIVED)
        Box(Modifier.fillMaxSize()) {
            when {
                navOnly -> NavScreen(s)
                s.d == D.RECEIVED -> Received(s)
                else -> Column(Modifier.fillMaxSize()) {
                    DHeader(pill(s))
                    Box(Modifier.weight(1f).fillMaxWidth().padding(start = 48.u, end = 48.u, top = 44.u, bottom = 48.u)) {
                        when (s.d) {
                            D.BOARD -> Board(s)
                            D.LANG_DETECT -> LangDetect(s)
                            D.LANG -> LangConfirm(s)
                            D.ASK_DEST -> AskDest(s)
                            D.LANG_LIST -> LangList(s)
                            D.LISTEN -> Listen(s)
                            D.CONFIRM -> ConfirmPlace(s)
                            D.ARRIVED -> Arrived(s)
                            D.LINKED -> Linked(s)
                            else -> {}
                        }
                    }
                }
            }
            if (s.talkOpen) DriverTalk(s)
            DemoPanel(s, Modifier.align(Alignment.BottomEnd).padding(14.u))
        }
    }
    // 도착 지점 근처 + 정차 → D06 도착 화면
    LaunchedEffect(s.d, s.arriving, s.moving) {
        if (s.d == D.NAV && s.arriving && !s.moving) {
            delay(1200); s.d = D.ARRIVED
        }
    }
}

private fun pill(s: AppState): String = when (s.d) {
    D.BOARD, D.LANG_LIST -> if (s.moving) "주행 중" else "정차 중"
    D.LANG_DETECT, D.LANG, D.ASK_DEST, D.LISTEN -> if (s.listening) "듣는 중" else "앞좌석 단독형"
    D.CONFIRM -> "기사 승인"
    D.ARRIVED -> "도착 · 정차"
    D.LINKED, D.RECEIVED -> "뒷좌석 연결됨"
    D.NAV -> "운행 중"
}

// ───────────────────────── 공통 부품 ─────────────────────────

@Composable
fun DHeader(pill: String?) {
    Row(
        Modifier.fillMaxWidth().height(112.u).background(DC.white).padding(horizontal = 48.u),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tx("Hy-Cierge", 32, DC.text, FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        if (pill != null) {
            Box(
                Modifier.height(64.u).widthIn(min = 250.u).clip(RoundedCornerShape(16.u)).background(DC.tile).padding(horizontal = 32.u),
                contentAlignment = Alignment.Center,
            ) { Tx(pill, 24, DC.blue, FontWeight.Bold) }
        }
    }
}

@Composable
fun DPage(
    title: String,
    sub: String,
    eyebrow: String? = null,
    buttons: @Composable RowScope.() -> Unit,
    body: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (eyebrow != null) {
            Tx(eyebrow, 22, DC.blue, FontWeight.Bold)
            Spacer(Modifier.height(8.u))
        }
        Tx(title, 60, DC.text, FontWeight.Bold, lh = 80)
        Spacer(Modifier.height(16.u))
        Tx(sub, 28, DC.sub, lh = 36)
        Spacer(Modifier.height(52.u))
        body()
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(24.u), verticalAlignment = Alignment.CenterVertically) { buttons() }
    }
}

@Composable
fun DCardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.u)).background(DC.white)
            .border(2.u, DC.line, RoundedCornerShape(28.u)).padding(32.u),
        content = content,
    )
}

@Composable
fun DTile(icon: Ic, size: Int = 96) {
    Box(Modifier.size(size.u).clip(RoundedCornerShape((size / 4).u)).background(DC.tile), contentAlignment = Alignment.Center) {
        Ico(icon, DC.icon, (size * 4 / 10).u)
    }
}

@Composable
fun DIconCard(icon: Ic, title: String, sub: String) {
    DCardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DTile(icon)
            Spacer(Modifier.width(28.u))
            Column {
                Tx(title, 36, DC.text, FontWeight.Bold)
                Spacer(Modifier.height(8.u))
                Tx(sub, 24, DC.sub)
            }
        }
    }
}

@Composable
fun DPrimary(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.height(100.u).widthIn(min = 340.u).clip(RoundedCornerShape(32.u)).background(DC.primary)
            .clickable(onClick = onClick).padding(horizontal = 48.u),
        contentAlignment = Alignment.Center,
    ) { Tx(text, 32, DC.text, FontWeight.Bold) }
}

@Composable
fun DSecondary(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.height(88.u).widthIn(min = 300.u).clip(RoundedCornerShape(24.u)).background(DC.white)
            .border(2.u, DC.line, RoundedCornerShape(24.u)).clickable(onClick = onClick).padding(horizontal = 44.u),
        contentAlignment = Alignment.Center,
    ) { Tx(text, 30, DC.text, FontWeight.Bold) }
}

@Composable
fun DDivider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 26.u).height(2.u).background(DC.line))
}

// ───────────────────────── D01 탑승 ─────────────────────────

@Composable
fun Board(s: AppState) = DPage(
    "외국인 승객이 탑승했나요?",
    "승객과 목적지를 확인하고 다국어 통역을 시작하세요.",
    buttons = {
        DPrimary("탑승 시작", { s.dStart() })
        DSecondary("연동형 탑승 예시", { s.dLinked() })
    },
) {
    DIconCard(Ic.CHAT, "승객 화면 연결 상태", "연결된 승객 화면이 있으면 승객이 직접 언어와 목적지를 입력할 수 있어요.")
}

// ───────────────────────── D02 언어 확인 ─────────────────────────

@Composable
fun LangConfirm(s: AppState) = DPage(
    "어느 언어로 통역할까요?",
    "승객에게 출신 국가나 사용할 언어를 물어보세요.",
    eyebrow = "언어 확인",
    buttons = {
        DPrimary("${ro(s.lang.koName)} 계속", { s.dContinue() })
        DSecondary("다른 언어", { s.d = D.LANG_LIST })
    },
) {
    DIconCard(Ic.GLOBE, "${s.lang.koName}  ${s.lang.native}", "음성에서 감지한 언어예요.")
}

// ───────────────────────── D09 언어 목록 ─────────────────────────

@Composable
fun LangList(s: AppState) {
    Column(Modifier.fillMaxSize()) {
        Tx("승객 언어를 선택하세요", 60, DC.text, FontWeight.Bold, lh = 80)
        Spacer(Modifier.height(16.u))
        Tx("언어를 찾기 어려우면 승객에게 다시 물어보세요.", 28, DC.sub, lh = 36)
        Spacer(Modifier.height(40.u))
        Column(verticalArrangement = Arrangement.spacedBy(20.u)) {
            for (l in listOf(Lang.EN, Lang.JA, Lang.ZH)) {
                Row(
                    Modifier.fillMaxWidth().height(110.u).clip(RoundedCornerShape(24.u)).background(DC.white)
                        .border(2.u, if (l == s.lang) DC.icon else DC.line, RoundedCornerShape(24.u))
                        .clickable { s.dPickLang(l) }.padding(horizontal = 24.u),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DTile(Ic.GLOBE, 64)
                    Spacer(Modifier.width(24.u))
                    Column(Modifier.weight(1f)) {
                        Tx(l.native, 30, DC.text, FontWeight.Bold)
                        Tx(l.koName, 20, DC.sub)
                    }
                    Ico(Ic.ARROW, DC.icon, 36.u)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        DSecondary("뒤로", { s.d = D.LANG })
    }
}


// ───────────────────────── 듣기 공통 (가운데 문구 + 바닥 음파) ─────────────────────────

@Composable
fun DDark(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        Modifier.height(100.u).widthIn(min = 340.u).clip(RoundedCornerShape(32.u))
            .background(if (enabled) DC.text else DC.line).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 48.u),
        contentAlignment = Alignment.Center,
    ) { Tx(text, 30, Color.White, FontWeight.Bold) }
}

@Composable
fun ListenArea(s: AppState, idle: String, modifier: Modifier) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(28.u))) {
        VoiceGlow(s.level, s.listening, Modifier.fillMaxWidth().height(220.u).align(Alignment.BottomCenter), light = true)
        val text = when {
            s.liveText.isNotBlank() -> s.liveText
            s.listening -> "듣고 있어요…"
            else -> idle
        }
        Box(Modifier.align(Alignment.Center).padding(horizontal = 60.u)) {
            Tx(
                text, if (s.liveText.isNotBlank()) 40 else 40,
                if (s.liveText.isNotBlank()) DC.text else Color(0xFF9EA3AD), FontWeight.Bold,
                lh = 56, align = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        if (s.listening) Tx(
            "MIC · ${s.engine}", 18, DC.sub,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.u),
        )
    }
}

// ───────────────────────── D02A 승객 언어 듣기 ─────────────────────────

@Composable
fun LangDetect(s: AppState) = DPage(
    "어떤 언어를 사용하시나요?",
    "승객에게 사용할 언어를 물어보세요.",
    buttons = {
        DDark(if (s.listening) "듣는 중…" else "음성으로 언어 확인", { s.dDetectLang() }, enabled = !s.listening)
        DSecondary("언어 직접 선택", { s.stopListening(); s.d = D.LANG_LIST })
    },
) {
    ListenArea(s, "승객 언어 감지 중...", Modifier.weight(1f).padding(bottom = 30.u))
}

// ───────────────────────── D03A 목적지 말하기 ─────────────────────────

@Composable
fun AskDest(s: AppState) = DPage(
    "승객에게 목적지를 물어보세요",
    "승객이 목적지와 원하는 하차 지점을 말하면 통역해요.",
    buttons = {
        DDark(if (s.listening) "듣는 중…" else if (s.notFound) "다시 듣기" else "목적지 듣기", { s.dListenDest() }, enabled = !s.listening)
        if (s.notFound) for (d in Content.dests) DSmall(d.name.ko) { s.dPickDest(d) }
    },
) {
    if (s.notFound) {
        DCardBox {
            Tx("목적지를 찾지 못했어요 · 아래에서 직접 고를 수도 있어요", 22, Color(0xFFC0392B), FontWeight.Bold)
            Spacer(Modifier.height(10.u))
            Tx(s.heard.ifBlank { "(들린 말 없음)" }, 30, DC.text, lh = 40)
        }
    } else {
        ListenArea(s, "\"목적지 듣기\"를 누르고 승객에게 마이크를 넘겨주세요", Modifier.weight(1f).padding(bottom = 30.u))
    }
}

// ───────────────────────── D03 목적지 듣기 ─────────────────────────

@Composable
fun Listen(s: AppState) = DPage(
    "승객이 원하는 목적지를 들어보세요.",
    "인식한 문장을 확인한 후 목적지를 선택할 수 있어요.",
    buttons = {
        DPrimary("목적지 확인", { s.dConfirm() })
        DSecondary("다시 듣기", { s.dListenDest() })
        DSecondary("승객 통역", { s.talkOpen = true })
    },
) {
    DCardBox {
        Tx("승객 발화 · ${s.lang.native}", 22, DC.blue, FontWeight.Bold)
        Spacer(Modifier.height(10.u))
        Tx(s.heard.ifBlank { Content.request(s.lang, s.dest, s.entr) }, 28, DC.text, lh = 38)
        DDivider()
        Tx("한국어 번역", 22, DC.blue, FontWeight.Bold)
        Spacer(Modifier.height(10.u))
        Tx(Content.request(Lang.KO, s.dest, s.entr), 32, DC.text, FontWeight.Bold, lh = 42)
    }
}

// ───────────────────────── D04 장소 확인 ─────────────────────────

@Composable
fun ConfirmPlace(s: AppState) = DPage(
    "이 장소로 안내할까요?",
    "장소와 하차 지점을 확인해 내비게이션에 전달하세요.",
    buttons = {
        DPrimary("확정하고 내비로 전달", { s.startRide() })
        DSecondary("다시 확인", { s.d = D.LISTEN })
    },
) {
    DCardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DTile(Ic.PIN)
            Spacer(Modifier.width(28.u))
            Column {
                Tx("${s.dest.name.ko} ${s.entr.name.ko}", 40, DC.text, FontWeight.Bold)
                Spacer(Modifier.height(6.u))
                Tx(s.dest.address.ko, 26, DC.sub)
            }
        }
        DDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tx("하차 지점", 22, DC.sub, modifier = Modifier.width(260.u))
            Tx("${s.entr.side.ko} ${s.entr.name.ko}", 28, DC.text, FontWeight.Bold)
        }
    }
}

// ───────────────────────── D06 도착 ─────────────────────────

@Composable
fun Arrived(s: AppState) = DPage(
    "목적지에 도착했어요",
    "하차가 끝나면 탑승을 종료하세요.",
    buttons = { DPrimary("탑승 종료", { s.dEnd() }) },
) {
    DIconCard(Ic.CHECK, "${s.dest.name.ko} ${s.entr.name.ko}", "${s.entr.side.ko} ${s.entr.name.ko}")
}

// ───────────────────────── D07 연동형 대기 ─────────────────────────

@Composable
fun Linked(s: AppState) = DPage(
    "승객의 목적지 입력을 기다리고 있어요",
    "승객이 뒷좌석에서 언어를 선택하고 목적지를 입력합니다.",
    buttons = {
        DSecondary("앞좌석에서 통역", { s.d = D.LANG })
        DPrimary("수신 요청 예시", {
            s.d = D.RECEIVED
            s.speak(Content.request(Lang.KO, s.dest, s.entr), Lang.KO)
        })
    },
) {
    DIconCard(Ic.WAVE, "뒷좌석 연결됨", "요청을 받으면 기사 화면에서 장소와 하차 지점을 확인할 수 있어요.")
}

// ───────────────────────── D08 요청 수신 (앱 + 내비 분할) ─────────────────────────

@Composable
fun Received(s: AppState) {
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            DHeader(null)
            Column(Modifier.weight(1f).fillMaxWidth().padding(start = 40.u, end = 40.u, top = 40.u, bottom = 40.u)) {
                Tx("목적지를 확인하세요", 48, DC.text, FontWeight.Bold, lh = 64)
                Spacer(Modifier.height(12.u))
                Tx("승객이 요청한 하차 지점이에요.", 26, DC.sub)
                Spacer(Modifier.height(40.u))
                DCardBox {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DTile(Ic.PIN, 72)
                        Spacer(Modifier.width(20.u))
                        Tx("${s.dest.name.ko} ${s.entr.name.ko}", 36, DC.text, FontWeight.Bold)
                    }
                    Spacer(Modifier.height(24.u))
                    Tx(s.dest.address.ko, 26, DC.sub)
                    Spacer(Modifier.height(6.u))
                    Tx("${s.entr.side.ko} ${s.entr.name.ko}", 22, DC.sub)
                }
                Spacer(Modifier.weight(1f))
                DPrimary("확정하고 내비로 전달", { s.startRide() }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.u))
                DSecondary("다시 확인", { s.d = D.LINKED }, Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.width(16.u))
        Column(
            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(32.u)).background(DC.mapPanel).padding(40.u),
        ) {
            DTile(Ic.PIN, 72)
            Spacer(Modifier.height(36.u))
            Tx("내비게이션", 36, DC.text, FontWeight.Bold)
            Spacer(Modifier.height(10.u))
            Tx("시스템 앱 영역", 24, DC.sub)
        }
    }
}

// ───────────────────────── D05 내비게이션 (주행 중 화면) ─────────────────────────

@Composable
fun NavScreen(s: AppState) {
    Column(
        Modifier.fillMaxSize().padding(16.u).clip(RoundedCornerShape(32.u)).background(DC.mapPanel).padding(40.u),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tx("내비게이션", 32, DC.text, FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            val label = when {
                s.arriving -> "곧 도착"
                s.moving -> "주행 중 · 음성으로만 안내"
                else -> "정차 중"
            }
            Box(
                Modifier.height(56.u).clip(RoundedCornerShape(14.u)).background(if (s.moving) DC.blue else DC.white)
                    .padding(horizontal = 28.u),
                contentAlignment = Alignment.Center,
            ) { Tx(label, 24, if (s.moving) Color.White else DC.blue, FontWeight.Bold) }
        }
        Spacer(Modifier.height(28.u))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(24.u)).background(DC.mapBlock)) {
            MapDrawing()
            // 목적지 말풍선
            Box(
                Modifier.offset(x = maxWidth * 0.6f, y = maxHeight * 0.2f).clip(RoundedCornerShape(14.u)).background(DC.white)
                    .padding(horizontal = 24.u, vertical = 16.u),
            ) { Tx("${s.dest.name.ko} ${s.entr.name.ko}", 26, DC.text, FontWeight.Bold) }

            // 창밖 이야기 (승객 언어로 음성 안내 중)
            s.story?.let { poi ->
                Row(
                    Modifier.align(Alignment.BottomStart).padding(24.u).widthIn(max = 760.u).clip(RoundedCornerShape(20.u))
                        .background(DC.white).padding(24.u),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DTile(Ic.WAVE, 64)
                    Spacer(Modifier.width(20.u))
                    Column {
                        Tx("창밖 이야기 · ${poi.name.ko}", 26, DC.text, FontWeight.Bold)
                        Tx("승객에게 ${ro(s.lang.koName)} 안내 중", 20, DC.sub)
                    }
                }
            }

            // 정차 중일 때만 버튼
            if (!s.moving) {
                Row(
                    Modifier.align(Alignment.TopStart).padding(24.u),
                    horizontalArrangement = Arrangement.spacedBy(16.u),
                ) {
                    DSmall("승객 통역") { s.talkOpen = true }
                    if (s.riding) DSmall("도착 처리") { s.d = D.ARRIVED }
                    else DSmall("처음으로") { s.d = D.BOARD }
                }
            }
        }
    }
}

@Composable
fun DSmall(text: String, onClick: () -> Unit) {
    Box(
        Modifier.height(64.u).clip(RoundedCornerShape(16.u)).background(DC.white).border(2.u, DC.line, RoundedCornerShape(16.u))
            .clickable(onClick = onClick).padding(horizontal = 28.u),
        contentAlignment = Alignment.Center,
    ) { Tx(text, 24, DC.text, FontWeight.Bold) }
}

/** Figma D05 지도 그림: 블록 + 흰 도로 + 강 + 파란 경로 */
@Composable
fun MapDrawing() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val road = w * 0.018f
        for (fx in listOf(0.12f, 0.3f, 0.5f, 0.77f, 0.9f)) drawLine(Color.White, Offset(w * fx, 0f), Offset(w * fx, h), road)
        for (fy in listOf(0.18f, 0.4f, 0.7f, 0.86f)) drawLine(Color.White, Offset(0f, h * fy), Offset(w, h * fy), road)
        val river = Path().apply {
            moveTo(0f, h * 0.78f)
            cubicTo(w * 0.25f, h * 0.7f, w * 0.35f, h * 0.6f, w * 0.55f, h * 0.62f)
            cubicTo(w * 0.75f, h * 0.64f, w * 0.85f, h * 0.55f, w, h * 0.5f)
        }
        drawPath(river, DC.river, style = Stroke(width = w * 0.03f, cap = StrokeCap.Round))
        val route = Path().apply {
            moveTo(w * 0.3f, h * 0.84f)
            lineTo(w * 0.3f, h * 0.68f)
            lineTo(w * 0.5f, h * 0.68f)
            lineTo(w * 0.5f, h * 0.38f)
            lineTo(w * 0.75f, h * 0.38f)
        }
        drawPath(route, DC.route, style = Stroke(width = w * 0.009f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(DC.route, w * 0.012f, Offset(w * 0.75f, h * 0.38f))
        drawCircle(Color.White, w * 0.005f, Offset(w * 0.75f, h * 0.38f))
        drawCircle(DC.route, w * 0.014f, Offset(w * 0.3f, h * 0.84f))
    }
}

// ───────────────────────── 기사 → 승객 통역 창 ─────────────────────────

@Composable
fun DriverTalk(s: AppState) {
    Box(Modifier.fillMaxSize().background(Color(0x66000000)).clickable { s.talkOpen = false }, contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(1100.u).clip(RoundedCornerShape(32.u)).background(DC.white).clickable { }.padding(40.u),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tx("승객에게 말하기 · ${ro(s.lang.koName)} 전달", 36, DC.text, FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                DSmall("닫기") { s.talkOpen = false }
            }
            s.shown?.let {
                Spacer(Modifier.height(24.u))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.u)).background(DC.tile).padding(28.u)) {
                    Tx(it.source, 22, DC.sub)
                    Spacer(Modifier.height(8.u))
                    Tx(it.target, 40, DC.text, FontWeight.Bold, lh = 52)
                }
            }
            Spacer(Modifier.height(24.u))
            val rows = Content.driverPhrases.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(16.u)) {
                for (r in rows) Row(horizontalArrangement = Arrangement.spacedBy(16.u)) {
                    for (ph in r) Box(
                        Modifier.weight(1f).height(88.u).clip(RoundedCornerShape(20.u)).background(DC.bg)
                            .border(2.u, DC.line, RoundedCornerShape(20.u)).clickable { s.driverSays(ph) }.padding(horizontal = 24.u),
                        contentAlignment = Alignment.CenterStart,
                    ) { Tx(ph.text.ko, 26, DC.text, FontWeight.Bold, maxLines = 1) }
                }
            }
        }
    }
}
