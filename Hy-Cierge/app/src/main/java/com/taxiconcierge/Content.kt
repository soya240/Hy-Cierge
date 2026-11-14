package com.taxiconcierge

import java.util.Locale

/** 승객 언어 (기획서 1차: 한·영·중·일) */
enum class Lang(val code: String, val native: String, val koName: String, val enName: String, val locale: Locale) {
    KO("KO", "한국어", "한국어", "Korean", Locale.KOREAN),
    EN("EN", "English", "영어", "English", Locale.US),
    ZH("CN", "中文", "중국어", "Chinese", Locale.SIMPLIFIED_CHINESE),
    JA("JP", "日本語", "일본어", "Japanese", Locale.JAPANESE),
}

/** 4개 언어 문장 묶음 */
data class T(val ko: String, val en: String, val zh: String, val ja: String) {
    operator fun get(l: Lang): String = when (l) {
        Lang.KO -> ko
        Lang.EN -> en
        Lang.ZH -> zh
        Lang.JA -> ja
    }

    /** %s 자리에 언어별 값 넣기 (한국어 "(으)로"는 받침에 맞게 자동 변환) */
    fun fmt(l: Lang, arg: T): String = fmt(l, arg[l])

    fun fmt(l: Lang, arg: String): String =
        get(l).replace("%s(으)로", ro(arg)).replace("%s", arg)
}

/** "경복궁" → "경복궁으로", "홍대" → "홍대로", "영어" → "영어로" */
fun ro(word: String): String {
    val c = word.trimEnd().lastOrNull() ?: return word
    if (c !in '가'..'힣') return "$word(으)로"
    val jong = (c - '가') % 28
    return if (jong == 0 || jong == 8) "${word}로" else "${word}으로"   // 8 = ㄹ 받침
}

data class Phrase(val id: String, val text: T)

/** 하차 지점: 이름 + 어느 쪽인지 */
data class Entrance(val name: T, val side: T, val lat: Double = 0.0, val lng: Double = 0.0)

/** 목적지 근처 장소 (Near your destination) */
data class Spot(val name: T, val sub: T, val lat: Double = 0.0, val lng: Double = 0.0, val img: String = "")

data class Dest(
    val id: String,
    val name: T,
    val address: T,
    val lat: Double,
    val lng: Double,
    val blurb: T,
    val cardSub: T,
    val entrances: List<Entrance>,
    val nearby: List<Spot>,
    val storyTitle: T,
    val story: T,
    /** 사진 키 (Content.wiki 참고) */
    val img: String = "",
)

data class Region(val id: String, val name: T, val lat: Double, val lng: Double, val radiusM: Float, val intro: T, val tip: T)

data class Poi(val id: String, val name: T, val lat: Double, val lng: Double, val story: T)

/** 스캔하면 바로 그 앱 설치 페이지가 열리는 스토어 링크 (안드로이드 / 아이폰) */
data class KitApp(
    val name: String,
    val category: T,
    val desc: T,
    val playId: String,
    val appleId: String,
    val tile: Long,
    val glyph: String,
    val glyphColor: Long,
) {
    val androidUrl: String get() = "https://play.google.com/store/apps/details?id=$playId"
    val iosUrl: String get() = "https://apps.apple.com/app/id$appleId"
}

data class Food(val tag: T, val name: T, val priceKrw: Int, val note: T, val img: String = "")

data class Currency(val symbol: String, val code: String, val krwPerUnit: Double, val decimals: Int, val per: Int)

object Content {

    // ───────── 안내 멘트 ─────────
    val greet = T(
        "안녕하세요! 하이시어지입니다. 가시는 동안 제가 통역해 드릴게요.",
        "Hello! I'm Hy-Cierge. I'll translate for you during the ride.",
        "您好！我是Hy-Cierge。乘车途中由我为您翻译。",
        "こんにちは！ハイシアージです。乗車中は私が通訳します。",
    )
    val whereTo = T(
        "어디로 모실까요?",
        "Where would you like to go?",
        "您要去哪里？",
        "どちらまで行かれますか？",
    )
    val heading = T(
        "이제 %s(으)로 모시겠습니다.",
        "Heading to %s now.",
        "现在送您去%s。",
        "これから%sへ向かいます。",
    )
    val arrivalSoon = T(
        "곧 도착해요. 두고 내리는 물건이 없는지 확인해 주세요.",
        "We're almost there. Please check you have all your belongings.",
        "即将到达，请确认随身物品。",
        "まもなく到着です。お忘れ物のないようご確認ください。",
    )
    val arrived = T(
        "도착했습니다. 이용해 주셔서 감사합니다.",
        "We've arrived. Thank you for riding with us.",
        "已到达，感谢您的乘坐。",
        "到着しました。ご乗車ありがとうございました。",
    )
    val dropChanged = T(
        "하차 지점을 %s(으)로 바꿔 주세요.",
        "Please change my drop-off to %s.",
        "请把下车地点改到%s。",
        "降車地点を%sに変更してください。",
    )

    /** 승객이 말한 목적지 요청 문장 (D03 · P04) */
    fun request(l: Lang, d: Dest, e: Entrance): String = when (l) {
        Lang.KO -> "${d.name.ko} ${e.name.ko}에서 내려주실 수 있나요?"
        Lang.EN -> {
            val en = e.name.en
            if (en.endsWith("entrance") || en.endsWith("Gate"))
                "Could you drop me at the ${if (en.split(" ").first() in setOf("Main", "South", "North", "Food")) en.replaceFirstChar { it.lowercase() } else en} of ${d.name.en}?"
            else "Could you drop me at ${d.name.en}, ${en}?"
        }
        Lang.ZH -> "可以在${d.name.zh}${e.name.zh}让我下车吗？"
        Lang.JA -> "${d.name.ja}の${e.name.ja}で降ろしてもらえますか？"
    }


