import argparse
import asyncio
import json
import uuid

import websockets


async def run(ws_url: str, session_id: str, resume: bool = False):
    client_id = str(uuid.uuid4())
    async with websockets.connect(f"{ws_url}/ws/sessions/{session_id}") as ws:
        first = {"type": "session.resume" if resume else "session.join", "client_id": client_id, "last_seen_message_id": None}
        if not resume:
            first.pop("last_seen_message_id")
        await ws.send(json.dumps(first))

        while True:
            msg = json.loads(await ws.recv())
            print(msg)
            if msg.get("type") == "turn.request_human":
                await ws.send(json.dumps({"type": "human.message", "client_id": client_id, "text": "오늘은 출근길 지하철이 너무 답답해서 진이 다 빠졌어."}))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--ws-base", default="ws://127.0.0.1:8000")
    parser.add_argument("--session-id", required=True)
    parser.add_argument("--resume", action="store_true")
    args = parser.parse_args()
    asyncio.run(run(args.ws_base, args.session_id, args.resume))
