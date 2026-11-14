package com.taxiconcierge

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * ──────────────────────────────────────────────────────────────────────────
 *  Live — 인터넷·GPS로 실시간 바뀌는 것들
 *   ① Rates     : 실시간 환율 (ExchangeRate-API, 하루 1번 갱신되는 기준 환율)
 *   ② PlaceImages : 장소·음식 사진 (위키백과 대표 사진 = 위키미디어 공용 공개 라이선스)
 *   ③ Tiles/RealMap : 진짜 지도 (OpenStreetMap) 를 홀로그램 색으로 물들여 그림
 *   ④ DriveSim  : 서울역 → 목적지 하차 지점까지 GPS 주행 시뮬레이션
 *  인터넷이 안 되면 전부 예시값·그림으로 자동 대체 → 시연이 멈추지 않음
 * ──────────────────────────────────────────────────────────────────────────
 */

private const val TAG = "TAXI"

object Net {
    private const val UA = "HyCierge/1.0 (Pleos Playground contest demo app)"

    /** 주소에서 바이트 받기 (실패하면 null) */
    fun get(url: String, timeoutMs: Int = 8000): ByteArray? {
        var u = url
        repeat(4) {
            try {
                val c = (URL(u).openConnection() as HttpURLConnection).apply {
                    connectTimeout = timeoutMs
                    readTimeout = timeoutMs
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", UA)
                }
                val code = c.responseCode
                if (code in 300..399) {
                    val loc = c.getHeaderField("Location")
                    c.disconnect()
                    if (loc == null) return null
                    u = URL(URL(u), loc).toString()
                    return@repeat
                }
                if (code != 200) {
                    c.disconnect(); return null
                }
                val bytes = c.inputStream.use { it.readBytes() }
                c.disconnect()
                return bytes
            } catch (t: Throwable) {
                Log.w(TAG, "인터넷 받기 실패 $u : ${t.message}")
                return null
            }
        }
        return null
    }
}

// ───────────────────────── ① 실시간 환율 ─────────────────────────

object Rates {
    /** 통화 1단위가 몇 원인지 (USD, CNY, JPY) */
    private var map by mutableStateOf<Map<String, Double>>(emptyMap())
    /** 갱신 시각 (예: 11/14 09:00) */
    var updated by mutableStateOf<String?>(null)
        private set

    val live: Boolean get() = map.isNotEmpty()
    fun krwPer(code: String): Double? = map[code]

    /**
     * USD 기준으로 받아서 원화로 바꿈 (원화 기준으로 받으면 유효숫자가 3자리라 오차가 커짐)
     * 1 USD = KRW 원 / 1 CNY = KRW ÷ CNY 원 / 1 JPY = KRW ÷ JPY 원
     */
    suspend fun refresh(ctx: Context) {
        val cache = File(ctx.filesDir, "rates.json")
        val body = withContext(Dispatchers.IO) {
            Net.get("https://open.er-api.com/v6/latest/USD")?.toString(Charsets.UTF_8)?.also {
                runCatching { cache.writeText(it) }
            } ?: runCatching { cache.readText() }.getOrNull()
        } ?: return
        try {
            val o = JSONObject(body)
            if (o.optString("result") != "success") return
            val r = o.getJSONObject("rates")
            val krw = r.getDouble("KRW")
            map = mapOf("USD" to krw, "CNY" to krw / r.getDouble("CNY"), "JPY" to krw / r.getDouble("JPY"))
            val t = o.optLong("time_last_update_unix", 0L)
            updated = if (t > 0) SimpleDateFormat("MM/dd HH:mm", Locale.US).format(Date(t * 1000)) else null
            Log.d(TAG, "실시간 환율: $map ($updated)")
        } catch (t: Throwable) {
            Log.w(TAG, "환율 해석 실패", t)
        }
    }
}

// ───────────────────────── ② 장소·음식 사진 ─────────────────────────

object PlaceImages {
    private val mem = HashMap<String, ImageBitmap>()
    fun cached(key: String): ImageBitmap? = mem[key]

