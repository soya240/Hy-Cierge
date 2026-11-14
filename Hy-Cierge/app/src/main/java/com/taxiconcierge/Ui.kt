package com.taxiconcierge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay

/**
 * 앱 시작점.
 *  - 기사 모드: Pleos 앞좌석 (Figma D01~D09, 라이트)
 *  - 승객 모드: 뒷좌석 태블릿 (Figma P01~P15, 다크)
 */
@Composable
fun App(s: AppState) {
    when (s.mode) {
        Mode.PICK -> ModePicker(s)
        Mode.DRIVER -> DriverApp(s)
        Mode.PASSENGER -> PassengerApp(s)
    }
    AutoDemo(s)
    DriveSim(s)
    // 실시간 환율: 켤 때 한 번 + 1시간마다 (제공처가 하루 1번 갱신)
    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        while (true) {
            Rates.refresh(ctx)
            delay(60 * 60 * 1000L)
        }
    }
}

// ───────────────────────── 모드 선택 ─────────────────────────

@Composable
fun ModePicker(s: AppState) {
    Scaled(1600f, 900f, PC.bg) {
        PassengerBackdrop(s)
        Column(Modifier.fillMaxSize().padding(80.u), verticalArrangement = Arrangement.Center) {
            Tx("Hy-Cierge", 30, PC.sub, FontWeight.Medium)
            Spacer(Modifier.height(12.u))
            Tx("어느 화면으로 시작할까요?", 64, PC.text, FontWeight.SemiBold)
            Spacer(Modifier.height(60.u))
            Row(horizontalArrangement = Arrangement.spacedBy(32.u)) {
                ModeCard(
                    "기사 화면", "Pleos 앞좌석 디스플레이",
                    "정차 중에는 카드로, 주행 중에는 내비와 음성으로만 안내해요.",
                    Ic.CHAT, Modifier.weight(1f),
                ) { s.choose(Mode.DRIVER) }
                ModeCard(
                    "승객 화면", "뒷좌석 태블릿 · Passenger",
                    "언어 선택, 목적지 요청, 디지털 키트 QR, 로컬 이야기, 환율.",
                    Ic.GLOBE, Modifier.weight(1f),
                ) { s.choose(Mode.PASSENGER) }
            }
        }
    }
}

@Composable
fun ModeCard(title: String, tag: String, desc: String, icon: Ic, modifier: Modifier, onClick: () -> Unit) {
    PCard(modifier.height(380.u), onClick, padding = 44) {
        Row {
            PTile(icon, 96)
            Spacer(Modifier.weight(1f))
            Ico(Ic.ARROW, PC.text, 40.u)
        }
        Spacer(Modifier.weight(1f))
        Tx(tag, 24, PC.cyan, FontWeight.SemiBold)
        Spacer(Modifier.height(10.u))
        Tx(title, 52, PC.text, FontWeight.SemiBold)
        Spacer(Modifier.height(14.u))
        Tx(desc, 26, PC.sub, lh = 36)
    }
}

// ───────────────────────── 시연 패널 (영상 촬영용) ─────────────────────────

