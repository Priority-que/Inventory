package com.xixi.agent.service;

import com.xixi.agent.dto.WorkflowAgentRequest;
import com.xixi.agent.model.AgentWorkflowEvent;
import com.xixi.agent.vo.WorkflowAgentResponse;

import java.util.function.Consumer;

public interface AgentWorkflowService {
    WorkflowAgentResponse execute(WorkflowAgentRequest request, Consumer<AgentWorkflowEvent> eventConsumer);
}