    suspend fun load(ctx: Context, key: String): ImageBitmap? = withContext(Dispatchers.IO) {
        mem[key]?.let { return@withContext it }
        val file = File(ctx.filesDir, "img/$key.jpg")
        var bytes: ByteArray? = if (file.exists()) runCatching { file.readBytes() }.getOrNull() else null
        if (bytes == null) {
            val title = Content.wiki[key] ?: return@withContext null
            val js = Net.get("https://en.wikipedia.org/api/rest_v1/page/summary/$title") ?: return@withContext null
            val o = runCatching { JSONObject(js.toString(Charsets.UTF_8)) }.getOrNull() ?: return@withContext null
            val thumb = o.optJSONObject("thumbnail")?.optString("source")?.takeIf { it.isNotBlank() }
            val orig = o.optJSONObject("originalimage")?.optString("source")?.takeIf { it.isNotBlank() }
            val big = thumb?.replace(Regex("/\\d+px-"), "/960px-")
            bytes = listOfNotNull(big, thumb, orig).firstNotNullOfOrNull { Net.get(it) }
            bytes?.let { b -> runCatching { file.parentFile?.mkdirs(); file.writeBytes(b) } }
        }
        val b = bytes ?: return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(b, 0, b.size, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > 1400) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(b, 0, b.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return@withContext null
        bmp.asImageBitmap().also { mem[key] = it }
    }
}

/** 사진 가져오기: ① 앱에 미리 넣은 사진(res/drawable-nodpi/img_키.jpg) ② 인터넷(위키백과) ③ 없으면 null */
@Composable
fun rememberPlaceImage(key: String): ImageBitmap? {
    val bundled = if (key.isBlank()) null else figmaImage("img_$key")
    val ctx = LocalContext.current
    val net by produceState(PlaceImages.cached(key), key, bundled) {
        if (bundled == null && value == null && key.isNotBlank()) value = PlaceImages.load(ctx, key)
    }
    return bundled ?: net
}

/** 사진 칸: 진짜 사진이 오면 부드럽게 바뀜 + 오른쪽 아래 출처 */
@Composable
fun PlacePhoto(key: String, seed: Int, modifier: Modifier, credit: Boolean = true) {
    val img = rememberPlaceImage(key)
    val a by animateFloatAsState(if (img != null) 1f else 0f, tween(600), label = "photoIn")
    Box(modifier.clipToBounds()) {
        Photo(seed, Modifier.fillMaxSize())
        if (img != null) {
            Image(img, contentDescription = null, contentScale = ContentScale.Crop, alpha = a, modifier = Modifier.fillMaxSize())
            if (credit) Box(
                Modifier.align(Alignment.BottomEnd).padding(8.u).clip(RoundedCornerShape(6.u)).background(Color(0x99000000))
                    .padding(horizontal = 8.u, vertical = 2.u),
            ) { Tx("Photo · Wikimedia Commons", 11, Color(0xCCFFFFFF)) }
        }
    }
}

// ───────────────────────── ③ 진짜 지도 (OpenStreetMap) ─────────────────────────

object Geo {
    fun wx(lng: Double, z: Int): Double = (lng + 180.0) / 360.0 * 256.0 * 2.0.pow(z)
    fun wy(lat: Double, z: Int): Double {
        val s = sin(lat * PI / 180.0).coerceIn(-0.9999, 0.9999)
        return (0.5 - ln((1 + s) / (1 - s)) / (4 * PI)) * 256.0 * 2.0.pow(z)
    }

    /** 진행 방향 (북=0°, 동=90°) */
    fun bearing(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Float {
        val dy = bLat - aLat
        val dx = (bLng - aLng) * cos(aLat * PI / 180.0)
        return ((atan2(dx, dy) * 180.0 / PI + 360.0) % 360.0).toFloat()
    }

    /** 점들이 화면에 다 들어오는 줌 */
    fun fitZoom(pts: List<Pair<Double, Double>>, wPx: Float, hPx: Float, minZ: Double, maxZ: Double): Double {
        if (pts.size < 2) return maxZ
        val xs = pts.map { wx(it.second, 0) }
        val ys = pts.map { wy(it.first, 0) }
        val spanX = (xs.max() - xs.min()).coerceAtLeast(1e-9)
        val spanY = (ys.max() - ys.min()).coerceAtLeast(1e-9)
        val z = min(log2(wPx * 0.62 / spanX), log2(hPx * 0.62 / spanY))
        return z.coerceIn(minZ, maxZ)
    }
}

/**
 * 앱 안에 넣어 둔 서울 도심 지도 (큰길·물길·궁궐·공원). 인터넷을 전혀 쓰지 않아 차량 위치가 밖으로 나가지 않음.
 * 좌표는 주요 지점을 이은 대략적인 선 (시연용 안내 지도)
 */
object CityMap {
    data class Line(val name: T, val pts: List<Pair<Double, Double>>, val major: Boolean = true)
    data class Area(val name: T, val s: Double, val w: Double, val n: Double, val e: Double, val park: Boolean = false)

