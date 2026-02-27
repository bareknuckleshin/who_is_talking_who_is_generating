import random
import unittest

from app.utils import clamp_text, pick_next_speaker, seat_labels, transcript_from_messages


class UtilsTestCase(unittest.TestCase):
    def test_clamp_text_normalizes_whitespace_and_limits_length(self):
        self.assertEqual(clamp_text("  hello   world  ", limit=20), "hello world")
        self.assertEqual(clamp_text("a" * 10, limit=4), "aaaa")

    def test_seat_labels_includes_human_and_llm_seats(self):
        self.assertEqual(seat_labels(1), ["A", "B"])
        self.assertEqual(seat_labels(3), ["A", "B", "C", "D"])

    def test_pick_next_speaker_picks_underfilled_min_count(self):
        rng = random.Random(42)
        counts = {"A": 1, "B": 0, "C": 0}
        picked = pick_next_speaker(counts, turns_per_speaker=2, rng=rng)
        self.assertIn(picked, {"B", "C"})

    def test_pick_next_speaker_returns_none_when_all_filled(self):
        rng = random.Random(42)
        self.assertIsNone(pick_next_speaker({"A": 2, "B": 2}, turns_per_speaker=2, rng=rng))

    def test_transcript_from_messages_renders_lines(self):
        rendered = transcript_from_messages([
            {"seat": "A", "text": "안녕"},
            {"seat": "B", "text": "반가워"},
        ])
        self.assertEqual(rendered, "A: 안녕\nB: 반가워")


if __name__ == "__main__":
    unittest.main()
