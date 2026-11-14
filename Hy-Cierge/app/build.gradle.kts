plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.taxiconcierge"
    compileSdk {
        version = release(37)
    }

    // AAOS 표준 차량 API(기어·속도). Pleos 문서: 표준 데이터는 AAOS Car API 사용
    useLibrary("android.car")

    defaultConfig {
        applicationId = "com.taxiconcierge.app"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // 제출용 release: 코드 압축(R8)은 끔 → Pleos SDK·음성 기능이 디버그와 똑같이 동작
            optimization {
                enable = false
            }
            // 따로 만든 서명 키가 없어서 안드로이드 기본(디버그) 키로 서명 → 바로 설치·실행 가능
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Pleos Vehicle SDK (예전 택시앱·공식 샘플 기준 버전)
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    // Pleos 음성 합성 (차량 스피커로 안내)
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
    // Pleos 음성 인식 (차량 마이크 → 글자)
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
    // 디지털 키트 QR 코드 생성
    implementation("com.google.zxing:core:3.5.3")
}

// ───────────────────────────────────────────────────────────
// Run(▷) 누를 때마다 자동으로 Pleos 에뮬 시연 설정
//  - 주행 중 앱 허용(enable-uxr false), 서울 시간, 예전 택시앱 정리, Virtual Console 포트
//  - 에뮬이 꺼져 있거나 실패해도 빌드는 그대로 진행됨
// ───────────────────────────────────────────────────────────
val pleosAuto = tasks.register<Exec>("pleosDemoSetup") {
    val script = rootProject.file("tools/pleos-auto.bat")
    onlyIf { script.exists() && System.getProperty("os.name").lowercase().contains("windows") }
    commandLine("cmd", "/c", script.absolutePath)
    isIgnoreExitValue = true
}
tasks.named("preBuild") { dependsOn(pleosAuto) }
