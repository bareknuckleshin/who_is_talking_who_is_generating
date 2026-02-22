# Who-is-Human Web UI (React + Vite + TS)

## 실행 방법

```bash
cd web
cp .env.example .env
npm install
npm run dev
```

기본 개발 서버: `http://localhost:5173`

## 환경 변수

`web/.env` 파일:

```bash
VITE_API_BASE_URL=http://localhost:8000
```

- HTTP 호출: `POST {VITE_API_BASE_URL}/sessions`
- WebSocket 호출: `http(s)`를 `ws(s)`로 변환 후 `ws_url` 경로를 이어서 연결

## 백엔드 연동 흐름

1. Home 화면에서 세션 생성 (`/sessions`)
2. `/s/:sessionId` 이동
3. WS 오픈 시 `client_id`와 `last_seen_message_id` 기준으로
   - 첫 접속: `session.join`
   - 재접속: `session.resume`
4. `message.new` 수신 시 `message_id` 중복 제거 및 `last_seen:{sessionId}` 업데이트
5. 끊김 시 backoff(1s/2s/5s) 재연결 후 자동 resume

## 주요 화면

- `/`: Topic, num_llm_speakers, turns_per_speaker 입력 후 Create
- `/s/:sessionId`: 채팅 로그/typing/human 입력창/종료 judge 결과 모달
