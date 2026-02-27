# Backend - Human-or-LLM 추리 대화 게임 (FastAPI + WebSocket)

## 실행

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
# .env에 LLM_API_KEY 등 값 설정
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

## 문서

- Docker 실행: [`DOCKER_RUN.md`](./DOCKER_RUN.md)
- Windows 11 + WSL2 설정: [`WSL_WINDOWS11_SETUP.md`](./WSL_WINDOWS11_SETUP.md)

## 로컬 유닛 테스트

```bash
cd backend
python -m unittest discover -s tests -v
```

## WebSocket CLI 테스트

```bash
cd backend
python scripts/ws_test.py --session-id <session_id>
```

## 프로젝트 구조

```
backend/
  app/
  tests/
  scripts/
  requirements.txt
  Dockerfile
```

## Web 프론트엔드 실행

React + Vite + TypeScript 기반 Web UI는 `../web-frontend` 디렉터리에 있습니다.

```bash
cd web-frontend
cp .env.example .env
npm install
npm run dev
```