    /** 데모 예시: 언어 감지용 한 마디 */
    val hello = T(
        "안녕하세요, 한국어로 말해요.",
        "Hi, I'm from the US.",
        "你好，我来自中国。",
        "こんにちは、日本から来ました。",
    )

    /** 목적지 별명 (말이나 타이핑에서 찾기용) */
    private val aliases = mapOf(
        "gwangjang" to listOf("gwangjang", "kwangjang", "광장", "广藏", "広蔵", "クァンジャン"),
        "insadong" to listOf("insadong", "insa-dong", "insa dong", "인사동", "仁寺", "インサドン"),
        "gyeongbokgung" to listOf("gyeongbok", "kyungbok", "gyeongbokgung", "경복", "景福", "キョンボク"),
        "myeongdong" to listOf("myeongdong", "myeong-dong", "myungdong", "myeong dong", "명동", "明洞", "ミョンドン"),
    )

    /**
     * 요청 의도 파악: 장소 이름을 몰라도 "하고 싶은 것"으로 목적지를 고름
     * 예) "길거리 음식 먹고 싶어요" → 광장시장 / "궁궐 보고 싶어요" → 경복궁 / "화장품 사러" → 명동 / "전통 찻집" → 인사동
     */
    private val intents = mapOf(
        "gwangjang" to listOf("street food", "food market", "market", "mung bean", "bindaetteok", "길거리 음식", "시장", "빈대떡", "먹자", "小吃", "市场", "夜市", "屋台", "市場", "グルメ"),
        "gyeongbokgung" to listOf("palace", "king", "royal", "hanbok", "궁", "왕", "한복", "宫", "王宫", "韩服", "宮", "王宮", "韓服"),
        "myeongdong" to listOf("shopping", "cosmetic", "skincare", "beauty", "쇼핑", "화장품", "购物", "化妆品", "买", "ショッピング", "コスメ", "化粧品"),
        "insadong" to listOf("tea", "craft", "souvenir", "gallery", "traditional", "찻집", "차 ", "공예", "기념품", "전통", "茶", "工艺", "纪念品", "伝統", "工芸", "お土産"),
    )

    /** 관심사 분류 (탑승할 때 고른 관심사에 맞춰 순서를 바꿈): 0 역사 1 음식 2 쇼핑 3 산책 4 문화 */
    val category = mapOf(
        "gwanghwamun" to 0, "bosingak" to 0, "jongmyo" to 0, "sungnyemun" to 0, "deoksugung" to 0, "palacemuseum" to 0, "gyeongbokgung" to 0,
        "gwangjang" to 1, "bindaetteok" to 1, "gimbap" to 1, "yukhoe" to 1,
        "myeongdong" to 2, "ssamziegil" to 2, "insadong" to 2,
        "cheonggyecheon" to 3, "seochon" to 3, "nseoultower" to 3,
        "jogyesa" to 4, "cathedral" to 4,
    )

    /** 말한 문장에서 목적지·입구 찾기. 못 찾으면 null */
    fun match(text: String): Pair<Dest, Int>? {
        val q = text.lowercase()
        if (q.isBlank()) return null
        val d = dests.firstOrNull { dst ->
            val keys = aliases[dst.id].orEmpty() + listOf(dst.name.ko, dst.name.en, dst.name.zh, dst.name.ja).map { it.lowercase() }
            keys.any { it.isNotBlank() && q.contains(it) }
        } ?: dests.firstOrNull { dst -> intents[dst.id].orEmpty().any { q.contains(it) } }   // 이름이 없으면 의도로
        ?: return null
        val e = d.entrances.indexOfFirst { en ->
            listOf(en.name.ko, en.name.en, en.name.zh, en.name.ja).any { q.contains(it.lowercase()) }
        }.let { if (it < 0) 0 else it }
        return d to e
    }

    // ───────── 통역 문장 ─────────
    val passengerPhrases = listOf(
        Phrase("p_time", T("얼마나 걸려요?", "How long will it take?", "大概要多久？", "どのくらいかかりますか？")),
        Phrase("p_card", T("카드로 결제할 수 있나요?", "Can I pay by card?", "可以刷卡吗？", "カードで払えますか？")),
        Phrase("p_stop", T("여기서 내려 주세요.", "Please stop here.", "请在这里停车。", "ここで降ろしてください。")),
        Phrase("p_trunk", T("트렁크 좀 열어 주세요.", "Could you open the trunk?", "请帮我打开后备箱。", "トランクを開けてもらえますか？")),
        Phrase("p_ac", T("에어컨 좀 세게 해 주세요.", "Could you turn up the A/C?", "空调可以开大一点吗？", "エアコンを強くしてもらえますか？")),
        Phrase("p_receipt", T("영수증 주세요.", "Can I have a receipt?", "请给我收据。", "領収書をください。")),
    )

    val driverPhrases = listOf(
        Phrase("d_where", whereTo),
        Phrase("d_20min", T("20분 정도 걸립니다.", "It will take about 20 minutes.", "大约需要20分钟。", "20分ほどかかります。")),
        Phrase("d_card", T("카드 결제 됩니다.", "Card payment is available.", "可以刷卡。", "カードでお支払いいただけます。")),
        Phrase("d_traffic", T("길이 막혀서 조금 늦어질 수 있어요.", "Traffic is heavy, so it may take a bit longer.", "现在堵车，可能会晚一点。", "道が混んでいるので、少し遅れるかもしれません。")),
        Phrase("d_belt", T("안전벨트 매 주세요.", "Please fasten your seat belt.", "请系好安全带。", "シートベルトをお締めください。")),
        Phrase("d_arrived", T("도착했습니다.", "We have arrived.", "到了。", "到着しました。")),
    )

