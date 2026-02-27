import json

from fastapi import FastAPI, HTTPException

from app.config import settings
from app.llm_client import LLMClient
from app.orchestrator import GameOrchestrator
from app.schemas import CreateSessionRequest, CreateSessionResponse, ResultResponse
from app.store import SQLiteStore
from app.ws import make_ws_router

store = SQLiteStore(settings.db_path)
llm_client = LLMClient()
orchestrator = GameOrchestrator(store, llm_client)

app = FastAPI(title="Human-or-LLM 추리 대화 게임")
app.include_router(make_ws_router(orchestrator))


@app.post("/sessions", response_model=CreateSessionResponse)
async def create_session(req: CreateSessionRequest):
    session_id = orchestrator.create_session(
        topic=req.topic,
        num_llm_speakers=req.num_llm_speakers,
        turns_per_speaker=req.turns_per_speaker,
        max_chars=req.max_chars,
        difficulty=req.difficulty,
    )
    return CreateSessionResponse(session_id=session_id, ws_url=f"/ws/sessions/{session_id}")


@app.get("/sessions/{session_id}")
async def get_session(session_id: str):
    session = store.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="session not found")
    turn_state = json.loads(session["turn_state_json"])
    return {
        "session_id": session_id,
        "status": session["status"],
        "topic": session["topic"],
        "participants": [{"seat": p["seat"]} for p in store.list_participants(session_id) if p["type"] != "judge"],
        "turns_per_speaker": session["turns_per_speaker"],
        "turn_counts": turn_state["turn_counts"],
        "current_speaker_seat": turn_state["current_speaker_seat"],
        "max_chars": session["max_chars"],
    }


@app.get("/sessions/{session_id}/messages")
async def get_messages(session_id: str):
    if not store.get_session(session_id):
        raise HTTPException(status_code=404, detail="session not found")
    return {"session_id": session_id, "messages": store.list_messages(session_id)}


@app.get("/sessions/{session_id}/result", response_model=ResultResponse)
async def get_result(session_id: str):
    result = store.get_result(session_id)
    if not result:
        raise HTTPException(status_code=404, detail="result not ready")
    return ResultResponse(session_id=session_id, pick_seat=result["pick_seat"], confidence=result["confidence"], why=result["why"])
