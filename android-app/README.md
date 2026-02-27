# Android App (WhoIsTalkingAndroid)

Android 폰에서 웹 게임을 바로 실행할 수 있도록 `Kotlin + WebView` 기반 앱 스캐폴드를 제공합니다.

## 동작 방식

- 앱이 `Config.WEB_APP_URL`(기본: `http://10.0.2.2:5173`)을 WebView로 로드합니다.
- Android 에뮬레이터에서는 `10.0.2.2`가 개발 PC의 localhost를 가리킵니다.
- 실기기 테스트 시에는 `10.0.2.2` 대신 **개발 PC의 로컬 IP**로 바꿔 주세요.
  - 예: `http://192.168.0.23:5173`

## 파일 구성

- `settings.gradle.kts`, `build.gradle.kts`: 프로젝트/플러그인 설정
- `app/build.gradle.kts`: Android 앱 모듈 설정
- `app/src/main/java/com/whoistalking/androidapp/MainActivity.kt`: WebView 엔트리 액티비티
- `app/src/main/java/com/whoistalking/androidapp/Config.kt`: 웹 URL 설정
- `app/src/main/res/xml/network_security_config.xml`: 개발용 http(cleartext) 허용

## 실행 방법 (Android Studio)

1. Android Studio에서 `android-app/` 디렉터리를 Open
2. Gradle Sync 완료
3. 필요 시 `Config.kt`의 `WEB_APP_URL`을 현재 환경에 맞게 수정
4. 에뮬레이터 또는 실기기를 선택해서 Run

## 주의사항

- 개발 단계에서 로컬 서버 접속을 위해 cleartext(`http`)를 허용했습니다.
- 운영 배포 시에는 `https` URL을 사용하고, `network_security_config` 정책을 강화하는 것을 권장합니다.