    // ───────── 목적지 (Figma 기준: 광장시장이 기본 시나리오) ─────────
    val dests = listOf(
        Dest(
            id = "gwangjang",
            name = T("광장시장", "Gwangjang Market", "广藏市场", "広蔵市場"),
            address = T("서울 종로구 창경궁로 88", "88 Changgyeonggung-ro, Jongno-gu", "钟路区昌庆宫路88号", "鍾路区昌慶宮路88"),
            lat = 37.5700, lng = 126.9996,
            blurb = T(
                "지붕 덮인 골목을 따라 한국 길거리 음식, 원단 가게, 시장 노점이 이어져요.",
                "Follow the covered aisles for Korean street food, fabric shops and market stalls.",
                "沿着有顶棚的通道，可以找到韩国街头小吃、布料店和市场摊位。",
                "屋根付きの通路に、韓国の屋台グルメや生地屋、市場の露店が並びます。",
            ),
            cardSub = T("길거리 음식과 전통시장 노점", "Street food and traditional market stalls", "街头小吃与传统市场摊位", "屋台グルメと伝統市場の露店"),
            entrances = listOf(
                Entrance(T("종로 쪽 입구", "Jongno-side entrance", "钟路一侧入口", "鍾路側の入口"), T("종로5가역 8번 출구에서 가까워요", "Close to Jongno 5-ga Station Exit 8", "靠近钟路5街站8号出口", "鍾路5街駅8番出口のすぐ近く"), 37.5706, 126.9993),
                Entrance(T("먹자골목 입구", "Food alley entrance", "美食街入口", "屋台通りの入口"), T("시장 한가운데 먹자골목 바로 앞", "Right at the food stalls in the middle of the market", "市场中央美食摊位正前方", "市場の真ん中、屋台通りの目の前"), 37.5701, 127.0004),
                Entrance(T("청계천 쪽 입구", "Cheonggyecheon-side entrance", "清溪川一侧入口", "清渓川側の入口"), T("을지로4가역 4번 출구 쪽", "Toward Euljiro 4-ga Station Exit 4", "乙支路4街站4号出口方向", "乙支路4街駅4番出口方面"), 37.5694, 126.9991),
            ),
            nearby = listOf(
                Spot(T("청계천", "Cheonggyecheon", "清溪川", "清渓川"), T("물길 따라 산책", "A walk along the stream", "沿溪散步", "小川沿いの散歩"), 37.5694, 126.9985, "cheonggyecheon"),
                Spot(T("종묘", "Jongmyo Shrine", "宗庙", "宗廟"), T("조선 왕실의 사당", "A royal ancestral shrine", "朝鲜王室的祠堂", "朝鮮王室の祠堂"), 37.5744, 126.9941, "jongmyo"),
            ),
            storyTitle = T("광장시장 이야기", "Inside Gwangjang Market", "广藏市场的故事", "広蔵市場の話"),
            story = T(
                "광장시장은 1905년에 문을 연, 한국에서 가장 오래된 상설 시장 중 하나예요. 녹두를 갈아 부치는 빈대떡과 한입 크기 마약김밥이 유명해요.",
                "Gwangjang Market opened in 1905 and is one of Korea's oldest permanent markets. Try the mung bean pancakes ground to order and the bite-size mayak gimbap.",
                "广藏市场于1905年开业，是韩国历史最悠久的常设市场之一。现磨现煎的绿豆煎饼和一口大小的麻药紫菜包饭很有名。",
                "広蔵市場は1905年に開かれた、韓国で最も古い常設市場のひとつです。その場で挽いて焼くピンデトックと、ひと口サイズの麻薬キンパが有名です。",
            ),
            img = "gwangjang",
        ),
        Dest(
            id = "insadong",
            name = T("인사동", "Insadong", "仁寺洞", "仁寺洞"),
            address = T("서울 종로구 인사동길 일대", "Insadong-gil, Jongno-gu", "钟路区仁寺洞街一带", "鍾路区仁寺洞キル一帯"),
            lat = 37.5740, lng = 126.9850,
            blurb = T(
                "전통 공예품 가게와 찻집, 작은 갤러리가 골목마다 이어지는 거리예요.",
                "A street of craft shops, tea houses and small galleries in every alley.",
                "传统工艺品店、茶馆和小画廊遍布每条小巷。",
                "伝統工芸の店や茶屋、小さなギャラリーが路地ごとに続く通りです。",
            ),
            cardSub = T("전통 공예와 찻집 거리", "Crafts and traditional tea houses", "传统工艺与茶馆街", "伝統工芸と茶屋の街"),
            entrances = listOf(
                Entrance(T("북인사마당", "Bukinsa Madang (north entrance)", "北仁寺广场（北口）", "北仁寺広場（北口）"), T("안국역 쪽 · 인사동길 북쪽 끝", "Anguk Station side, north end of Insadong-gil", "安国站一侧，仁寺洞街北端", "安国駅側、仁寺洞キルの北端"), 37.5754, 126.9852),
                Entrance(T("남인사마당", "Naminsa Madang (south entrance)", "南仁寺广场（南口）", "南仁寺広場（南口）"), T("종로 쪽 · 탑골공원 옆", "Jong-ro side, next to Tapgol Park", "钟路一侧，塔谷公园旁", "鍾路側、タプコル公園の隣"), 37.5718, 126.9873),
            ),
            nearby = listOf(
                Spot(T("조계사", "Jogyesa Temple", "曹溪寺", "曹渓寺"), T("도심 속 절", "A Buddhist temple downtown", "市中心的寺庙", "都心のお寺"), 37.574, 126.9818, "jogyesa"),
                Spot(T("쌈지길", "Ssamziegil", "森吉街", "サムジキル"), T("나선형 공예 골목", "A spiral lane of craft shops", "螺旋式手工艺街", "らせん状の工芸路地"), 37.5743, 126.985, "ssamziegil"),
            ),
            storyTitle = T("이름에 담긴 이야기", "Where the name came from", "名字的由来", "名前の由来"),
            story = T(
                "인사동이라는 이름은 조선 시대 관인방과 대사동에서 한 글자씩 따 왔어요. 지금은 전통 공예와 찻집 거리로 유명해요.",
                "Insadong takes one syllable each from two Joseon-era districts, Gwaninbang and Daesadong. Today it's known for crafts and tea houses.",
                "仁寺洞的名字分别取自朝鲜时代宽仁坊和大寺洞各一个字。如今以传统工艺和茶馆闻名。",
                "仁寺洞という名前は、朝鮮時代の寛仁坊と大寺洞から一文字ずつ取ったものです。今は伝統工芸と茶屋の街として有名です。",
            ),
            img = "insadong",
        ),
        Dest(
            id = "gyeongbokgung",
            name = T("경복궁", "Gyeongbokgung Palace", "景福宫", "景福宮"),
            address = T("서울 종로구 사직로 161", "161 Sajik-ro, Jongno-gu", "钟路区社稷路161号", "鍾路区社稷路161"),
            lat = 37.5796, lng = 126.9770,
            blurb = T(
                "1395년에 세운 조선의 첫 번째 왕궁이에요. 광화문을 지나 넓은 궁궐 마당이 펼쳐져요.",
                "The first royal palace of the Joseon dynasty, built in 1395. Wide courtyards open up past Gwanghwamun gate.",
                "建于1395年的朝鲜王朝第一座王宫。穿过光化门，宽阔的宫殿庭院展现在眼前。",
                "1395年に建てられた朝鮮王朝最初の王宮です。光化門をくぐると広い宮殿の庭が広がります。",
            ),
            cardSub = T("조선의 첫 번째 왕궁", "Joseon's first royal palace", "朝鲜王朝第一座王宫", "朝鮮王朝最初の王宮"),
            entrances = listOf(
                Entrance(T("광화문", "Gwanghwamun Gate", "光化门", "光化門"), T("궁의 정문 · 세종대로 쪽", "The palace's main gate, Sejong-daero side", "王宫正门，世宗大路一侧", "王宮の正門、世宗大路側"), 37.5759, 126.9768),
                Entrance(T("국립민속박물관 입구", "Folk Museum entrance", "国立民俗博物馆入口", "国立民俗博物館の入口"), T("궁 동쪽 · 삼청로 쪽", "East side of the palace, Samcheong-ro", "王宫东侧，三清路一侧", "王宮の東側、三清路側"), 37.5813, 126.9796),
            ),
            nearby = listOf(
                Spot(T("국립고궁박물관", "National Palace Museum", "国立古宫博物馆", "国立古宮博物館"), T("조선 왕실 유물", "Royal treasures of Joseon", "朝鲜王室文物", "朝鮮王室の宝物"), 37.5765, 126.9753, "palacemuseum"),
                Spot(T("서촌", "Seochon", "西村", "西村（ソチョン）"), T("한옥 골목과 카페", "Hanok lanes and cafés", "韩屋小巷与咖啡馆", "韓屋の路地とカフェ"), 37.5797, 126.9705, "seochon"),
            ),
            storyTitle = T("광화문의 뜻", "What Gwanghwamun means", "光化门的含义", "光化門の意味"),
            story = T(
                "광화문은 경복궁의 정문이에요. 빛으로 나라를 널리 비춘다는 뜻을 담고 있어요.",
                "Gwanghwamun is the main gate of Gyeongbokgung. Its name means light spreading across the nation.",
                "光化门是景福宫的正门，名字寓意光芒普照全国。",
                "光化門は景福宮の正門です。光が国中を照らすという意味が込められています。",
            ),
            img = "gyeongbokgung",
        ),
        Dest(
            id = "myeongdong",
            name = T("명동", "Myeongdong", "明洞", "明洞"),
            address = T("서울 중구 명동길 일대", "Myeongdong-gil, Jung-gu", "中区明洞街一带", "中区明洞キル一帯"),
            lat = 37.5636, lng = 126.9850,
            blurb = T(
                "화장품 가게와 길거리 음식, 쇼핑 거리가 모인 서울의 대표 번화가예요.",
                "Seoul's classic shopping district, packed with beauty stores and street food.",
                "首尔代表性的商圈，美妆店和街头小吃云集。",
                "コスメ店や屋台グルメが集まる、ソウルを代表する繁華街です。",
            ),
            cardSub = T("쇼핑과 길거리 음식", "Shopping and street food", "购物与街头小吃", "ショッピングと屋台グルメ"),
            entrances = listOf(
                Entrance(T("남쪽 입구", "South entrance", "南侧入口", "南側の入口"), T("명동역 6번 출구 앞 · 남산 방향", "In front of Myeong-dong Station Exit 6, toward Namsan", "明洞站6号出口前，南山方向", "明洞駅6番出口の前、南山方面"), 37.5609, 126.9861),
                Entrance(T("북쪽 입구", "North entrance", "北侧入口", "北側の入口"), T("을지로입구역 쪽 · 명동 거리 북쪽 끝", "Euljiro 1-ga Station side, north end of the shopping streets", "乙支路入口站一侧，商业街北端", "乙支路入口駅側、ショッピング街の北端"), 37.5651, 126.9831),
            ),
            nearby = listOf(
                Spot(T("명동성당", "Myeongdong Cathedral", "明洞天主教堂", "明洞聖堂"), T("1898년 붉은 벽돌 성당", "A red-brick cathedral from 1898", "1898年的红砖教堂", "1898年の赤レンガ聖堂"), 37.5633, 126.9873, "cathedral"),
                Spot(T("N서울타워", "N Seoul Tower", "N首尔塔", "Nソウルタワー"), T("남산 위 전망대", "City views from Namsan", "南山上的观景台", "南山からの眺め"), 37.5512, 126.9882, "nseoultower"),
            ),
            storyTitle = T("저녁의 명동", "Myeongdong after dark", "夜晚的明洞", "夜の明洞"),
            story = T(
                "저녁이 되면 명동 거리 한가운데에 노점이 들어서서 길 전체가 길거리 음식 골목으로 바뀌어요.",
                "In the evening, food stalls roll into the middle of Myeongdong's streets and the whole area turns into a street-food lane.",
                "到了傍晚，小吃摊会摆到明洞街道中央，整条街变成美食街。",
                "夕方になると明洞の通りの真ん中に屋台が並び、通り全体が屋台グルメの路地に変わります。",
            ),
            img = "myeongdong",
        ),
    )

