# iOS App (WhoIsTalkingIOS) - Native SwiftUI Client

이 앱은 WebView 래퍼가 아니라, **SwiftUI + Swift Concurrency 기반 네이티브 클라이언트**입니다.

## 구현 범위

- 세션 생성 화면(Form, Stepper, Button)
- 실시간 채팅 화면(List, TextField, Alert)
- FastAPI `/sessions` 호출로 세션 생성
- `AsyncThrowingStream` 기반 WebSocket 수신 루프
- `message.delta`(토큰 스트리밍) + `message.new`(완성 메시지) 처리
- 세션 종료/화면 이탈 시 Task 및 소켓 명시적 `cancel`
- 장시간 플레이 대비 메시지 아카이빙(JSONL)으로 메모리 사용량 제한
- 재연결 Exponential Backoff + `last_seen_message_id`/`last_sequence_id` 기반 resume

## 파일 구성

- `WhoIsTalkingIOSApp.swift`: 앱 진입점
- `ContentView.swift`: 홈/세션 화면 전환
- `HomeView.swift`: 세션 생성 UI
- `SessionView.swift`: 대화 UI + 입력
- `SessionWebSocketClient.swift`: AsyncStream WebSocket + 재연결/아카이브/스트리밍 상태관리
- `APIClient.swift`: HTTP API 클라이언트
- `Models.swift`: DTO/이벤트 모델
- `Config.swift`: 백엔드 URL 설정

## 구현 포인트

- **UI 프리징 방지:** 소켓 수신은 백그라운드 Task, `@MainActor`에서는 상태 반영만 수행
- **토큰 단위 실시간 렌더링:** `message.delta` 이벤트마다 기존 메시지의 `text` append
- **메모리 보호:** 메모리 메시지 수가 임계치 초과 시 오래된 메시지를 캐시 파일로 이동
- **복구력:** 네트워크 장애 시 지수 백오프 재연결, 마지막 수신 위치를 resume payload에 포함
- **보안 원칙:** 앱 내 LLM API 키 저장 금지(모든 호출은 FastAPI 백엔드 경유)

## 실행 방법 (현재 맥북 환경 기준)

현재 개발 도구 체인이 깨진 상태(`xcrun`/Xcode active developer dir 문제)이므로, 먼저 아래를 복구해야 iOS 빌드가 가능합니다.

1. Xcode 설치(또는 재설치) 후 실행
2. `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`
3. `xcodebuild -version` / `xcrun --version` 정상 동작 확인
4. 프로젝트를 Xcode에서 열고 Run

## 주의사항

- 개발용 HTTP URL 사용 시 ATS(App Transport Security) 예외 설정이 필요할 수 있습니다.
- 실기기 테스트 시 `localhost` 대신 개발 맥의 LAN IP를 사용하세요.
