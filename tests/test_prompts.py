import unittest

from app.prompts import build_judge_messages, build_speaker_messages, difficulty_to_window


class PromptsTestCase(unittest.TestCase):
    def test_difficulty_to_window_defaults(self):
        self.assertEqual(difficulty_to_window("easy"), 4)
        self.assertEqual(difficulty_to_window("normal"), 8)
        self.assertEqual(difficulty_to_window("hard"), 14)
        self.assertEqual(difficulty_to_window("unknown"), 8)

    def test_build_speaker_messages_uses_recent_window(self):
        messages = [{"seat": "A", "text": f"m{i}"} for i in range(10)]
        prompt = build_speaker_messages("주제", "B", "페르소나", messages, "easy")
        self.assertEqual(prompt[0]["role"], "system")
        self.assertIn("A: m6", prompt[1]["content"])
        self.assertIn("A: m9", prompt[1]["content"])
        self.assertNotIn("A: m5", prompt[1]["content"])

    def test_build_judge_messages_includes_seats_and_logs(self):
        prompt = build_judge_messages("주제", ["A", "B"], [{"seat": "A", "text": "안녕"}])
        self.assertIn("좌석: A, B", prompt[1]["content"])
        self.assertIn("A: 안녕", prompt[1]["content"])


if __name__ == "__main__":
    unittest.main()
