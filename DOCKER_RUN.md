# Docker 실행 가이드

이 문서는 `who_is_talking_who_is_generating` 프로젝트를 Docker로 실행하는 방법을 설명합니다.

## 1) 사전 준비

- Docker Desktop(Windows/macOS) 또는 Docker Engine(Linux) 설치
- 프로젝트 루트에 `.env` 파일 준비

```bash
cp .env.example .env
```

`.env`에 최소한 아래 값을 채우는 것을 권장합니다.

- `LLM_API_KEY`
- 필요 시 `LLM_BASE_URL`, `LLM_MODEL_SPEAKER`, `LLM_MODEL_JUDGE`

---

## 2) 이미지 빌드

프로젝트 루트(`Dockerfile`이 있는 위치)에서 실행:

```bash
docker build -t who-is-talking .
```

---

## 3) 컨테이너 실행

기본 실행:

```bash
docker run --rm -p 8000:8000 --env-file .env who-is-talking
```

실행 후 접속:

- API 문서: <http://localhost:8000/docs>
- 헬스 체크(예: 루트 경로): <http://localhost:8000/>

---

## 4) DB 파일(`game.db`) 영속화(권장)

기본 설정에서 SQLite 파일은 컨테이너 내부 `/app/game.db`에 생성됩니다.
재시작 시 데이터를 유지하려면 볼륨을 마운트하세요.

```bash
docker run --rm \
  -p 8000:8000 \
  --env-file .env \
  -v "$(pwd)/data:/app/data" \
  -e DB_PATH=/app/data/game.db \
  who-is-talking
```

> Windows PowerShell은 `$(pwd)` 대신 `${PWD}`를 사용할 수 있습니다.

---

## 5) 동작 확인 예시

세션 생성 테스트:

```bash
curl -X POST http://127.0.0.1:8000/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "일상 대화: 오늘 가장 짜증났던 일",
    "num_llm_speakers": 2,
    "turns_per_speaker": 5,
    "max_chars": 160,
    "language": "ko",
    "difficulty": "normal"
  }'
```

---

## 6) 자주 겪는 문제

### 6-1) `LLM_API_KEY` 미설정
- 증상: LLM 호출 실패
- 해결: `.env`에 `LLM_API_KEY` 설정 후 컨테이너 재실행

### 6-2) 포트 충돌(`8000`)
- 증상: `Bind for 0.0.0.0:8000 failed`
- 해결: 호스트 포트를 변경해 실행

```bash
docker run --rm -p 8001:8000 --env-file .env who-is-talking
```

### 6-3) 권한 문제(볼륨 마운트 시)
- 증상: DB 파일 생성/쓰기 실패
- 해결: 마운트한 로컬 폴더(`./data`)에 쓰기 권한 부여
