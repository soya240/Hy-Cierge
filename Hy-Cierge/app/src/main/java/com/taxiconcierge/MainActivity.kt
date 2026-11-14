package com.taxiconcierge

import ai.pleos.playground.vehicle.Vehicle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * 택시 컨시어지 1차 구현판
 * - 기사님 화면(Pleos 앞좌석) 단독 시나리오
 * - 기어·속도(AAOS Car API) → 주행 중이면 음성만, 정차 중이면 카드
 * - 위치(GPS 또는 DEMO) → 명소 근처에서 승객 언어로 1분 이야기
 * - 통역은 1차로 상황별 문장 세트 + 기기 TTS (LLM 실번역은 2차)
 */
class MainActivity : ComponentActivity() {

    private lateinit var speaker: Speaker
    private lateinit var state: AppState
    private var drive: DriveMonitor? = null
    private var location: LocationWatcher? = null
    private var vehicle: Vehicle? = null
    private var mic: Mic? = null

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        "android.car.permission.CAR_SPEED",
        Manifest.permission.RECORD_AUDIO,
    )

    private val askPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            Log.d("TAXI", "권한 결과: $result")
            startSensors()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speaker = Speaker(this)
        state = AppState { text, lang -> speaker.speak(text, lang.locale) }
        // 마이크 음성 인식 (Pleos STT → 안드로이드 인식 → 예시 문장 순서로 자동 선택)
        mic = Mic(this).also { state.mic = it }

        // Pleos Vehicle SDK 연결 (세차 앱과 같은 방식). 와이퍼·도어 연동은 2차에서 여기 붙인다.
        try {
            vehicle = Vehicle(applicationContext).also { it.initialize() }
            Log.d("TAXI", "Pleos Vehicle SDK 초기화")
        } catch (t: Throwable) {
            Log.w("TAXI", "Pleos Vehicle SDK 초기화 실패", t)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) startSensors() else askPermissions.launch(missing.toTypedArray())

        setContent { App(state) }
    }

    private fun startSensors() {
        if (drive == null) {
            drive = try {
                DriveMonitor(
                    this,
                    onGear = { g -> runOnUiThread { state.carGear = g } },
                    onSpeed = { s -> runOnUiThread { state.carSpeedMs = s } },
                ).also { state.carConnected = it.start() }
            } catch (t: Throwable) {
                Log.w("TAXI", "차량 API 사용 불가 → 시연 모드", t)
                null
            }
        }
        if (location == null) {
            location = LocationWatcher(this) { la, ln ->
                runOnUiThread { state.updatePosition(la, ln, "GPS") }
            }.also { it.start() }
        }
    }

    override fun onDestroy() {
        drive?.stop()
        location?.stop()
        speaker.shutdown()
        mic?.release()
        try {
            vehicle?.release()
        } catch (_: Throwable) {
        }
        super.onDestroy()
    }
}
