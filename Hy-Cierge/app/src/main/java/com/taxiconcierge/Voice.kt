package com.taxiconcierge

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import ai.pleos.playground.stt.SpeechToText as PleosSTT
import ai.pleos.playground.stt.constant.ErrorType as SttError
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInit
import ai.pleos.playground.stt.listener.ResultListener as SttResult
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

private const val TAG = "TAXI"

/** 음성 인식 결과를 화면으로 보내는 통로 (항상 메인 스레드에서 불림) */
interface VoiceEvents {
    fun onLevel(v: Float)          // 0~1 소리 크기 → 음파 효과
    fun onPartial(text: String)    // 말하는 중 실시간 문장
    fun onFinal(text: String)      // 최종 문장 (못 알아들으면 "")
}

/**
 * 마이크 음성 인식
 *  1순위: Pleos SpeechToText SDK (ON_DEVICE, 차 안 엔진 · 앱 등록 불필요) — Pleos 에뮬/차량
 *  2순위: 안드로이드 기본 음성 인식 (구글) — 일반 태블릿 에뮬
 *  3순위: 예시 문장이 한 글자씩 써지는 시연 모드 — 마이크가 없거나 인식이 안 될 때
 * 어떤 경우에도 시연이 멈추지 않게, 실패하면 다음 순위로 자동 전환한다.
 */