    val roads = listOf(
        Line(T("세종대로", "Sejong-daero", "世宗大路", "世宗大路"), listOf(37.5770 to 126.9769, 37.5705 to 126.9770, 37.5663 to 126.9779, 37.5600 to 126.9753, 37.5547 to 126.9707)),
        Line(T("종로", "Jong-ro", "钟路", "鍾路"), listOf(37.5705 to 126.9770, 37.5701 to 126.9830, 37.5704 to 126.9919, 37.5708 to 126.9980, 37.5709 to 127.0018, 37.5713 to 127.0095)),
        Line(T("을지로", "Euljiro", "乙支路", "乙支路"), listOf(37.5660 to 126.9785, 37.5660 to 126.9826, 37.5663 to 126.9917, 37.5667 to 126.9980, 37.5668 to 127.0080)),
        Line(T("퇴계로", "Toegye-ro", "退溪路", "退渓路"), listOf(37.5575 to 126.9735, 37.5588 to 126.9790, 37.5609 to 126.9863, 37.5612 to 126.9940, 37.5650 to 127.0080)),
        Line(T("남대문로", "Namdaemun-ro", "南大门路", "南大門路"), listOf(37.5600 to 126.9753, 37.5625 to 126.9790, 37.5660 to 126.9826, 37.5701 to 126.9830)),
        Line(T("삼일대로", "Samil-daero", "三一大路", "三一大路"), listOf(37.5765 to 126.9855, 37.5703 to 126.9878, 37.5662 to 126.9880, 37.5610 to 126.9890)),
        Line(T("율곡로", "Yulgok-ro", "栗谷路", "栗谷路"), listOf(37.5765 to 126.9790, 37.5765 to 126.9855, 37.5767 to 126.9910, 37.5745 to 127.0000)),
        Line(T("창경궁로", "Changgyeonggung-ro", "昌庆宫路", "昌慶宮路"), listOf(37.5785 to 126.9988, 37.5708 to 126.9988, 37.5667 to 126.9985, 37.5612 to 126.9975)),
        Line(T("사직로", "Sajik-ro", "社稷路", "社稷路"), listOf(37.5759 to 126.9769, 37.5757 to 126.9735, 37.5755 to 126.9680)),
        Line(T("인사동길", "Insadong-gil", "仁寺洞街", "仁寺洞キル"), listOf(37.5754 to 126.9852, 37.5735 to 126.9862, 37.5718 to 126.9873), major = false),
        Line(T("명동길", "Myeongdong-gil", "明洞街", "明洞キル"), listOf(37.5651 to 126.9831, 37.5636 to 126.9850, 37.5609 to 126.9861), major = false),
    )
    val stream = Line(T("청계천", "Cheonggyecheon", "清溪川", "清渓川"), listOf(37.5692 to 126.9784, 37.5690 to 126.9830, 37.5688 to 126.9900, 37.5693 to 126.9990, 37.5700 to 127.0080))
    val areas = listOf(
        Area(T("경복궁", "Gyeongbokgung", "景福宫", "景福宮"), 37.5762, 126.9740, 37.5845, 126.9800),
        Area(T("창덕궁", "Changdeokgung", "昌德宫", "昌徳宮"), 37.5770, 126.9890, 37.5830, 126.9960),
        Area(T("종묘", "Jongmyo", "宗庙", "宗廟"), 37.5715, 126.9915, 37.5770, 126.9965),
        Area(T("덕수궁", "Deoksugung", "德寿宫", "徳寿宮"), 37.5645, 126.9735, 37.5670, 126.9765),
        Area(T("남산", "Namsan", "南山", "南山"), 37.5470, 126.9800, 37.5570, 126.9960, park = true),
        Area(T("북악산", "Bugaksan", "北岳山", "北岳山"), 37.5880, 126.9650, 37.5960, 126.9900, park = true),
    )

