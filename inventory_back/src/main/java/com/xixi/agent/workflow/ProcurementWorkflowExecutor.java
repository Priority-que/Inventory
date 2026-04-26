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