    // ───────── 로컬 큐레이션 (주행 중 위치 기반 1분 이야기) ─────────
    val pois = listOf(
        Poi(
            "gwanghwamun", T("광화문", "Gwanghwamun", "光化门", "光化門"), 37.5759, 126.9768,
            T(
                "정면에 보이는 곳은 광화문, 조선 왕궁 경복궁의 정문이에요. 빛으로 나라를 널리 비춘다는 뜻을 담고 있어요.",
                "Straight ahead is Gwanghwamun, the main gate of Gyeongbokgung Palace. Its name means light spreading across the nation.",
                "正前方是光化门，朝鲜王朝王宫景福宫的正门。名字寓意光芒普照全国。",
                "正面に見えるのは光化門。朝鮮王朝の王宮、景福宮の正門です。光が国中を照らすという意味が込められています。",
            ),
        ),
        Poi(
            "cheonggyecheon", T("청계천", "Cheonggyecheon", "清溪川", "清渓川"), 37.5690, 126.9780,
            T(
                "지금 지나는 청계천은 도심을 가로지르는 개천이에요. 한때 도로로 덮였다가 2005년에 다시 물길이 열렸어요.",
                "We're passing Cheonggyecheon, a stream through downtown. It was once covered by roads and reopened in 2005.",
                "我们正经过清溪川，这条小河横穿市中心。它曾被道路覆盖，2005年重新恢复了水道。",
                "いま通っているのは清渓川。都心を流れる小川で、かつては道路に覆われていましたが、2005年に再び流れが戻りました。",
            ),
        ),
        Poi(
            "bosingak", T("보신각", "Bosingak", "普信阁", "普信閣"), 37.5700, 126.9830,
            T(
                "보신각은 조선 시대에 종을 쳐 도성 문을 여닫던 곳이에요. 지금도 새해 자정이면 제야의 종을 울려요.",
                "Bosingak's bell once signaled the opening and closing of the city gates. It still rings in the New Year at midnight.",
                "普信阁在朝鲜时代用钟声通知城门开关。如今每年新年零点仍在这里敲响钟声。",
                "普信閣は朝鮮時代、鐘を鳴らして都の門の開閉を知らせた場所です。今も新年の0時に鐘が鳴らされます。",
            ),
        ),
        Poi(
            "jongmyo", T("종묘", "Jongmyo Shrine", "宗庙", "宗廟"), 37.5744, 126.9941,
            T(
                "오른쪽 숲 안쪽이 종묘예요. 조선 왕과 왕비의 신위를 모신 사당으로, 유네스코 세계유산이에요.",
                "Behind the trees on the right is Jongmyo, the shrine for Joseon kings and queens. It's a UNESCO World Heritage site.",
                "右边树林深处是宗庙，供奉朝鲜国王和王后的祠堂，是联合国教科文组织世界遗产。",
                "右手の森の奥が宗廟です。朝鮮の王と王妃を祀る祠堂で、ユネスコ世界遺産です。",
            ),
        ),
        Poi(
            "insadong", T("인사동", "Insadong", "仁寺洞", "仁寺洞"), 37.5735, 126.9855,
            T(
                "인사동이라는 이름은 조선 시대 관인방과 대사동에서 한 글자씩 따 왔어요. 지금은 전통 공예와 찻집 거리로 유명해요.",
                "Insadong takes one syllable each from two Joseon-era districts, Gwaninbang and Daesadong. Today it's known for crafts and tea houses.",
                "仁寺洞的名字分别取自朝鲜时代宽仁坊和大寺洞各一个字。如今以传统工艺和茶馆闻名。",
                "仁寺洞という名前は、朝鮮時代の寛仁坊と大寺洞から一文字ずつ取ったものです。今は伝統工芸と茶屋の街として有名です。",
            ),
        ),
    )

