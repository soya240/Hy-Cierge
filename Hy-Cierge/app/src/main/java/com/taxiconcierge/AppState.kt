package com.taxiconcierge

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 앱을 켜면 고르는 화면: 기사(Pleos 앞좌석) / 승객(뒷좌석 태블릿) */
enum class Mode { PICK, DRIVER, PASSENGER }

/** 기사 화면 = Figma D01~D09 (+ D02A 언어 듣기, D03A 목적지 듣기) */
enum class D { BOARD, LANG_DETECT, LANG, LANG_LIST, ASK_DEST, LISTEN, CONFIRM, NAV, ARRIVED, LINKED, RECEIVED }

/** 승객 화면 = Figma P01~P27 (음성 우선 흐름) */
enum class P {
    WELCOME, PROMPT, ASK, LISTEN, REVIEW, UNCLEAR, TYPE, EDIT, LANGUAGE, WAITING, CONFIRMED,
    HOME, KIT, NEAR, PLACE, TOOLS, FOOD, DROP, CONVERT, ALMOST,
    PLACE_INFO, SEND, SPOT, TALK, FOOD_PREF, STREET, BOOK, DROP_CONFIRM, DROP_PENDING, DROP_DONE,
    LIVE, PLAZA,
}

/** 화면 위에 뜨는 창 */
sealed interface Sheet {
    data class Qr(val title: String, val sub: String, val codes: List<Pair<String, String>>) : Sheet
    data class Story(val label: String, val title: String, val text: String) : Sheet
    data class Info(val label: String, val title: String, val text: String) : Sheet
    data object Translate : Sheet
    data object Foods : Sheet
}

/** 통역 결과 한 줄 */
data class Shown(val source: String, val target: String)

/**
 * 앱 전체 상태. 화면(Compose)은 이 값만 보고 그린다.
 * 차량 신호(기어·속도·위치)와 마이크 소리가 들어오면 여기 값이 바뀌고, 화면이 알아서 다시 그려진다.
 */
class AppState(private val say: (String, Lang) -> Unit) {

    var mode by mutableStateOf(Mode.PICK)
    var d by mutableStateOf(D.BOARD)
    var p by mutableStateOf(P.WELCOME)

    var lang by mutableStateOf(Lang.EN)
    var dest by mutableStateOf(Content.dests.first())
    var entrance by mutableIntStateOf(0)
    var pendingEntrance by mutableIntStateOf(1)
    var riding by mutableStateOf(false)

    var sheet by mutableStateOf<Sheet?>(null)
    var toast by mutableStateOf<String?>(null)
    var shown by mutableStateOf<Shown?>(null)
    var talkOpen by mutableStateOf(false)
    var kitPage by mutableIntStateOf(0)
    /** Tap to speak 순간 오브 활성화 표시 */
    var orbActivating by mutableStateOf(false)
    /** 다 들은 뒤 결과 화면으로 넘어가기 전 '처리 중' (오브가 가운데로 모임) */
    var awaitingResult by mutableStateOf(false)
    /** 언어 고르기 화면을 닫으면 돌아갈 곳 */
    var langReturn by mutableStateOf(P.ASK)
    /** 통역(P16)을 닫으면 돌아갈 곳 */
    var talkReturn by mutableStateOf(P.HOME)
    /** 목적지 주변 장소 상세(P22/P23): -1 = 목적지 자체 */
    var spotIdx by mutableIntStateOf(0)
    /** 휴대폰으로 보내기(P14)에서 보여줄 대상: -1 = 목적지, 0.. = 주변 장소 */
    var sendIdx by mutableIntStateOf(-1)
    var sendReturn by mutableStateOf(P.PLACE)
    /** 통역 대화 */
    var talkYou by mutableStateOf("")
    var talkDriver by mutableStateOf("")
    /** 식당 취향 (P26) */
    var cuisineKorean by mutableStateOf(false)
    var budgetLow by mutableStateOf(false)
    /** 디지털 키트에서 '받았어요' 누른 앱 번호 */
    val kitAdded = androidx.compose.runtime.mutableStateListOf<Int>()
    /** 홈의 3단계 중 다녀온 곳 (키트·주변·도구) */
    val visited = androidx.compose.runtime.mutableStateListOf<P>()
    var krwAmount by mutableIntStateOf(20000)
    var waitDone by mutableStateOf(false)
    private var samples = 0

