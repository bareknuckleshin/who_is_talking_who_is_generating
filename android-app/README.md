# Android App (WhoIsTalkingAndroid)

Android 폰에서 게임을 **완전 네이티브 Android 앱**으로 실행할 수 있도록 `Kotlin + Jetpack Compose` 기반 구현을 제공합니다.

## 구현 범위

- WebView 사용 없음 (웹 페이지 로드하지 않음)
- 홈 화면(UI)에서 토픽/옵션 입력 후 세션 생성
- HTTP로 세션 생성 요청 (`POST /sessions`)
- WebSocket으로 실시간 이벤트 수신/전송
- 네이티브 상태관리(ViewModel + StateFlow), 카운트다운, 재연결 배너, 채팅 UI 처리

## 동작 방식

- 기본 API URL: `http://10.0.2.2:8000` (`Config.API_BASE_URL`)
- 에뮬레이터에서는 `10.0.2.2`가 개발 PC localhost를 의미
- 실기기 테스트 시에는 PC의 로컬 IP로 변경
  - 예: `http://192.168.0.23:8000`

## 주요 파일

- `app/src/main/java/com/whoistalking/androidapp/MainActivity.kt`: 앱 엔트리
- `app/src/main/java/com/whoistalking/androidapp/MainViewModel.kt`: 상태/이벤트 로직
- `app/src/main/java/com/whoistalking/androidapp/GameRepository.kt`: HTTP + WebSocket 네트워크 처리
- `app/src/main/java/com/whoistalking/androidapp/ui/AppRoot.kt`: Compose 화면(UI)

## 실행 방법

1. Android Studio에서 `android-app/` 디렉터리 Open
2. Gradle Sync 완료
3. 필요 시 `Config.API_BASE_URL`을 환경에 맞게 수정
4. 에뮬레이터 또는 실기기에서 Run

## 주의사항

- 개발 환경에서 로컬 서버(`http`) 접속을 위해 cleartext 트래픽을 허용했습니다.
- 운영 배포 시에는 `https` 사용 및 네트워크 보안 정책 강화를 권장합니다.
