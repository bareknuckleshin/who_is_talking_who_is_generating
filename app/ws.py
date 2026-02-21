import json

from fastapi import APIRouter, WebSocket, WebSocketDisconnect

from app.orchestrator import GameOrchestrator
from app.schemas import HumanMessageEvent, JoinEvent, RequestStateEvent, ResumeEvent

router = APIRouter()


def make_ws_router(orchestrator: GameOrchestrator) -> APIRouter:
    @router.websocket("/ws/sessions/{session_id}")
    async def session_socket(websocket: WebSocket, session_id: str):
        await websocket.accept()
        client_id = None
        try:
            while True:
                payload = await websocket.receive_text()
                data = json.loads(payload)
                event_type = data.get("type")
                if event_type == "session.join":
                    ev = JoinEvent(**data)
                    client_id = ev.client_id
                    await orchestrator.register_client(session_id, client_id, websocket)
                    await websocket.send_json(orchestrator.session_snapshot(session_id))
                    orchestrator.ensure_engine(session_id)
                elif event_type == "session.resume":
                    ev = ResumeEvent(**data)
                    client_id = ev.client_id
                    await orchestrator.register_client(session_id, client_id, websocket)
                    await websocket.send_json(orchestrator.session_snapshot(session_id))
                    for m in orchestrator.store.list_messages(session_id):
                        await websocket.send_json(
                            {
                                "type": "message.new",
                                "message_id": m["id"],
                                "turn_index": m["turn_index"],
                                "seat": m["seat"],
                                "text": m["text"],
                            }
                        )
                    orchestrator.ensure_engine(session_id)
                elif event_type == "human.message":
                    ev = HumanMessageEvent(**data)
                    await orchestrator.handle_human_message(session_id, ev.client_id, ev.text)
                elif event_type == "session.request_state":
                    RequestStateEvent(**data)
                    await websocket.send_json(orchestrator.session_snapshot(session_id))
        except WebSocketDisconnect:
            if client_id:
                await orchestrator.unregister_client(session_id, client_id, websocket)

    return router