    val entr: Entrance get() = dest.entrances[entrance.coerceIn(0, dest.entrances.lastIndex)]

    // ── 음성 입력 ──
    var mic: Mic? = null
    var listening by mutableStateOf(false)
    var liveText by mutableStateOf("")
    var level by mutableFloatStateOf(0f)
    /** 승객이 실제로 말한(또는 입력한) 문장 */
    var heard by mutableStateOf("")
    /** 기사 D03A에서 목적지를 못 찾았을 때 */
    var notFound by mutableStateOf(false)
    /** DEMO 스위치: true면 마이크 대신 예시 문장 */
    var demoVoice by mutableStateOf(false)
    val engine: String get() = mic?.engine ?: "-"

    // ── 차량 신호 ──
    var carConnected by mutableStateOf(false)
    var carGear by mutableStateOf<Int?>(null)
    var carSpeedMs by mutableStateOf<Float?>(null)

    /** DEMO 패널에서 누른 기어 (차량 값보다 우선) */
    var demoGear by mutableStateOf<Int?>(null)

    /** 주행 중이면 기사 화면은 내비만 보이고 음성으로만 안내 */
    val moving: Boolean
        get() {
            demoGear?.let { return it != Gear.PARK && it != Gear.NEUTRAL }
            carGear?.let { return it != Gear.PARK && it != Gear.NEUTRAL }
            carSpeedMs?.let { return it > 0.5f }
            return false
        }

    // ── 위치 · 큐레이션 ──
    var posSource by mutableStateOf("-")
    /** 지금 차 위치 (GPS 또는 시뮬레이션). 아직 모르면 null */
    var carLat by mutableStateOf<Double?>(null)
    var carLng by mutableStateOf<Double?>(null)
    var carBearing by mutableFloatStateOf(0f)
    /** 지금 지나는 동네 (지역 큐레이션) */
    var region by mutableStateOf<Region?>(null)
    /** 화면 위에 잠깐 뜨는 '○○에 들어왔어요' 알림 */
    var regionBanner by mutableStateOf<Region?>(null)
    /** DEMO: 서울역 → 하차 지점 GPS 주행 시뮬레이션 중 */
    var simDriving by mutableStateOf(false)
    /** Virtual Console(8088)에서 기어를 D로 바꿔 시작한 주행이면 true → 기어·속도를 그대로 따름 */
    var simAuto by mutableStateOf(false)
    /** 라이더 광장: 내가 보낸 말·도장 */
    val plazaMine = androidx.compose.runtime.mutableStateListOf<String>()
    var plazaStamps by mutableIntStateOf(0)
    /** 탑승할 때 고른 관심사 (Content.category 번호) */
    val interests = androidx.compose.runtime.mutableStateListOf<Int>()
    var story by mutableStateOf<Poi?>(null)
    private val told = mutableSetOf<String>()
    var arriving by mutableStateOf(false)

    // ── DEMO ──
    var demoOpen by mutableStateOf(false)
    var autoDemo by mutableStateOf(false)

    fun speak(text: String, l: Lang = lang) = say(text, l)

    fun choose(m: Mode) {
        reset()
        mode = m
        d = D.BOARD
        p = P.WELCOME
    }

    // ───────────────── 마이크 ─────────────────

