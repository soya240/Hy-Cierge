# Hy-Cierge (택시 컨시어지)

방한 외국인 관광객을 위한 Pleos Connect 택시 인포테인먼트 앱.
앱 하나에서 **기사 모드**(앞좌석 Pleos 화면)와 **승객 모드**(뒷좌석 태블릿 1600×900)를 고릅니다.

## 설치·실행
1. APK 설치: Pleos Connect 에뮬레이터 화면에 `Hy-Cierge_v1.0.apk` 를 끌어다 놓기
   (또는 `adb install Hy-Cierge_v1.0.apk`)
2. 첫 실행 때 마이크·위치 권한 허용
3. 소스로 실행: Android Studio → Open → 이 폴더 → Pleos 에뮬레이터 켜고 ▶ Run

## ⚠️ 먼저 한 번: 주행 중에도 앱 화면 쓰기
Pleos 에뮬레이터는 기어를 D 로 바꾸면 "주행 중에는 사용할 수 없습니다" 창으로 앱을 가릴 수 있습니다.
Hy-Cierge 는 주행 중에도 승객 안내·통역·지역 큐레이션이 계속 돌아가는 앱이므로, 시연 전에 아래 명령을 한 번 실행해 주세요.
```
adb shell cmd car_service enable-uxr false
```
- 앱 매니페스트에 주행 중 사용 표시(`distractionOptimized`)를 넣어 두었습니다
- 주행 중에도 기사 화면은 내비·음성 안내만 남기고 단순해집니다 (운전 집중 방해 방지)
- 승객 화면은 주행 중에도 모든 기능이 그대로 동작합니다

## Virtual Console 로 차량 신호 넣기 (기어·속도)
```
adb forward tcp:8088 tcp:8088
```
브라우저에서 http://127.0.0.1:8088 → Motion → Gear

- 목적지를 확정한 뒤 기어를 **D** 로 바꾸면, 서울역 → 하차 지점까지 GPS 주행이 자동으로 시작됩니다
  (에뮬레이터 GPS 는 움직이지 않으므로 실제 도로 좌표를 앱이 순서대로 넣습니다)
- **P** 로 바꾸면 그 자리에 멈추고, 속도를 올리면 더 빨리 이동합니다
- 주행 중(D)에는 기사 화면이 내비·음성 안내만 남기고 최소화됩니다

## 시연 순서 (승객 모드)
1. 첫 화면 → 오브 등장 → Tap to speak → 목적지 말하기 (예: "Gwangjang Market, Jongno-side entrance")
2. Send to driver → 기사 확인 → 홈
3. 디지털 키트 → 목적지 주변(홀로그램 지도) → 여행 도구(식당·정밀 하차·환율)
4. Virtual Console 기어 D (또는 오른쪽 아래 DEMO → ▶ 실제 경로 주행)
   → 동네 소개 · 명소 이야기 · 곧 도착 · 하차 실시간 지도
5. 통번역: 오른쪽 아래 말풍선 아이콘

## 사용 SDK·데이터
- Pleos SpeechToText 3.3.1 / TextToSpeech 3.3.1 / Vehicle 2.3.2, AAOS Car API (GEAR_SELECTION, CURRENT_GEAR, PERF_VEHICLE_SPEED)
- 지도: 앱에 내장한 서울 도심 지도 (인터넷 사용 안 함, 차량 위치가 밖으로 나가지 않음)
- 환율: ExchangeRate-API 기준 환율 (하루 1회 갱신, 연결 안 되면 예시값)
- 장소 사진: 위키백과 대표 사진 (Wikimedia Commons, 자유 라이선스)
- 라이더 광장의 다른 여행자·대화는 시연용 가상 데이터입니다

Logcat 필터: `tag:TAXI`
