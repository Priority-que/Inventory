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
import com.xixi.agent.workflow.state.WorkflowEntity;
import com.xixi.agent.workflow.state.WorkflowStateKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
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
        requireLogin(userId);
        if (threadId != null && !threadId.isBlank()) {
            AgentSession session = agentSessionMapper.getByThreadId(threadId);
            if (session != null) {
                if (!userId.equals(session.getUserId())) {
                    throw new AccessDeniedException("无权访问该会话");
                }
                return session;
            }
        }

        AgentSession session = new AgentSession();
        session.setSessionNo(generateSessionNo());
        session.setThreadId(threadId != null && !threadId.isBlank() ? threadId : generateThreadId());
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
        requireLogin(userId);
        return agentSessionMapper.getSessionListByUserId(userId);
    }

    @Override
    public List<AgentMessageVO> getMessagesByThreadId(String threadId, Long userId) {
        requireLogin(userId);
        AgentSession session = agentSessionMapper.getByThreadIdAndUserId(threadId, userId);
        if (session == null) {
            throw new AccessDeniedException("无权访问该会话");
        }
        return agentMessageMapper.getMessagesByThreadId(threadId);
    }

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

    private void requireLogin(Long userId) {
        if (userId == null) {
            throw new AuthenticationCredentialsNotFoundException("请先登录");
        }
    }
}
