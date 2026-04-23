from typing import Any


class ApiException(Exception):
    def __init__(
        self,
        code: int = 500,
        msg: str = "系统异常",
        data: Any = None,
        http_status_code: int | None = None,
    ):
        super().__init__(msg)
        self.code = code
        self.msg = msg
        self.data = data
        self.http_status_code = http_status_code if http_status_code is not None else self._guess_http_status(code)

    @staticmethod
    def _guess_http_status(code: int) -> int:
        if code in (401, 403):
            return code
        return 200


class BackendBusinessException(ApiException):
    pass


class BackendHttpException(ApiException):
    def __init__(self, msg: str, data: Any = None):
        super().__init__(code=500, msg=msg, data=data, http_status_code=500)