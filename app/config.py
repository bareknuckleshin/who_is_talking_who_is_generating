from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    db_path: str = "game.db"

    llm_base_url: str = "https://api.openai.com/v1"
    llm_api_key: str = ""
    llm_model_speaker: str = "gpt-4o-mini"
    llm_model_judge: str = "gpt-4o-mini"
    llm_timeout_secs: float = 15.0
    llm_max_tokens_speaker: int = 128
    llm_max_tokens_judge: int = 128
    llm_temperature_speaker: float = 0.9
    llm_temperature_judge: float = 0.2

    human_turn_timeout_secs: int = 60
    typing_delay_per_char: float = 0.1
    typing_delay_min_secs: float = 0.2
    typing_delay_max_secs: float = 2.4


settings = Settings()
