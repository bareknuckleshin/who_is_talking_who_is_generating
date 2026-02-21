import tempfile
import unittest

from app.store import SQLiteStore


class SQLiteStoreTestCase(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.store = SQLiteStore(f"{self.temp_dir.name}/test.db")

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_session_message_and_result_lifecycle(self):
        session_id = self.store.create_session(
            topic="테스트 주제",
            turns_per_speaker=2,
            max_chars=160,
            difficulty="normal",
            turn_state={"current_speaker_seat": None, "turn_counts": {"A": 0}, "turn_index": 0},
            config={},
        )
        self.store.add_participant(session_id, "A", "human")

        message_id = self.store.add_message(session_id, "A", 1, "메시지")
        self.assertEqual(self.store.get_message_index(session_id, message_id), 1)
        self.assertEqual(len(self.store.list_messages(session_id)), 1)

        self.store.save_result(session_id, "A", 0.8, "근거")
        result = self.store.get_result(session_id)
        self.assertEqual(result["pick_seat"], "A")
        self.assertEqual(result["confidence"], 0.8)


if __name__ == "__main__":
    unittest.main()
