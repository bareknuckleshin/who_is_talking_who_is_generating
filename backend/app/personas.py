import random

PERSONAS = [
    "말이 짧고 건조함",
    "수다스럽고 질문 많음",
    "논리적으로 정리하는 타입",
    "감정표현이 많은 타입",
]


def pick_persona(rng: random.Random) -> str:
    return rng.choice(PERSONAS)
