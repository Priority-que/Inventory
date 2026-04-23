from typing import Any

import httpx

from app.core.config import get_settings
from app.core.exceptions import ApiException, BackendBusinessException, BackendHttpException
from app.schemas.common import CurrentUserVO


class InventoryBackendClient:
    def __init__(self):
        self.settings = get_settings()

    def _headers(self, authorization: str | None) -> dict[str, str]:
        if not authorization:
            raise ApiException(code=401, msg="未登录或者登录已失效", http_status_code=401)
        if not authorization.startswith("Bearer "):
            raise ApiException(code=401, msg="Authorization 必须使用 Bearer Token", http_status_code=401)
        return {
            "Authorization": authorization,
            "Content-Type": "application/json",
        }

    async def _request(
        self,
        method: str,
        path: str,
        authorization: str | None,
        params: dict[str, Any] | None = None,
        json: dict[str, Any] | None = None,
    ) -> Any:
        url = self.settings.java_backend_base_url.rstrip("/") + path
        headers = self._headers(authorization)

        try:
            async with httpx.AsyncClient(timeout=self.settings.java_backend_timeout, trust_env=False) as client:
                response = await client.request(
                    method=method,
                    url=url,
                    headers=headers,
                    params=params,
                    json=json,
                )
        except httpx.RequestError as exc:
            raise BackendHttpException(f"请求 Java 后端失败：{exc}") from exc

        if response.status_code in (401, 403):
            try:
                body = response.json()
                raise ApiException(
                    code=int(body.get("code", response.status_code)),
                    msg=body.get("msg", response.text),
                    data=body.get("data"),
                    http_status_code=response.status_code,
                )
            except ValueError:
                raise ApiException(code=response.status_code, msg=response.text, http_status_code=response.status_code)

        if response.status_code >= 400:
            raise BackendHttpException(
                f"Java 后端 HTTP 异常：method={method} url={url} "
                f"status={response.status_code} body={response.text}"
            )

        try:
            body = response.json()
        except ValueError as exc:
            raise BackendHttpException(f"Java 后端返回非 JSON：{response.text}") from exc

        if not isinstance(body, dict) or "code" not in body:
            raise BackendHttpException(f"Java 后端返回不是标准 Result 结构：{body}")

        code = int(body.get("code", 500))
        msg = body.get("msg") or "Java 后端业务异常"
        data = body.get("data")
        if code != 200:
            raise BackendBusinessException(code=code, msg=msg, data=data)
        return data

    async def get_current_user(self, authorization: str | None) -> CurrentUserVO:
        data = await self._request("GET", "/auth/me", authorization)
        return CurrentUserVO(**data)

    async def get_purchase_order_by_order_no(self, order_no: str, authorization: str) -> dict[str, Any] | None:
        data = await self._request(
            "GET",
            "/purchaseOrder/getPurchaseOrderPage",
            authorization,
            params={"pageNum": 1, "pageSize": 1, "orderNo": order_no},
        )
        if not isinstance(data, dict):
            return None
        records = data.get("records") or []
        return records[0] if records else None

    async def list_purchase_orders(
        self,
        authorization: str,
        status: str | None = None,
        supplier_name: str | None = None,
        page_size: int = 200,
    ) -> list[dict[str, Any]]:
        params: dict[str, Any] = {"pageNum": 1, "pageSize": page_size}
        if status:
            params["status"] = status
        if supplier_name:
            params["supplierName"] = supplier_name
        data = await self._request("GET", "/purchaseOrder/getPurchaseOrderPage", authorization, params=params)
        if not isinstance(data, dict):
            return []
        return data.get("records") or []

    async def get_purchase_order_items(self, order_id: int, authorization: str) -> list[dict[str, Any]]:
        data = await self._request(
            "GET",
            f"/purchaseOrderItem/getPurchaseOrderItemByOrderId/{order_id}",
            authorization,
        )
        return data if isinstance(data, list) else []

    async def list_arrivals(
        self,
        authorization: str,
        order_no: str | None = None,
        page_size: int = 200,
    ) -> list[dict[str, Any]]:
        params: dict[str, Any] = {"pageNum": 1, "pageSize": page_size}
        if order_no:
            params["orderNo"] = order_no
        data = await self._request("GET", "/arrival/getArrivalPage", authorization, params=params)
        if not isinstance(data, dict):
            return []
        return data.get("records") or []

    async def list_inbounds(
        self,
        authorization: str,
        order_no: str | None = None,
        arrival_no: str | None = None,
        status: str | None = None,
        page_size: int = 200,
    ) -> list[dict[str, Any]]:
        params: dict[str, Any] = {"pageNum": 1, "pageSize": page_size}
        if order_no:
            params["orderNo"] = order_no
        if arrival_no:
            params["arrivalNo"] = arrival_no
        if status:
            params["status"] = status
        data = await self._request("GET", "/inbound/getInboundPage", authorization, params=params)
        if not isinstance(data, dict):
            return []
        return data.get("records") or []

    async def list_suppliers(self, authorization: str, page_size: int = 200) -> list[dict[str, Any]]:
        data = await self._request(
            "GET",
            "/supplier/getSupplierPage",
            authorization,
            params={"pageNum": 1, "pageSize": page_size},
        )
        if not isinstance(data, dict):
            return []
        return data.get("records") or []

    async def get_supplier_by_id(self, supplier_id: int, authorization: str) -> dict[str, Any] | None:
        suppliers = await self.list_suppliers(authorization)
        for supplier in suppliers:
            if int(supplier.get("id", 0)) == int(supplier_id):
                return supplier
        return None

    async def rag_search(self, payload: dict[str, Any], authorization: str) -> list[dict[str, Any]]:
        data = await self._request("POST", "/agent/rag/search", authorization, json=payload)
        return data if isinstance(data, list) else []


    async def get_agent_order_context(self, order_no: str, authorization: str) -> dict[str, Any] | None:
        data = await self._request("GET", f"/agent/context/order/{order_no}", authorization)
        return data if isinstance(data, dict) else None

    async def get_agent_warning_context(self, days: int, authorization: str) -> dict[str, Any] | None:
        data = await self._request("GET", "/agent/context/warnings", authorization, params={"days": days})
        return data if isinstance(data, dict) else None

    async def get_agent_supplier_context(self, supplier_id: int, days: int, authorization: str) -> dict[str, Any] | None:
        data = await self._request(
            "GET",
            f"/agent/context/supplier/{supplier_id}",
            authorization,
            params={"days": days},
        )
        return data if isinstance(data, dict) else None

