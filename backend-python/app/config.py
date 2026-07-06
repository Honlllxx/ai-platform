from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    # Application
    app_name: str = "AI Platform - Python AI Service"
    app_version: str = "1.0.0"
    debug: bool = True

    # PostgreSQL (read-only) — override via env vars
    db_host: str = "localhost"
    db_port: int = 5432
    db_user: str = "postgres"
    db_password: str = ""
    db_name: str = "biz_form_recog_db"

    # SiliconFlow LLM API — override via SILICONFLOW_API_KEY env var
    siliconflow_api_key: str = ""
    siliconflow_base_url: str = "https://api.siliconflow.cn/v1"
    siliconflow_model: str = "zai-org/GLM-5.2"

    # OCR
    ocr_lang: str = "ch"
    ocr_use_gpu: bool = False

    # File upload
    upload_dir: str = "./uploads"
    max_upload_size_mb: int = 50

    # Pipeline
    pipeline_llm_timeout: int = 120  # seconds
    pipeline_ocr_timeout: int = 60

    @property
    def database_url(self) -> str:
        return (
            f"postgresql+asyncpg://{self.db_user}:{self.db_password}"
            f"@{self.db_host}:{self.db_port}/{self.db_name}"
        )

    @property
    def database_url_sync(self) -> str:
        return (
            f"postgresql+psycopg2://{self.db_user}:{self.db_password}"
            f"@{self.db_host}:{self.db_port}/{self.db_name}"
        )

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    return Settings()
