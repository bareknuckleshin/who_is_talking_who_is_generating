# Windows 11에서 WSL2 + 프로젝트 개발 환경 설정 가이드

이 문서는 **Windows 11 사용자**가 WSL2(Ubuntu 기준)에서 이 프로젝트를 실행/개발할 수 있도록 최소~권장 설정을 순서대로 정리한 가이드입니다.

## 1) Windows 11에서 WSL2 설치

PowerShell(관리자)에서 아래 명령 실행:

```powershell
wsl --install
```

설치가 끝나면 재부팅 후 Ubuntu(또는 기본 배포판) 최초 실행 시
리눅스 사용자 계정(아이디/비밀번호)을 생성합니다.

> 이미 설치되어 있다면 버전 확인:
>
> ```powershell
> wsl --status
> wsl -l -v
> ```
>
> 배포판 버전이 2가 아니라면:
>
> ```powershell
> wsl --set-version <DistroName> 2
> ```

---

## 2) Ubuntu(WSL) 기본 패키지 준비

WSL 터미널에서 아래를 실행합니다.

```bash
sudo apt update
sudo apt install -y \
  python3 python3-venv python3-pip \
  build-essential ca-certificates curl git
```

Python 버전 확인(권장: 3.12 이상):

```bash
python3 --version
```

---

## 3) 프로젝트 위치 권장

성능/파일 감시(핫리로드) 이슈를 줄이려면, 프로젝트를 **리눅스 파일시스템 내부**(예: `~/workspace`)에 두는 것을 권장합니다.

```bash
mkdir -p ~/workspace
cd ~/workspace
# 예시: git clone <repo_url>
```

> `/mnt/c/...` 경로에서도 실행은 가능하지만 I/O 속도가 느릴 수 있습니다.

---

## 4) 프로젝트 파이썬 가상환경 구성

프로젝트 루트에서:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
pip install -r requirements.txt
```

---

## 5) 환경 변수 파일 설정

```bash
cp .env.example .env
```

`.env`에서 최소한 아래 값을 환경에 맞게 채워 주세요.

- `LLM_BASE_URL`
- `LLM_API_KEY`
- `LLM_MODEL_SPEAKER`
- `LLM_MODEL_JUDGE`
- `DB_PATH` (예: `./game.db`)

회사/기관 프록시 환경에서 패키지 설치가 실패하면 `HTTP_PROXY`/`HTTPS_PROXY`를 WSL에 설정한 뒤 다시 설치합니다.

---

## 6) 서버 실행

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

접속:

- WSL 내부: `http://127.0.0.1:8000`
- Windows 브라우저: `http://localhost:8000`

Windows에서 접속이 안 되면 방화벽/보안 솔루션 정책을 확인합니다.

---

## 7) 빠른 동작 확인

세션 생성 API 테스트:

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

WebSocket 테스트:

```bash
python scripts/ws_test.py --session-id <session_id>
```

---

## 8) 테스트 실행

```bash
python -m unittest discover -s tests -v
```

---

## 9) 자주 겪는 문제 해결

1. **`pip install` 실패(SSL/인증서/프록시)**
   - `ca-certificates` 설치 여부 확인
   - 프록시 환경이면 `HTTP_PROXY`, `HTTPS_PROXY` 설정

2. **Windows 브라우저에서 `localhost:8000` 접속 불가**
   - 서버가 `--host 0.0.0.0`로 실행 중인지 확인
   - Windows 방화벽/백신 정책 확인

3. **SQLite 성능 저하**
   - `DB_PATH`를 WSL 리눅스 파일시스템 내부 경로로 설정

4. **가상환경 활성화 누락**
   - 매 세션 시작 시 `source .venv/bin/activate` 실행

---

## 10) 권장 개발 루틴

```bash
# 1) WSL 터미널 진입 후
cd <project_root>
source .venv/bin/activate

# 2) 서버 실행
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

# 3) 별도 터미널에서 테스트
python -m unittest discover -s tests -v
```

필요 시 `.env`를 환경별로 분리(`.env.local`, `.env.dev`)해 관리하세요.
