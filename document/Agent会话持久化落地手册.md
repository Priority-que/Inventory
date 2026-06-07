# Agent 会话持久化落地手册

## 0. 文档目标

你后续准备统一使用：

```text
POST /agent/workflow/execute
```

作为 Agent 的唯一主入口。

因此，会话持久化的目标不是简单保存聊天文本，而是让这个统一入口具备以下能力：

```text
1. 一个用户可以创建多个 Agent 会话
2. 一个 threadId 对应一个会话
3. 同一个 threadId 可以连续追问
4. 用户消息和 Agent 回复可以落库
5. Workflow 的最新 state 可以落库
6. 订单诊断、采购预警、供应商评分的结构化结果可以落库
```

核心关系：

```text
user_id : thread_id = 1 : N
thread_id : agent_session = 1 : 1
agent_session : agent_message = 1 : N
agent_session : agent_session_state = 1 : 1
agent_session : agent_result = 1 : N
```

## 1. 业务设计说明

### 1.1 threadId 是什么

`threadId` 不是用户 ID。

正确理解：

```text
threadId = 一次 Agent 会话的唯一标识
```

例如同一个用户可以有多个会话：

```text
用户 1
  ├── threadId = agt-001：诊断 PO2026040011
  ├── threadId = agt-002：扫描采购风险
  └── threadId = agt-003：分析供应商 1
```

### 1.2 如果请求没传 threadId 怎么办

不能再使用固定值：

```java
threadId = "workflow-default";
```

原因：

```text
所有没传 threadId 的用户都会混进同一个会话上下文
```

正确做法：

```text
如果没传 threadId，就创建新会话，并生成新的 threadId。
```

### 1.3 统一 workflow 的请求流程

最终 `/agent/workflow/execute` 的流程应该是：

```text
前端请求
    ↓
读取当前登录用户 userId
    ↓
如果 threadId 为空：创建新 session
如果 threadId 不为空：查询并校验 session
    ↓
保存 USER 消息
    ↓
执行 workflow
    ↓
保存 ASSISTANT 消息
    ↓
保存 workflow state
    ↓
保存结构化 result
    ↓
返回 threadId + answer + data
```

## 2. 数据库表设计

建议新增一个 SQL 文件：

```text
inventory_back/sql/V4__agent_session.sql
```

内容如下：

```sql
USE inventory;

CREATE TABLE IF NOT EXISTS `agent_session` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `session_no` VARCHAR(64) NOT NULL COMMENT '会话编号',
    `thread_id` VARCHAR(128) NOT NULL COMMENT 'Agent线程ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `title` VARCHAR(128) DEFAULT NULL COMMENT '会话标题',
    `agent_type` VARCHAR(64) NOT NULL DEFAULT 'WORKFLOW_AGENT' COMMENT 'Agent类型',
    `current_intent` VARCHAR(64) DEFAULT NULL COMMENT '最近一次意图',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/CLOSED',
    `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY `uk_agent_session_no` (`session_no`),
    UNIQUE KEY `uk_agent_session_thread` (`thread_id`),
    KEY `idx_agent_session_user` (`user_id`),
    KEY `idx_agent_session_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent会话表';

CREATE TABLE IF NOT EXISTS `agent_message` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `thread_id` VARCHAR(128) NOT NULL COMMENT 'Agent线程ID',
    `message_role` VARCHAR(32) NOT NULL COMMENT '消息角色 USER/ASSISTANT/TOOL/SYSTEM',
    `message_type` VARCHAR(32) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型 TEXT/TOOL_CALL/TOOL_RESULT/WORKFLOW_EVENT',
    `content` TEXT DEFAULT NULL COMMENT '消息内容',
    `node_name` VARCHAR(128) DEFAULT NULL COMMENT 'Workflow节点名称',
    `tool_name` VARCHAR(128) DEFAULT NULL COMMENT '工具名称',
    `tool_request_json` MEDIUMTEXT DEFAULT NULL COMMENT '工具请求JSON',
    `tool_response_json` MEDIUMTEXT DEFAULT NULL COMMENT '工具响应JSON',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    KEY `idx_agent_message_session` (`session_id`),
    KEY `idx_agent_message_thread` (`thread_id`),
    KEY `idx_agent_message_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent消息表';

