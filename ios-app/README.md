# iOS App (WhoIsTalkingIOS)

웹 게임을 iPhone에서 바로 실행할 수 있도록 `SwiftUI + WKWebView` 기반 앱 스캐폴드를 제공합니다.

## 동작 방식

- 앱이 `WEB_APP_URL`(기본: `http://localhost:5173`)을 WebView로 로드합니다.
- iPhone 실기기 테스트 시 `localhost` 대신 **PC의 로컬 IP**를 사용하세요.
  - 예: `http://192.168.0.23:5173`

## 파일 구성

- `WhoIsTalkingIOSApp.swift`: 앱 엔트리 포인트
- `ContentView.swift`: 메인 화면
- `WebGameView.swift`: WKWebView 브릿지
- `Config.swift`: 웹 URL 설정

## 실행 방법 (Xcode)

1. Xcode에서 iOS App 프로젝트를 새로 만든 뒤(템플릿: App, Interface: SwiftUI)
2. 생성된 기본 파일을 이 디렉터리 파일로 교체
3. `Config.swift`의 `WEB_APP_URL`을 환경에 맞게 수정
4. iPhone 시뮬레이터 또는 실기기에서 Run

## 주의사항

- iOS App Transport Security(ATS) 정책으로 `http` 접속이 차단될 수 있습니다.
- 개발 단계에서는 Info.plist에 예외를 추가하거나, 가능하면 `https` 개발 URL을 사용하세요.