    /** 듣기 시작 → 실시간 문장(liveText)·소리 크기(level) 갱신 → 끝나면 done(문장) */
    fun listen(l: Lang, sample: String, done: (String) -> Unit) {
        liveText = ""
        level = 0f
        listening = true
        val m = mic
        if (m == null) {
            listening = false; done(sample); return
        }
        m.start(l.locale, sample, demoVoice, object : VoiceEvents {
            override fun onLevel(v: Float) { level = v }
            override fun onPartial(text: String) { liveText = text }
            override fun onFinal(text: String) {
                listening = false
                level = 0f
                liveText = text
                done(text)
            }
        })
    }

    /** P03C "Finish": 지금까지 들린 말로 바로 끝내기 */
    fun finishListening() {
        val m = mic
        if (m != null && listening) m.finishNow() else pHeard(liveText)
    }

    fun stopListening() {
        mic?.stop()
        listening = false
        level = 0f
    }

    /** 시연용 예시 문장 (부를 때마다 다음 목적지) */
    private fun nextSample(l: Lang): String {
        val dst = Content.dests[samples % Content.dests.size]
        samples++
        return Content.request(l, dst, dst.entrances.first())
    }

    // ───────────────── 기사 (D01~D09) ─────────────────

    /** D01 "탑승 시작" → D02A 승객 언어 듣기 */
    fun dStart() {
        reset(); samples = 0; d = D.LANG_DETECT
    }

    /** D01 "연동형 탑승 예시" → 뒷좌석 태블릿과 연결된 흐름 */
    fun dLinked() {
        reset(); d = D.LINKED
    }

    /** D02A "음성으로 언어 확인" */
    fun dDetectLang() {
        listen(lang, Content.hello[lang]) { t ->
            if (t.isNotBlank()) lang = guessLang(t, lang).let { if (it == Lang.KO) Lang.EN else it }
            d = D.LANG
        }
    }

    fun dPickLang(l: Lang) {
        lang = l; d = D.LANG
    }

    /** D02 "영어로 계속" → 승객에게 인사 + 어디 가세요? → D03A */
    fun dContinue() {
        d = D.ASK_DEST
        notFound = false
        speak(Content.greet[lang] + " " + Content.whereTo[lang], lang)
    }

    /** D03A "목적지 듣기" */
    fun dListenDest() {
        notFound = false
        d = D.ASK_DEST
        listen(lang, nextSample(lang)) { t ->
            heard = t
            val m = Content.match(t)
            if (m != null) {
                dest = m.first; entrance = m.second
                d = D.LISTEN
                speak(Content.request(Lang.KO, dest, entr), Lang.KO)
            } else {
                notFound = true
            }
        }
    }

    /** D03A 직접 고르기 */
    fun dPickDest(dst: Dest) {
        dest = dst; entrance = 0
        heard = ""
        d = D.LISTEN
    }

    fun dConfirm() {
        d = D.CONFIRM
    }

    /** D04/D08 "확정하고 내비로 전달" → 운행 시작 */
    fun startRide() {
        riding = true
        d = D.NAV
        speak(Content.heading.fmt(lang, dest.name), lang)
    }

    fun dEnd() {
        speak(Content.arrived[lang], lang)
        reset()
        d = D.BOARD
    }

    fun driverSays(ph: Phrase) {
        shown = Shown(ph.text.ko, ph.text[lang])
        speak(ph.text[lang], lang)
    }

    // ───────────────── 승객 (P01~P27) ─────────────────

    /** P03A "Tap to speak" / 다시 말하기 → P03C Listening → P03 실시간 문장 */
    fun pListen() {
        p = P.LISTEN
        awaitingResult = false
        listen(lang, nextSample(lang)) { t ->
            heard = t
            awaitingResult = true          // 오브가 잠깐 '처리 중' 을 보여준 뒤 PassengerApp 이 pHeard 호출
        }
    }

    /** 통역(P16): 승객이 말하면 기사님께 한국어로, 기사님 답을 승객 언어로 (시연용 답) */
    fun talkListen() {
        val sample = Content.passengerPhrases[(samples++) % Content.passengerPhrases.size].text[lang]
        listen(lang, sample) { t ->
            if (t.isBlank()) return@listen
            val ph = Content.passengerPhrases.firstOrNull { it.text[lang].equals(t, true) }
            talkYou = t
            speak(ph?.text?.ko ?: t, Lang.KO)
            talkDriver = replyFor(ph).text[lang]
        }
    }

