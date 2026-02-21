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
                    snapshot = orchestrator.session_snapshot(session_id)
                    await websocket.send_json(snapshot)

                    messages = orchestrator.store.list_messages(session_id)
                    if ev.last_seen_message_id:
                        last_turn_index = orchestrator.store.get_message_index(session_id, ev.last_seen_message_id)
                        if last_turn_index is not None:
                            messages = [m for m in messages if m["turn_index"] > last_turn_index]

                    for m in messages:
                        await websocket.send_json(
                            {
                                "type": "message.new",
                                "message_id": m["id"],
                                "turn_index": m["turn_index"],
                                "seat": m["seat"],
                                "text": m["text"],
                            }
                        )

                    if snapshot["status"] == "FINISHED":
                        result = orchestrator.store.get_result(session_id)
                        if result:
                            await websocket.send_json(
                                {
                                    "type": "session.finished",
                                    "judge": {
                                        "pick_seat": result["pick_seat"],
                                        "confidence": result["confidence"],
                                        "why": result["why"],
                                    },
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
