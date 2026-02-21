import json
import sqlite3
from contextlib import contextmanager
from datetime import datetime, timezone
from pathlib import Path
from uuid import uuid4


class SQLiteStore:
    def __init__(self, db_path: str):
        self.db_path = db_path
        Path(db_path).parent.mkdir(parents=True, exist_ok=True)
        self._init_db()

    @contextmanager
    def _conn(self):
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        try:
            yield conn
            conn.commit()
        finally:
            conn.close()

    def _init_db(self):
        with self._conn() as conn:
            conn.executescript(
                """
                CREATE TABLE IF NOT EXISTS sessions (
                    id TEXT PRIMARY KEY,
                    topic TEXT NOT NULL,
                    status TEXT NOT NULL,
                    turns_per_speaker INTEGER NOT NULL,
                    max_chars INTEGER NOT NULL,
                    difficulty TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    turn_state_json TEXT NOT NULL,
                    config_json TEXT
                );
                CREATE TABLE IF NOT EXISTS participants (
                    id TEXT PRIMARY KEY,
                    session_id TEXT NOT NULL,
                    seat TEXT NOT NULL,
                    type TEXT NOT NULL,
                    persona_id TEXT,
                    display_name TEXT,
                    FOREIGN KEY(session_id) REFERENCES sessions(id)
                );
                CREATE TABLE IF NOT EXISTS messages (
                    id TEXT PRIMARY KEY,
                    session_id TEXT NOT NULL,
                    seat TEXT NOT NULL,
                    turn_index INTEGER NOT NULL,
                    text TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY(session_id) REFERENCES sessions(id)
                );
                CREATE TABLE IF NOT EXISTS results (
                    session_id TEXT PRIMARY KEY,
                    pick_seat TEXT NOT NULL,
                    confidence REAL NOT NULL,
                    why TEXT NOT NULL,
                    FOREIGN KEY(session_id) REFERENCES sessions(id)
                );
                """
            )

    def create_session(self, topic: str, turns_per_speaker: int, max_chars: int, difficulty: str, turn_state: dict, config: dict | None):
        session_id = str(uuid4())
        now = datetime.now(timezone.utc).isoformat()
        with self._conn() as conn:
            conn.execute(
                """INSERT INTO sessions (id, topic, status, turns_per_speaker, max_chars, difficulty, created_at, updated_at, turn_state_json, config_json)
                VALUES (?, ?, 'LOBBY', ?, ?, ?, ?, ?, ?, ?)""",
                (session_id, topic, turns_per_speaker, max_chars, difficulty, now, now, json.dumps(turn_state), json.dumps(config or {})),
            )
        return session_id

    def add_participant(self, session_id: str, seat: str, p_type: str, persona_id: str | None = None):
        with self._conn() as conn:
            conn.execute(
                "INSERT INTO participants (id, session_id, seat, type, persona_id) VALUES (?, ?, ?, ?, ?)",
                (str(uuid4()), session_id, seat, p_type, persona_id),
            )

    def get_session(self, session_id: str):
        with self._conn() as conn:
            row = conn.execute("SELECT * FROM sessions WHERE id = ?", (session_id,)).fetchone()
            return dict(row) if row else None

    def update_session(self, session_id: str, *, status: str | None = None, turn_state: dict | None = None):
        if status is None and turn_state is None:
            return
        sets, values = [], []
        if status is not None:
            sets.append("status = ?")
            values.append(status)
        if turn_state is not None:
            sets.append("turn_state_json = ?")
            values.append(json.dumps(turn_state))
        sets.append("updated_at = ?")
        values.append(datetime.now(timezone.utc).isoformat())
        values.append(session_id)
        with self._conn() as conn:
            conn.execute(f"UPDATE sessions SET {', '.join(sets)} WHERE id = ?", tuple(values))

    def list_participants(self, session_id: str):
        with self._conn() as conn:
            rows = conn.execute("SELECT seat, type, persona_id, display_name FROM participants WHERE session_id = ? ORDER BY seat", (session_id,)).fetchall()
            return [dict(r) for r in rows]

    def add_message(self, session_id: str, seat: str, turn_index: int, text: str):
        message_id = str(uuid4())
        with self._conn() as conn:
            conn.execute(
                "INSERT INTO messages (id, session_id, seat, turn_index, text, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                (message_id, session_id, seat, turn_index, text, datetime.now(timezone.utc).isoformat()),
            )
        return message_id

    def list_messages(self, session_id: str):
        with self._conn() as conn:
            rows = conn.execute(
                "SELECT id, seat, turn_index, text, created_at FROM messages WHERE session_id = ? ORDER BY turn_index ASC, created_at ASC",
                (session_id,),
            ).fetchall()
            return [dict(r) for r in rows]

    def get_message_index(self, session_id: str, message_id: str):
        with self._conn() as conn:
            row = conn.execute(
                "SELECT turn_index FROM messages WHERE session_id = ? AND id = ?",
                (session_id, message_id),
            ).fetchone()
            return row["turn_index"] if row else None

    def save_result(self, session_id: str, pick_seat: str, confidence: float, why: str):
        with self._conn() as conn:
            conn.execute(
                "INSERT OR REPLACE INTO results (session_id, pick_seat, confidence, why) VALUES (?, ?, ?, ?)",
                (session_id, pick_seat, confidence, why),
            )

    def get_result(self, session_id: str):
        with self._conn() as conn:
            row = conn.execute("SELECT * FROM results WHERE session_id = ?", (session_id,)).fetchone()
            return dict(row) if row else None
