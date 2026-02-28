from typing import Literal
from uuid import UUID

from pydantic import BaseModel, Field


class CreateSessionRequest(BaseModel):
    topic: str
    num_llm_speakers: int = Field(1, ge=1, le=8)
    turns_per_speaker: int = Field(5, ge=1, le=50)
    max_chars: int = Field(160, ge=20, le=400)
    language: str = "ko"
    difficulty: Literal["easy", "normal", "hard"] = "normal"


class CreateSessionResponse(BaseModel):
    session_id: UUID
    ws_url: str


class SessionSnapshot(BaseModel):
    type: Literal["session.state"] = "session.state"
    session_id: UUID
    status: str
    topic: str
    participants: list[dict]
    turns_per_speaker: int
    turn_counts: dict[str, int]
    current_speaker_seat: str | None
    max_chars: int


class JoinEvent(BaseModel):
    type: Literal["session.join"]
    client_id: str


class ResumeEvent(BaseModel):
    type: Literal["session.resume"]
    client_id: str
    last_seen_message_id: str | None = None


class HumanMessageEvent(BaseModel):
    type: Literal["human.message"]
    client_id: str
    text: str


class RequestStateEvent(BaseModel):
    type: Literal["session.request_state"]
    client_id: str


class ResultResponse(BaseModel):
    session_id: UUID
    pick_seat: str
    confidence: float
    why: str
