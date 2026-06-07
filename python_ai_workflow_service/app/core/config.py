from functools import lru_cache
from pydantic import Field
from pydantic_settings import BaseSettings,SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "inventory-python-ai-workflow-service"
    app_env: str = "dev"

    java_backend_base_url: str = "http://localhost:8080"
    java_backend_timeout: float = 10.0

    model_api_key: str = ""
    ai_dashscope_api_key: str = ""
    model_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    model_name: str = "deepseek-v4-pro"
    model_timeout: float = 30.0
    model_enable_thinking: bool = True
    model_reasoning_effort: str = "high"

    rag_embedding_model: str = "tongyi-embedding-vision-plus-2026-03-06"
    rag_embedding_timeout: float = 30.0
    rag_index_name: str = "inventory:rag:index"
    rag_key_prefix: str = "inventory:rag:chunk:"
    rag_default_top_k: int = 4
    rag_max_top_k: int = 10
    rag_similarity_threshold: float = 0.35

    redis_host: str = "114.132.43.247"
    redis_port: int = 6379
    redis_db: int = 0
    redis_password: str | None = None
    redis_socket_timeout: float = 5.0

    mysql_host: str = "localhost"
    mysql_port: int = 3306
    mysql_user: str = "root"
    mysql_password: str = ""
    mysql_database: str = "inventory"
    mysql_charset: str = "utf8mb4"

@lru_cache
def get_settings() -> Settings:
    return Settings()