    /** 지도 그리기: 바닥 격자 → 공원·궁궐 → 물길 → 큰길 → 이름 */
    fun DrawScope.drawCity(p: MapProj, lang: Lang, centerLat: Double, labelPx: Float) {
        val cyan = Color(0xFF7FE3FF)
        val m = p.pxPerMeter(centerLat)
        // 동네 블록 느낌의 가는 격자 (약 120m 간격)
        val step = (120f * m).coerceAtLeast(14f)
        var x = (size.width / 2) % step
        while (x < size.width) { drawLine(cyan.copy(alpha = 0.05f), Offset(x, 0f), Offset(x, size.height), 1f); x += step }
        var y = (size.height / 2) % step
        while (y < size.height) { drawLine(cyan.copy(alpha = 0.05f), Offset(0f, y), Offset(size.width, y), 1f); y += step }
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER; textSize = labelPx
            setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
        }
        for (a in areas) {
            val tl = p.at(a.n, a.w)
            val br = p.at(a.s, a.e)
            val col = if (a.park) Color(0xFF6FE0B0) else cyan
            drawRect(col.copy(alpha = if (a.park) 0.07f else 0.10f), tl, androidx.compose.ui.geometry.Size(br.x - tl.x, br.y - tl.y))
            drawRect(col.copy(alpha = 0.35f), tl, androidx.compose.ui.geometry.Size(br.x - tl.x, br.y - tl.y), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
            paint.color = android.graphics.Color.argb(170, 190, 240, 255)
            drawContext.canvas.nativeCanvas.drawText(a.name[lang], (tl.x + br.x) / 2, (tl.y + br.y) / 2, paint)
        }
        fun path(l: Line) = androidx.compose.ui.graphics.Path().apply {
            l.pts.forEachIndexed { i, (la, ln) -> val o = p.at(la, ln); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
        }
        val sp = path(stream)
        drawPath(sp, Color(0xFF4FA8FF).copy(alpha = 0.18f), style = androidx.compose.ui.graphics.drawscope.Stroke((26f * m).coerceIn(6f, 30f), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawPath(sp, Color(0xFF8FD0FF).copy(alpha = 0.6f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        for (r in roads) {
            val rp = path(r)
            drawPath(rp, cyan.copy(alpha = 0.10f), style = androidx.compose.ui.graphics.drawscope.Stroke(if (r.major) (30f * m).coerceIn(8f, 34f) else 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawPath(rp, cyan.copy(alpha = if (r.major) 0.6f else 0.35f), style = androidx.compose.ui.graphics.drawscope.Stroke(if (r.major) 2.4f else 1.4f))
        }
        paint.textSize = labelPx * 0.85f
        paint.color = android.graphics.Color.argb(150, 160, 225, 245)
        for (r in roads + stream) {
            val (la, ln) = r.pts[r.pts.size / 2]
            val o = p.at(la, ln)
            drawContext.canvas.nativeCanvas.drawText(r.name[lang], o.x, o.y - 8f, paint)
        }
    }
}

object Tiles {
    /** false = 인터넷 지도 타일을 쓰지 않음 (차량 위치 보호). 앱 안 지도만 사용 */
    const val ONLINE = false

    private val mem = object : LinkedHashMap<String, ImageBitmap>(96, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?) = size > 160
    }

    fun cached(k: String): ImageBitmap? = synchronized(mem) { mem[k] }

    suspend fun load(ctx: Context, z: Int, x: Int, y: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        val k = "$z/$x/$y"
        cached(k)?.let { return@withContext it }
        val file = File(ctx.filesDir, "tiles/${z}_${x}_$y.png")
        val bytes = (if (file.exists()) runCatching { file.readBytes() }.getOrNull() else null)
            ?: Net.get("https://tile.openstreetmap.org/$z/$x/$y.png", 6000)?.also { b -> runCatching { file.parentFile?.mkdirs(); file.writeBytes(b) } }
            ?: return@withContext null
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
        bmp.asImageBitmap().also { synchronized(mem) { mem[k] = it } }
    }
}

/** 밝은 OSM 지도 → 어두운 바탕에 청록색으로 빛나는 홀로그램 지도 (글자·건물은 밝게, 빈 땅은 어둡게) */
private val holoFilter: ColorFilter = run {
    val k = 1.7f
    val (cr, cg, cb) = Triple(0.42f, 0.86f, 1.0f)
    fun row(c: Float) = floatArrayOf(-0.299f * k * c, -0.587f * k * c, -0.114f * k * c, 0f, 255f * k * c)
    ColorFilter.colorMatrix(ColorMatrix(row(cr) + row(cg) + row(cb) + floatArrayOf(0f, 0f, 0f, 1f, 0f)))
}

/** 위도·경도 → 화면 좌표 변환기 */
class MapProj(private val cx: Double, private val cy: Double, private val z: Int, private val scale: Double, private val w: Float, private val h: Float) {
    fun at(lat: Double, lng: Double): Offset =
        Offset(((Geo.wx(lng, z) - cx) * scale + w / 2).toFloat(), ((Geo.wy(lat, z) - cy) * scale + h / 2).toFloat())

    /** 1미터가 화면에서 몇 픽셀인지 (대략) */
    fun pxPerMeter(lat: Double): Float = (256.0 * 2.0.pow(z) * scale / (40075016.686 * cos(lat * PI / 180.0))).toFloat()
}

/**
 * 진짜 지도 한 장.
 * @param fit  이 점들이 다 보이게 중심·줌을 자동으로 맞춤 (null 이면 center·zoom 사용)
 * @param overlay 지도 위에 그릴 것 (자동차, 하차 지점, 명소 점 등)
 */
@Composable
fun RealMap(
    centerLat: Double, centerLng: Double, zoom: Double, modifier: Modifier,
    fit: List<Pair<Double, Double>>? = null, minZoom: Double = 13.0, maxZoom: Double = 18.0,
    holo: Boolean = true,
    lang: Lang = Lang.EN,
    overlay: DrawScope.(MapProj) -> Unit = {},
) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    BoxWithConstraints(modifier.clipToBounds()) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        // 목표 중심·줌 (fit 이 있으면 점들을 다 담게)
        val tLat: Double; val tLng: Double; val tZoom: Double
        if (fit != null && fit.isNotEmpty()) {
            tLat = (fit.maxOf { it.first } + fit.minOf { it.first }) / 2
            tLng = (fit.maxOf { it.second } + fit.minOf { it.second }) / 2
            tZoom = Geo.fitZoom(fit, wPx, hPx, minZoom, maxZoom)
        } else {
            tLat = centerLat; tLng = centerLng; tZoom = zoom
        }
        // 부드럽게 따라감 (GPS 가 2초마다 와도 툭툭 튀지 않게)
        val aLat by animateFloatAsState((tLat - 37.0).toFloat(), tween(1200), label = "mapLat")
        val aLng by animateFloatAsState((tLng - 126.0).toFloat(), tween(1200), label = "mapLng")
        val aZoom by animateFloatAsState(tZoom.toFloat(), tween(1200), label = "mapZoom")
        val lat = 37.0 + aLat
        val lng = 126.0 + aLng
        // 지도 글자가 너무 작지 않게: 화면 배율에 맞춰 한 단계 위 타일을 크게 씀
        val z = floor(aZoom.toDouble()).toInt().coerceIn(3, 19)
        val scale = 2.0.pow(aZoom.toDouble() - z) * (density.density / 1.6).coerceIn(0.8, 1.6)
        val cx = Geo.wx(lng, z)
        val cy = Geo.wy(lat, z)
        val proj = MapProj(cx, cy, z, scale, wPx, hPx)
        // 필요한 타일 목록
        val tilePx = 256.0 * scale
        val x0 = floor((cx - wPx / 2 / scale) / 256).toInt()
        val x1 = floor((cx + wPx / 2 / scale) / 256).toInt()
        val y0 = floor((cy - hPx / 2 / scale) / 256).toInt()
        val y1 = floor((cy + hPx / 2 / scale) / 256).toInt()
        val need = if (!Tiles.ONLINE) emptyList() else buildList { for (x in x0..x1) for (y in y0..y1) add(Triple(z, x, y)) }
        val labelPx = with(density) { 15.u.toPx() }
        val got = remember { mutableStateMapOf<String, ImageBitmap>() }
        LaunchedEffect(need) {
            for ((tz, tx, ty) in need) {
                val k = "$tz/$tx/$ty"
                if (got[k] == null) {
                    Tiles.cached(k)?.let { got[k] = it } ?: launch { Tiles.load(ctx, tz, tx, ty)?.let { got[k] = it } }
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0xFF06131D))
            for ((tz, tx, ty) in need) {
                val img = got["$tz/$tx/$ty"] ?: continue
                val ox = ((tx * 256.0 - cx) * scale + wPx / 2)
                val oy = ((ty * 256.0 - cy) * scale + hPx / 2)
                val sz = tilePx.roundToInt() + 1
                drawImage(
                    img, dstOffset = IntOffset(ox.roundToInt(), oy.roundToInt()), dstSize = IntSize(sz, sz),
                    alpha = if (holo) 0.8f else 1f, colorFilter = if (holo) holoFilter else null, blendMode = if (holo) BlendMode.Screen else BlendMode.SrcOver,
                )
            }
            if (!Tiles.ONLINE) with(CityMap) { drawCity(proj, lang, lat, labelPx) }
            overlay(proj)
        }
        Tx(if (Tiles.ONLINE) "© OpenStreetMap contributors" else "Hy-Cierge offline map", 11, Color(0x99CFEFFF), modifier = Modifier.align(Alignment.BottomStart).padding(8.u))
    }
}

// ───────────────────────── ④ GPS 주행 시뮬레이션 ─────────────────────────

/** DEMO 패널 '실제 경로 주행' → 서울역에서 목적지 하차 지점까지 GPS 좌표를 0.65초마다 22m씩 보냄 */
@Composable
fun DriveSim(s: AppState) {
    LaunchedEffect(s.simDriving) {
        if (!s.simDriving) return@LaunchedEffect
        s.startRiding()
        val pts = Content.route(s.dest, s.entr).dropLast(1)
        var cur = pts.first()
        s.updatePosition(cur.first, cur.second, "SIM")
        suspend fun goTo(target: () -> Pair<Double, Double>, stopAt: Float) {
            while (s.simDriving) {
                // Virtual Console 연동: P·N 이거나 속도 0 이면 그 자리에 멈춰 있음, 속도가 빠르면 더 많이 감 (화면은 3배속)
                var step = 22f
                if (s.simAuto) {
                    val g = s.demoGear ?: s.carGear
                    val v = s.carSpeedMs
                    if (g == Gear.PARK || g == Gear.NEUTRAL || (v != null && v < 0.3f)) { delay(400); continue }
                    if (v != null) step = (v * 0.65f * 3f).coerceIn(6f, 60f)
                }
                val t = target()
                val d = distanceM(cur.first, cur.second, t.first, t.second)
                if (d <= stopAt) return
                val f = (step / d).coerceAtMost(1f).toDouble()
                cur = (cur.first + (t.first - cur.first) * f) to (cur.second + (t.second - cur.second) * f)
                delay(650)
                s.updatePosition(cur.first, cur.second, "SIM")
            }
        }
        for (p in pts.drop(1)) goTo({ p }, 4f)
        // 마지막 구간: 하차 지점이 바뀌면 바뀐 곳으로 다시 감 (정밀 하차 실시간 반영)
        goTo({ val e = s.entr; (if (e.lat != 0.0) e.lat else s.dest.lat) to (if (e.lng != 0.0) e.lng else s.dest.lng) }, 6f)
        s.simDriving = false
        s.simAuto = false
    }
    // Virtual Console 에서 기어를 D 로 바꾸면 → 목적지가 정해진 운행이면 실제 경로 주행을 자동으로 시작
    LaunchedEffect(s.carGear, s.riding) {
        val g = s.carGear ?: return@LaunchedEffect
        if (g != Gear.PARK && g != Gear.NEUTRAL && s.riding && !s.simDriving && !s.arriving) {
            s.simAuto = true
            s.simDriving = true
        }
    }
}

// ───────────────────────── 실시간 기능 문구 ─────────────────────────

object LT {
    val aroundYou = T("지금 내 주변", "Around you now", "我附近", "いま周辺")
    val aroundSub = T("GPS로 지금 지나는 동네와 가까운 명소를 실시간으로 골라 드려요.", "GPS picks the neighborhood you're in and the closest sights in real time.", "通过GPS实时为您挑选所在街区和最近的景点。", "GPSで今いる町と近くの名所をリアルタイムで選びます。")
    val nowIn = T("지금 지나는 동네", "You're in", "您正在经过", "いま通っているのは")
    val localTip = T("동네 팁", "Local tip", "本地贴士", "地元のヒント")
    val waitingGps = T("GPS 신호를 기다리는 중", "Waiting for GPS", "正在等待GPS信号", "GPS信号を待っています")
    val startSim = T("서울역에서 주행 시작 (시뮬레이션)", "Start a drive from Seoul Station (simulation)", "从首尔站开始行驶（模拟）", "ソウル駅から走行開始（シミュレーション）")
    val closest = T("가까운 명소", "Closest sights", "最近的景点", "近くの名所")
    val enter = T("%s에 들어왔어요", "Now entering %s", "已进入%s", "%sに入りました")
    val liveGps = T("실시간 GPS", "LIVE GPS", "实时GPS", "リアルタイムGPS")
    val simGps = T("시뮬레이션 GPS", "SIMULATED GPS", "模拟GPS", "シミュレーションGPS")
    val toDrop = T("하차 지점까지", "To your drop-off", "距下车点", "降車地点まで")
    val byCar = T("차로 약 %s분", "About %s min by car", "车程约%s分钟", "車で約%s分")
    val arrivedAt = T("하차 지점 도착", "At your drop-off", "已到达下车点", "降車地点に到着")
    val select = T("선택", "Select", "选择", "選択")
    // 탑승 연결 (QR + 관심사)
    val connectTitle = T("휴대폰 연결하고 관심사 고르기", "Connect your phone and pick your interests", "连接手机并选择兴趣", "スマホをつないで興味を選ぶ")
    val connectSub = T("회원가입 없이 스캔만 하면 이번 여정(목적지·하차 지점)이 휴대폰에 열려요.", "No sign-up. Scan to open this ride's destination and drop-off on your phone.", "无需注册，扫码即可在手机上打开本次行程的目的地和下车点。", "会員登録不要。スキャンするとこの乗車の目的地と降車地点がスマホで開きます。")
    val scanRide = T("이번 여정 열기", "Open this ride", "打开本次行程", "この乗車を開く")
    val interests = T("이번 탑승에서 궁금한 것", "What interests you on this ride?", "本次乘车感兴趣的内容", "この乗車で気になること")
    val interestHint = T("고른 관심사에 맞춰 주변 명소와 이야기 순서를 바꿔 드려요.", "We'll put matching sights and stories first.", "我们会优先推荐相关的景点和故事。", "興味に合う名所と話を先にご案内します。")
    val start = T("시작하기", "Let's go", "开始", "はじめる")
    val catHistory = T("역사·궁궐", "History & palaces", "历史·宫殿", "歴史・宮殿")
    val catFood = T("음식·시장", "Food & markets", "美食·市场", "グルメ・市場")
    val catShop = T("쇼핑·공예", "Shopping & crafts", "购物·工艺", "ショッピング・工芸")
    val catWalk = T("산책·풍경", "Walks & views", "散步·风景", "散歩・景色")
    val catCulture = T("종교·문화", "Faith & culture", "宗教·文化", "宗教・文化")
    val fromDrop = T("하차 지점에서 %s", "%s from your drop-off", "距下车点%s", "降車地点から%s")
    val bookAlt = T("한국 휴대폰 번호가 필요한 예약이면 현장 대기나 원격 줄서기를 이용하세요.", "If a booking asks for a Korean phone number, use walk-in or the remote waitlist instead.", "如果预约需要韩国手机号，请改用现场排队或远程排号。", "予約に韓国の電話番号が必要な場合は、店頭待ちかリモート順番待ちを使いましょう。")
    val forYou = T("관심사 추천", "For you", "为你推荐", "あなた向け")
    val liveRate = T("실시간 환율", "Live rate", "实时汇率", "リアルタイムレート")
    val rateSource = T("ExchangeRate-API 기준 환율 · %s 갱신 · 카드 수수료는 따로 붙을 수 있어요.", "Reference rate by ExchangeRate-API · updated %s · Card fees may apply.", "ExchangeRate-API参考汇率 · %s更新 · 可能另收刷卡手续费。", "ExchangeRate-API の参考レート · %s 更新 · カード手数料が別途かかる場合があります。")
    // 라이더 광장
    val plaza = T("라이더 광장", "Rider Plaza", "乘客广场", "ライダー広場")
    val plazaSub = T("지금 서울에서 택시를 탄 여행자들이 모이는 곳이에요. 위치는 동네 단위로만 보여요.", "Where travelers riding taxis in Seoul right now meet up. Locations show only at neighborhood level.", "此刻在首尔乘坐出租车的游客聚集在这里。位置只显示到街区。", "いまソウルでタクシーに乗っている旅行者が集まる場所。位置は町単位でのみ表示されます。")
    val plazaCard = T("라이더 광장", "Rider Plaza", "乘客广场", "ライダー広場")
    val ridersNow = T("지금 %s명 접속 중", "%s riders here now", "现在%s人在线", "いま%s人が参加中")
    val trending = T("지금 많이 가는 곳", "Trending now", "现在热门", "いま人気の行き先")
    val chat = T("광장 대화 · 자동 번역", "Plaza chat · auto-translated", "广场聊天 · 自动翻译", "広場チャット · 自動翻訳")
    val riders = T("%s명", "%s riders", "%s人", "%s人")
    val you = T("나", "You", "我", "あなた")
    val from = T("%s에서 온 여행자", "Rider from %s", "来自%s的乘客", "%sから来た旅行者")
    val demoNote = T("시연용 가상 라이더", "Demo · simulated riders", "演示 · 模拟乘客", "デモ · 仮想ライダー")
    val stamp = T("여기 왔어요 도장 찍기", "Leave a stamp here", "在这里盖章", "ここにスタンプ")
    val reactHi = T("👋 안녕하세요!", "👋 Hi!", "👋 大家好！", "👋 こんにちは！")
    val reactTip = T("🍜 맛집 추천?", "🍜 Food tips?", "🍜 有好吃的？", "🍜 おすすめは？")
    val reactHere = T("📍 가는 중이에요", "📍 On my way!", "📍 正在路上", "📍 向かってます")
}

/** 라이더 광장 채팅 (시연용 가상 대화, 출신 나라 + 4개 언어) */
data class PlazaMsg(val from: T, val text: T)

val plazaFeed = listOf(
    PlazaMsg(T("일본", "Japan", "日本", "日本"), T("광장시장 빈대떡 줄 10분 정도예요!", "The bindaetteok line at Gwangjang is about 10 min!", "广藏市场绿豆煎饼排队大概10分钟！", "広蔵市場のピンデトック、待ち10分くらいです！")),
    PlazaMsg(T("미국", "the US", "美国", "アメリカ"), T("경복궁 수문장 교대식 곧 시작해요", "The guard ceremony at Gyeongbokgung is starting soon", "景福宫守门将交接仪式马上开始", "景福宮の守門将交代式がもうすぐ始まります")),
    PlazaMsg(T("중국", "China", "中国", "中国"), T("명동 노점 지금 막 열었어요", "Myeongdong street stalls just opened", "明洞小吃摊刚开张", "明洞の屋台がちょうど開きました")),
    PlazaMsg(T("프랑스", "France", "法国", "フランス"), T("인사동 찻집 조용하고 좋아요", "Tea houses in Insadong are quiet and lovely", "仁寺洞的茶馆安静又舒服", "仁寺洞の茶屋、静かで素敵です")),
    PlazaMsg(T("대만", "Taiwan", "台湾", "台湾"), T("청계천 야경 꼭 보세요", "Don't miss Cheonggyecheon at night", "一定要看清溪川夜景", "清渓川の夜景はぜひ")),
    PlazaMsg(T("호주", "Australia", "澳大利亚", "オーストラリア"), T("광장시장 동쪽 입구가 덜 붐벼요", "The food alley entrance at Gwangjang is less crowded", "广藏市场美食街入口人比较少", "広蔵市場は屋台通りの入口が空いています")),
)

/** 라이더 광장 가상 라이더: 출신 언어 코드 + 오가는 두 지점 + 속도 */
data class Rider(val code: String, val aLat: Double, val aLng: Double, val bLat: Double, val bLng: Double, val speed: Float, val phase: Float)

val plazaRiders: List<Rider> = run {
    val spots = Content.dests.map { it.lat to it.lng } + Content.allPois.map { it.lat to it.lng }
    val codes = listOf("JP", "US", "CN", "FR", "TW", "AU", "DE", "TH", "VN", "GB", "ES", "SG")
    val r = java.util.Random(7L)
    List(22) { i ->
        val a = spots[r.nextInt(spots.size)]
        val b = Content.dests[r.nextInt(Content.dests.size)].let { it.lat to it.lng }
        Rider(codes[i % codes.size], a.first, a.second, b.first + (r.nextDouble() - 0.5) * 0.002, b.second + (r.nextDouble() - 0.5) * 0.002, 0.03f + r.nextFloat() * 0.05f, r.nextFloat())
    }
}
