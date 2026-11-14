package com.taxiconcierge

import android.annotation.SuppressLint
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.speech.tts.TextToSpeech
import ai.pleos.playground.tts.TextToSpeech as PleosTTS
import ai.pleos.playground.tts.constant.Mode as PleosMode
import ai.pleos.playground.tts.constant.ErrorType as PleosErrorType
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.Locale

private const val TAG = "TAXI"

/** AAOS 기어 값 (VehicleGear) */
object Gear {
    const val NEUTRAL = 1
    const val REVERSE = 2
    const val PARK = 4
    const val DRIVE = 8

    fun label(g: Int?): String = when (g) {
        PARK -> "P"
        REVERSE -> "R"
        NEUTRAL -> "N"
        null -> "-"
        else -> "D"
    }
}

/**
 * 기어·속도를 AAOS Car API(CarPropertyManager)로 받는다.
 * 에뮬레이터에서는 adb로 VHAL 값을 넣으면 바로 반영된다.
 * 차량 API가 없으면 start()가 false를 돌려주고, 앱은 시연(DEMO) 모드로 계속 돈다.
 */
class DriveMonitor(
    private val context: Context,
    private val onGear: (Int) -> Unit,
    private val onSpeed: (Float) -> Unit,
) {
    private var car: Car? = null
    private var manager: CarPropertyManager? = null

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            when (value.propertyId) {
                VehiclePropertyIds.GEAR_SELECTION, VehiclePropertyIds.CURRENT_GEAR ->
                    (value.value as? Int)?.let(onGear)
                VehiclePropertyIds.PERF_VEHICLE_SPEED ->
                    (value.value as? Float)?.let(onSpeed)
            }
        }

        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w(TAG, "차량 속성 오류 propId=$propId")
        }
    }

    @Suppress("DEPRECATION")
    fun start(): Boolean {
        return try {
            val c = Car.createCar(context) ?: return false
            car = c
            val m = c.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            manager = m
            var ok = false
            for ((prop, rate) in listOf(
                VehiclePropertyIds.GEAR_SELECTION to CarPropertyManager.SENSOR_RATE_ONCHANGE,
                VehiclePropertyIds.CURRENT_GEAR to CarPropertyManager.SENSOR_RATE_ONCHANGE,
                VehiclePropertyIds.PERF_VEHICLE_SPEED to CarPropertyManager.SENSOR_RATE_NORMAL,
            )) {
                try {
                    if (m.registerCallback(callback, prop, rate)) ok = true
                } catch (t: Throwable) {
                    Log.w(TAG, "구독 실패 prop=$prop : ${t.message}")
                }
            }
            Log.d(TAG, "차량 API 연결: $ok")
            ok
        } catch (t: Throwable) {
            Log.w(TAG, "차량 API 없음 → 시연 모드", t)
            false
        }
    }

    fun stop() {
        try {
            manager?.unregisterCallback(callback)
            car?.disconnect()
        } catch (_: Throwable) {
        }
    }
}

/**
 * 음성 출력
 * - 한국어·영어: Pleos TextToSpeech SDK (차량 스피커, ON_DEVICE 모드라 앱 등록 불필요)
 *   Pleos TTS 지원 언어: ko-KR, en-US, fr-CA, es-US/MX (공식 문서 기준)
 * - 중국어·일본어 또는 Pleos TTS 초기화 실패 시: 안드로이드 기본 TTS로 자동 전환
 */
class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private val android = TextToSpeech(context, this)
    private var androidReady = false

    private val pleos: PleosTTS? = try {
        PleosTTS(context.applicationContext, PleosMode.ON_DEVICE)
    } catch (t: Throwable) {
        Log.w(TAG, "Pleos TTS 생성 실패", t); null
    }
    var pleosReady = false
        private set

    init {
        try {
            pleos?.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    pleosReady = true
                    Log.d(TAG, "Pleos TTS 준비 완료")
                }

                override fun onFailure(errorType: PleosErrorType) {
                    Log.w(TAG, "Pleos TTS 초기화 실패: $errorType → 기본 TTS 사용")
                }
            })
        } catch (t: Throwable) {
            Log.w(TAG, "Pleos TTS 초기화 예외", t)
        }
    }

    override fun onInit(status: Int) {
        androidReady = status == TextToSpeech.SUCCESS
        Log.d(TAG, "기본 TTS 준비: $androidReady")
    }

    private fun pleosSupports(locale: Locale) = locale.language == "ko" || locale.language == "en"

    fun speak(text: String, locale: Locale) {
        if (pleosReady && pleosSupports(locale)) {
            try {
                val loc = if (locale.language == "ko") Locale.KOREA else Locale.US
                pleos?.setLocale(loc)
                pleos?.speak(text)
                Log.d(TAG, "음성[Pleos TTS ${loc}] $text")
                return
            } catch (t: Throwable) {
                Log.w(TAG, "Pleos TTS 실패 → 기본 TTS", t)
            }
        }
        if (!androidReady) {
            Log.w(TAG, "음성 엔진 없음: $text"); return
        }
        try {
            android.setLanguage(locale)
            android.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tc-${System.currentTimeMillis()}")
            Log.d(TAG, "음성[기본 TTS ${locale}] $text")
        } catch (t: Throwable) {
            Log.w(TAG, "기본 TTS 실패", t)
        }
    }

    fun stop() {
        try { pleos?.stop() } catch (_: Throwable) {}
        android.stop()
    }

    fun shutdown() {
        try { pleos?.release() } catch (_: Throwable) {}
        android.shutdown()
    }
}

/** 실제 위치(GPS). 에뮬레이터 Extended controls → Location 으로 좌표를 보낼 수 있다. */
class LocationWatcher(context: Context, private val onLocation: (Double, Double) -> Unit) {
    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val listener = LocationListener { loc: Location -> onLocation(loc.latitude, loc.longitude) }

    @SuppressLint("MissingPermission")
    fun start() {
        for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            try {
                if (lm.isProviderEnabled(p)) {
                    lm.requestLocationUpdates(p, 2000L, 10f, listener, Looper.getMainLooper())
                }
            } catch (t: Throwable) {
                Log.w(TAG, "위치 구독 실패 $p : ${t.message}")
            }
        }
    }

    fun stop() {
        try {
            lm.removeUpdates(listener)
        } catch (_: Throwable) {
        }
    }
}

fun distanceM(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Float {
    val out = FloatArray(1)
    Location.distanceBetween(aLat, aLng, bLat, bLng, out)
    return out[0]
}

/** QR 코드 이미지 만들기 */
fun qrImage(text: String, size: Int = 480): ImageBitmap {
    val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to 1)
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] = if (matrix[x, y]) 0xFF111111.toInt() else 0xFFFFFFFF.toInt()
        }
    }
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bmp.setPixels(pixels, 0, size, 0, 0, size, size)
    return bmp.asImageBitmap()
}
