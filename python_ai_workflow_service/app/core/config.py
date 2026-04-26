from functools import lru_cache
from pydantic import AliasChoices,Field
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

    model_api_key :str =Field(
        default="",
        validation_alias=AliasChoices("MODEL_API_KEY","AI_DASHSCOPE_API_KEY"),
    )
    model_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    model_name: str = "glm-5"
    model_timeout: float = 30.0

    mysql_host: str = "localhost"
    mysql_port: int = 3306
    mysql_user: str = "root"
    mysql_password: str = ""
    mysql_database: str = "inventory"
    mysql_charset: str = "utf8mb4"

@lru_cache
def get_settings() -> Settings:
    return Settings()