# Who Is Talking / Who Is Generating - Monorepo

요청하신 형태로 최상위 디렉터리를 다음 4개 축으로 정리했습니다.

```
.
├─ backend/        # FastAPI + WebSocket 서버
├─ web-frontend/   # React + Vite 웹 클라이언트
├─ ios-app/        # iPhone 앱(iOS SwiftUI 래퍼)
└─ android-app/    # Android 앱(현재 미구현 placeholder)
```

## 추천 구조 반영 사항

요청하신 4분할 구조를 그대로 적용했고, 운영 편의를 위해 아래 원칙을 함께 적용했습니다.

- 백엔드 관련 문서/테스트/도커 파일을 `backend/`로 일원화
- 웹 프론트는 `web-frontend/`로 독립 실행 가능
- iOS는 웹 게임을 바로 실행할 수 있는 SwiftUI + WKWebView 앱 스캐폴드 제공
- Android는 추후 구현을 위한 명확한 placeholder 문서 제공

## 빠른 시작

### 1) Backend

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 2) Web Frontend

```bash
cd web-frontend
cp .env.example .env
npm install
npm run dev
```

### 3) iOS App

```bash
cd ios-app
# README 참고: Xcode에서 WhoIsTalkingIOSApp.swift를 시작점으로 실행
```

세부 안내:
- backend 문서: [`backend/README.md`](./backend/README.md)
- web 문서: [`web-frontend/README.md`](./web-frontend/README.md)
- ios 문서: [`ios-app/README.md`](./ios-app/README.md)
- android placeholder: [`android-app/README.md`](./android-app/README.md)
