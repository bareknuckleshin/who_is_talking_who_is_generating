# Android App (WhoIsTalkingAndroid)

안드로이드 개발 경험이 없는 개발자도 **Windows 11 Pro 노트북**에서 이 프로젝트를 내려받아 실행/테스트/배포 준비까지 할 수 있도록 정리한 가이드입니다.

---

## 1) 프로젝트 개요

이 앱은 Modern Android 권장 스택을 사용합니다.

- **UI**: Jetpack Compose + Material3
- **아키텍처**: MVI-inspired UDF + Clean Architecture + 멀티 모듈
  - `:app`
  - `:feature:session`
  - `:core:data`
  - `:core:domain`
  - `:core:ui`
- **DI**: Hilt
- **데이터**: Room + Paging3 + RemoteMediator
- **언어/빌드**: Kotlin, Gradle(KTS), AGP 8.4.2, Java 17

---

## 2) 사전 준비 (Windows 11 Pro)

### 2-1. 필수 설치 항목

1. **Git for Windows**
2. **Android Studio (최신 안정 버전 권장)**
3. **JDK 17**
   - Android Studio 번들 JDK 사용 권장
4. (선택) **WSL2**
   - 백엔드 실행을 리눅스 환경에서 하고 싶을 때 유용

> 이 프로젝트는 `compileSdk=34`, `minSdk=24`, `targetSdk=34`, `Java 17` 기준입니다.

### 2-2. Android Studio SDK 설치 체크

Android Studio → `More Actions` → `SDK Manager`에서 아래 항목이 설치되어 있는지 확인하세요.

- **Android SDK Platform 34**
- **Android SDK Build-Tools**
- **Android SDK Platform-Tools (adb)**
- (에뮬레이터 사용할 경우) **Android Emulator**

### 2-3. 권장 에뮬레이터 스펙

- Device: Pixel 계열 아무거나
- System Image: Android 14 (API 34, x86_64)
- RAM: 2GB 이상
- Graphics: Hardware 가속

---

## 3) 코드 다운로드 및 프로젝트 열기

PowerShell 또는 Git Bash에서:

```bash
git clone <이 레포 주소>
cd who_is_talking_who_is_generating
```

Android Studio에서:

1. `Open` 선택
2. 레포 루트가 아니라 **`android-app/` 폴더**를 열기
3. Gradle Sync 완료까지 대기

---

## 4) 첫 실행 전 필수 설정

### 4-1. 백엔드 주소 확인

현재 기본 API 주소는 아래 파일에 하드코딩되어 있습니다.

- `app/src/main/java/com/whoistalking/androidapp/Config.kt`

기본값:

- `API_BASE_URL = "http://10.0.2.2:8000"`

설명:

- `10.0.2.2`는 **Android Emulator에서 호스트 PC(localhost)** 를 가리키는 특수 주소입니다.
- 에뮬레이터에서 실행한다면 백엔드가 PC의 8000 포트에서 떠 있을 때 동작합니다.
- 실기기 테스트 시에는 PC의 LAN IP(예: `http://192.168.0.10:8000`)로 바꿔야 합니다.

### 4-2. 백엔드 서버 실행

레포 루트 기준:

```bash
cd backend
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

> Windows PowerShell에서 venv 활성화 스크립트 실행 정책 이슈가 있으면 `Set-ExecutionPolicy -Scope Process Bypass`를 현재 세션에만 적용하고 다시 활성화하세요.

---

## 5) 앱 실행 (디버그)

### 5-1. Android Studio에서 실행

1. 상단 실행 구성을 `app`으로 선택
2. 에뮬레이터(또는 USB 연결 기기) 선택
3. `Run`(▶) 클릭

### 5-2. 터미널(Gradle)로 실행/빌드

이 저장소에는 Gradle Wrapper(`gradlew`)가 보이지 않을 수 있으므로, 팀 표준에 맞게 아래 중 하나를 사용하세요.

- Android Studio 내부 Gradle 실행
- 로컬 Gradle 설치 후 실행

대표 명령(Wrapper가 있다면 권장):

```bash
# Windows
.\gradlew.bat :app:assembleDebug

# macOS/Linux
./gradlew :app:assembleDebug
```

---

## 6) 유닛 테스트 방법

이 프로젝트에는 예시로 `feature/session` 모듈의 ViewModel 단위 테스트가 포함되어 있습니다.

- 테스트 파일: `feature/session/src/test/java/com/whoistalking/feature/session/vm/SessionViewModelTest.kt`
- 사용 도구: JUnit5, MockK, Turbine, Coroutines Test

실행 명령(Wrapper 기준):

```bash
# 전체 테스트
.\gradlew.bat test

