from pydantic import ConfigDict, BaseModel, Field


class ApiModel(BaseModel):
    model_config = ConfigDict(populate_by_name=True,arbitrary_types_allowed=True)

class CurrentUserVO(ApiModel):
    id: int
    username: str | None = None
    name: str | None = None
    dept: str | None = None
    status: str | None = None
    role_codes: list[str] = Field(default_factory=list, alias="roleCodes")
