# Android App (WhoIsTalkingAndroid)

본 앱은 **확장성 + 테스트 가능성** 중심의 Modern Android Development 샘플 구조로 재구성되었습니다.

## 핵심 설계

- **MVI-inspired UDF**: Intent → ViewModel → UiState + UiSideEffect
- **Clean Architecture / Multi-module**
  - `:core:ui`
  - `:core:domain`
  - `:core:data`
  - `:feature:session`
  - `:app`
- **SSoT**: Room + Paging3 + RemoteMediator
- **DI**: Hilt 기반 의존성 주입
- **Process Death 대응**: `SavedStateHandle`로 draft/session 상태 복구
- **보안/운영**: Certificate Pinning, EncryptedSharedPreferences, Integrity/Telemetry 스캐폴딩

## 실행

1. Android Studio에서 `android-app/` Open
2. Gradle Sync
3. `api.example.com` / pin 값 / Remote Config 연동부를 실서비스 설정으로 교체
4. Run
