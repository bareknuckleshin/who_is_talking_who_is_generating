import asyncio
import json
import sys
import tempfile
import types
import unittest

if "fastapi" not in sys.modules:
    fastapi_stub = types.ModuleType("fastapi")

    class WebSocket:  # pragma: no cover - test stub only
        pass

    fastapi_stub.WebSocket = WebSocket
    sys.modules["fastapi"] = fastapi_stub


if "httpx" not in sys.modules:
    httpx_stub = types.ModuleType("httpx")

    class TimeoutException(Exception):
        pass

    class TransportError(Exception):
        pass

    class HTTPStatusError(Exception):
        def __init__(self, *args, **kwargs):
            super().__init__(*args)

    class AsyncClient:  # pragma: no cover - test stub only
        def __init__(self, *args, **kwargs):
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, tb):
            return False

        async def post(self, *args, **kwargs):
            raise NotImplementedError

    httpx_stub.TimeoutException = TimeoutException
    httpx_stub.TransportError = TransportError
    httpx_stub.HTTPStatusError = HTTPStatusError
    httpx_stub.AsyncClient = AsyncClient
    sys.modules["httpx"] = httpx_stub

if "pydantic_settings" not in sys.modules:
    pydantic_settings_stub = types.ModuleType("pydantic_settings")

    class BaseSettings:  # pragma: no cover - test stub only
        def __init__(self, **kwargs):
            for name, value in self.__class__.__dict__.items():
                if name.startswith("_") or callable(value):
                    continue
                setattr(self, name, kwargs.get(name, value))

    def SettingsConfigDict(**kwargs):  # pragma: no cover - test stub only
        return kwargs

    pydantic_settings_stub.BaseSettings = BaseSettings
    pydantic_settings_stub.SettingsConfigDict = SettingsConfigDict
    sys.modules["pydantic_settings"] = pydantic_settings_stub

from app.orchestrator import PASS_MESSAGES, GameOrchestrator
from app.store import SQLiteStore


class DummyLLMClient:
    async def chat(self, **kwargs):
        return "PICK=A CONF=0.7 WHY=테스트"


class OrchestratorTestCase(unittest.IsolatedAsyncioTestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.store = SQLiteStore(f"{self.temp_dir.name}/test.db")
        self.orchestrator = GameOrchestrator(self.store, DummyLLMClient())
        self.orchestrator.ensure_engine = lambda session_id: None

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_create_session_sets_participants(self):
        session_id = self.orchestrator.create_session("주제", num_llm_speakers=2, turns_per_speaker=3, max_chars=160, difficulty="normal")
        participants = self.store.list_participants(session_id)
        seats = [p["seat"] for p in participants]
        self.assertEqual(seats, ["A", "B", "C", "J"])

    def test_parse_judge_parses_fields_and_clamps_confidence(self):
        parsed = self.orchestrator._parse_judge("PICK=B CONF=3.2 WHY=이유", ["A", "B"])
        self.assertEqual(parsed["pick_seat"], "B")
        self.assertEqual(parsed["confidence"], 1.0)
        self.assertEqual(parsed["why"], "이유")

    async def test_timeout_worker_returns_cleanly_when_cancelled(self):
        session_id = self.orchestrator.create_session("주제", num_llm_speakers=1, turns_per_speaker=1, max_chars=8, difficulty="normal")
        task = self.orchestrator._timeout_tasks.get(session_id)
        self.assertIsNone(task)

        task = asyncio.create_task(self.orchestrator._timeout_worker(session_id))
        await asyncio.sleep(0)
        task.cancel()
        await task
        self.assertFalse(task.cancelled())


    async def test_handle_timeout_pass_uses_random_pass_pool(self):
        session_id = self.orchestrator.create_session("주제", num_llm_speakers=1, turns_per_speaker=5, max_chars=160, difficulty="normal")
        self.store.update_session(
            session_id,
            status="IN_PROGRESS",
            turn_state={"current_speaker_seat": "A", "turn_counts": {"A": 0, "B": 0}, "turn_index": 0},
        )

        self.orchestrator.rng.seed(7)
        auto_messages = []
        for idx in range(3):
            await self.orchestrator.handle_timeout_pass(session_id)
            latest = self.store.list_messages(session_id)[-1]["text"]
            auto_messages.append(latest)
            self.assertIn(latest, PASS_MESSAGES)
            self.assertLessEqual(len(latest), 160)
            if idx < 2:
                session = self.store.get_session(session_id)
                turn_state = json.loads(session["turn_state_json"])
                turn_state["current_speaker_seat"] = "A"
                self.store.update_session(session_id, status="IN_PROGRESS", turn_state=turn_state)

        self.assertGreaterEqual(len(set(auto_messages)), 2)

    async def test_handle_human_message_updates_turn_state_and_status(self):
        session_id = self.orchestrator.create_session("주제", num_llm_speakers=1, turns_per_speaker=1, max_chars=8, difficulty="normal")
        self.store.update_session(
            session_id,
            status="IN_PROGRESS",
            turn_state={"current_speaker_seat": "A", "turn_counts": {"A": 0, "B": 1}, "turn_index": 0},
        )

        await self.orchestrator.handle_human_message(session_id, client_id="c1", text="  안녕   하세요 여러분  ")

        session = self.store.get_session(session_id)
        turn_state = json.loads(session["turn_state_json"])
        self.assertEqual(session["status"], "JUDGING")
        self.assertIsNone(turn_state["current_speaker_seat"])
        self.assertEqual(turn_state["turn_counts"]["A"], 1)

        messages = self.store.list_messages(session_id)
        self.assertEqual(len(messages), 1)
        self.assertEqual(messages[0]["text"], "안녕 하세요 여러분"[:8])


if __name__ == "__main__":
    unittest.main()
