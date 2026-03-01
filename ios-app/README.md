# iOS 앱 개발 가이드 (WhoIsTalkingIOS)

이 문서는 **iOS 개발 경험이 없는 개발자**가 이 저장소의 `ios-app` 코드를 맥북에 내려받아,
로컬 실행/테스트/배포 준비까지 진행할 수 있도록 단계별로 설명합니다.

---

## 1) 이 폴더에 들어있는 코드가 무엇인지 먼저 이해하기

`ios-app` 폴더는 SwiftUI 기반 iPhone 클라이언트 소스 파일 모음입니다.
현재 저장소에는 `.xcodeproj`가 포함되어 있지 않으므로, **Xcode에서 새 프로젝트를 만든 뒤 파일을 추가**해서 실행하는 방식으로 시작합니다.

### 주요 파일

- `WhoIsTalkingIOSApp.swift`: 앱 진입점
- `ContentView.swift`: 홈/세션 화면 전환
- `HomeView.swift`: 세션 생성 UI
- `SessionView.swift`: 채팅 UI
- `SessionWebSocketClient.swift`: WebSocket 연결/재연결/메시지 처리
- `APIClient.swift`: `/sessions` HTTP API 호출
- `Models.swift`: 이벤트/DTO 모델
- `Config.swift`: API Base URL, WS URL 생성
- `WebGameView.swift`: WebView 관련 화면

---

## 2) 개발 환경 준비 (맥북)

## 필수

1. **macOS 최신 업데이트** 권장
2. **Xcode 설치** (App Store)
3. 첫 실행 후 라이선스/추가 컴포넌트 설치
4. 터미널에서 아래 확인

```bash
xcode-select -p
xcodebuild -version
xcrun --version
swift --version
```

문제가 있다면:

```bash
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
```

## 선택(권장)

- Homebrew (도구 설치 편의)
- VS Code + Swift 확장 (보조 편집기 용도)
- iPhone 실기기 테스트를 위한 Apple ID 로그인

---

## 3) 코드 다운로드 및 기본 점검

```bash
git clone <이-저장소-URL>
cd who_is_talking_who_is_generating
```

iOS 코드 위치 확인:

```bash
ls ios-app
```

---

## 4) 백엔드 먼저 실행 (iOS 앱은 백엔드에 연결됨)

앱은 `Config.swift`의 기본값으로 `http://localhost:8000` 백엔드를 바라봅니다.
즉, iOS 앱 실행 전에 백엔드를 켜야 정상 동작합니다.

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

백엔드 실행 후 새 터미널에서 iOS 작업을 진행하세요.

---

## 5) Xcode 프로젝트 생성 및 파일 연결

> 이 저장소는 iOS 소스 파일 중심 구조이므로, 아래 과정을 한 번 수동으로 수행해야 합니다.

1. Xcode 실행 → **File > New > Project...**
2. iOS 탭에서 **App** 선택
3. Product Name 예: `WhoIsTalkingIOS`
4. Interface: **SwiftUI**, Language: **Swift**, Use Core Data 해제
5. 저장 위치를 저장소의 `ios-app` 폴더로 지정(또는 임시 생성 후 이동)

그 다음, 생성된 기본 파일 대신 현재 저장소 파일을 사용합니다.

6. Xcode Project Navigator에서 기본 `ContentView.swift`/`<AppName>App.swift`와 충돌 여부 확인
7. `ios-app` 폴더의 Swift 파일들을 드래그 앤 드롭해 프로젝트에 추가
8. 추가 팝업에서:
   - **Copy items if needed**: 보통 해제(같은 폴더 관리 시)
   - Target 체크: `WhoIsTalkingIOS` 앱 타깃 체크
9. Build Settings에서 iOS Deployment Target (예: iOS 16+) 설정

---

## 6) 첫 실행 방법 (시뮬레이터)

1. 상단 Scheme에서 iPhone 15/16 시뮬레이터 선택
2. `⌘R` 실행
3. 홈 화면에서 주제/인원/턴 설정 후 세션 생성

문제 해결 팁:

- `Connection refused` → 백엔드 미실행 가능성
- JSON decode 오류 → 백엔드 응답 형식 확인
- WS 연결 실패 → `Config.swift`의 Base URL/스킴 확인

---

## 7) 실기기(iPhone)에서 실행하기

시뮬레이터에서는 `localhost`가 맥을 바라보지만,
**실기기에서 `localhost`는 아이폰 자신을 뜻합니다.** 따라서 `Config.swift`를 수정해야 합니다.

```swift
static let apiBaseURL = URL(string: "http://<맥북-LAN-IP>:8000")!
```

예: `http://192.168.0.10:8000`

추가 체크:

- 맥북/아이폰이 같은 Wi-Fi
- 방화벽/보안 소프트웨어가 8000 포트 차단하지 않는지 확인
- 백엔드 실행 시 `--host 0.0.0.0` 사용

---

## 8) 환경 설정(개발/운영) 추천 구조

현재는 `Config.swift` 상수로 관리합니다. 운영 품질을 위해 아래를 권장합니다.

1. **Build Configuration 분리** (`Debug`, `Release`)
2. `xcconfig` 파일 도입
   - `Debug.xcconfig`: 개발 서버 URL
   - `Release.xcconfig`: 운영 HTTPS URL