    /** 경로 위 추가 명소 (서울역 → 도심 코스) */
    val morePois = listOf(
        Poi(
            "sungnyemun", T("숭례문", "Sungnyemun Gate", "崇礼门", "崇礼門"), 37.5600, 126.9753,
            T(
                "지금 보이는 큰 성문이 숭례문, 흔히 남대문이라고 불러요. 조선 시대 한양 도성의 남쪽 정문이었어요.",
                "That big gate is Sungnyemun, often called Namdaemun. It was the main southern gate of Joseon-era Seoul.",
                "眼前这座城门是崇礼门，俗称南大门，曾是朝鲜时代汉阳都城的南正门。",
                "見えている大きな門が崇礼門、通称南大門です。朝鮮時代の漢陽都城の南の正門でした。",
            ),
        ),
        Poi(
            "deoksugung", T("덕수궁", "Deoksugung Palace", "德寿宫", "徳寿宮"), 37.5658, 126.9752,
            T(
                "왼쪽 돌담 안이 덕수궁이에요. 대한문 앞에서는 낮에 수문장 교대식을 볼 수 있어요.",
                "Behind the stone wall on the left is Deoksugung Palace. You can catch the royal guard ceremony at Daehanmun Gate during the day.",
                "左边石墙里面是德寿宫。白天可以在大汉门前观看守门将交接仪式。",
                "左の石垣の中が徳寿宮です。昼間は大漢門の前で守門将交代式が見られます。",
            ),
        ),
        Poi(
            "cathedral", T("명동성당", "Myeongdong Cathedral", "明洞天主教堂", "明洞聖堂"), 37.5633, 126.9873,
            T(
                "언덕 위 붉은 벽돌 건물이 명동성당이에요. 1898년에 지어진 한국 최초의 벽돌 고딕 성당이에요.",
                "The red-brick church on the hill is Myeongdong Cathedral, built in 1898 as Korea's first brick Gothic church.",
                "山坡上的红砖建筑是明洞天主教堂，建于1898年，是韩国第一座砖砌哥特式教堂。",
                "丘の上の赤レンガの建物が明洞聖堂。1898年に建てられた韓国初のレンガ造りゴシック聖堂です。",
            ),
        ),
    )
    val allPois: List<Poi> get() = pois + morePois

