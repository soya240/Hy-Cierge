# Hy-Cierge

## 앱 이름 및 한 줄 소개

**Hy-Cierge**는 방한 외국인 관광객과 택시 기사의 다국어 소통을 돕고, 이동 경로와 목적지 주변의 관광 정보를 안내하는 Pleos Connect용 택시 컨시어지 앱입니다.

## 실행 방법

1. [APK 파일 위치](https://github.com/soya240/Hy-Cierge/releases/tag/v1.0)에서 APK를 내려받습니다.
2. Pleos Connect 에뮬레이터를 실행하고, Android SDK Platform-Tools의 `adb`로 연결을 확인한 뒤 APK를 설치합니다. 명령의 파일명은 내려받은 APK의 실제 경로로 바꿉니다.

   ```bash
   adb devices
   adb install "The.warrior.of.gongmo_Hy-Cierge_v1.0_release.apk"
   ```

3. 에뮬레이터의 앱 목록에서 **Hy-Cierge**를 엽니다.
4. **기사 화면**에서 **탑승 시작**을 누릅니다. **승객 화면**에서 언어를 선택하고 목적지를 요청한 뒤, 기사 화면에서 요청 내용을 확인합니다.
5. **GPS 주행 시뮬레이션**에서 서울역부터 하차 지점까지의 지역·명소 안내를 체험합니다. 승객 화면에서 한국 여행 시 필요한 앱 설치 QR, 주변 식당 찾기, 환율도 확인할 수 있습니다.

## 테스트 환경 및 필요한 설정

| 항목 | 환경 및 설정 |
| --- | --- |
| 실행 플랫폼 | Pleos Connect 에뮬레이터 · Android Automotive OS |
| 앱 정보 | 버전 1.0 · 패키지 `com.taxiconcierge.app` · 최소 Android API 28 |
| 마이크 및 위치 권한 | 음성 인식에는 마이크 권한, 현재 위치 기반 안내에는 위치 권한을 허용합니다. |
| 네트워크 | 온라인 환율 및 지도 데이터를 확인하려면 에뮬레이터를 인터넷에 연결합니다. |
| 차량 신호 시연 | 차량 API를 사용할 수 없는 환경에서는 시연 모드로 차량 신호를 재현할 수 있습니다. |

## 개발에 사용한 Pleos Connect 에뮬레이터 버전과 화면 규격

| 항목 | 버전 및 규격 |
| --- | --- |
| Pleos Connect 에뮬레이터 버전 | Pleos Connect v2.1.0 (Android API 34) |
| 앞좌석 기사 화면 | Connect-S 14.6인치, 2048 × 1152 px 디자인 기준 |
| 뒷좌석 승객 화면 | 가로 1600 × 900 px 디자인 기준 |

## APK 파일 위치 및 GitHub Release 링크

[GitHub Release v1.0 열기](https://github.com/soya240/Hy-Cierge/releases/tag/v1.0)

릴리스 페이지의 **Assets**에서 `.apk` 파일을 내려받습니다.