    /** 통역(P16) 자주 쓰는 문장 버튼 */
    fun talkSay(ph: Phrase) {
        stopListening()
        talkYou = ph.text[lang]
        speak(ph.text.ko, Lang.KO)
        talkDriver = replyFor(ph).text[lang]
    }

    private fun replyFor(ph: Phrase?): Phrase = when (ph?.id) {
        "p_time" -> Content.driverPhrases[1]
        "p_card" -> Content.driverPhrases[2]
        "p_stop", "p_trunk" -> Phrase("d_ok", T("네, 알겠습니다.", "Sure, no problem.", "好的，没问题。", "はい、わかりました。"))
        else -> Phrase("d_entrance", T("네, 정문 앞에 내려드릴게요.", "Yes. I'll drop you off in front of the main entrance.", "好的，我会在正门前让您下车。", "はい、正門の前で降ろしますね。"))
    }

    /** 말하거나 입력한 문장 처리 → 목적지 찾으면 P03 확인, 못 찾으면 P03D */
    fun pHeard(t: String) {
        heard = t
        if (t.isNotBlank()) {
            val g = guessLang(t, lang)
            if (g != Lang.KO) lang = g
        }
        val m = Content.match(t)
        if (m != null) {
            dest = m.first; entrance = m.second
            p = P.REVIEW
        } else {
            p = P.UNCLEAR
        }
    }

    fun pPickLang(l: Lang) {
        lang = l
        p = langReturn
        if (langReturn == P.ASK) speak(Content.whereTo[l], l)
    }

    fun pPickDest(dst: Dest, e: Int) {
        dest = dst; entrance = e
        heard = Content.request(lang, dst, dst.entrances[e])
        p = P.REVIEW
    }

    /** 화면에 보여줄 요청 문장 */
    val requestText: String get() = heard.ifBlank { Content.request(lang, dest, entr) }

    /** P03 "Send to driver" → 기사님께 한국어로 */
    fun pSend() {
        stopListening()
        waitDone = false
        p = P.WAITING
        speak(Content.request(Lang.KO, dest, entr), Lang.KO)
    }

    fun pConfirmed() {
        p = P.CONFIRMED
        riding = true
        speak(Content.heading.fmt(lang, dest.name), lang)
    }

    fun pOpenDrop() {
        pendingEntrance = (entrance + 1) % dest.entrances.size
        p = P.DROP
    }

    fun pCycleDrop() {
        val n = dest.entrances.size
        var next = (pendingEntrance + 1) % n
        if (next == entrance) next = (next + 1) % n
        pendingEntrance = next
    }

    /** P12 변경 요청 보내기 → P24 기사님 확인 대기 */
    fun pSendDrop() {
        val e = dest.entrances[pendingEntrance.coerceIn(0, dest.entrances.lastIndex)]
        speak(Content.dropChanged.fmt(Lang.KO, "${dest.name.ko} ${e.name.ko}"), Lang.KO)
        p = P.DROP_PENDING
    }

    /** P24 → P24A 기사님 확인 완료 */
    fun pDropConfirmed() {
        entrance = pendingEntrance
        heard = ""
        p = P.DROP_DONE
    }

    fun passengerSays(ph: Phrase) {
        shown = Shown(ph.text[lang], ph.text.ko)
        speak(ph.text.ko, Lang.KO)
    }

    fun mapQr(): Sheet.Qr = Sheet.Qr(
        "${dest.name[lang]} · ${entr.name[lang]}",
        PT.scanWithPhone[lang],
        listOf(PT.openInMaps[lang] to Content.mapsSearch("${dest.name.ko} ${entr.name.ko}")),
    )

