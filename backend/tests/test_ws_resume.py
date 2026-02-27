import asyncio
import json
import sys
import types
import unittest

if "fastapi" not in sys.modules:
    fastapi_stub = types.ModuleType("fastapi")

    class WebSocket:  # pragma: no cover - test stub only
        pass

    class WebSocketDisconnect(Exception):
        pass

    class APIRouter:
        def __init__(self):
            self.routes = []

        def websocket(self, path):
            def decorator(fn):
                self.routes.append(types.SimpleNamespace(path=path, endpoint=fn))
                return fn

            return decorator

    fastapi_stub.WebSocket = WebSocket
    fastapi_stub.WebSocketDisconnect = WebSocketDisconnect
    fastapi_stub.APIRouter = APIRouter
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


if "pydantic" not in sys.modules:
    pydantic_stub = types.ModuleType("pydantic")

    class BaseModel:  # pragma: no cover - test stub only
        def __init__(self, **kwargs):
            annotations = getattr(self.__class__, "__annotations__", {})
            for field in annotations:
                default = getattr(self.__class__, field, None)
                value = kwargs.get(field, default)
                setattr(self, field, value)

    def Field(default=None, **kwargs):  # pragma: no cover - test stub only
        del kwargs
        return default

    pydantic_stub.BaseModel = BaseModel
    pydantic_stub.Field = Field
    sys.modules["pydantic"] = pydantic_stub
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

from fastapi import WebSocketDisconnect

from app.ws import make_ws_router


class DummyStore:
    def __init__(self):
        self.messages = []
        self.message_indexes = {}
        self.result = None

    def list_messages(self, session_id: str):
        del session_id
        return list(self.messages)

    def get_message_index(self, session_id: str, message_id: str):
        del session_id
        return self.message_indexes.get(message_id)

    def get_result(self, session_id: str):
        del session_id
        return self.result


class DummyOrchestrator:
    def __init__(self, store: DummyStore, status: str = "IN_PROGRESS"):
        self.store = store
        self._status = status

    async def register_client(self, session_id: str, client_id: str, websocket):
        del session_id, client_id, websocket

    async def unregister_client(self, session_id: str, client_id: str, websocket):
        del session_id, client_id, websocket

    def session_snapshot(self, session_id: str):
        return {"type": "session.state", "session_id": session_id, "status": self._status}

    def ensure_engine(self, session_id: str):
        del session_id


class FakeWebSocket:
    def __init__(self, events: list[dict]):
        self._payloads = [json.dumps(ev) for ev in events]
        self.sent = []

    async def accept(self):
        return None

    async def receive_text(self):
        if not self._payloads:
            raise WebSocketDisconnect()
        return self._payloads.pop(0)

    async def send_json(self, payload: dict):
        self.sent.append(payload)


class WsResumeTestCase(unittest.TestCase):
    def _run_resume(self, orchestrator: DummyOrchestrator, payload: dict):
        router = make_ws_router(orchestrator)
        endpoint = router.routes[-1].endpoint
        ws = FakeWebSocket([payload])
        asyncio.run(endpoint(ws, "s1"))
        return ws.sent

    def test_resume_filters_messages_after_last_seen_turn_index(self):
        store = DummyStore()
        store.messages = [
            {"id": "m1", "turn_index": 1, "seat": "A", "text": "one"},
            {"id": "m2", "turn_index": 2, "seat": "B", "text": "two"},
            {"id": "m3", "turn_index": 3, "seat": "A", "text": "three"},
        ]
        store.message_indexes = {"m2": 2}

        sent = self._run_resume(
            DummyOrchestrator(store),
            {"type": "session.resume", "client_id": "c1", "last_seen_message_id": "m2"},
        )

        self.assertEqual(sent[0]["type"], "session.state")
        self.assertEqual([m["message_id"] for m in sent[1:]], ["m3"])

    def test_resume_with_unknown_last_seen_message_sends_all_messages(self):
        store = DummyStore()
        store.messages = [
            {"id": "m1", "turn_index": 1, "seat": "A", "text": "one"},
            {"id": "m2", "turn_index": 2, "seat": "B", "text": "two"},
        ]

        sent = self._run_resume(
            DummyOrchestrator(store),
            {"type": "session.resume", "client_id": "c1", "last_seen_message_id": "unknown"},
        )

        self.assertEqual([m["message_id"] for m in sent[1:]], ["m1", "m2"])

    def test_resume_repushes_finished_payload_for_finished_session(self):
        store = DummyStore()
        store.result = {"pick_seat": "A", "confidence": 0.61, "why": "reason"}

        sent = self._run_resume(
            DummyOrchestrator(store, status="FINISHED"),
            {"type": "session.resume", "client_id": "c1", "last_seen_message_id": None},
        )

        self.assertEqual(sent[0]["type"], "session.state")
        self.assertEqual(sent[1]["type"], "session.finished")
        self.assertEqual(sent[1]["judge"]["pick_seat"], "A")


if __name__ == "__main__":
    unittest.main()
