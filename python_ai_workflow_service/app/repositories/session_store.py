import json
import uuid
from datetime import datetime
from typing import Any

import pymysql
from pymysql.cursors import DictCursor

from app.core.config import get_settings
from app.core.exceptions import ApiException
from app.schemas.workflow import WorkflowAgentResponse
from app.workflows.state import WorkflowStateKeys


class SessionStore:
    def __init__(self):
        self.settings = get_settings()
        self._init_db()

    def _conn(self):
        return pymysql.connect(
            host=self.settings.mysql_host,
            port=self.settings.mysql_port,
            user=self.settings.mysql_user,
            password=self.settings.mysql_password,
            database=self.settings.mysql_database,
            charset=self.settings.mysql_charset,
            autocommit=True,
            cursorclass=DictCursor,
        )

    def _init_db(self) -> None:
        ddl_list = [
            """
            create table if not exists agent_session (
                id bigint primary key auto_increment,
                session_no varchar(64) not null unique,
                thread_id varchar(128) not null unique,
                user_id bigint not null,
                title varchar(128) default null,
                agent_type varchar(64) not null default 'WORKFLOW_AGENT',
                current_intent varchar(64) default null,
                status varchar(32) not null default 'ACTIVE',
                last_message_time datetime default null,
                create_time datetime not null default current_timestamp,
                update_time datetime not null default current_timestamp on update current_timestamp,
                deleted tinyint(1) not null default 0,
                key idx_agent_session_user (user_id),
                key idx_agent_session_time (last_message_time)
            ) engine=innodb default charset=utf8mb4 collate=utf8mb4_unicode_ci
            """,
            """
            create table if not exists agent_message (
                id bigint primary key auto_increment,
                session_id bigint not null,
                thread_id varchar(128) not null,
                message_role varchar(32) not null,
                message_type varchar(32) not null,
                content text default null,
                node_name varchar(128) default null,
                tool_name varchar(128) default null,
                tool_request_json mediumtext default null,
                tool_response_json mediumtext default null,
                create_time datetime not null default current_timestamp,
                deleted tinyint(1) not null default 0,
                key idx_agent_message_session (session_id),
                key idx_agent_message_thread (thread_id),
                key idx_agent_message_time (create_time)
            ) engine=innodb default charset=utf8mb4 collate=utf8mb4_unicode_ci
            """,
            """
            create table if not exists agent_session_state (
                id bigint primary key auto_increment,
                session_id bigint not null unique,
                thread_id varchar(128) not null unique,
                current_node varchar(128) default null,
                current_intent varchar(64) default null,
                state_json mediumtext default null,
                create_time datetime not null default current_timestamp,
                update_time datetime not null default current_timestamp on update current_timestamp,
                deleted tinyint(1) not null default 0
            ) engine=innodb default charset=utf8mb4 collate=utf8mb4_unicode_ci
            """,
            """
            create table if not exists agent_result (
                id bigint primary key auto_increment,
                session_id bigint not null,
                thread_id varchar(128) not null,
                agent_type varchar(64) not null default 'WORKFLOW_AGENT',
                biz_type varchar(64) default null,
                biz_id bigint default null,
                biz_no varchar(64) default null,
                result_json mediumtext default null,
                summary varchar(1000) default null,
                create_time datetime not null default current_timestamp,
                deleted tinyint(1) not null default 0,
                key idx_agent_result_session (session_id),
                key idx_agent_result_thread (thread_id),
                key idx_agent_result_biz (biz_type, biz_id)
            ) engine=innodb default charset=utf8mb4 collate=utf8mb4_unicode_ci
            """,
        ]

        with self._conn() as conn:
            with conn.cursor() as cur:
                for ddl in ddl_list:
                    cur.execute(ddl)

    def prepare_session(self, thread_id: str | None, user_id: int, first_message: str | None) -> dict[str, Any]:
        if user_id is None:
            raise ApiException(code=401, msg="请先登录", http_status_code=401)

        with self._conn() as conn:
            with conn.cursor() as cur:
                if thread_id:
                    cur.execute(
                        "select * from agent_session where thread_id = %s and deleted = 0",
                        (thread_id,),
                    )
                    row = cur.fetchone()
                    if row is not None:
                        if int(row["user_id"]) != int(user_id):
                            raise ApiException(code=403, msg="无权访问该会话", http_status_code=403)
                        return row

                now = self._now()
                real_thread_id = thread_id or f"agt-{uuid.uuid4().hex}"
                session_no = "AS" + datetime.now().strftime("%Y%m%d%H%M%S%f")[:-3]
                cur.execute(
                    """
                    insert into agent_session (
                        session_no, thread_id, user_id, title, agent_type, status,
                        last_message_time, create_time, update_time
                    ) values (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """,
                    (
                        session_no,
                        real_thread_id,
                        user_id,
                        self._build_title(first_message),
                        "WORKFLOW_AGENT",
                        "ACTIVE",
                        now,
                        now,
                        now,
                    ),
                )
                cur.execute(
                    "select * from agent_session where thread_id = %s and deleted = 0",
                    (real_thread_id,),
                )
                return cur.fetchone()

    def save_user_message(self, session: dict[str, Any], content: str | None) -> None:
        self._save_message(session, "USER", "TEXT", content=content)

    def save_assistant_message(self, session: dict[str, Any], content: str | None) -> None:
        self._save_message(session, "ASSISTANT", "TEXT", content=content)

    def save_tool_message(
        self,
        thread_id: str,
        tool_name: str,
        tool_request_json: str,
        tool_response_json: str,
    ) -> None:
        with self._conn() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    "select * from agent_session where thread_id = %s and deleted = 0",
                    (thread_id,),
                )
                row = cur.fetchone()
                if row is None:
                    return
        self._save_message(
            row,
            "TOOL",
            "TOOL_RESULT",
            tool_name=tool_name,
            tool_request_json=tool_request_json,
            tool_response_json=tool_response_json,
        )

    def save_state(
        self,
        session: dict[str, Any],
        current_node: str,
        current_intent: str | None,
        state_data: dict[str, Any],
    ) -> None:
        safe_state = dict(state_data)
        safe_state.pop("authorization", None)
        safe_state.pop("finalResponse", None)
        safe_state_json = self._to_json(safe_state)
        now = self._now()

        with self._conn() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    """
                    insert into agent_session_state (
                        session_id, thread_id, current_node, current_intent, state_json, create_time, update_time
                    ) values (%s, %s, %s, %s, %s, %s, %s)
                    on duplicate key update
                        current_node = values(current_node),
                        current_intent = values(current_intent),
                        state_json = values(state_json),
                        update_time = values(update_time),
                        deleted = 0
                    """,
                    (
                        session["id"],
                        session["thread_id"],
                        current_node,
                        current_intent,
                        safe_state_json,
                        now,
                        now,
                    ),
                )

    def load_state_by_thread_id(self, thread_id: str | None) -> dict[str, Any]:
        if not thread_id:
            return {}

        with self._conn() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    "select state_json from agent_session_state where thread_id = %s and deleted = 0",
                    (thread_id,),
                )
                row = cur.fetchone()
                if row is None or not row["state_json"]:
                    return {}

        try:
            raw = json.loads(row["state_json"])
        except json.JSONDecodeError:
            return {}

        restored: dict[str, Any] = {}
        restorable_keys = [
            WorkflowStateKeys.INTENT,
            WorkflowStateKeys.ACTIVE_INTENT,
            WorkflowStateKeys.ENTITY,
            WorkflowStateKeys.CONVERSATION_MEMORY,
            WorkflowStateKeys.ORDER_CONTEXT,
            WorkflowStateKeys.ORDER_SNAPSHOT,
            WorkflowStateKeys.ORDER_DIAGNOSIS,
            WorkflowStateKeys.WARNING_CONTEXT,
            WorkflowStateKeys.WARNING_ANALYSIS,
            WorkflowStateKeys.SUPPLIER_CONTEXT,
            WorkflowStateKeys.SUPPLIER_METRICS,
            WorkflowStateKeys.SUPPLIER_SCORE,
            WorkflowStateKeys.ANSWER_PLAN,
            WorkflowStateKeys.SELECTED_CONTEXT,
            WorkflowStateKeys.ANSWER_CARD,
        ]

        for key in restorable_keys:
            if key in raw:
                restored[key] = raw[key]

        return restored

    def save_result(self, session: dict[str, Any], response: WorkflowAgentResponse) -> None:
        if response.data is None:
            return

        biz_type = None
        biz_id = None
        biz_no = None

        if response.intent == "ORDER_DIAGNOSIS":
            biz_type = "PURCHASE_ORDER"
            if isinstance(response.data, dict):
                biz_no = response.data.get("orderNo")
        elif response.intent == "WARNING_SCAN":
            biz_type = "WARNING_SCAN"
        elif response.intent == "SUPPLIER_SCORE":
            biz_type = "SUPPLIER"
            if isinstance(response.data, dict):
                biz_id = response.data.get("supplierId")

        with self._conn() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    """
                    insert into agent_result (
                        session_id, thread_id, agent_type, biz_type, biz_id, biz_no,
                        result_json, summary, create_time
                    ) values (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """,
                    (
                        session["id"],
                        session["thread_id"],
                        "WORKFLOW_AGENT",
                        biz_type,
                        biz_id,
                        biz_no,
                        self._to_json(response.model_dump(by_alias=True)),
                        response.answer,
                        self._now(),
                    ),
                )

    def update_session_intent(self, session_id: int, current_intent: str | None) -> None:
        now = self._now()
        with self._conn() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    """
                    update agent_session
                    set current_intent = %s, last_message_time = %s, update_time = %s
                    where id = %s and deleted = 0
                    """,
                    (current_intent, now, now, session_id),
                )

    def list_sessions(self, user_id: int) -> list[dict[str, Any]]:
        with self._conn() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    """
                    select id, session_no, thread_id, user_id, title, agent_type,
                           current_intent, status, last_message_time, create_time
                    from agent_session
                    where user_id = %s and deleted = 0
                    order by last_message_time desc, id desc
                    """,
                    (user_id,),
                )
                return cur.fetchall()

    def get_messages(self, thread_id: str, user_id: int) -> list[dict[str, Any]]:
        with self._conn() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    """
                    select * from agent_session
                    where thread_id = %s and user_id = %s and deleted = 0
                    """,
                    (thread_id, user_id),
                )
                session = cur.fetchone()
                if session is None:
                    raise ApiException(code=403, msg="无权访问该会话", http_status_code=403)

                cur.execute(
                    """
                    select id, session_id, thread_id, message_role, message_type, content, create_time
                    from agent_message
                    where thread_id = %s and deleted = 0
                    order by create_time asc, id asc
                    """,
                    (thread_id,),
                )
                return cur.fetchall()

    def _save_message(
        self,
        session: dict[str, Any],
        role: str,
        message_type: str,
        content: str | None = None,
        node_name: str | None = None,
        tool_name: str | None = None,
        tool_request_json: str | None = None,
        tool_response_json: str | None = None,
    ) -> None:
        now = self._now()
        with self._conn() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    """
                    insert into agent_message (
                        session_id, thread_id, message_role, message_type, content,
                        node_name, tool_name, tool_request_json, tool_response_json, create_time
                    ) values (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """,
                    (
                        session["id"],
                        session["thread_id"],
                        role,
                        message_type,
                        content,
                        node_name,
                        tool_name,
                        tool_request_json,
                        tool_response_json,
                        now,
                    ),
                )
                cur.execute(
                    """
                    update agent_session
                    set last_message_time = %s, update_time = %s
                    where id = %s and deleted = 0
                    """,
                    (now, now, session["id"]),
                )

    def _build_title(self, message: str | None) -> str:
        if not message or not message.strip():
            return "新会话"
        return message.strip()[:30]

    def _to_json(self, value: Any) -> str:
        return json.dumps(value, ensure_ascii=False, default=str)

    def _now(self) -> datetime:
        return datetime.now()