CREATE TABLE IF NOT EXISTS `agent_session_state` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `thread_id` VARCHAR(128) NOT NULL COMMENT 'Agent线程ID',
    `current_node` VARCHAR(128) DEFAULT NULL COMMENT '当前节点',
    `current_intent` VARCHAR(64) DEFAULT NULL COMMENT '当前意图',
    `state_json` MEDIUMTEXT DEFAULT NULL COMMENT 'Workflow状态JSON',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY `uk_agent_state_session` (`session_id`),
    UNIQUE KEY `uk_agent_state_thread` (`thread_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent会话状态表';

CREATE TABLE IF NOT EXISTS `agent_result` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `thread_id` VARCHAR(128) NOT NULL COMMENT 'Agent线程ID',
    `agent_type` VARCHAR(64) NOT NULL DEFAULT 'WORKFLOW_AGENT' COMMENT 'Agent类型',
    `biz_type` VARCHAR(64) DEFAULT NULL COMMENT '业务类型 PURCHASE_ORDER/WARNING_SCAN/SUPPLIER',
    `biz_id` BIGINT DEFAULT NULL COMMENT '业务ID',
    `biz_no` VARCHAR(64) DEFAULT NULL COMMENT '业务编号',
    `result_json` MEDIUMTEXT DEFAULT NULL COMMENT '结构化结果JSON',
    `summary` VARCHAR(1000) DEFAULT NULL COMMENT '摘要',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    KEY `idx_agent_result_session` (`session_id`),
    KEY `idx_agent_result_thread` (`thread_id`),
    KEY `idx_agent_result_biz` (`biz_type`, `biz_id`),
    KEY `idx_agent_result_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent分析结果表';
```

## 3. Entity 实体类

新增包：

```text
inventory_back/src/main/java/com/xixi/agent/entity
```

### 3.1 AgentSession

文件：

```text
inventory_back/src/main/java/com/xixi/agent/entity/AgentSession.java
```

代码：

```java
package com.xixi.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("agent_session")
public class AgentSession implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionNo;

    private String threadId;

    private Long userId;

    private String title;

    private String agentType;

    private String currentIntent;

    private String status;

    private LocalDateTime lastMessageTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

### 3.2 AgentMessage

文件：

```text
inventory_back/src/main/java/com/xixi/agent/entity/AgentMessage.java
```

代码：

```java
package com.xixi.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("agent_message")
public class AgentMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private String threadId;

    private String messageRole;

    private String messageType;

    private String content;

    private String nodeName;

    private String toolName;

    private String toolRequestJson;

    private String toolResponseJson;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
```

### 3.3 AgentSessionState

文件：

```text
inventory_back/src/main/java/com/xixi/agent/entity/AgentSessionState.java
```

代码：

```java
package com.xixi.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("agent_session_state")
public class AgentSessionState implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private String threadId;

    private String currentNode;

    private String currentIntent;

    private String stateJson;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

### 3.4 AgentResult

文件：

```text
inventory_back/src/main/java/com/xixi/agent/entity/AgentResult.java
```

代码：

```java
package com.xixi.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("agent_result")
public class AgentResult implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private String threadId;

    private String agentType;

    private String bizType;

    private Long bizId;

    private String bizNo;

    private String resultJson;

    private String summary;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
```

## 4. DTO / VO

### 4.1 修改 WorkflowAgentResponse

你当前已有：

```text
inventory_back/src/main/java/com/xixi/agent/vo/WorkflowAgentResponse.java
```

建议新增两个字段：

```java
private Long sessionId;
private String threadId;
```

完整建议：

```java
package com.xixi.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowAgentResponse {
    private Long sessionId;

    private String threadId;

    private String intent;

    private String answer;

    private String currentStage;

    private String riskLevel;

    private String suggestOwner;

    private String suggestAction;

    private List<String> evidence;

    private Object data;
}
```

说明：

- `sessionId`：数据库会话主键
- `threadId`：前端后续继续追问时必须继续传这个值

### 4.2 AgentSessionVO

文件：

```text
inventory_back/src/main/java/com/xixi/agent/vo/AgentSessionVO.java
```

代码：

```java
package com.xixi.agent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentSessionVO {
    private Long id;

    private String sessionNo;

    private String threadId;

    private Long userId;

    private String title;

    private String agentType;

    private String currentIntent;

    private String status;

    private LocalDateTime lastMessageTime;

    private LocalDateTime createTime;
}
```

### 4.3 AgentMessageVO

文件：

```text
inventory_back/src/main/java/com/xixi/agent/vo/AgentMessageVO.java
```

代码：

```java
package com.xixi.agent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentMessageVO {
    private Long id;

    private Long sessionId;

    private String threadId;

    private String messageRole;

    private String messageType;

    private String content;

    private LocalDateTime createTime;
}
```

## 5. Mapper 接口

新增包仍然使用：

```text
inventory_back/src/main/java/com/xixi/agent/mapper
```

### 5.1 AgentSessionMapper

文件：

```text
inventory_back/src/main/java/com/xixi/agent/mapper/AgentSessionMapper.java
```

代码：

```java
package com.xixi.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xixi.agent.entity.AgentSession;
import com.xixi.agent.vo.AgentSessionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AgentSessionMapper extends BaseMapper<AgentSession> {

    @Select("select * from agent_session where thread_id = #{threadId} and deleted = 0")
    AgentSession getByThreadId(@Param("threadId") String threadId);

    List<AgentSessionVO> getSessionListByUserId(@Param("userId") Long userId);

    @Update("""
            update agent_session
            set current_intent = #{currentIntent},
                last_message_time = #{lastMessageTime}
            where id = #{id}
              and deleted = 0
            """)
    int updateRuntimeInfo(@Param("id") Long id,
                          @Param("currentIntent") String currentIntent,
                          @Param("lastMessageTime") LocalDateTime lastMessageTime);
}
```

### 5.2 AgentMessageMapper

文件：

```text
inventory_back/src/main/java/com/xixi/agent/mapper/AgentMessageMapper.java
```

代码：

```java
package com.xixi.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xixi.agent.entity.AgentMessage;
import com.xixi.agent.vo.AgentMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentMessageMapper extends BaseMapper<AgentMessage> {
    List<AgentMessageVO> getMessagesByThreadId(@Param("threadId") String threadId);
}
```

### 5.3 AgentSessionStateMapper

文件：

```text
inventory_back/src/main/java/com/xixi/agent/mapper/AgentSessionStateMapper.java
```

代码：

```java
package com.xixi.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xixi.agent.entity.AgentSessionState;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AgentSessionStateMapper extends BaseMapper<AgentSessionState> {

    @Select("select * from agent_session_state where thread_id = #{threadId} and deleted = 0")
    AgentSessionState getByThreadId(@Param("threadId") String threadId);
}
```

### 5.4 AgentResultMapper

文件：

```text
inventory_back/src/main/java/com/xixi/agent/mapper/AgentResultMapper.java
```

代码：

```java
package com.xixi.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xixi.agent.entity.AgentResult;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentResultMapper extends BaseMapper<AgentResult> {
}
```

## 6. Mapper XML

新增：

```text
inventory_back/src/main/resources/com/xixi/mapper/AgentSessionMapper.xml
inventory_back/src/main/resources/com/xixi/mapper/AgentMessageMapper.xml
```

### 6.1 AgentSessionMapper.xml

```xml
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.xixi.agent.mapper.AgentSessionMapper">
    <select id="getSessionListByUserId" resultType="com.xixi.agent.vo.AgentSessionVO">
        select id, session_no, thread_id, user_id, title, agent_type,
               current_intent, status, last_message_time, create_time
        from agent_session
        where deleted = 0
        <if test="userId != null">
            and user_id = #{userId}
        </if>
        order by last_message_time desc, id desc
    </select>
</mapper>
```

### 6.2 AgentMessageMapper.xml

```xml
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.xixi.agent.mapper.AgentMessageMapper">
    <select id="getMessagesByThreadId" resultType="com.xixi.agent.vo.AgentMessageVO">
        select id, session_id, thread_id, message_role, message_type,
               content, create_time
        from agent_message
        where thread_id = #{threadId}
          and deleted = 0
        order by create_time asc, id asc
    </select>
</mapper>
```

## 7. Service 接口

### 7.1 AgentSessionService

文件：

```text
inventory_back/src/main/java/com/xixi/agent/service/AgentSessionService.java
```

代码：

```java
package com.xixi.agent.service;

import com.xixi.agent.entity.AgentSession;
import com.xixi.agent.vo.AgentMessageVO;
import com.xixi.agent.vo.AgentSessionVO;
import com.xixi.agent.vo.WorkflowAgentResponse;

import java.util.List;
import java.util.Map;

public interface AgentSessionService {
    AgentSession prepareSession(String threadId, Long userId, String firstMessage);

    void saveUserMessage(AgentSession session, String content);

    void saveAssistantMessage(AgentSession session, String content);

    void saveState(AgentSession session, String currentNode, String currentIntent, Map<String, Object> stateData);

    void saveResult(AgentSession session, WorkflowAgentResponse response);

    void updateSessionIntent(AgentSession session, String currentIntent);

    List<AgentSessionVO> getSessionList(Long userId);

    List<AgentMessageVO> getMessagesByThreadId(String threadId);
}
```

## 8. Service 实现

### 8.1 AgentSessionServiceImpl

文件：

```text
inventory_back/src/main/java/com/xixi/agent/service/impl/AgentSessionServiceImpl.java
```

完整代码：

```java
package com.xixi.agent.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xixi.agent.entity.AgentMessage;
import com.xixi.agent.entity.AgentResult;
import com.xixi.agent.entity.AgentSession;
import com.xixi.agent.entity.AgentSessionState;
import com.xixi.agent.mapper.AgentMessageMapper;
import com.xixi.agent.mapper.AgentResultMapper;
import com.xixi.agent.mapper.AgentSessionMapper;
import com.xixi.agent.mapper.AgentSessionStateMapper;
import com.xixi.agent.service.AgentSessionService;
import com.xixi.agent.vo.AgentMessageVO;
import com.xixi.agent.vo.AgentSessionVO;
import com.xixi.agent.vo.OrderDiagnosisVO;
import com.xixi.agent.vo.SupplierScoreVO;
import com.xixi.agent.vo.WarningScanVO;
import com.xixi.agent.vo.WorkflowAgentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentSessionServiceImpl implements AgentSessionService {
    private final AgentSessionMapper agentSessionMapper;
    private final AgentMessageMapper agentMessageMapper;
    private final AgentSessionStateMapper agentSessionStateMapper;
    private final AgentResultMapper agentResultMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AgentSession prepareSession(String threadId, Long userId, String firstMessage) {
        if (threadId != null && !threadId.isBlank()) {
            AgentSession session = agentSessionMapper.getByThreadId(threadId);
            if (session != null) {
                return session;
            }
        }

        AgentSession session = new AgentSession();
        session.setSessionNo(generateSessionNo());
        session.setThreadId(generateThreadId());
        session.setUserId(userId);
        session.setTitle(buildTitle(firstMessage));
        session.setAgentType("WORKFLOW_AGENT");
        session.setStatus("ACTIVE");
        session.setLastMessageTime(LocalDateTime.now());
        agentSessionMapper.insert(session);
        return session;
    }

    @Override
    public void saveUserMessage(AgentSession session, String content) {
        saveMessage(session, "USER", "TEXT", content, null, null, null, null);
    }

    @Override
    public void saveAssistantMessage(AgentSession session, String content) {
        saveMessage(session, "ASSISTANT", "TEXT", content, null, null, null, null);
    }

    @Override
    public void saveState(AgentSession session, String currentNode, String currentIntent, Map<String, Object> stateData) {
        AgentSessionState state = agentSessionStateMapper.getByThreadId(session.getThreadId());
        String stateJson = toJson(safeStateData(stateData));

        if (state == null) {
            state = new AgentSessionState();
            state.setSessionId(session.getId());
            state.setThreadId(session.getThreadId());
            state.setCurrentNode(currentNode);
            state.setCurrentIntent(currentIntent);
            state.setStateJson(stateJson);
            agentSessionStateMapper.insert(state);
            return;
        }

        state.setCurrentNode(currentNode);
        state.setCurrentIntent(currentIntent);
        state.setStateJson(stateJson);
        agentSessionStateMapper.updateById(state);
    }

    @Override
    public void saveResult(AgentSession session, WorkflowAgentResponse response) {
        if (response == null || response.getData() == null) {
            return;
        }

        AgentResult result = new AgentResult();
        result.setSessionId(session.getId());
        result.setThreadId(session.getThreadId());
        result.setAgentType("WORKFLOW_AGENT");
        result.setResultJson(toJson(response.getData()));
        result.setSummary(response.getAnswer());

        fillBizInfo(result, response);
        agentResultMapper.insert(result);
    }

    @Override
    public void updateSessionIntent(AgentSession session, String currentIntent) {
        agentSessionMapper.updateRuntimeInfo(session.getId(), currentIntent, LocalDateTime.now());
    }

    @Override
    public List<AgentSessionVO> getSessionList(Long userId) {
        return agentSessionMapper.getSessionListByUserId(userId);
    }

    @Override
    public List<AgentMessageVO> getMessagesByThreadId(String threadId) {
        return agentMessageMapper.getMessagesByThreadId(threadId);
    }

    private void saveMessage(AgentSession session,
                             String role,
                             String type,
                             String content,
                             String nodeName,
                             String toolName,
                             String toolRequestJson,
                             String toolResponseJson) {
        AgentMessage message = new AgentMessage();
        message.setSessionId(session.getId());
        message.setThreadId(session.getThreadId());
        message.setMessageRole(role);
        message.setMessageType(type);
        message.setContent(content);
        message.setNodeName(nodeName);
        message.setToolName(toolName);
        message.setToolRequestJson(toolRequestJson);
        message.setToolResponseJson(toolResponseJson);
        agentMessageMapper.insert(message);
    }

    private void fillBizInfo(AgentResult result, WorkflowAgentResponse response) {
        Object data = response.getData();
        if (data instanceof OrderDiagnosisVO orderDiagnosisVO) {
            result.setBizType("PURCHASE_ORDER");
            result.setBizNo(orderDiagnosisVO.getOrderNo());
            return;
        }
        if (data instanceof WarningScanVO) {
            result.setBizType("WARNING_SCAN");
            return;
        }
        if (data instanceof SupplierScoreVO supplierScoreVO) {
            result.setBizType("SUPPLIER");
            result.setBizId(supplierScoreVO.getSupplierId());
        }
    }

    private Map<String, Object> safeStateData(Map<String, Object> stateData) {
        Map<String, Object> copy = new HashMap<>();
        if (stateData != null) {
            copy.putAll(stateData);
        }
        copy.remove("finalResponse");
        return copy;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String generateSessionNo() {
        return "AS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String generateThreadId() {
        return "agt-" + UUID.randomUUID();
    }

    private String buildTitle(String message) {
        if (message == null || message.isBlank()) {
            return "新会话";
        }
        String title = message.trim();
        return title.length() > 30 ? title.substring(0, 30) : title;
    }
}
```

### 8.2 代码说明

#### `prepareSession`

作用：

```text
根据 threadId 找旧会话；
如果没有 threadId 或找不到旧会话，就创建新会话。
```

这里是会话持久化最核心的方法。

#### `saveUserMessage`

保存用户输入：

```text
message_role = USER
message_type = TEXT
```

#### `saveAssistantMessage`

保存 Agent 回复：

```text
message_role = ASSISTANT
message_type = TEXT
```

#### `saveState`

保存最新 workflow state。

注意：

```java
copy.remove("finalResponse");
```

这是为了避免 `finalResponse -> data -> finalResponse` 的循环引用问题。

#### `saveResult`

保存结构化业务分析结果。

例如：

- 订单诊断结果
- 采购预警扫描结果
- 供应商评分结果

## 9. 修改 WorkflowAgentResponse

文件：

```text
inventory_back/src/main/java/com/xixi/agent/vo/WorkflowAgentResponse.java
```

确保有：

```java
private Long sessionId;
private String threadId;
```

这样第一次请求没传 `threadId` 时，后端生成新 `threadId` 后能返回给前端。

## 10. 修改 ProcurementWorkflowExecutor

文件：

```text
inventory_back/src/main/java/com/xixi/agent/workflow/ProcurementWorkflowExecutor.java
```

替换为下面完整代码：

```java
package com.xixi.agent.workflow;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.xixi.agent.dto.WorkflowAgentRequest;
import com.xixi.agent.entity.AgentSession;
import com.xixi.agent.service.AgentSessionService;
import com.xixi.agent.vo.WorkflowAgentResponse;
import com.xixi.agent.workflow.state.WorkflowStateKeys;
import com.xixi.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProcurementWorkflowExecutor {
    private final StateGraph procurementStateGraph;
    private final AgentSessionService agentSessionService;

    public WorkflowAgentResponse execute(WorkflowAgentRequest request) throws Exception {
        Long userId = SecurityUtils.getCurrentUserId();
        AgentSession session = agentSessionService.prepareSession(request.getThreadId(), userId, request.getMessage());

        agentSessionService.saveUserMessage(session, request.getMessage());

        CompiledGraph compiledGraph = procurementStateGraph.compile(CompileConfig.builder().build());

        RunnableConfig config = RunnableConfig.builder()
                .threadId(session.getThreadId())
                .build();

        Map<String, Object> input = Map.of(
                WorkflowStateKeys.MESSAGE, request.getMessage(),
                WorkflowStateKeys.THREAD_ID, session.getThreadId()
        );

        Optional<OverAllState> result = compiledGraph.invoke(input, config);
        if (result.isEmpty()) {
            WorkflowAgentResponse response = new WorkflowAgentResponse();
            response.setSessionId(session.getId());
            response.setThreadId(session.getThreadId());
            response.setAnswer("工作流执行失败，未返回结果。");
            agentSessionService.saveAssistantMessage(session, response.getAnswer());
            return response;
        }

        Object finalResponse = result.get().value(WorkflowStateKeys.FINAL_RESPONSE).orElse(null);
        WorkflowAgentResponse response;
        if (finalResponse instanceof WorkflowAgentResponse workflowAgentResponse) {
            response = workflowAgentResponse;
        } else {
            response = new WorkflowAgentResponse();
            response.setAnswer("工作流执行完成，但最终响应格式异常。");
            Map<String, Object> debugData = new HashMap<>(result.get().data());
            debugData.remove(WorkflowStateKeys.FINAL_RESPONSE);
            response.setData(debugData);
        }

        response.setSessionId(session.getId());
        response.setThreadId(session.getThreadId());

        agentSessionService.updateSessionIntent(session, response.getIntent());
        agentSessionService.saveAssistantMessage(session, response.getAnswer());
        agentSessionService.saveState(session, "END", response.getIntent(), result.get().data());
        agentSessionService.saveResult(session, response);

        return response;
    }
}
```

### 10.1 这段代码在做什么

核心新增了 5 步：

```text
1. prepareSession：创建或恢复会话
2. saveUserMessage：保存用户输入
3. 执行 workflow
4. saveAssistantMessage：保存 Agent 回复
5. saveState + saveResult：保存状态和结构化结果
```

### 10.2 为什么这里不用默认 threadId

旧逻辑：

```java
threadId = "workflow-default";
```

新逻辑：

```java
AgentSession session = agentSessionService.prepareSession(...);
```

如果请求不带 `threadId`，会创建新会话并生成唯一 `threadId`。

## 11. Agent 会话 Controller

你原来的 workflow 执行接口可以继续保留。

建议新增一个 Controller：

```text
inventory_back/src/main/java/com/xixi/agent/controller/AgentSessionController.java
```

代码：

```java
package com.xixi.agent.controller;

import com.xixi.agent.service.AgentSessionService;
import com.xixi.pojo.vo.Result;
import com.xixi.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent/session")
@RequiredArgsConstructor
public class AgentSessionController {
    private final AgentSessionService agentSessionService;

    @GetMapping("/list")
    public Result list() {
        return Result.success(agentSessionService.getSessionList(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/messages/{threadId}")
    public Result messages(@PathVariable String threadId) {
        return Result.success(agentSessionService.getMessagesByThreadId(threadId));
    }
}
```

## 12. Spring Security 放行

如果你本地 Apifox 测试阶段暂时放行 Agent 接口：

```java
.requestMatchers(
        "/auth/login",
        "/error",
        "/agent/**"
).permitAll()
```

正式版本不建议全放行。

正式版本应该要求登录，并用 `userId` 隔离会话。

## 13. Apifox 测试流程

### 13.1 第一次请求不传 threadId

接口：

```text
POST /agent/workflow/execute
```

Body：

```json
{
  "message": "帮我看看 PO2026040011 为什么没完成"
}
```

预期返回：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "sessionId": 1,
    "threadId": "agt-xxxx",
    "intent": "ORDER_DIAGNOSIS",
    "answer": "...",
    "data": {
      "orderNo": "PO2026040011",
      "currentStage": "...",
      "blockReason": "..."
    }
  }
}
```

重点：

```text
前端要保存返回的 threadId。
```

### 13.2 第二次请求带上 threadId

Body：

```json
{
  "threadId": "agt-xxxx",
  "message": "那下一步谁处理？"
}
```

这次会复用同一个会话。

### 13.3 查看会话列表

```text
GET /agent/session/list
```

### 13.4 查看消息历史

```text
GET /agent/session/messages/{threadId}
```

## 14. 当前版本的能力边界

完成本手册后，你会获得：

```text
1. 自动创建会话
2. 自动生成 threadId
3. 保存用户消息
4. 保存 Agent 回复
5. 保存最新 workflow state
6. 保存结构化分析结果
7. 支持会话列表
8. 支持消息历史查询
```

当前还没有做：

```text
1. 从数据库 state_json 恢复完整 OverAllState
2. Tool 调用过程逐条落 agent_message
3. 多轮省略实体时自动从 state_json 恢复 orderNo/supplierId
```

这些可以作为第二阶段。

## 15. 第二阶段：基于 threadId 恢复上下文

第一阶段的会话持久化已经能做到：

```text
1. 创建会话
2. 保存消息
3. 保存最新 state_json
4. 保存最终结果
```

但第一阶段还不能解决这个问题：

```text
第一句：帮我看看 PO2026040011 为什么没完成
第二句：那下一步谁处理？
```

第二句没有订单号，如果系统只靠当前这条 message，就拿不到 `orderNo`。

所以第二阶段要做的是：

```text
根据 threadId 恢复上一轮会话中的业务上下文
```

最小目标：

```text
恢复上一次的 intent
恢复上一次的 entity（例如 orderNo、supplierId、days）
```

注意：

```text
不要恢复整个 orderSnapshot / warningAnalysis / finalResponse
```

因为这些是“上一次计算结果”，不是“本次请求新的事实输入”。

正确做法是：

```text
恢复最小上下文（intent + entity）
然后让 workflow 重新加载业务上下文并重新分析
```

### 15.1 第二阶段的完整执行流程

```text
用户请求 /agent/workflow/execute
    ↓
根据 threadId 找 session
    ↓
从 agent_session_state 里读取 state_json
    ↓
恢复最小上下文：
    - intent
    - entity
    ↓
把恢复后的上下文和当前 message 合并成新的 workflow input
    ↓
执行 workflow
    ↓
新的 Node 再重新查订单 / 预警 / 供应商指标
    ↓
返回结果并保存新的 state_json
```

### 15.2 为什么不能直接把整个 state_json 全部恢复

例如上一次 state 里可能有：

```text
ORDER_SNAPSHOT
ORDER_DIAGNOSIS
WARNING_ANALYSIS
SUPPLIER_SCORE
FINAL_RESPONSE
```

如果这些都恢复回来，会有两个问题：

1. **旧数据污染新问题**
   例如用户第一句问订单诊断，第二句问供应商评分，如果把旧快照全恢复回来，会把订单上下文污染到供应商问题里。

2. **容易产生循环引用或过大状态**
   特别是 `FINAL_RESPONSE`、`LLM_ANSWER` 这类结果对象，不应该再次作为输入。

所以第二阶段建议只恢复：

```text
INTENT
ENTITY
```

最多再恢复：

```text
RAG_DOCS（可选，不推荐第一时间恢复）
```

### 15.3 修改 AgentSessionService 接口

文件：

```text
inventory_back/src/main/java/com/xixi/agent/service/AgentSessionService.java
```

在现有接口基础上增加一个方法：

```java
package com.xixi.agent.service;

import com.xixi.agent.entity.AgentSession;
import com.xixi.agent.vo.AgentMessageVO;
import com.xixi.agent.vo.AgentSessionVO;
import com.xixi.agent.vo.WorkflowAgentResponse;

import java.util.List;
import java.util.Map;

public interface AgentSessionService {
    AgentSession prepareSession(String threadId, Long userId, String firstMessage);

    void saveUserMessage(AgentSession session, String content);

    void saveAssistantMessage(AgentSession session, String content);

    void saveState(AgentSession session, String currentNode, String currentIntent, Map<String, Object> stateData);

    void saveResult(AgentSession session, WorkflowAgentResponse response);

    void updateSessionIntent(AgentSession session, String currentIntent);

    List<AgentSessionVO> getSessionList(Long userId);

    List<AgentMessageVO> getMessagesByThreadId(String threadId);

    Map<String, Object> loadStateByThreadId(String threadId);
}
```

说明：

```text
loadStateByThreadId 用于从数据库中恢复会话状态。
```

### 15.4 修改 AgentSessionServiceImpl

文件：

```text
inventory_back/src/main/java/com/xixi/agent/service/impl/AgentSessionServiceImpl.java
```

#### 15.4.1 需要新增的 import

在顶部 import 中补：

```java
import com.fasterxml.jackson.core.type.TypeReference;
import com.xixi.agent.workflow.state.WorkflowEntity;
import com.xixi.agent.workflow.state.WorkflowStateKeys;
```

#### 15.4.2 新增 loadStateByThreadId 方法

把下面这段代码直接加到类里：

```java
    @Override
    public Map<String, Object> loadStateByThreadId(String threadId) {
        if (threadId == null || threadId.isBlank()) {
            return new HashMap<>();
        }

        AgentSessionState state = agentSessionStateMapper.getByThreadId(threadId);
        if (state == null || state.getStateJson() == null || state.getStateJson().isBlank()) {
            return new HashMap<>();
        }

        try {
            Map<String, Object> raw = objectMapper.readValue(
                    state.getStateJson(),
                    new TypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> restored = new HashMap<>();

            Object intent = raw.get(WorkflowStateKeys.INTENT);
            if (intent != null) {
                restored.put(WorkflowStateKeys.INTENT, intent.toString());
            }

            Object entityObj = raw.get(WorkflowStateKeys.ENTITY);
            if (entityObj != null) {
                WorkflowEntity entity = objectMapper.convertValue(entityObj, WorkflowEntity.class);
                restored.put(WorkflowStateKeys.ENTITY, entity);
            }

            return restored;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
```

#### 15.4.3 这段代码在做什么

`agent_session_state.state_json` 里存的是 JSON 字符串。  
恢复时流程是：

```text
1. 先查出 state_json
2. 用 ObjectMapper 转成 Map<String, Object>
3. 只取出需要恢复的字段
4. 把 entity 从普通 Map 再转回 WorkflowEntity
```

注意这里这句：

```java
WorkflowEntity entity = objectMapper.convertValue(entityObj, WorkflowEntity.class);
```

这一步必须有。  
因为 JSON 反序列化后，`ENTITY` 默认会变成 `LinkedHashMap`，如果你直接塞回 state，后面在 `LoadOrderContextNode` 里强转：

```java
(WorkflowEntity) state.value(...).orElse(null)
```

就会报类型转换错误。

### 15.5 修改 ProcurementWorkflowExecutor

文件：

```text
inventory_back/src/main/java/com/xixi/agent/workflow/ProcurementWorkflowExecutor.java
```

请把当前第一阶段版本替换成下面这版：

```java
package com.xixi.agent.workflow;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.xixi.agent.dto.WorkflowAgentRequest;
import com.xixi.agent.entity.AgentSession;
import com.xixi.agent.service.AgentSessionService;
import com.xixi.agent.vo.WorkflowAgentResponse;
import com.xixi.agent.workflow.state.WorkflowStateKeys;
import com.xixi.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProcurementWorkflowExecutor {
    private final StateGraph procurementStateGraph;
    private final AgentSessionService agentSessionService;

    public WorkflowAgentResponse execute(WorkflowAgentRequest request) throws Exception {
        Long userId = SecurityUtils.getCurrentUserId();
        AgentSession session = agentSessionService.prepareSession(
                request.getThreadId(),
                userId,
                request.getMessage()
        );

        agentSessionService.saveUserMessage(session, request.getMessage());

        CompiledGraph compiledGraph = procurementStateGraph.compile(CompileConfig.builder().build());

        RunnableConfig config = RunnableConfig.builder()
                .threadId(session.getThreadId())
                .build();

        Map<String, Object> input = new HashMap<>();

        Map<String, Object> restoredState = agentSessionService.loadStateByThreadId(session.getThreadId());
        if (restoredState != null && !restoredState.isEmpty()) {
            input.putAll(restoredState);
        }

        input.put(WorkflowStateKeys.MESSAGE, request.getMessage());
        input.put(WorkflowStateKeys.THREAD_ID, session.getThreadId());

        Optional<OverAllState> result = compiledGraph.invoke(input, config);
        if (result.isEmpty()) {
            WorkflowAgentResponse response = new WorkflowAgentResponse();
            response.setSessionId(session.getId());
            response.setThreadId(session.getThreadId());
            response.setAnswer("工作流执行失败，未返回结果。");
            agentSessionService.saveAssistantMessage(session, response.getAnswer());
            return response;
        }

        Object finalResponse = result.get().value(WorkflowStateKeys.FINAL_RESPONSE).orElse(null);
        WorkflowAgentResponse response;
        if (finalResponse instanceof WorkflowAgentResponse workflowAgentResponse) {
            response = workflowAgentResponse;
        } else {
            response = new WorkflowAgentResponse();
            response.setAnswer("工作流执行完成，但最终响应格式异常。");
            Map<String, Object> debugData = new HashMap<>(result.get().data());
            debugData.remove(WorkflowStateKeys.FINAL_RESPONSE);
            response.setData(debugData);
        }

        response.setSessionId(session.getId());
        response.setThreadId(session.getThreadId());

        agentSessionService.updateSessionIntent(session, response.getIntent());
        agentSessionService.saveAssistantMessage(session, response.getAnswer());
        agentSessionService.saveState(session, "END", response.getIntent(), result.get().data());
        agentSessionService.saveResult(session, response);

        return response;
    }
}
```

#### 15.5.1 这段代码和第一阶段的区别

最关键的新增是这几行：

```java
Map<String, Object> input = new HashMap<>();

Map<String, Object> restoredState = agentSessionService.loadStateByThreadId(session.getThreadId());
if (restoredState != null && !restoredState.isEmpty()) {
    input.putAll(restoredState);
}

input.put(WorkflowStateKeys.MESSAGE, request.getMessage());
input.put(WorkflowStateKeys.THREAD_ID, session.getThreadId());
```

意思是：

```text
先恢复老状态
再把当前这次请求里的 message 和 threadId 覆盖进去
```

这样既能保留历史上下文，又不会让旧消息覆盖新消息。

### 15.6 为什么只恢复 ENTITY 和 INTENT 就够了

因为你的工作流后面还会重新跑这些节点：

- `LoadOrderContextNode`
- `OrderRuleAnalyzeNode`
- `LoadWarningContextNode`
- `WarningRuleAnalyzeNode`
- `LoadSupplierContextNode`
- `SupplierScoreRuleNode`

这些节点本身就会重新查数据库、重新做规则计算。

所以恢复：

```text
ORDER_SNAPSHOT
ORDER_DIAGNOSIS
WARNING_ANALYSIS
SUPPLIER_SCORE
```

反而容易把旧结果污染到新请求里。

最稳的做法就是只恢复：

```text
ENTITY
INTENT
```

### 15.7 修改 EntityExtractNode

这是第二阶段最容易漏掉的一步。

如果你只恢复 `ENTITY`，但 `EntityExtractNode` 每次都重新 new 一个全新的 `WorkflowEntity`，那么第二句追问又会把旧实体覆盖掉。

文件：

```text
inventory_back/src/main/java/com/xixi/agent/workflow/node/EntityExtractNode.java
```

请改成下面这版：

```java
package com.xixi.agent.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.xixi.agent.workflow.state.WorkflowEntity;
import com.xixi.agent.workflow.state.WorkflowStateKeys;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityExtractNode implements NodeAction {
    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("PO\\d+");
    private static final Pattern DAYS_PATTERN = Pattern.compile("(最近|近)?(\\d+)\\s*天");
    private static final Pattern SUPPLIER_ID_PATTERN = Pattern.compile("供应商\\s*(\\d+)");

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String message = state.value(WorkflowStateKeys.NORMALIZED_MESSAGE, "").toString();

        WorkflowEntity entity = (WorkflowEntity) state.value(WorkflowStateKeys.ENTITY).orElse(new WorkflowEntity());

        String orderNo = findFirst(ORDER_NO_PATTERN, message, 0);
        if (orderNo != null) {
            entity.setOrderNo(orderNo);
        }

        String days = findFirst(DAYS_PATTERN, message, 2);
        if (days != null) {
            entity.setDays(Integer.parseInt(days));
        } else if (entity.getDays() == null) {
            entity.setDays(30);
        }

        String supplierId = findFirst(SUPPLIER_ID_PATTERN, message, 1);
        if (supplierId != null) {
            entity.setSupplierId(Long.parseLong(supplierId));
        }

        return Map.of(WorkflowStateKeys.ENTITY, entity);
    }

    private String findFirst(Pattern pattern, String text, int group) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(group);
        }
        return null;
    }
}
```

#### 15.7.1 这段代码为什么很关键

第一阶段的逻辑是：

```text
每次请求都完整带实体
```

第二阶段的逻辑是：

```text
允许用户省略实体，继续追问
```

所以：

```java
WorkflowEntity entity = (WorkflowEntity) state.value(...).orElse(new WorkflowEntity());
```

这里必须优先使用 state 里恢复出来的旧 entity。

然后：

```text
当前消息如果抽到了新实体，就覆盖旧值；
如果当前消息没抽到，就保留旧值。
```

### 15.8 可选：修改 IntentClassifyNode

第二阶段如果你发现用户输入：

```text
那下一步谁处理？
```

经常被模型识别错意图，你可以把旧意图也传给 Prompt。

文件：

```text
inventory_back/src/main/java/com/xixi/agent/workflow/node/IntentClassifyNode.java
```

关键改法：

```java
String previousIntent = state.value(WorkflowStateKeys.INTENT, "UNKNOWN").toString();
```

然后在 prompt 里加：

```text
上一次会话意图：{previousIntent}
如果当前问题明显是对上一轮问题的追问，请优先沿用上一轮意图。
```

这一条不是必须，但会让多轮追问更稳。

### 15.9 第二阶段测试流程

#### 第一步

不带 threadId：

```json
{
  "message": "帮我看看 PO2026040011 为什么没完成"
}
```

返回：

```json
{
  "data": {
    "sessionId": 1,
    "threadId": "agt-xxxx",
    "intent": "ORDER_DIAGNOSIS",
    "answer": "...",
    "data": {
      "orderNo": "PO2026040011",
      "currentStage": "...",
      "blockReason": "..."
    }
  }
}
```

#### 第二步

复用同一个 threadId：

```json
{
  "threadId": "agt-xxxx",
  "message": "那下一步谁处理？"
}
```

预期：

```text
系统自动恢复 orderNo = PO2026040011
并继续走 ORDER_DIAGNOSIS 分支
```

#### 第三步

再追问：

```json
{
  "threadId": "agt-xxxx",
  "message": "风险大吗？"
}
```

预期：

```text
仍然围绕同一张订单回答
```

### 15.10 第二阶段完成标准

做到下面这些，就算第二阶段上下文恢复完成：

```text
1. 第二轮追问不再必须重新输入 orderNo
2. 第二轮追问能复用上一轮 intent
3. state_json 能正确恢复 entity
4. 恢复后 workflow 仍然重新加载业务上下文，而不是复用旧快照
5. 不出现 finalResponse 循环引用
```

### 15.10A 为什么恢复了 threadId，回答还是像完整诊断

很多人做到第二阶段后，会发现：

```text
第二轮请求已经带上了 threadId
系统也能恢复出 orderNo
但用户问“那下一步谁处理？”时，返回仍然是一整段完整诊断
```

这不是因为上下文恢复失败，而是因为：

```text
恢复了上下文，但最终给 LLM 的 Prompt 仍然是固定的“完整订单诊断 Prompt”
```

也就是说，系统目前知道：

```text
当前问题属于 ORDER_DIAGNOSIS
orderNo = PO2026040011
```

但 `BusinessAnswerGenerateNode` 没有把当前用户问题：

```text
那下一步谁处理？
```

传给 LLM，所以模型只能按完整诊断模板回答。

### 15.10B 最小修复思路

不需要新增节点，只需要做两处小改动：

#### 改动 1：修改 `WorkflowPrompts.ORDER_BUSINESS_PROMPT`

文件：

```text
inventory_back/src/main/java/com/xixi/agent/workflow/prompt/WorkflowPrompts.java
```

把订单诊断 Prompt 改成下面这版：

```java
public static final String ORDER_BUSINESS_PROMPT = """
        你是采购订单流程阻塞诊断专家。
        你会收到：
        1. 用户当前问题
        2. 采购订单执行快照
        3. Java 状态机规则判断结果
        4. 可选业务规则文档片段

        你的任务：
        - 优先回答用户当前问题
        - 如果用户问“谁处理”或“下一步谁处理”，请重点回答建议处理角色和建议动作
        - 如果用户问“为什么没完成”，再解释当前阶段、阻塞原因和关键证据
        - 用业务人员能理解的语言解释订单当前阶段
        - 解释为什么卡住
        - 给出下一步处理角色和动作
        - 不允许编造系统没有返回的数据
        - 不允许输出与 Java 规则判断相反的结论
        - Java 规则结果是主结论，你只能做解释，不能改写阶段结论

        输出格式：
        当前阶段：
        阻塞原因：
        关键证据：
        建议处理人：
        建议动作：

        用户当前问题：
        {message}

        订单快照：
        {orderSnapshot}

        Java 规则结果：
        {orderDiagnosis}

        业务规则文档：
        {ragDocs}
        """;
```

#### 改动 2：修改 `BusinessAnswerGenerateNode`

文件：

```text
inventory_back/src/main/java/com/xixi/agent/workflow/node/BusinessAnswerGenerateNode.java
```

把订单诊断分支改成下面这版：

```java
package com.xixi.agent.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.xixi.agent.workflow.prompt.WorkflowPrompts;
import com.xixi.agent.workflow.state.WorkflowStateKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

@RequiredArgsConstructor
public class BusinessAnswerGenerateNode implements NodeActionWithConfig {
    private final ChatClient chatClient;

    @Override
    public Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception {
        String intent = state.value(WorkflowStateKeys.INTENT, "UNKNOWN").toString();
        String message = state.value(WorkflowStateKeys.MESSAGE, "").toString();
        String ragDocs = state.value(WorkflowStateKeys.RAG_DOCS, "").toString();
        String prompt;

        if ("ORDER_DIAGNOSIS".equals(intent)) {
            Object snapshot = state.value(WorkflowStateKeys.ORDER_SNAPSHOT).orElse(null);
            Object diagnosis = state.value(WorkflowStateKeys.ORDER_DIAGNOSIS).orElse(null);
            prompt = WorkflowPrompts.ORDER_BUSINESS_PROMPT
                    .replace("{message}", message)
                    .replace("{orderSnapshot}", String.valueOf(snapshot))
                    .replace("{orderDiagnosis}", String.valueOf(diagnosis))
                    .replace("{ragDocs}", ragDocs);
        } else if ("WARNING_SCAN".equals(intent)) {
            Object warningAnalysis = state.value(WorkflowStateKeys.WARNING_ANALYSIS).orElse(null);
            prompt = WorkflowPrompts.WARNING_BUSINESS_PROMPT
                    .replace("{warningItems}", String.valueOf(warningAnalysis))
                    .replace("{ragDocs}", ragDocs);
        } else if ("SUPPLIER_SCORE".equals(intent)) {
            Object metrics = state.value(WorkflowStateKeys.SUPPLIER_METRICS).orElse(null);
            Object score = state.value(WorkflowStateKeys.SUPPLIER_SCORE).orElse(null);
            prompt = WorkflowPrompts.SUPPLIER_BUSINESS_PROMPT
                    .replace("{supplierMetrics}", String.valueOf(metrics))
                    .replace("{supplierScore}", String.valueOf(score))
                    .replace("{ragDocs}", ragDocs);
        } else {
            prompt = "用户问题无法识别，请提示用户补充订单号、供应商ID或扫描范围。";
        }

        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return Map.of(WorkflowStateKeys.LLM_ANSWER, answer);
    }
}
```

### 15.10C 修完后会发生什么

第二轮请求：

```json
{
  "threadId": "agt-xxxx",
  "message": "那下一步谁处理？"
}
```

修复前：

```text
LLM 按固定模板输出完整订单诊断
```

修复后：

```text
LLM 会优先回答“谁处理”
例如：
“下一步建议由采购员处理，请采购员催促供应商补齐剩余到货。”
```

### 15.10D 这一步的本质

这一步不是“恢复上下文”，而是：

```text
让 LLM 感知当前这轮用户真正想问什么
```

所以：

```text
第二阶段需要两部分同时成立：
1. 恢复上下文（orderNo、intent）
2. Prompt 中显式传入当前用户问题
```

### 15.10E 剩余两个分支也要做同样的“当前问题感知”

如果你后面统一使用：

```text
/agent/workflow/execute
```

那么不仅 `ORDER_DIAGNOSIS`，另外两个分支也会遇到同样的问题：

#### `WARNING_SCAN`

第一句：

```text
扫描最近7天采购执行风险
```

第二句：

```text
还有哪些高风险？
```

如果不把当前用户问题传给 Prompt，LLM 仍然会把第二句当成“重新生成完整预警报告”，而不是“只回答高风险事项”。

#### `SUPPLIER_SCORE`

第一句：

```text
分析供应商1最近90天履约情况
```

第二句：

```text
这个分数意味着什么？
```

如果不把当前用户问题传给 Prompt，LLM 会继续输出整段完整评分分析，而不是聚焦回答“分数含义”和“合作建议”。

所以：

```text
第二阶段不仅要恢复历史 entity / intent，
还要让不同分支的最终 LLM Prompt 感知“当前用户问题”。
```

### 15.10F 修改 WorkflowPrompts，让预警和评分也支持追问

文件：

```text
inventory_back/src/main/java/com/xixi/agent/workflow/prompt/WorkflowPrompts.java
```

#### 修改 1：INTENT_CLASSIFY_PROMPT

把意图识别 Prompt 改成这样：

```java
public static final String INTENT_CLASSIFY_PROMPT = """
        你是供应商协同采购入库系统的意图识别器。
        你的任务是判断用户输入属于哪一种业务意图。

        可选意图：
        1. ORDER_DIAGNOSIS：用户想诊断采购订单卡在哪、为什么没完成、下一步谁处理。
        2. WARNING_SCAN：用户想扫描采购执行风险、预警、待处理事项。
        3. SUPPLIER_SCORE：用户想分析供应商履约表现、评分、合作建议。
        4. KNOWLEDGE_QA：用户询问系统规则、状态流转、为什么某流程不能操作。
        5. UNKNOWN：无法判断。

        请只输出一个意图编码，不要输出解释。
        如果用户当前问题明显是“那还有呢、下一步呢、继续分析、风险大吗、哪些更严重”这类追问，
        并且上一次会话意图不是 UNKNOWN，请优先沿用上一次会话意图。

        上一次会话意图：
        {previousIntent}

        用户输入：
        {message}
        """;
```

#### 修改 2：WARNING_BUSINESS_PROMPT

```java
public static final String WARNING_BUSINESS_PROMPT = """
        你是采购执行预警分析专家。
        你会收到系统通过 Java 规则扫描出的风险列表。

        你的任务：
        - 优先回答用户当前问题
        - 如果用户在追问“还有哪些高风险”“哪个最严重”“该先处理什么”，请重点围绕优先级回答
        - 总结本次风险概况
        - 按优先级说明最应该处理的风险
        - 识别是否存在同类风险集中出现
        - 给出建议处理角色和动作
        - 不允许新增风险列表中不存在的单据

        输出格式：
        风险概况：
        高优先级事项：
        风险集中点：
        建议处理顺序：

        用户当前问题：
        {message}

        风险列表：
        {warningItems}

        业务规则文档：
        {ragDocs}
        """;
```

#### 修改 3：SUPPLIER_BUSINESS_PROMPT

```java
public static final String SUPPLIER_BUSINESS_PROMPT = """
        你是供应商履约分析专家。
        你会收到：
        1. 用户当前问题
        2. Java 计算出的供应商履约指标
        3. Java 计算出的评分和等级
        4. 可选业务规则文档片段

        你的任务：
        - 优先回答用户当前问题
        - 如果用户追问“这个分数意味着什么”“能不能继续合作”，请优先围绕评价和建议回答
        - 解释供应商履约分数
        - 说明主要优势和主要风险
        - 给出合作建议
        - 不允许修改 Java 算出的分数
        - 不允许把“统计周期内无订单”说成“供应商不存在”

        输出格式：
        总体评价：
        主要优势：
        主要风险：
        合作建议：

        用户当前问题：
        {message}

        供应商指标：
        {supplierMetrics}

        Java 评分：
        {supplierScore}

        业务规则文档：
        {ragDocs}
        """;
```

### 15.10G 修改 IntentClassifyNode

文件：

```text
inventory_back/src/main/java/com/xixi/agent/workflow/node/IntentClassifyNode.java
```

把代码改成下面这版：

```java
package com.xixi.agent.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.xixi.agent.workflow.prompt.WorkflowPrompts;
import com.xixi.agent.workflow.state.WorkflowIntent;
import com.xixi.agent.workflow.state.WorkflowStateKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;

@RequiredArgsConstructor
public class IntentClassifyNode implements NodeActionWithConfig {
    private final ChatClient chatClient;

    @Override
    public Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception {
        String message = state.value(WorkflowStateKeys.NORMALIZED_MESSAGE, "").toString();
        String previousIntent = state.value(WorkflowStateKeys.INTENT, "UNKNOWN").toString();

        PromptTemplate template = new PromptTemplate(WorkflowPrompts.INTENT_CLASSIFY_PROMPT);
        String intentText = chatClient.prompt()
                .user(user -> user
                        .text(template.getTemplate())
                        .param("previousIntent", previousIntent)
                        .param("message", message))
                .call()
                .content();

        WorkflowIntent intent = parseIntent(intentText);
        if (intent == WorkflowIntent.UNKNOWN && !"UNKNOWN".equals(previousIntent)) {
            intent = parseIntent(previousIntent);
        }

        return Map.of(WorkflowStateKeys.INTENT, intent.name());
    }

    private WorkflowIntent parseIntent(String text) {
        if (text == null) {
            return WorkflowIntent.UNKNOWN;
        }
        String value = text.trim();
        for (WorkflowIntent intent : WorkflowIntent.values()) {
            if (value.contains(intent.name())) {
                return intent;
            }
        }
        return WorkflowIntent.UNKNOWN;
    }
}
```

说明：

```text
如果模型本轮没识别出来（UNKNOWN），但上轮已经有明确意图，就优先沿用旧意图。
```

这样对于：

```text
还有哪些高风险？
这个分数意味着什么？
```

这类追问会更稳。

### 15.10H 修改 BusinessAnswerGenerateNode

文件：

```text
inventory_back/src/main/java/com/xixi/agent/workflow/node/BusinessAnswerGenerateNode.java
```

改成下面这版：

```java
package com.xixi.agent.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.xixi.agent.workflow.prompt.WorkflowPrompts;
import com.xixi.agent.workflow.state.WorkflowStateKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

@RequiredArgsConstructor
public class BusinessAnswerGenerateNode implements NodeActionWithConfig {
    private final ChatClient chatClient;

    @Override
    public Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception {
        String intent = state.value(WorkflowStateKeys.INTENT, "UNKNOWN").toString();
        String message = state.value(WorkflowStateKeys.MESSAGE, "").toString();
        String ragDocs = state.value(WorkflowStateKeys.RAG_DOCS, "").toString();
        String prompt;

        if ("ORDER_DIAGNOSIS".equals(intent)) {
            Object snapshot = state.value(WorkflowStateKeys.ORDER_SNAPSHOT).orElse(null);
            Object diagnosis = state.value(WorkflowStateKeys.ORDER_DIAGNOSIS).orElse(null);
            prompt = WorkflowPrompts.ORDER_BUSINESS_PROMPT
                    .replace("{message}", message)
                    .replace("{orderSnapshot}", String.valueOf(snapshot))
                    .replace("{orderDiagnosis}", String.valueOf(diagnosis))
                    .replace("{ragDocs}", ragDocs);
        } else if ("WARNING_SCAN".equals(intent)) {
            Object warningAnalysis = state.value(WorkflowStateKeys.WARNING_ANALYSIS).orElse(null);
            prompt = WorkflowPrompts.WARNING_BUSINESS_PROMPT
                    .replace("{message}", message)
                    .replace("{warningItems}", String.valueOf(warningAnalysis))
                    .replace("{ragDocs}", ragDocs);
        } else if ("SUPPLIER_SCORE".equals(intent)) {
            Object metrics = state.value(WorkflowStateKeys.SUPPLIER_METRICS).orElse(null);
            Object score = state.value(WorkflowStateKeys.SUPPLIER_SCORE).orElse(null);
            prompt = WorkflowPrompts.SUPPLIER_BUSINESS_PROMPT
                    .replace("{message}", message)
                    .replace("{supplierMetrics}", String.valueOf(metrics))
                    .replace("{supplierScore}", String.valueOf(score))
                    .replace("{ragDocs}", ragDocs);
        } else {
            prompt = "用户问题无法识别，请提示用户补充订单号、供应商ID或扫描范围。";
        }

        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return Map.of(WorkflowStateKeys.LLM_ANSWER, answer);
    }
}
```

说明：

```text
这一步让三条分支都不再只是输出“第一轮完整报告”，
而是能够围绕用户当前这句追问来回答。
```

### 15.10I 这三条分支现在的持久化状态

到这一步为止，三条分支都已经共享了同一套持久化机制：

#### ORDER_DIAGNOSIS

```text
恢复 entity.orderNo
恢复旧 intent
重新加载订单上下文
保存 result
```

#### WARNING_SCAN

```text
恢复 entity.days
恢复旧 intent
重新扫描预警
保存 result
```

#### SUPPLIER_SCORE

```text
恢复 entity.supplierId
恢复 entity.days
恢复旧 intent
重新计算评分
保存 result
```

也就是说：

```text
三条分支的“会话持久化”代码已经统一完成，
差别只在每个分支恢复的 entity 字段不同。
```

### 15.11 后续可以再做什么

第二阶段稳定后，再考虑：

#### 15.11.1 保存 Tool 调用记录

统一 `/agent/workflow/execute` 之后，只存用户消息和 Agent 最终回答还不够。

如果你想知道：

- Agent 实际调用了哪个 Tool
- Tool 收到了什么请求参数
- Tool 返回了什么结构化数据

那就应该把 Tool 执行结果也落到 `agent_message`。

建议约定：

```text
message_role = TOOL
message_type = TOOL_RESULT
```

### 15.11.1A 为什么工具调用要落库

原因有三个：

1. **排查问题方便**
   比如：
   - 为什么订单诊断结果和预期不一致
   - 为什么预警扫描结果为空
   - 为什么供应商评分说“数据不足”

   你能直接从数据库回看 Tool 当时查到了什么。

2. **Agent 可解释性更强**
   你不仅能说“AI 给了这个结果”，还能说：

   ```text
   它先调用了订单快照工具，
   再调用了状态机规则节点，
   最后生成答案。
   ```

3. **为后续会话回放做准备**
   后面你如果要做会话详情页，就可以展示：

   - 用户消息
   - Tool 调用结果
   - Agent 最终回复

### 15.11.1B 数据库存储设计

你第一阶段的表里其实已经有字段了：

```sql
`tool_name` VARCHAR(128) DEFAULT NULL COMMENT '工具名称',
`tool_request_json` MEDIUMTEXT DEFAULT NULL COMMENT '工具请求JSON',
`tool_response_json` MEDIUMTEXT DEFAULT NULL COMMENT '工具响应JSON',
```

所以这一块**不需要再改表结构**，只需要真正把这些字段用起来。

### 15.11.1C 修改 AgentSessionService 接口

文件：

```text
inventory_back/src/main/java/com/xixi/agent/service/AgentSessionService.java
```

增加一个新方法：

```java
void saveToolMessage(String threadId,
                     String toolName,
                     String toolRequestJson,
                     String toolResponseJson);
```

完整接口建议如下：

```java
package com.xixi.agent.service;

import com.xixi.agent.entity.AgentSession;
import com.xixi.agent.vo.AgentMessageVO;
import com.xixi.agent.vo.AgentSessionVO;
import com.xixi.agent.vo.WorkflowAgentResponse;

import java.util.List;
import java.util.Map;

public interface AgentSessionService {
    AgentSession prepareSession(String threadId, Long userId, String firstMessage);

    void saveUserMessage(AgentSession session, String content);

    void saveAssistantMessage(AgentSession session, String content);

    void saveToolMessage(String threadId,
                         String toolName,
                         String toolRequestJson,
                         String toolResponseJson);

    void saveState(AgentSession session, String currentNode, String currentIntent, Map<String, Object> stateData);

    void saveResult(AgentSession session, WorkflowAgentResponse response);

    void updateSessionIntent(AgentSession session, String currentIntent);

    List<AgentSessionVO> getSessionList(Long userId);

    List<AgentMessageVO> getMessagesByThreadId(String threadId);

    Map<String, Object> loadStateByThreadId(String threadId);
}
```

### 15.11.1D 修改 AgentSessionServiceImpl

文件：

```text
inventory_back/src/main/java/com/xixi/agent/service/impl/AgentSessionServiceImpl.java
```

新增方法：

```java
@Override
public void saveToolMessage(String threadId,
                            String toolName,
                            String toolRequestJson,
                            String toolResponseJson) {
    if (threadId == null || threadId.isBlank()) {
        return;
    }

    AgentSession session = agentSessionMapper.getByThreadId(threadId);
    if (session == null) {
        return;
    }

    saveMessage(session, "TOOL", "TOOL_RESULT", null, null, toolName, toolRequestJson, toolResponseJson);
}
```

说明：

- 这里直接根据 `threadId` 反查 `AgentSession`
- 找不到 session 就直接 return，不抛异常
- 使用你现有的 `saveMessage(...)` 复用插入逻辑

### 15.11.1E 修改三个 Tool 类

这一步是真正把 SQL 业务落进工具执行链路。

#### 一、修改 `PurchaseOrderSnapshotTool`

文件：

```text
inventory_back/src/main/java/com/xixi/agent/tool/PurchaseOrderSnapshotTool.java
```

完整代码建议：

```java
package com.xixi.agent.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xixi.agent.dto.PurchaseOrderSnapshotRequest;
import com.xixi.agent.mapper.AgentQueryMapper;
import com.xixi.agent.service.AgentSessionService;
import com.xixi.agent.vo.OrderSnapshotVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.BiFunction;

@Component
@RequiredArgsConstructor
public class PurchaseOrderSnapshotTool implements BiFunction<PurchaseOrderSnapshotRequest, ToolContext, String> {
    private final AgentQueryMapper agentQueryMapper;
    private final AgentSessionService agentSessionService;
    private final ObjectMapper objectMapper;

    @Override
    public String apply(PurchaseOrderSnapshotRequest request, ToolContext toolContext) {
        try {
            String threadId = extractThreadId(toolContext);
            String requestJson = toJson(request);

            if (request == null || request.getOrderNo() == null || request.getOrderNo().isBlank()) {
                String responseJson = objectMapper.writeValueAsString(Map.of(
                        "success", false,
                        "message", "orderNo不能为空"
                ));
                agentSessionService.saveToolMessage(threadId, "getPurchaseOrderSnapshot", requestJson, responseJson);
                return responseJson;
            }

            OrderSnapshotVO snapshot = agentQueryMapper.getOrderSnapshotByOrderNo(request.getOrderNo().trim());
            if (snapshot == null) {
                String responseJson = objectMapper.writeValueAsString(Map.of(
                        "success", false,
                        "message", "采购订单不存在",
                        "orderNo", request.getOrderNo()
                ));
                agentSessionService.saveToolMessage(threadId, "getPurchaseOrderSnapshot", requestJson, responseJson);
                return responseJson;
            }

            String responseJson = objectMapper.writeValueAsString(Map.of(
                    "success", true,
                    "data", snapshot
            ));
            agentSessionService.saveToolMessage(threadId, "getPurchaseOrderSnapshot", requestJson, responseJson);
            return responseJson;
        } catch (Exception e) {
            return "{\"success\":false,\"message\":\"工具执行失败\"}";
        }
    }

    private String extractThreadId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object threadId = toolContext.getContext().get("threadId");
        return threadId == null ? null : threadId.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
```

#### 二、修改 `ProcurementWarningSnapshotTool`

文件：

```text
inventory_back/src/main/java/com/xixi/agent/tool/ProcurementWarningSnapshotTool.java
```

完整代码建议：

```java
package com.xixi.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xixi.agent.dto.WarningScanRequest;
import com.xixi.agent.mapper.AgentWarningMapper;
import com.xixi.agent.service.AgentSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

@Component
@RequiredArgsConstructor
public class ProcurementWarningSnapshotTool implements BiFunction<WarningScanRequest, ToolContext, String> {
    private final AgentWarningMapper agentWarningMapper;
    private final AgentSessionService agentSessionService;
    private final ObjectMapper objectMapper;

    @Override
    public String apply(WarningScanRequest request, ToolContext toolContext) {
        try {
            String threadId = extractThreadId(toolContext);
            String requestJson = toJson(request);

            Integer days = request == null || request.getDays() == null ? 7 : request.getDays();

            Map<String, Object> result = new HashMap<>();
            result.put("waitConfirmOverdueOrders", agentWarningMapper.getWaitConfirmOverdueOrders(days));
            result.put("inProgressWithoutArrival", agentWarningMapper.getInProgressWithoutArrivalOrders(days));
            result.put("partialArrivalStuck", agentWarningMapper.getPartialArrivalStuckOrders(days));
            result.put("arrivedWithoutInbound", agentWarningMapper.getArrivedWithoutInboundRecords(days));
            result.put("pendingInboundOverdue", agentWarningMapper.getPendingInboundOverdueRecords(days));

            String responseJson = objectMapper.writeValueAsString(Map.of(
                    "success", true,
                    "data", result
            ));
            agentSessionService.saveToolMessage(threadId, "procurementWarningSnapshotTool", requestJson, responseJson);
            return responseJson;
        } catch (Exception e) {
            return "{\"success\":false,\"message\":\"采购预警工具执行失败\"}";
        }
    }

    private String extractThreadId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object threadId = toolContext.getContext().get("threadId");
        return threadId == null ? null : threadId.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
```

#### 三、修改 `SupplierPerformanceSnapshotTool`

文件：

```text
inventory_back/src/main/java/com/xixi/agent/tool/SupplierPerformanceSnapshotTool.java
```

完整代码建议：

```java
package com.xixi.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xixi.agent.dto.SupplierScoreRequest;
import com.xixi.agent.mapper.SupplierPerformanceMapper;
import com.xixi.agent.service.AgentSessionService;
import com.xixi.agent.vo.SupplierPerformanceMetricsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.BiFunction;

@Component
@RequiredArgsConstructor
public class SupplierPerformanceSnapshotTool implements BiFunction<SupplierScoreRequest, ToolContext, String> {
    private final SupplierPerformanceMapper supplierPerformanceMapper;
    private final AgentSessionService agentSessionService;
    private final ObjectMapper objectMapper;

    @Override
    public String apply(SupplierScoreRequest request, ToolContext toolContext) {
        try {
            String threadId = extractThreadId(toolContext);
            String requestJson = toJson(request);

            if (request == null || request.getSupplierId() == null) {
                String responseJson = objectMapper.writeValueAsString(Map.of(
                        "success", false,
                        "message", "supplierId不能为空"
                ));
                agentSessionService.saveToolMessage(threadId, "supplierPerformanceSnapshotTool", requestJson, responseJson);
                return responseJson;
            }

            Integer days = request.getDays() == null ? 30 : request.getDays();
            SupplierPerformanceMetricsVO metrics =
                    supplierPerformanceMapper.getSupplierPerformanceMetrics(request.getSupplierId(), days);

            if (metrics == null) {
                String responseJson = objectMapper.writeValueAsString(Map.of(
                        "success", false,
                        "message", "供应商不存在"
                ));
                agentSessionService.saveToolMessage(threadId, "supplierPerformanceSnapshotTool", requestJson, responseJson);
                return responseJson;
            }

            String responseJson = objectMapper.writeValueAsString(Map.of(
                    "success", true,
                    "data", metrics
            ));
            agentSessionService.saveToolMessage(threadId, "supplierPerformanceSnapshotTool", requestJson, responseJson);
            return responseJson;
        } catch (Exception e) {
            return "{\"success\":false,\"message\":\"供应商履约工具执行失败\"}";
        }
    }

    private String extractThreadId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object threadId = toolContext.getContext().get("threadId");
        return threadId == null ? null : threadId.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
```

### 15.11.1F 这些代码具体在做什么

每个 Tool 都多做了 4 件事：

```text
1. 从 ToolContext 中拿 threadId
2. 把工具请求对象序列化成 requestJson
3. 把工具返回结果序列化成 responseJson
4. 调用 agentSessionService.saveToolMessage(...) 落库
```

也就是说，现在工具不仅能返回结果给模型，还会在数据库里留痕。

### 15.11.1G 为什么要从 ToolContext 里拿 threadId

因为 Tool 是在模型调用时执行的，它本身拿不到 Controller 请求对象。

但 Spring AI 会把上下文包装进：

```java
ToolContext toolContext
```

你现在用的是：

```java
toolContext.getContext().get("threadId")
```

这样就能找到当前会话对应的 `threadId`，进一步找到 `agent_session`。

### 15.11.1H 现在工具调用落库后的效果

当 Agent 调用了：

- `getPurchaseOrderSnapshot`
- `procurementWarningSnapshotTool`
- `supplierPerformanceSnapshotTool`

数据库的 `agent_message` 会新增一条：

```text
message_role = TOOL
message_type = TOOL_RESULT
tool_name = 工具名称
tool_request_json = 本次工具请求参数
tool_response_json = 本次工具返回结果
```

### 15.11.1I 第二阶段补充完成后的实际能力

到这里，你的会话持久化已经不只是：

```text
保存用户消息
保存 Agent 回复
保存 state
保存最终结果
```

还新增了：

```text
保存 Tool 调用轨迹
```

所以你现在的 `threadId` 已经能对应完整会话过程：

```text
USER
  ↓
TOOL
  ↓
ASSISTANT
  ↓
state_json
  ↓
result
```

#### 15.11.2 session 标题自动生成

第一版标题是截取用户第一句话。

第二版可以让模型生成：

```text
PO2026040011 订单阻塞诊断
供应商1履约评分
采购执行风险扫描
```

## 16. 总结

统一 `/agent/workflow/execute` 后，会话持久化应该围绕 `threadId` 设计。

核心原则：

```text
threadId 代表会话，不代表用户。
一个用户可以有多个 threadId。
没有 threadId 就创建新会话。
有 threadId 就恢复旧会话。
```

最终链路：

```text
用户请求
    ↓
创建/恢复 agent_session
    ↓
保存 USER 消息
    ↓
执行 Workflow
    ↓
保存 ASSISTANT 消息
    ↓
保存 agent_session_state
    ↓
保存 agent_result
    ↓
返回 threadId 给前端
```

这就是你当前项目最小可执行的 Agent 会话持久化方案。
