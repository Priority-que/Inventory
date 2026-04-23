from typing import Any

from fastapi.encoders import jsonable_encoder


def success(data:Any = None,msg: str="success") -> dict[str, Any]:
    return{
        "code": 200,
        "msg": msg,
        "data": jsonable_encoder(data,by_alias=True)
    }
def fail(code: int = 500, msg: str = "系统异常", data: Any = None) -> dict[str, Any]:
    return {
        "code": code,
        "msg": msg,
        "data": jsonable_encoder(data, by_alias=True),
    }