@Composable
fun DemoPanel(s: AppState, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.End) {
        if (s.demoOpen) {
            Column(
                Modifier.width(520.u).clip(RoundedCornerShape(20.u)).background(Color(0xF01B1E26))
                    .border(2.u, Color(0xFF2E3340), RoundedCornerShape(20.u)).padding(22.u),
                verticalArrangement = Arrangement.spacedBy(12.u),
            ) {
                Tx("DEMO · 시연 제어", 22, Color(0xFFA3B8FF), FontWeight.Bold)
                Tx(
                    "차량 API ${if (s.carConnected) "연결" else "없음"} · 기어 ${Gear.label(s.carGear)} · " +
                        "속도 ${s.carSpeedMs?.let { "%.0f".format(it * 3.6f) + "km/h" } ?: "-"} · 위치 ${s.posSource}",
                    18, Color(0xFF8B90A0),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.u)) {
                    Chip("마이크 인식", !s.demoVoice) { s.demoVoice = false }
                    Chip("예시 문장", s.demoVoice) { s.demoVoice = true }
                    Tx("엔진: ${s.engine}", 18, Color(0xFF8B90A0))
                }
                if (s.mode == Mode.DRIVER) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.u)) {
                        Chip("P 정차", s.demoGear == Gear.PARK) { s.demoGear = Gear.PARK }
                        Chip("D 주행", s.demoGear == Gear.DRIVE) { s.demoGear = Gear.DRIVE }
                        Chip("차량값", s.demoGear == null) { s.demoGear = null }
                    }
                }
                Tx("GPS 주행 시뮬레이션 (서울역 → 하차 지점, 지역·명소 큐레이션 자동)", 18, Color(0xFF8B90A0))
                Row(horizontalArrangement = Arrangement.spacedBy(10.u)) {
                    Chip(if (s.simDriving) "■ 주행 멈춤" else "▶ 실제 경로 주행", s.simDriving) { s.simDriving = !s.simDriving }
                    Tx("지역: ${s.region?.name?.ko ?: "-"}", 18, Color(0xFF8B90A0))
                }
                Tx("위치 이동 → 창밖 이야기", 18, Color(0xFF8B90A0))
                Row(horizontalArrangement = Arrangement.spacedBy(10.u)) {
                    for (p in Content.allPois.take(4)) Chip(p.name.ko, false) { ensureRiding(s); s.updatePosition(p.lat, p.lng, "DEMO") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.u)) {
                    for (p in Content.allPois.drop(4).take(4)) Chip(p.name.ko, false) { ensureRiding(s); s.updatePosition(p.lat, p.lng, "DEMO") }
                    Chip("목적지 도착", false) { ensureRiding(s); s.updatePosition(s.dest.lat, s.dest.lng, "DEMO") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.u)) {
                    if (s.mode == Mode.DRIVER) Chip(if (s.autoDemo) "■ 자동 시연 멈춤" else "▶ 자동 시연", s.autoDemo) { s.autoDemo = !s.autoDemo }
                    Chip("처음으로", false) { s.choose(s.mode) }
                    Chip("모드 선택", false) { s.choose(Mode.PICK) }
                }
            }
            Spacer(Modifier.height(10.u))
        }
        Box(
            Modifier.clip(RoundedCornerShape(12.u)).background(Color(0x55000000)).clickable { s.demoOpen = !s.demoOpen }
                .padding(horizontal = 16.u, vertical = 8.u),
        ) { Tx(if (s.demoOpen) "DEMO 닫기" else "DEMO", 18, Color(0xCCFFFFFF), FontWeight.SemiBold) }
    }
}

/** DEMO 위치 버튼을 누르면 운행 중으로 간주 */
private fun ensureRiding(s: AppState) {
    if (s.riding) return
    s.riding = true
    if (s.mode == Mode.DRIVER) s.d = D.NAV
    if (s.mode == Mode.PASSENGER && s.p.ordinal < P.HOME.ordinal) s.p = P.HOME
}

@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(12.u)).background(if (on) Color(0xFF688CF9) else Color(0xFF14161C))
            .border(2.u, if (on) Color(0xFF688CF9) else Color(0xFF272B35), RoundedCornerShape(12.u))
            .clickable(onClick = onClick).padding(horizontal = 16.u, vertical = 10.u),
    ) { Tx(text, 19, if (on) Color.White else Color(0xFFE9EBF0), FontWeight.SemiBold) }
}

/** ▶ 자동 시연 (기사 화면): 광장시장 코스를 차례로 재생 → 그대로 화면 녹화하면 시연 영상 */
@Composable
fun AutoDemo(s: AppState) {
    LaunchedEffect(s.autoDemo) {
        if (!s.autoDemo) return@LaunchedEffect
        s.demoOpen = false
        val poi = Content.pois.associateBy { it.id }
        if (!s.riding) {
            s.dest = Content.dests.first(); s.entrance = 0
            s.d = D.BOARD; delay(2500)
            s.d = D.LANG; delay(2500)
            s.dContinue(); delay(6000)
            val was = s.demoVoice
            s.demoVoice = true; s.dListenDest(); delay(7000)
            s.demoVoice = was
            s.dConfirm(); delay(3500)
            s.startRide(); delay(4000)
        }
        s.demoGear = Gear.DRIVE; delay(2500)
        poi["gwanghwamun"]?.let { s.updatePosition(it.lat, it.lng, "DEMO 자동") }; delay(11000)
        poi["cheonggyecheon"]?.let { s.updatePosition(it.lat, it.lng, "DEMO 자동") }; delay(10000)
        s.demoGear = Gear.PARK; delay(3000)                                   // 신호 대기: 정차 중 화면
        s.demoGear = Gear.DRIVE; delay(1500)
        poi["jongmyo"]?.let { s.updatePosition(it.lat, it.lng, "DEMO 자동") }; delay(10000)
        s.updatePosition(s.dest.lat, s.dest.lng, "DEMO 자동"); delay(6000)  // 곧 도착
        s.demoGear = Gear.PARK                                                // 정차 → D06 도착
        s.autoDemo = false
    }
}