class Mic(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var events: VoiceEvents? = null
    private var sample = ""
    private var locale: Locale = Locale.US
    private var session = 0
    private var lastPartial = ""

    /** 지금 쓰는 엔진 이름 (DEMO 패널 표시용) */
    var engine = "-"
        private set

    // ───────── Pleos STT ─────────
    private val pleos: PleosSTT? = try {
        PleosSTT(context.applicationContext, SttMode.ON_DEVICE)
    } catch (t: Throwable) {
        Log.w(TAG, "Pleos STT 생성 실패", t); null
    }
    private var pleosReady = false
    private var pleosActive = false
    private var rmsMax = 1.0

    private val pleosListener = object : SttResult {
        override fun onReady() = Unit
        override fun onStartedRecognition() = Unit
        override fun onUpdated(stt: String, completed: Boolean) {
            main.post {
                if (!pleosActive) return@post
                if (completed) finish(stt) else partial(stt)
            }
        }
        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
        override fun onUpdatedRms(rms: Double) {
            main.post {
                if (!pleosActive) return@post
                rmsMax = maxOf(rmsMax * 0.995, rms, 1.0)
                events?.onLevel((rms / rmsMax).toFloat().coerceIn(0f, 1f))
            }
        }
        override fun onEndedRecognition() {
            main.post { if (pleosActive) finish(lastPartial) }
        }
        override fun onError(errorType: SttError) {
            main.post {
                if (!pleosActive) return@post
                Log.w(TAG, "Pleos STT 오류: $errorType → 다음 엔진")
                pleosActive = false
                if (lastPartial.isNotBlank()) finish(lastPartial) else startAndroidOrDemo()
            }
        }
    }

    init {
        try {
            pleos?.initialize(object : SttInit {
                override fun onSuccess() {
                    pleosReady = true
                    try { pleos?.addListener(pleosListener) } catch (t: Throwable) { Log.w(TAG, "STT 리스너 등록 실패", t) }
                    Log.d(TAG, "Pleos STT 준비 완료")
                }

                override fun onFailure(errorType: SttError) {
                    Log.w(TAG, "Pleos STT 초기화 실패: $errorType → 안드로이드 인식 사용")
                }
            })
        } catch (t: Throwable) {
            Log.w(TAG, "Pleos STT 초기화 예외", t)
        }
    }

    // ───────── 안드로이드 기본 인식 ─────────
    private var android: SpeechRecognizer? = null

    private val hasPermission: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /**
     * 듣기 시작
     * @param sample 시연 모드에서 써질 예시 문장
     * @param demo   true면 마이크 없이 바로 예시 문장 (DEMO 패널 스위치)
     */
    fun start(l: Locale, sample: String, demo: Boolean, ev: VoiceEvents) {
        stop()
        session++
        events = ev
        this.sample = sample
        locale = l
        lastPartial = ""
        if (demo || !hasPermission) {
            if (!hasPermission) Log.w(TAG, "마이크 권한 없음 → 예시 문장")
            startDemo(); return
        }
        val stt = pleos
        if (pleosReady && stt != null) {
            try {
                // setLocale 이름·형태가 버전마다 다를 수 있어 안전하게 호출
                runCatching { stt.javaClass.getMethod("setLocale", Locale::class.java).invoke(stt, l) }
                pleosActive = true
                engine = "Pleos STT"
                stt.request()
                armTimeout()
                Log.d(TAG, "듣기 시작 [Pleos STT $l]")
                return
            } catch (t: Throwable) {
                Log.w(TAG, "Pleos STT 요청 실패", t)
                pleosActive = false
            }
        }
        startAndroidOrDemo()
    }

    private fun startAndroidOrDemo() {
        if (hasPermission && SpeechRecognizer.isRecognitionAvailable(context)) startAndroid() else startDemo()
    }

    private fun startAndroid() {
        try {
            val r = SpeechRecognizer.createSpeechRecognizer(context)
            android = r
            engine = "Android"
            r.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) {
                    events?.onLevel(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
                }
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onError(error: Int) {
                    Log.w(TAG, "안드로이드 인식 오류 $error")
                    val nothingHeard = error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    when {
                        lastPartial.isNotBlank() -> finish(lastPartial)
                        nothingHeard -> finish("")
                        else -> startDemo()   // 엔진 자체가 안 되면 시연 모드로
                    }
                }
                override fun onResults(results: Bundle?) {
                    val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    finish(t.ifBlank { lastPartial })
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        ?.takeIf { it.isNotBlank() }?.let { partial(it) }
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            r.startListening(intent)
            armTimeout()
            Log.d(TAG, "듣기 시작 [Android ${locale.toLanguageTag()}]")
        } catch (t: Throwable) {
            Log.w(TAG, "안드로이드 인식 시작 실패", t)
            startDemo()
        }
    }

    // ───────── 시연 모드: 예시 문장이 한 글자씩 ─────────
    private fun startDemo() {
        android?.let { runCatching { it.destroy() } }; android = null
        engine = "DEMO"
        val my = session
        val text = sample
        var i = 0
        val step = object : Runnable {
            override fun run() {
                if (my != session) return
                i++
                val wobble = abs(sin(i * 0.55)).toFloat() * 0.7f + (if (i % 4 == 0) 0.25f else 0.05f)
                events?.onLevel(wobble.coerceIn(0f, 1f))
                if (i <= text.length) {
                    partial(text.take(i))
                    main.postDelayed(this, 55)
                } else {
                    main.postDelayed({ if (my == session) finish(text) }, 500)
                }
            }
        }
        main.postDelayed(step, 900)   // 잠깐 "듣는 중" 보여준 다음 써지기 시작
    }

    // ───────── 공통 ─────────
    private fun armTimeout() {
        val my = session
        main.postDelayed({ if (my == session && events != null) finish(lastPartial) }, 12_000)
    }

    private fun partial(t: String) {
        lastPartial = t
        events?.onPartial(t)
    }

    private fun finish(t: String) {
        val ev = events ?: return
        events = null
        session++
        stopEngines()
        ev.onLevel(0f)
        ev.onFinal(t.trim())
    }

    private fun stopEngines() {
        if (pleosActive) {
            pleosActive = false
            runCatching { pleos?.stop() }
        }
        android?.let { runCatching { it.cancel(); it.destroy() } }
        android = null
    }

    /** 듣는 중에 바로 끝내기 (Finish 버튼). 시연 모드면 예시 문장 전체로 */
    fun finishNow() {
        if (events == null) return
        finish(if (lastPartial.isNotBlank() && engine != "DEMO") lastPartial else if (engine == "DEMO") sample else lastPartial)
    }

    fun stop() {
        events = null
        session++
        stopEngines()
    }

    fun release() {
        stop()
        runCatching { pleos?.removeListener(pleosListener) }
        runCatching { pleos?.release() }
    }
}

/** 말한 문장의 글자 모양으로 언어 짐작 (한글 / 가나 / 한자 / 알파벳) */
fun guessLang(text: String, fallback: Lang): Lang {
    if (text.any { it in '가'..'힣' }) return Lang.KO
    if (text.any { it in '぀'..'ヿ' }) return Lang.JA
    if (text.any { it in '一'..'鿿' }) return Lang.ZH
    if (text.any { it in 'a'..'z' || it in 'A'..'Z' }) return Lang.EN
    return fallback
}
