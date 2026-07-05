package com.xixi.agent.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xixi.agent.dto.WorkflowAgentRequest;
import com.xixi.agent.entity.AgentSession;
import com.xixi.agent.model.AgentAction;
import com.xixi.agent.model.AgentDecision;
import com.xixi.agent.model.AgentObservation;
import com.xixi.agent.model.AgentToolResult;
import com.xixi.agent.model.AgentWorkflowEvent;
import com.xixi.agent.service.AgentDecisionService;
import com.xixi.agent.service.AgentSessionService;
import com.xixi.agent.service.AgentToolExecutor;
import com.xixi.agent.service.AgentWorkflowService;
import com.xixi.agent.vo.AgentMessageVO;
import com.xixi.agent.vo.WorkflowAgentResponse;
import com.xixi.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class AgentWorkflowServiceImpl implements AgentWorkflowService {
    private static final int MAX_LOOP_STEPS = 3;
    private static final int RECENT_CONTEXT_MAX_MESSAGES = 40;
    private static final int RECENT_CONTEXT_MAX_CHARS = 8000;
    private static final int RECENT_CONTEXT_SINGLE_MESSAGE_MAX_CHARS = 500;

    private final AgentSessionService agentSessionService;
    private final AgentDecisionService agentDecisionService;
    private final AgentToolExecutor agentToolExecutor;
    private final ObjectMapper objectMapper;

    @Override
    public WorkflowAgentResponse execute(WorkflowAgentRequest request, Consumer<AgentWorkflowEvent> eventConsumer) {
        Consumer<AgentWorkflowEvent> events = eventConsumer == null ? event -> { } : eventConsumer;
        String message = normalizeMessage(request == null ? null : request.getMessage());
        Long userId = SecurityUtils.getCurrentUserId();
        AgentSession session = agentSessionService.prepareSession(
                request == null ? null : request.getThreadId(),
                userId,
                message
        );

        List<AgentMessageVO> recentMessages = agentSessionService.getRecentConversationMessages(
                session.getThreadId(),
                userId,
                RECENT_CONTEXT_MAX_MESSAGES
        );
        String recentConversationContext = buildRecentConversationContext(recentMessages);
        Map<String, Object> previousState = agentSessionService.loadStateByThreadId(session.getThreadId());

        if (!message.isBlank()) {
            agentSessionService.saveUserMessage(session, message);
        }

        events.accept(AgentWorkflowEvent.of("start", "START", "AI 助手已接收请求"));

        List<AgentObservation> observations = new ArrayList<>();
        AgentDecision terminalDecision = null;
        List<String> roleCodes = safeRoles();

        for (int step = 1; step <= MAX_LOOP_STEPS; step++) {
            AgentDecision decision = agentDecisionService.decide(
                    message,
                    observations,
                    roleCodes,
                    recentConversationContext,
                    previousState
            );
            decision = decision == null ? AgentDecision.finalAnswer(null) : decision;

            if (AgentAction.CALL_TOOL.equals(decision.getAction())) {
                String status = defaultText(decision.getUserVisibleStatus(), "正在查询业务数据");
                events.accept(AgentWorkflowEvent.of("step", "CALL_TOOL", status));
                AgentToolResult toolResult = executeTool(session, decision, step);
                AgentObservation observation = new AgentObservation();
                observation.setStep(step);
                observation.setToolName(decision.getToolName());
                observation.setArguments(decision.getArguments());
                observation.setResult(toolResult);
                observation.setCreateTime(LocalDateTime.now());
                observations.add(observation);
                if (!toolResult.isSuccess()) {
                    terminalDecision = AgentDecision.finalAnswer(toolResult.getSummary());
                    break;
                }
                continue;
            }

            terminalDecision = decision;
            break;
        }

        if (terminalDecision == null) {
            terminalDecision = AgentDecision.finalAnswer("我已经查询到一些信息，但还需要你补充更具体的问题才能继续。");
        }

        String answer = resolveAnswer(message, observations, terminalDecision, recentConversationContext, previousState);
        WorkflowAgentResponse response = buildResponse(session, terminalDecision, observations, answer);

        agentSessionService.saveAssistantMessage(session, answer);
        agentSessionService.saveState(session, response.getCurrentStage(), response.getIntent(), buildState(response, observations));
        agentSessionService.updateSessionIntent(session, response.getIntent());
        agentSessionService.saveResult(session, response);

        events.accept(AgentWorkflowEvent.of("step", "FINAL_ANSWER", "正在生成最终回答"));
        events.accept(AgentWorkflowEvent.token(answer));
        return response;
    }

    private AgentToolResult executeTool(AgentSession session, AgentDecision decision, int step) {
        if (decision.getToolName() == null || !agentToolExecutor.supports(decision.getToolName())) {
            return AgentToolResult.failure(decision.getToolName(), "模型选择了不在白名单内的工具");
        }
        Map<String, Object> arguments = decision.getArguments() == null ? Map.of() : decision.getArguments();
        AgentToolResult result = agentToolExecutor.execute(decision.getToolName(), arguments);
        agentSessionService.saveToolMessage(
                session.getThreadId(),
                decision.getToolName(),
                toJson(Map.of("step", step, "arguments", arguments)),
                toJson(result)
        );
        return result;
    }

    private String resolveAnswer(String message,
                                 List<AgentObservation> observations,
                                 AgentDecision terminalDecision,
                                 String recentConversationContext,
                                 Map<String, Object> previousState) {
        if (AgentAction.ASK_CLARIFY.equals(terminalDecision.getAction())) {
            return defaultText(terminalDecision.getFinalAnswer(), "我还需要你补充更明确的信息。");
        }
        if (observations == null || observations.isEmpty()) {
            String fallback = defaultText(terminalDecision.getFinalAnswer(),
                    "好呀，我在。你可以先放松一下，等想继续看业务数据时再告诉我。");
            return agentDecisionService.buildFinalAnswer(message, observations, fallback, recentConversationContext, previousState);
        }
        String fallback = defaultText(terminalDecision.getFinalAnswer(), buildEvidenceAnswer(observations));
        return agentDecisionService.buildFinalAnswer(message, observations, fallback, recentConversationContext, previousState);
    }

    private WorkflowAgentResponse buildResponse(AgentSession session,
                                                AgentDecision terminalDecision,
                                                List<AgentObservation> observations,
                                                String answer) {
        WorkflowAgentResponse response = new WorkflowAgentResponse();
        response.setSessionId(session.getId());
        response.setThreadId(session.getThreadId());
        response.setIntent(resolveIntent(terminalDecision, observations));
        response.setCurrentStage("FINAL_ANSWER");
        response.setRiskLevel("NONE");
        response.setSuggestOwner("CURRENT_USER");
        response.setSuggestAction(resolveSuggestAction(terminalDecision, observations));
        response.setEvidence(buildEvidence(observations));
        response.setAnswer(answer);
        response.setData(buildData(terminalDecision, observations));
        return response;
    }

    private String resolveIntent(AgentDecision terminalDecision, List<AgentObservation> observations) {
        if (terminalDecision != null && AgentAction.ASK_CLARIFY.equals(terminalDecision.getAction())) {
            return "ASK_CLARIFY";
        }
        if (observations == null || observations.isEmpty()) {
            return "GENERAL_CHAT";
        }
        boolean hasTodo = observations.stream().anyMatch(item -> "list_role_todos".equals(item.getToolName()));
        if (hasTodo) {
            return "BUSINESS_TODO";
        }
        boolean hasRag = observations.stream().anyMatch(item -> "search_business_knowledge".equals(item.getToolName()));
        if (hasRag) {
            return "BUSINESS_KNOWLEDGE_QA";
        }
        return "BUSINESS_QA";
    }

    private String resolveSuggestAction(AgentDecision terminalDecision, List<AgentObservation> observations) {
        if (terminalDecision != null && AgentAction.ASK_CLARIFY.equals(terminalDecision.getAction())) {
            return "补充必要信息";
        }
        if (observations == null || observations.isEmpty()) {
            return "继续对话";
        }
        for (AgentObservation observation : observations) {
            Object nextAction = observation.getResult().getFacts().get("nextAction");
            if (nextAction != null) {
                return String.valueOf(nextAction);
            }
        }
        return "按待办建议处理";
    }

    private Object buildData(AgentDecision terminalDecision, List<AgentObservation> observations) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", terminalDecision == null ? AgentAction.FINAL_ANSWER : terminalDecision.getAction());
        data.put("observations", observations);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("sourceTools", observations.stream().map(AgentObservation::getToolName).toList());
        evidence.put("items", observations.stream().flatMap(item -> item.getResult().getItems().stream()).toList());
        evidence.put("facts", mergeFacts(observations));
        evidence.put("errors", observations.stream()
                .map(AgentObservation::getResult)
                .filter(result -> !result.isSuccess())
                .map(AgentToolResult::getError)
                .toList());
        data.put("evidence", evidence);
        return data;
    }

    private Map<String, Object> mergeFacts(List<AgentObservation> observations) {
        Map<String, Object> facts = new LinkedHashMap<>();
        if (observations == null) {
            return facts;
        }
        for (AgentObservation observation : observations) {
            facts.putAll(observation.getResult().getFacts());
        }
        return facts;
    }

    private List<String> buildEvidence(List<AgentObservation> observations) {
        if (observations == null || observations.isEmpty()) {
            return List.of("本次未调用业务工具。");
        }
        List<String> evidence = new ArrayList<>();
        for (AgentObservation observation : observations) {
            evidence.add("第 " + observation.getStep() + " 步调用工具：" + observation.getToolName());
            evidence.addAll(observation.getResult().getEvidence());
        }
        return evidence;
    }

    private Map<String, Object> buildState(WorkflowAgentResponse response, List<AgentObservation> observations) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("lastIntent", response.getIntent());
        state.put("lastStage", response.getCurrentStage());
        state.put("lastSuggestAction", response.getSuggestAction());
        state.put("lastAnswer", truncate(response.getAnswer(), RECENT_CONTEXT_SINGLE_MESSAGE_MAX_CHARS));
        state.put("lastToolName", lastToolName(observations));
        state.put("lastArguments", lastArguments(observations));
        state.put("lastObservations", compactObservations(observations));
        return state;
    }

    private String buildRecentConversationContext(List<AgentMessageVO> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (AgentMessageVO message : messages) {
            String role = "USER".equals(message.getMessageRole()) ? "用户" : "助手";
            String content = truncate(message.getContent(), RECENT_CONTEXT_SINGLE_MESSAGE_MAX_CHARS);
            if (content == null || content.isBlank()) {
                continue;
            }
            String line = role + "：" + content.trim() + "\n";
            if (builder.length() + line.length() > RECENT_CONTEXT_MAX_CHARS) {
                break;
            }
            builder.append(line);
        }
        return builder.toString().trim();
    }

    private String lastToolName(List<AgentObservation> observations) {
        if (observations == null || observations.isEmpty()) {
            return null;
        }
        return observations.get(observations.size() - 1).getToolName();
    }

    private Map<String, Object> lastArguments(List<AgentObservation> observations) {
        if (observations == null || observations.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> arguments = observations.get(observations.size() - 1).getArguments();
        return arguments == null ? Map.of() : new LinkedHashMap<>(arguments);
    }

    private List<Object> compactObservations(List<AgentObservation> observations) {
        if (observations == null || observations.isEmpty()) {
            return List.of();
        }
        List<Object> compactList = new ArrayList<>();
        for (AgentObservation observation : observations) {
            Map<String, Object> compact = new LinkedHashMap<>();
            AgentToolResult result = observation.getResult();
            compact.put("step", observation.getStep());
            compact.put("toolName", observation.getToolName());
            compact.put("arguments", observation.getArguments());
            compact.put("success", result != null && result.isSuccess());
            compact.put("summary", result == null ? null : truncate(result.getSummary(), RECENT_CONTEXT_SINGLE_MESSAGE_MAX_CHARS));
            compact.put("facts", compactFacts(result == null ? null : result.getFacts()));
            compactList.add(compact);
        }
        return compactList;
    }

    private Map<String, Object> compactFacts(Map<String, Object> facts) {
        if (facts == null || facts.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> compact = new LinkedHashMap<>();
        copyFact(compact, facts, "stageText");
        copyFact(compact, facts, "nextAction");
        copyFact(compact, facts, "route");
        copyFact(compact, facts, "orderCreated");
        copyFact(compact, facts, "count");
        copyFact(compact, facts, "todoCount");
        copyFact(compact, facts, "stockStatus");
        copyFact(compact, facts, "supplierId");
        copyFact(compact, facts, "supplierName");
        copyFact(compact, facts, "status");
        copyFact(compact, facts, "activeFileCount");
        copyFact(compact, facts, "ragAvailable");
        return compact;
    }

    private void copyFact(Map<String, Object> target, Map<String, Object> source, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    private String buildEvidenceAnswer(List<AgentObservation> observations) {
        if (observations == null || observations.isEmpty()) {
            return "我在。你可以继续说。";
        }
        StringBuilder builder = new StringBuilder("我查到这些信息：\n");
        for (AgentObservation observation : observations) {
            builder.append("- ")
                    .append(observation.getResult().getSummary())
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private List<String> safeRoles() {
        List<String> roleCodes = SecurityUtils.getCurrentUserRoleCodes();
        return roleCodes == null ? List.of() : roleCodes;
    }

    private String normalizeMessage(String message) {
        return message == null ? "" : message.trim();
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