    // ───────── 지역 큐레이션 (GPS 가 이 동네 안에 들어오면 동네 소개) ─────────
    val regions = listOf(
        Region(
            "seoulstation", T("서울역·남대문", "Seoul Station & Namdaemun", "首尔站·南大门", "ソウル駅・南大門"), 37.5575, 126.9740, 650f,
            T("서울의 관문이에요. 남대문시장에서는 새벽부터 옷·그릇·길거리 음식을 팔아요.", "Seoul's gateway. Namdaemun Market sells clothes, kitchenware and street food from early morning.", "首尔的门户。南大门市场从清晨就开始卖衣服、餐具和街头小吃。", "ソウルの玄関口。南大門市場では早朝から服や食器、屋台グルメが売られています。"),
            T("남대문시장 갈치조림 골목", "Try the braised cutlassfish alley in Namdaemun Market", "推荐南大门市场的炖带鱼胡同", "南大門市場のタチウオ煮込み横丁がおすすめ"),
        ),
        Region(
            "cityhall", T("시청·덕수궁", "City Hall & Deoksugung", "市厅·德寿宫", "市庁・徳寿宮"), 37.5655, 126.9775, 420f,
            T("서울광장과 덕수궁 돌담길이 있는 곳이에요. 겨울엔 광장에 스케이트장이 열려요.", "Home to Seoul Plaza and the Deoksugung stone-wall road. An ice rink opens on the plaza in winter.", "这里有首尔广场和德寿宫石墙路。冬天广场会开放滑冰场。", "ソウル広場と徳寿宮の石垣道がある場所です。冬は広場にスケートリンクができます。"),
            T("덕수궁 돌담길 산책", "Walk the Deoksugung stone-wall road", "推荐德寿宫石墙路散步", "徳寿宮の石垣道の散歩がおすすめ"),
        ),
        Region(
            "gwanghwamun", T("광화문광장", "Gwanghwamun Square", "光化门广场", "光化門広場"), 37.5725, 126.9769, 480f,
            T("세종대왕과 이순신 장군 동상이 서 있는 서울의 중심 광장이에요. 북쪽 끝에 경복궁이 있어요.", "Seoul's central square with statues of King Sejong and Admiral Yi Sun-sin. Gyeongbokgung sits at the north end.", "首尔的中心广场，矗立着世宗大王和李舜臣将军的铜像，北端就是景福宫。", "世宗大王と李舜臣将軍の像が立つソウルの中心広場。北の端に景福宮があります。"),
            T("세종대왕 동상 아래 박물관은 무료예요", "The museum under the King Sejong statue is free", "世宗大王铜像下的博物馆免费参观", "世宗大王像の下の博物館は無料です"),
        ),
        Region(
            "jongno", T("종로·종각", "Jongno & Jonggak", "钟路·钟阁", "鍾路・鍾閣"), 37.5702, 126.9850, 450f,
            T("조선 시대부터 서울의 큰길이었던 종로예요. 보신각 종과 피맛골 골목 식당이 있어요.", "Jongno has been Seoul's main street since the Joseon era. Look for the Bosingak bell and the old Pimatgol alley eateries.", "钟路自朝鲜时代起就是首尔的大街，有普信阁钟和避马胡同的老餐馆。", "朝鮮時代からソウルの大通りだった鍾路。普信閣の鐘とピマッコル横丁の食堂があります。"),
            T("피맛골 생선구이 골목", "Grilled-fish lanes of Pimatgol", "推荐避马胡同烤鱼街", "ピマッコルの焼き魚横丁がおすすめ"),
        ),
        Region(
            "insadong", T("인사동·익선동", "Insadong & Ikseon-dong", "仁寺洞·益善洞", "仁寺洞・益善洞"), 37.5738, 126.9880, 380f,
            T("전통 공예 거리 인사동과, 1920년대 한옥 골목을 카페로 바꾼 익선동이 붙어 있어요.", "Craft-filled Insadong sits right next to Ikseon-dong, where 1920s hanok lanes became cafés.", "传统工艺街仁寺洞旁边就是益善洞，那里的1920年代韩屋小巷变成了咖啡馆。", "伝統工芸の街・仁寺洞のすぐ隣に、1920年代の韓屋路地をカフェにした益善洞があります。"),
            T("전통 찻집에서 대추차 한 잔", "Have jujube tea at a traditional tea house", "在传统茶馆喝一杯大枣茶", "伝統茶屋でナツメ茶を一杯"),
        ),
        Region(
            "bukchon", T("북촌·삼청동", "Bukchon & Samcheong-dong", "北村·三清洞", "北村・三清洞"), 37.5815, 126.9830, 480f,
            T("경복궁과 창덕궁 사이 언덕에 한옥 마을이 이어져요. 실제로 사람이 사는 동네라 조용히 둘러봐 주세요.", "A hillside hanok village between two palaces. People live here, so please keep voices low.", "两座宫殿之间的山坡上是韩屋村。这里有居民生活，请保持安静。", "二つの宮殿の間の丘に韓屋村が続きます。住民が暮らす町なので静かに見て回りましょう。"),
            T("북촌은 오후 5시 이후 관광 제한 구역이 있어요", "Parts of Bukchon limit visitors after 5 p.m.", "北村部分区域下午5点后限制游客进入", "北村の一部は午後5時以降の観光が制限されます"),
        ),
        Region(
            "seochon", T("서촌", "Seochon", "西村", "西村（ソチョン）"), 37.5790, 126.9700, 430f,
            T("경복궁 서쪽 동네예요. 통인시장 엽전 도시락과 작은 갤러리·카페가 많아요.", "The neighborhood west of Gyeongbokgung, known for Tongin Market's coin lunchbox and small galleries.", "景福宫西边的街区，以通仁市场的铜钱便当和小画廊著称。", "景福宮の西の町。通仁市場の葉銭弁当や小さなギャラリーが有名です。"),
            T("통인시장 엽전 도시락", "Tongin Market coin lunchbox", "推荐通仁市场铜钱便当", "通仁市場の葉銭弁当がおすすめ"),
        ),
        Region(
            "jongno5ga", T("종로5가·광장시장", "Jongno 5-ga & Gwangjang", "钟路5街·广藏市场", "鍾路5街・広蔵市場"), 37.5703, 127.0005, 430f,
            T("광장시장과 원단 도매 상가가 모인 동네예요. 청계천을 따라 동대문까지 걸어갈 수 있어요.", "Home to Gwangjang Market and fabric wholesalers. You can walk along Cheonggyecheon to Dongdaemun.", "广藏市场和布料批发商场集中的地方，可以沿清溪川步行到东大门。", "広蔵市場と生地の問屋街が集まる町。清渓川沿いに東大門まで歩けます。"),
            T("빈대떡은 갈아서 바로 부친 걸로", "Pick bindaetteok ground and fried to order", "绿豆煎饼要选现磨现煎的", "ピンデトックは挽きたて焼きたてを"),
        ),
        Region(
            "euljiro", T("을지로", "Euljiro", "乙支路", "乙支路"), 37.5665, 126.9915, 450f,
            T("인쇄소와 공구 가게 골목 사이에 숨은 술집과 카페가 많아 '힙지로'라고도 불러요.", "Hidden bars and cafés tucked between print shops and tool stores earned it the nickname Hipjiro.", "印刷厂和工具店之间藏着许多酒吧和咖啡馆，被称为“潮支路”。", "印刷所や工具店の路地に隠れたバーやカフェが多く、ヒップチロとも呼ばれます。"),
            T("노가리 골목에서 맥주 한잔", "Beer and dried pollack at Nogari Alley", "在明太鱼干胡同喝杯啤酒", "ノガリ横丁でビールを一杯"),
        ),
        Region(
            "myeongdong", T("명동", "Myeongdong", "明洞", "明洞"), 37.5626, 126.9852, 400f,
            T("화장품 가게와 길거리 음식으로 유명한 쇼핑 거리예요. 저녁이면 거리 가운데 노점이 들어서요.", "A shopping district famous for beauty stores and street food. Stalls fill the middle of the street in the evening.", "以美妆店和街头小吃闻名的购物街，傍晚街道中央会摆满小吃摊。", "コスメ店と屋台グルメで有名なショッピング街。夕方には通りの真ん中に屋台が並びます。"),
            T("저녁 5시 이후 노점 거리", "Street stalls after 5 p.m.", "傍晚5点后的小吃摊街", "夕方5時以降の屋台通り"),
        ),
    )

