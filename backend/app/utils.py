import random
import string
from typing import Iterable


def clamp_text(text: str, limit: int = 160) -> str:
    t = " ".join((text or "").split())
    return t if len(t) <= limit else t[:limit]


def seat_labels(num_llm_speakers: int) -> list[str]:
    return ["A", *list(string.ascii_uppercase[1 : num_llm_speakers + 1])]


def pick_next_speaker(turn_counts: dict[str, int], turns_per_speaker: int, rng: random.Random) -> str | None:
    underfilled = {seat: count for seat, count in turn_counts.items() if count < turns_per_speaker}
    if not underfilled:
        return None

    min_count = min(underfilled.values())
    candidates = sorted([seat for seat, count in underfilled.items() if count == min_count])
    rng.shuffle(candidates)
    return candidates[0]


def transcript_from_messages(messages: Iterable[dict]) -> str:
    lines = [f"{m['seat']}: {m['text']}" for m in messages]
    return "\n".join(lines)
