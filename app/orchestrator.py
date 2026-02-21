import asyncio
import json
import random
import re
from collections import defaultdict

from fastapi import WebSocket

from app.config import settings
from app.llm_client import LLMClient
from app.personas import pick_persona
from app.prompts import build_judge_messages, build_speaker_messages
from app.store import SQLiteStore
from app.utils import clamp_text, pick_next_speaker, seat_labels

PASS_MESSAGE = "잠깐 자리 비웠어. 다시 이어가자."
SPEAKER_FALLBACK = "음… 잠깐 생각이 끊겼네. 너는 어떻게 생각해?"
JUDGE_FALLBACK = "PICK=A CONF=0.5 WHY=판단 근거가 부족함"


class GameOrchestrator:
    def __init__(self, store: SQLiteStore, llm_client: LLMClient):
        self.store = store
        self.llm = llm_client
        self._locks: dict[str, asyncio.Lock] = defaultdict(asyncio.Lock)
        self._connected_clients: dict[str, dict[str, WebSocket]] = defaultdict(dict)
        self._timeout_tasks: dict[str, asyncio.Task] = {}
        self._engine_tasks: dict[str, asyncio.Task] = {}
        self.rng = random.Random()

    def create_session(self, topic: str, num_llm_speakers: int, turns_per_speaker: int, max_chars: int, difficulty: str) -> str:
        seats = seat_labels(num_llm_speakers)
        turn_counts = {seat: 0 for seat in seats}
        turn_state = {"current_speaker_seat": None, "turn_counts": turn_counts, "turn_index": 0}
        session_id = self.store.create_session(topic, turns_per_speaker, max_chars, difficulty, turn_state, config={})
        self.store.add_participant(session_id, "A", "human")
        for seat in seats[1:]:
            self.store.add_participant(session_id, seat, "llm_speaker", persona_id=pick_persona(self.rng))
        self.store.add_participant(session_id, "J", "judge")
        return session_id

    def _get_turn_state(self, session: dict) -> dict:
        return json.loads(session["turn_state_json"])

    def _public_participants(self, session_id: str):
        return [{"seat": p["seat"]} for p in self.store.list_participants(session_id) if p["type"] != "judge"]

    def session_snapshot(self, session_id: str) -> dict:
        session = self.store.get_session(session_id)
        if not session:
            raise ValueError("session not found")
        turn_state = self._get_turn_state(session)
        return {
            "type": "session.state",
            "session_id": session_id,
            "status": session["status"],
            "topic": session["topic"],
            "participants": self._public_participants(session_id),
            "turns_per_speaker": session["turns_per_speaker"],
            "turn_counts": turn_state["turn_counts"],
            "current_speaker_seat": turn_state.get("current_speaker_seat"),
            "max_chars": session["max_chars"],
        }

    async def register_client(self, session_id: str, client_id: str, websocket: WebSocket):
        async with self._locks[session_id]:
            old = self._connected_clients[session_id].get(client_id)
            self._connected_clients[session_id][client_id] = websocket
            if old and old is not websocket:
                await old.close(code=1000)

    async def unregister_client(self, session_id: str, client_id: str, websocket: WebSocket):
        async with self._locks[session_id]:
            current = self._connected_clients[session_id].get(client_id)
            if current is websocket:
                self._connected_clients[session_id].pop(client_id, None)

    async def _broadcast(self, session_id: str, payload: dict):
        dead = []
        for client_id, ws in self._connected_clients[session_id].items():
            try:
                await ws.send_json(payload)
            except Exception:
                dead.append(client_id)
        for client_id in dead:
            self._connected_clients[session_id].pop(client_id, None)

    def ensure_engine(self, session_id: str):
        task = self._engine_tasks.get(session_id)
        if task and not task.done():
            return
        self._engine_tasks[session_id] = asyncio.create_task(self._run_loop(session_id))

    async def _run_loop(self, session_id: str):
        while True:
            async with self._locks[session_id]:
                session = self.store.get_session(session_id)
                if not session:
                    return
                status = session["status"]
                turn_state = self._get_turn_state(session)
                if status == "LOBBY":
                    self.store.update_session(session_id, status="IN_PROGRESS")
                    status = "IN_PROGRESS"
                if status == "FINISHED":
                    return
                if status == "JUDGING":
                    await self._run_judge_locked(session_id, session, turn_state)
                    return

                if turn_state.get("current_speaker_seat") is None:
                    nxt = pick_next_speaker(turn_state["turn_counts"], session["turns_per_speaker"], self.rng)
                    if nxt is None:
                        self.store.update_session(session_id, status="JUDGING")
                        continue
                    turn_state["current_speaker_seat"] = nxt
                    self.store.update_session(session_id, turn_state=turn_state)
                    await self._broadcast(session_id, self.session_snapshot(session_id))

                current = turn_state["current_speaker_seat"]
                p_map = {p["seat"]: p for p in self.store.list_participants(session_id)}
                if p_map[current]["type"] == "human":
                    await self._broadcast(
                        session_id,
                        {
                            "type": "turn.request_human",
                            "current_speaker_seat": current,
                            "max_chars": session["max_chars"],
                            "timeout_secs": settings.human_turn_timeout_secs,
                        },
                    )
                    self._schedule_timeout(session_id)
                    return
            await self._run_llm_turn(session_id)

    def _schedule_timeout(self, session_id: str):
        task = self._timeout_tasks.get(session_id)
        if task and not task.done():
            task.cancel()
        self._timeout_tasks[session_id] = asyncio.create_task(self._timeout_worker(session_id))

    async def _timeout_worker(self, session_id: str):
        try:
            await asyncio.sleep(settings.human_turn_timeout_secs)
        except asyncio.CancelledError:
            return
        await self.handle_timeout_pass(session_id)

    async def _run_llm_turn(self, session_id: str):
        async with self._locks[session_id]:
            session = self.store.get_session(session_id)
            if not session or session["status"] != "IN_PROGRESS":
                return
            turn_state = self._get_turn_state(session)
            seat = turn_state["current_speaker_seat"]
            p_map = {p["seat"]: p for p in self.store.list_participants(session_id)}
            if p_map[seat]["type"] != "llm_speaker":
                return
            messages = self.store.list_messages(session_id)
            payload_messages = [
                {"seat": m["seat"], "text": m["text"]}
                for m in messages
            ]
            prompt = build_speaker_messages(
                topic=session["topic"],
                seat=seat,
                persona=p_map[seat]["persona_id"] or "평범함",
                messages=payload_messages,
                difficulty=session["difficulty"],
            )
        try:
            text = await self.llm.chat(
                model=settings.llm_model_speaker,
                messages=prompt,
                temperature=settings.llm_temperature_speaker,
                max_tokens=settings.llm_max_tokens_speaker,
            )
        except Exception:
            text = SPEAKER_FALLBACK

        await self._broadcast(session_id, {"type": "message.typing", "seat": seat})
        clamped = clamp_text(text, session["max_chars"])
        delay = min(max(len(clamped) * settings.typing_delay_per_char, settings.typing_delay_min_secs), settings.typing_delay_max_secs)
        await asyncio.sleep(delay)
        async with self._locks[session_id]:
            session = self.store.get_session(session_id)
            if not session or session["status"] != "IN_PROGRESS":
                return
            turn_state = self._get_turn_state(session)
            if turn_state.get("current_speaker_seat") != seat:
                return
            turn_state["turn_index"] += 1
            turn_idx = turn_state["turn_index"]
            msg_id = self.store.add_message(session_id, seat, turn_idx, clamped)
            turn_state["turn_counts"][seat] += 1
            turn_state["current_speaker_seat"] = None
            self.store.update_session(session_id, turn_state=turn_state)
            await self._broadcast(session_id, {"type": "message.new", "message_id": msg_id, "turn_index": turn_idx, "seat": seat, "text": clamped})
            nxt = pick_next_speaker(turn_state["turn_counts"], session["turns_per_speaker"], self.rng)
            if nxt is None:
                self.store.update_session(session_id, status="JUDGING")
            else:
                await self._broadcast(session_id, {"type": "turn.next", "current_speaker_seat": nxt, "turn_counts": turn_state["turn_counts"]})
        self.ensure_engine(session_id)

    async def handle_human_message(self, session_id: str, client_id: str, text: str):
        del client_id
        async with self._locks[session_id]:
            session = self.store.get_session(session_id)
            if not session or session["status"] != "IN_PROGRESS":
                return
            turn_state = self._get_turn_state(session)
            seat = turn_state.get("current_speaker_seat")
            if seat != "A":
                return
            task = self._timeout_tasks.get(session_id)
            if task and not task.done():
                task.cancel()
            clamped = clamp_text(text, session["max_chars"])
            turn_state["turn_index"] += 1
            turn_idx = turn_state["turn_index"]
            msg_id = self.store.add_message(session_id, seat, turn_idx, clamped)
            turn_state["turn_counts"][seat] += 1
            turn_state["current_speaker_seat"] = None
            self.store.update_session(session_id, turn_state=turn_state)
            await self._broadcast(session_id, {"type": "message.new", "message_id": msg_id, "turn_index": turn_idx, "seat": seat, "text": clamped})
            nxt = pick_next_speaker(turn_state["turn_counts"], session["turns_per_speaker"], self.rng)
            if nxt is None:
                self.store.update_session(session_id, status="JUDGING")
            else:
                await self._broadcast(session_id, {"type": "turn.next", "current_speaker_seat": nxt, "turn_counts": turn_state["turn_counts"]})
        self.ensure_engine(session_id)

    async def handle_timeout_pass(self, session_id: str):
        async with self._locks[session_id]:
            session = self.store.get_session(session_id)
            if not session or session["status"] != "IN_PROGRESS":
                return
            turn_state = self._get_turn_state(session)
            if turn_state.get("current_speaker_seat") != "A":
                return
            turn_state["turn_index"] += 1
            turn_idx = turn_state["turn_index"]
            clamped = clamp_text(PASS_MESSAGE, session["max_chars"])
            msg_id = self.store.add_message(session_id, "A", turn_idx, clamped)
            turn_state["turn_counts"]["A"] += 1
            turn_state["current_speaker_seat"] = None
            self.store.update_session(session_id, turn_state=turn_state)
            await self._broadcast(session_id, {"type": "message.new", "message_id": msg_id, "turn_index": turn_idx, "seat": "A", "text": clamped})
            nxt = pick_next_speaker(turn_state["turn_counts"], session["turns_per_speaker"], self.rng)
            if nxt is None:
                self.store.update_session(session_id, status="JUDGING")
            else:
                await self._broadcast(session_id, {"type": "turn.next", "current_speaker_seat": nxt, "turn_counts": turn_state["turn_counts"]})
        self.ensure_engine(session_id)

    async def _run_judge_locked(self, session_id: str, session: dict, turn_state: dict):
        del turn_state
        participants = [p for p in self.store.list_participants(session_id) if p["type"] != "judge"]
        seats = [p["seat"] for p in participants]
        logs = [{"seat": m["seat"], "text": m["text"]} for m in self.store.list_messages(session_id)]
        prompt = build_judge_messages(session["topic"], seats, logs)
        try:
            raw = await self.llm.chat(
                model=settings.llm_model_judge,
                messages=prompt,
                temperature=settings.llm_temperature_judge,
                max_tokens=settings.llm_max_tokens_judge,
            )
        except Exception:
            raw = JUDGE_FALLBACK
        parsed = self._parse_judge(raw, seats)
        self.store.save_result(session_id, parsed["pick_seat"], parsed["confidence"], clamp_text(parsed["why"], session["max_chars"]))
        self.store.update_session(session_id, status="FINISHED")
        await self._broadcast(
            session_id,
            {
                "type": "session.finished",
                "judge": {
                    "pick_seat": parsed["pick_seat"],
                    "confidence": parsed["confidence"],
                    "why": clamp_text(parsed["why"], session["max_chars"]),
                },
            },
        )

    def _parse_judge(self, text: str, seats: list[str]):
        content = clamp_text(text)
        pick = seats[0]
        conf = 0.5
        why = "판단 근거가 부족함"
        m_pick = re.search(r"PICK\s*=\s*([A-Z])", content)
        if m_pick and m_pick.group(1) in seats:
            pick = m_pick.group(1)
        m_conf = re.search(r"CONF\s*=\s*([0-9]*\.?[0-9]+)", content)
        if m_conf:
            conf = min(max(float(m_conf.group(1)), 0.0), 1.0)
        m_why = re.search(r"WHY\s*=\s*(.+)$", content)
        if m_why:
            why = m_why.group(1)
        return {"pick_seat": pick, "confidence": conf, "why": why}