    fun pBack() {
        stopListening()
        p = when (p) {
            P.WELCOME, P.PROMPT -> P.WELCOME
            P.ASK -> P.WELCOME
            P.LISTEN, P.REVIEW, P.UNCLEAR, P.TYPE, P.LANGUAGE -> P.ASK
            P.EDIT -> P.REVIEW
            P.WAITING -> P.REVIEW
            P.CONFIRMED -> P.REVIEW
            P.HOME -> P.HOME
            P.KIT, P.NEAR, P.TOOLS, P.ALMOST -> P.HOME
            P.PLACE -> P.NEAR
            P.FOOD, P.DROP, P.CONVERT -> P.TOOLS
            P.PLACE_INFO -> P.PLACE
            P.SEND -> sendReturn
            P.SPOT -> P.NEAR
            P.TALK -> talkReturn
            P.FOOD_PREF, P.STREET, P.BOOK -> P.FOOD
            P.DROP_CONFIRM -> P.DROP
            P.DROP_PENDING, P.DROP_DONE -> P.TOOLS
            P.LIVE, P.PLAZA -> P.HOME
        }
    }

    // ───────────────── 위치 ─────────────────

    /** 위치가 바뀔 때마다: 근처 명소 이야기 + 도착 임박 판단 */
    fun updatePosition(la: Double, ln: Double, source: String) {
        posSource = source
        val pl = carLat
        val pn = carLng
        if (pl != null && pn != null && distanceM(pl, pn, la, ln) > 3f) carBearing = Geo.bearing(pl, pn, la, ln)
        carLat = la; carLng = ln

        // 지역 큐레이션: 반경 안에 든 동네 중 가장 가운데에 가까운 곳
        val r = Content.regions
            .filter { distanceM(la, ln, it.lat, it.lng) < it.radiusM }
            .minByOrNull { distanceM(la, ln, it.lat, it.lng) / it.radiusM }
        var newRegion: Region? = null
        if (r != null && r.id != region?.id) {
            region = r
            newRegion = r
        }
        if (!riding) return
        if (newRegion != null && mode == Mode.PASSENGER) regionBanner = newRegion

        val near = Content.allPois
            .filter { it.id !in told }
            .minByOrNull { distanceM(la, ln, it.lat, it.lng) }
        if (near != null && distanceM(la, ln, near.lat, near.lng) < 300f) {
            told += near.id
            story = near
            speak(near.story[lang], lang)
            if (mode == Mode.PASSENGER) sheet = Sheet.Story(PT.localStory[lang], near.name[lang], near.story[lang])
            return
        }
        // 명소 이야기가 없을 때만 동네 소개를 말해 줌 (두 말이 겹치지 않게)
        newRegion?.let { speak(it.name[lang] + ". " + it.intro[lang], lang) }

        if (!arriving && distanceM(la, ln, dest.lat, dest.lng) < 350f) {
            arriving = true
            speak(Content.arrivalSoon[lang], lang)
            if (mode == Mode.PASSENGER) {
                sheet = null; p = P.ALMOST
            }
        }
    }

    /** 운행 시작으로 간주 (시뮬레이션 주행 · DEMO 위치 버튼) */
    fun startRiding() {
        if (riding) return
        riding = true
        if (mode == Mode.DRIVER) d = D.NAV
        if (mode == Mode.PASSENGER && p.ordinal < P.HOME.ordinal) p = P.HOME
    }

    private fun reset() {
        stopListening()
        entrance = 0; shown = null; sheet = null; toast = null
        story = null; told.clear(); arriving = false; riding = false
        talkOpen = false; kitPage = 0; talkYou = ""; talkDriver = ""; awaitingResult = false; orbActivating = false; kitAdded.clear(); visited.clear(); heard = ""; liveText = ""; notFound = false
        autoDemo = false; demoGear = null; simDriving = false; simAuto = false; region = null; regionBanner = null; plazaMine.clear(); interests.clear()
    }
}
