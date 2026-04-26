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

    List<AgentMessageVO> getMessagesByThreadId(String threadId, Long userId);
    Map<String, Object> loadStateByThreadId(String threadId);
}
