# Human-or-LLM 추리 대화 게임 (FastAPI + WebSocket)

## 실행

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
# .env에 LLM_API_KEY 등 값 설정
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

## Windows 11 + WSL(2) 체크리스트

WSL 환경에서 바로 실행 가능하지만, 아래 5가지를 확인하면 문제를 크게 줄일 수 있습니다.

1. **Python 버전 확인 (3.12 권장)**
   ```bash
   python --version
   ```

2. **필수 빌드 도구/SSL 패키지 설치 (최초 1회)**
   ```bash
   sudo apt update
   sudo apt install -y python3-venv python3-pip ca-certificates
   ```

3. **회사망/프록시 환경이면 pip 프록시 설정 필요**
   - `pip install`이 403/ProxyError로 실패하면, WSL 내부에서 `HTTPS_PROXY`/`HTTP_PROXY` 설정 후 재시도하세요.

4. **Windows에서 WSL 서버 접속**
   - 기본적으로 `http://localhost:8000`로 접근 가능합니다.
   - 방화벽/보안 정책으로 차단되면 Windows 방화벽 예외를 확인하세요.

5. **DB 파일 권한/위치**
   - SQLite 파일(`game.db`)은 WSL 리눅스 파일시스템(예: 프로젝트 폴더) 안에 두는 것을 권장합니다.
   - `/mnt/c/...`에서 실행해도 동작하지만 I/O가 느릴 수 있습니다.

## 환경 변수

`.env.example`를 복사해 `.env`를 만들고 설정하세요.

- `LLM_BASE_URL`
- `LLM_API_KEY`
- `LLM_MODEL_SPEAKER`
- `LLM_MODEL_JUDGE`
- `LLM_TIMEOUT_SECS`
- `LLM_MAX_TOKENS_SPEAKER`
- `LLM_MAX_TOKENS_JUDGE`
- `LLM_TEMPERATURE_SPEAKER`
- `LLM_TEMPERATURE_JUDGE`
- `HUMAN_TURN_TIMEOUT_SECS`
- `TYPING_DELAY_PER_CHAR`
- `TYPING_DELAY_MIN_SECS`
- `TYPING_DELAY_MAX_SECS`
- `DB_PATH`

## 세션 생성 curl 예시

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

응답 예시:

```json
{
  "session_id": "<uuid>",
  "ws_url": "/ws/sessions/<uuid>"
}
```


## 로컬 유닛 테스트

주요 로직(오케스트레이터/프롬프트/유틸/스토어)에 대한 유닛 테스트를 `tests/`에 추가했습니다.

```bash
python -m unittest discover -s tests -v
```

## WebSocket 테스트

```bash
python scripts/ws_test.py --session-id <session_id>
```

- 자동으로 `session.join` 후 이벤트를 출력합니다.
- `turn.request_human` 수신 시 샘플 `human.message`를 보냅니다.

## MVP 포함 기능

- Human 1 + LLM N 좌석(`A`, `B`, `C`...) 운영
- 세션별 락으로 턴 진행/인간 입력/타임아웃 처리 동시성 제어
- 160자 제한(공백 정규화 후 초과 절단)
- Human 턴 타임아웃 시 패스 메시지 삽입
- LLM `message.typing` + 글자수 기반 지연 전송
- 재연결/재개(`session.resume`) 시 상태 + 메시지 재전송
- 모든 화자 횟수 충족 시 Judge 호출 후 `session.finished` 브로드캐스트
- HTTP 리플레이/결과 조회

## 프로젝트 구조

```
app/
  main.py
  ws.py
  orchestrator.py
  llm_client.py
  prompts.py
  personas.py
  schemas.py
  store.py
  utils.py
  config.py
scripts/
  ws_test.py
```