3. 코드에서는 `Bundle`/`Info.plist`/`xcconfig` 값을 읽어 URL 구성
4. Release에서는 `allowInsecureWebSocketInDebug = false` 원칙 유지

최소 보안 원칙:

- 앱 내부에 LLM API Key 저장 금지 (현재 구조처럼 FastAPI 경유)
- Release는 반드시 HTTPS/WSS

---

## 9) 유닛 테스트 작성/실행 방법

현재 저장소에는 iOS 테스트 타깃 파일이 없으므로, **Xcode에서 테스트 타깃을 생성**해서 시작합니다.

### 9-1. 테스트 타깃 추가

1. Xcode → File → New → Target...
2. iOS Unit Testing Bundle 선택
3. 이름 예: `WhoIsTalkingIOSTests`
4. 테스트 타깃이 앱 타깃을 참조하도록 설정

### 9-2. 첫 테스트 추천 항목

- `Config.websocketURL(from:)` 스킴 변환 검증 (`http -> ws`, `https -> wss`)
- `APIClient`의 상태 코드/디코딩 처리 검증 (Mock URLProtocol 사용)
- `SessionWebSocketClient`의 상태 전이(연결 상태, 메시지 upsert) 검증

### 9-3. 실행 방법

Xcode:
- 전체 테스트: `⌘U`
- 개별 테스트: 테스트 함수 옆 다이아몬드 버튼

CLI:

```bash
xcodebuild test \
  -scheme WhoIsTalkingIOS \
  -destination 'platform=iOS Simulator,name=iPhone 15'
```

> 시뮬레이터 이름은 설치 환경마다 다를 수 있습니다. `xcodebuild -showdestinations -scheme WhoIsTalkingIOS`로 확인하세요.

### 9-4. UI 테스트(선택)

- 세션 생성 플로우, 메시지 입력, 연결 끊김 시 재시도 문구 표시 등 E2E 시나리오를 UI Test로 자동화하면 배포 안정성이 높아집니다.

---

## 10) 자주 겪는 이슈 정리

### A. `xcrun`/`xcodebuild` 명령 실패
- Xcode 미설치 또는 active developer dir 불일치 가능성
- `xcode-select -p` 확인 후 필요 시 재지정

### B. 실기기에서 서버 연결 불가
- `localhost` 사용 여부 확인
- LAN IP로 변경했는지 확인
- 같은 네트워크인지 확인

### C. WebSocket이 운영에서 차단됨
- `ws://` 대신 `wss://` 사용
- 운영 서버 인증서/리버스 프록시 설정 점검

### D. ATS(App Transport Security) 관련 차단
- 개발 단계에서만 임시 예외를 최소 범위로 사용
- 출시 전에는 ATS 예외 제거 + HTTPS 전환

---

## 11) 앱스토어 배포 가이드 (가능한 범위)

아래는 “처음 배포하는 개발자” 기준 체크리스트입니다.

### 11-1. Apple Developer 준비

1. Apple Developer Program 가입(유료)
2. App Store Connect에서 앱 생성
3. Bundle Identifier 확정 (`com.yourcompany.whoistalkingios`)

### 11-2. Xcode 서명/버전 설정

- TARGETS > Signing & Capabilities
  - Team 선택
  - Bundle Identifier 일치
  - Automatically manage signing 사용(초보 권장)
- General 탭
  - Version (예: 1.0.0)
  - Build (예: 1)

### 11-3. 배포 전 필수 점검

- Release 빌드에서 개발 URL/HTTP 사용 제거
- 개인정보 처리방침 URL 준비(필요 시)
- 앱 아이콘/스크린샷/설명문 준비
- 크래시/오류 최소화 (실기기+네트워크 불안정 테스트)

### 11-4. 아카이브 및 업로드

1. Any iOS Device (arm64) 선택
2. Product > Archive
3. Organizer에서 빌드 선택
4. Distribute App > App Store Connect > Upload

### 11-5. App Store Connect 메타데이터 입력

- 카테고리, 설명, 키워드, 스크린샷
- 연령 등급
- 개인정보 수집/추적 여부(App Privacy)

### 11-6. 심사 제출

- TestFlight 내부 테스트 권장
- 문제 없으면 심사 제출
- 리젝 시 사유 대응 후 재제출

---

## 12) 운영 전환 체크리스트 (요약)

- [ ] API URL을 운영 도메인(HTTPS)로 전환
- [ ] WebSocket URL이 WSS로 생성되는지 검증
- [ ] Debug/Release 설정 분리
- [ ] 유닛 테스트/기본 UI 테스트 통과
- [ ] 실기기 네트워크 장애 시나리오 점검
- [ ] 앱 아이콘/앱 프라이버시 정보 등록

---

## 13) 빠른 시작 요약

1. 백엔드 실행 (`localhost:8000`)
2. Xcode에서 iOS App 프로젝트 생성
3. `ios-app` Swift 파일 추가
4. 시뮬레이터에서 `⌘R`
5. 테스트 타깃 만들고 `⌘U`
6. 실기기 테스트 시 `Config.swift`를 LAN IP로 변경
7. Release 전 HTTPS/WSS/서명/메타데이터 점검

필요하면 다음 단계로, 제가 바로 **테스트 코드 템플릿(APIClient Mock, Config 테스트)**까지 작성해 드릴 수 있습니다.