    /** 사진: 키 → 위키백과 문서 제목 (공개 라이선스 사진, 앱이 자동으로 받아옴) */
    val wiki = mapOf(
        "gwangjang" to "Gwangjang_Market", "insadong" to "Insa-dong", "gyeongbokgung" to "Gyeongbokgung", "myeongdong" to "Myeong-dong",
        "cheonggyecheon" to "Cheonggyecheon", "jongmyo" to "Jongmyo", "jogyesa" to "Jogyesa", "ssamziegil" to "Ssamziegil",
        "palacemuseum" to "National_Palace_Museum_of_Korea", "seochon" to "Seochon", "cathedral" to "Myeongdong_Cathedral",
        "nseoultower" to "N_Seoul_Tower", "bindaetteok" to "Bindae-tteok", "gimbap" to "Gimbap", "yukhoe" to "Yukhoe",
        "gwanghwamun" to "Gwanghwamun", "bosingak" to "Bosingak", "sungnyemun" to "Sungnyemun", "deoksugung" to "Deoksugung",
    )

    /** 시뮬레이션 주행 경로: 서울역에서 출발해 목적지 하차 지점까지 (위도, 경도) */
    fun route(d: Dest, e: Entrance): List<Pair<Double, Double>> {
        val start = listOf(37.5547 to 126.9707, 37.5600 to 126.9753, 37.5663 to 126.9779)   // 서울역 → 숭례문 → 시청
        val via = when (d.id) {
            "gwangjang" -> listOf(37.5705 to 126.9770, 37.5700 to 126.9830, 37.5704 to 126.9920, 37.5710 to 126.9945, 37.5707 to 126.9985)
            "insadong" -> listOf(37.5705 to 126.9770, 37.5700 to 126.9830, 37.5712 to 126.9878)
            "gyeongbokgung" -> listOf(37.5705 to 126.9770, 37.5735 to 126.9769)
            else -> listOf(37.5596 to 126.9790, 37.5606 to 126.9840)
        }
        return start + via + listOf((if (e.lat != 0.0) e.lat else d.lat) to (if (e.lng != 0.0) e.lng else d.lng))
    }