# 특정 모듈 테스트
.\gradlew.bat :feature:session:test

# 특정 클래스만
.\gradlew.bat :feature:session:test --tests "*SessionViewModelTest"
```

Android Studio에서도 가능:

- 테스트 파일 우클릭 → `Run 'SessionViewModelTest'`
- 혹은 Gradle 탭에서 `test` task 실행

---

## 7) 자주 만나는 문제와 해결

### 문제 A) `SDK location not found`

- Android Studio에서 SDK를 설치하고,
- `local.properties`에 SDK 경로가 잡혔는지 확인

예시:

```properties
sdk.dir=C:\\Users\\<사용자명>\\AppData\\Local\\Android\\Sdk
```

### 문제 B) 에뮬레이터에서 API 호출 실패

확인 순서:

1. 백엔드가 `0.0.0.0:8000`으로 실행 중인지
2. 앱의 `API_BASE_URL`이 에뮬레이터 기준(`10.0.2.2`)인지
3. 방화벽이 Python/8000 포트를 차단하지 않는지

### 문제 C) Hilt/KSP 관련 빌드 오류

- Gradle Sync 재실행
- `Build > Clean Project`, `Rebuild Project`
- Kotlin/AGP 버전 불일치 여부 확인

---

## 8) 릴리즈 빌드 및 서명(AAB)

구글플레이 배포는 일반적으로 **AAB(Android App Bundle)** 로 진행합니다.

### 8-1. 서명 키 생성 (최초 1회)

```bash
keytool -genkeypair -v -keystore whoistalking-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias whoistalking
```

생성된 `whoistalking-release.jks`는 안전한 위치(예: 사내 비밀 저장소)에 보관하세요.

### 8-2. 서명 정보 주입

보통 아래 방식 중 하나를 사용합니다.

- `gradle.properties`(로컬/CI 시크릿)
- 환경변수
- CI Secret Manager

예시(로컬 `~/.gradle/gradle.properties`):

```properties
RELEASE_STORE_FILE=C:/secure/whoistalking-release.jks
RELEASE_STORE_PASSWORD=********
RELEASE_KEY_ALIAS=whoistalking
RELEASE_KEY_PASSWORD=********
```

그리고 `app/build.gradle.kts`에 `signingConfigs` + `release` 설정을 추가해 AAB 생성 가능하게 구성합니다.

### 8-3. AAB 생성

```bash
.\gradlew.bat :app:bundleRelease
```

산출물 예시:

- `app/build/outputs/bundle/release/app-release.aab`

---

## 9) Google Play 배포 절차 (가능한 범위)

### 9-1. Play Console 준비

1. Google Play Console 개발자 계정 생성
2. 앱 생성 (기본 정보, 앱 카테고리, 연락처)
3. 앱 서명(App Signing) 설정 확인

### 9-2. 필수 정책/콘텐츠 작성

- 개인정보처리방침 URL
- 데이터 보안(Data safety)
- 앱 액세스/광고 ID/콘텐츠 등 설문
- 대상 연령/콘텐츠 등급

### 9-3. 릴리즈 생성

1. `테스트(Internal testing)` 트랙 권장
2. 생성한 `app-release.aab` 업로드
3. 릴리즈 노트 작성
4. 테스터 추가 후 배포

### 9-4. 운영 배포 전 체크리스트

- 앱 버전(`versionCode`, `versionName`) 증가
- Proguard/R8, 크래시 로깅, 모니터링 점검
- API endpoint/핀닝/보안 설정 운영값 반영
- QA 시나리오 및 회귀 테스트 완료

---

## 10) CI/CD 확장 권장

초보자가 팀 개발로 넘어갈 때 다음 자동화를 추천합니다.

- PR마다 `test`, `lint`, `assembleDebug` 실행
- 태그 기반 `bundleRelease` 자동 빌드
- Play Developer API + Fastlane/Gradle Play Publisher로 내부 테스트 트랙 자동 배포

---

## 11) 참고 파일

- 프로젝트 Android 설정: `android-app/build.gradle.kts`, `android-app/settings.gradle.kts`
- 앱 모듈 설정: `android-app/app/build.gradle.kts`
- API 주소: `android-app/app/src/main/java/com/whoistalking/androidapp/Config.kt`
- 단위 테스트 예시: `android-app/feature/session/src/test/java/com/whoistalking/feature/session/vm/SessionViewModelTest.kt`

필요하면 다음 단계로, 위 내용을 기준으로 **팀 온보딩용 체크리스트 버전(1페이지 요약)** 도 만들어드릴 수 있습니다.
