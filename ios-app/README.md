# iOS App (WhoIsTalkingIOS) - Native SwiftUI Client

이 앱은 더 이상 WebView 래퍼가 아니라, **iOS 컴포넌트로 UI와 기능을 직접 구현한 네이티브 클라이언트**입니다.

## 구현 범위

- 세션 생성 화면(Form, Stepper, Button)
- 실시간 채팅 화면(List, TextField, Alert)
- FastAPI `/sessions` 호출로 세션 생성
- `URLSessionWebSocketTask` 기반 WebSocket 연결/재연결
- `session.state`, `message.new`, `turn.request_human`, `session.finished` 등 이벤트 처리

## 파일 구성

- `WhoIsTalkingIOSApp.swift`: 앱 진입점
- `ContentView.swift`: 홈/세션 화면 전환
- `HomeView.swift`: 세션 생성 UI
- `SessionView.swift`: 대화 UI + 입력
- `SessionWebSocketClient.swift`: WebSocket 통신/상태관리
- `APIClient.swift`: HTTP API 클라이언트
- `Models.swift`: DTO/이벤트 모델
- `Config.swift`: 백엔드 URL 설정

## 실행 방법

1. Xcode에서 iOS App 프로젝트(App + SwiftUI) 생성
2. 생성된 기본 파일을 이 디렉터리 파일로 교체
3. `Config.swift`의 `apiBaseURL`을 환경에 맞게 수정
   - 실기기 테스트 시 `localhost` 대신 개발 PC의 LAN IP 사용
4. Run

## 주의사항

- 개발용 HTTP URL 사용 시 ATS(App Transport Security) 예외 설정이 필요할 수 있습니다.