    // ───────── 디지털 키트 (Figma P07 순서) ─────────
    val kitApps = listOf(
        KitApp(
            "KakaoMap",
            T("지도 · 길찾기", "Maps and directions", "地图与导航", "地図・ルート案内"),
            T("장소를 찾고 도보·대중교통 길찾기를 할 수 있어요.", "Find a place and get walking or transit directions.", "查找地点，获取步行或公共交通路线。", "場所を探して、徒歩や公共交通のルートを調べられます。"),
            "net.daum.android.map", "304608425", 0xFFFFE500, "●", 0xFF1E6FFF,
        ),
        KitApp(
            "Kakao T",
            T("택시 · 이동", "Taxis and transport", "出租车与出行", "タクシー・移動"),
            T("택시를 부르고 예상 요금과 기사 정보를 확인할 수 있어요.", "Check taxi fares, call a ride and see your driver.", "叫车、查看预估车费和司机信息。", "タクシーを呼んで、料金の目安やドライバー情報を確認できます。"),
            "com.kakao.taxi", "981110422", 0xFF22263A, "T", 0xFFFFD84D,
        ),
        KitApp(
            "CATCHTABLE Global",
            T("식당 예약", "Restaurant bookings", "餐厅预订", "レストラン予約"),
            T("인기 식당을 예약하거나 원격 줄서기를 할 수 있어요.", "Book popular restaurants or join a waitlist remotely.", "预约热门餐厅或远程排队。", "人気店の予約やリモート順番待ちができます。"),
            "kr.co.catchtable.global.catchtable_global", "1639046576", 0xFFFF5A1F, "C", 0xFFFFFFFF,
        ),
        KitApp(
            "Mobile Tmoney",
            T("교통카드", "Transit card", "交通卡", "交通カード"),
            T("버스·지하철 요금을 휴대폰으로 낼 수 있어요.", "Pay bus and subway fares with your phone.", "用手机支付公交和地铁费用。", "スマホでバス・地下鉄の運賃を払えます。"),
            "com.lgt.tmoney", "1470361790", 0xFFFF7A1A, "T", 0xFFFFFFFF,
        ),
    )

    // ───────── 길거리 음식 (시연용 예시) ─────────
    val foods = listOf(
        Food(
            T("기사 픽", "Driver's pick", "司机推荐", "運転手のおすすめ"),
            T("빈대떡", "Mung bean pancake", "绿豆煎饼", "ピンデトック"), 5000,
            T("바로 먹을 수 있어요", "No wait", "无需排队", "すぐ食べられます"), "bindaetteok",
        ),
        Food(
            T("지역 인기", "Local favorite", "本地人气", "地元で人気"),
            T("마약김밥", "Mayak gimbap (mini rice rolls)", "麻药紫菜包饭", "麻薬キンパ"), 4000,
            T("포장 가능", "Takeaway available", "可外带", "持ち帰り可"), "gimbap",
        ),
        Food(
            T("여행자 최다 주문", "Most ordered by travelers", "游客最爱", "旅行者に人気"),
            T("육회", "Yukhoe (Korean beef tartare)", "生拌牛肉", "ユッケ"), 18000,
            T("대기 20분", "About a 20-min wait", "排队约20分钟", "待ち約20分"), "yukhoe",
        ),
    )

    /** 실제로 열리는 Google 지도 검색 링크 (공식 Maps URL 형식) */
    fun mapsSearch(query: String): String =
        "https://www.google.com/maps/search/?api=1&query=" + java.net.URLEncoder.encode(query, "UTF-8")

    // ───────── 환율 (Figma 표기: 예시값 Sample, 실서비스는 실시간 API) ─────────
    fun currencyOf(l: Lang): Currency? = when (l) {
        Lang.KO -> null
        // 실시간 환율을 받았으면 그 값, 아직이면 예시값
        Lang.EN -> Currency("$", "USD", Rates.krwPer("USD") ?: 1380.0, 2, 1)
        Lang.ZH -> Currency("¥", "CNY", Rates.krwPer("CNY") ?: 190.0, 1, 1)
        Lang.JA -> Currency("¥", "JPY", Rates.krwPer("JPY") ?: 9.0, 0, 100)
    }
}
