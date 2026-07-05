package com.xixi.agent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xixi.agent.model.AgentAction;
import com.xixi.agent.model.AgentDecision;
import com.xixi.agent.model.AgentObservation;
import com.xixi.agent.service.AgentDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AgentDecisionServiceImpl implements AgentDecisionService {
    private static final Set<String> ALLOWED_TOOLS = Set.of(
            "list_role_todos",
            "get_purchase_order_context",
            "get_purchase_request_context",
            "list_inventory_alerts",
            "get_supplier_profile_context",
            "search_business_knowledge"
    );

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final ObjectMapper objectMapper;

    @Override
    public AgentDecision decide(String userMessage,
                                List<AgentObservation> observations,
                                List<String> roleCodes,
                                String recentConversationContext,
                                Map<String, Object> previousState) {
        if (userMessage == null || userMessage.isBlank()) {
            return AgentDecision.askClarify("你可以直接告诉我想查什么，比如待办、订单状态、采购申请进度，或者只是随便聊聊。");
        }

        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return fallbackDecision(userMessage, observations, previousState);
        }

        try {
            String response = builder.build()
                    .prompt()
                    .system(buildDecisionSystemPrompt())
                    .user(buildDecisionUserPrompt(userMessage, observations, roleCodes, recentConversationContext, previousState))
                    .call()
                    .content();
            AgentDecision decision = parseDecision(response);
            return normalizeDecision(decision, userMessage, observations, previousState);
        } catch (Exception ex) {
            return fallbackDecision(userMessage, observations, previousState);
        }
    }

    @Override
    public String buildFinalAnswer(String userMessage,
                                   List<AgentObservation> observations,
                                   String fallbackAnswer,
                                   String recentConversationContext,
                                   Map<String, Object> previousState) {
        if (observations == null || observations.isEmpty()) {
            return callChat(
                    "你是库存采购系统里的温和聊天助手。用户没有要求查询业务数据时，轻松自然地回复。可以参考最近会话保持连贯，但不要主动把话题拉回业务。",
                    buildAnswerUserPrompt(userMessage, recentConversationContext, previousState, null),
                    fallbackAnswer
            );
        }

        String observationJson = toJson(observations);
        String system = """
                你是库存采购系统的业务助手。只能基于 Observation 中的真实工具结果回答。
                要求：
                1. 不要编造订单、库存、供应商、审批结果。
                2. 查不到就明确说查不到。
                3. 如果 Observation 包含跳转路径，可以在建议中直接给出。
                4. 回答要简洁，先给结论，再列关键依据和下一步建议。
                5. 最近会话和上一轮状态只用于理解指代，不能替代 Observation 中的最新业务事实。
                """;
        String user = buildAnswerUserPrompt(userMessage, recentConversationContext, previousState, observationJson);
        return callChat(system, user, fallbackAnswer);
    }

    private AgentDecision parseDecision(String content) throws Exception {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("empty decision");
        }
        String json = extractJson(content);
        return objectMapper.readValue(json, AgentDecision.class);
    }

    private String extractJson(String content) {
        String text = content.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private AgentDecision normalizeDecision(AgentDecision decision,
                                            String userMessage,
                                            List<AgentObservation> observations,
                                            Map<String, Object> previousState) {
        if (decision == null || decision.getAction() == null) {
            return fallbackDecision(userMessage, observations, previousState);
        }
        String action = decision.getAction().trim().toUpperCase(Locale.ROOT);
        decision.setAction(action);

        if (AgentAction.CALL_TOOL.equals(action)) {
            if (observations != null && observations.size() >= 3) {
                return AgentDecision.finalAnswer(null);
            }
            if (decision.getToolName() == null || !ALLOWED_TOOLS.contains(decision.getToolName())) {
                return fallbackDecision(userMessage, observations, previousState);
            }
            if (decision.getArguments() == null) {
                decision.setArguments(Map.of());
            }
            if (decision.getUserVisibleStatus() == null || decision.getUserVisibleStatus().isBlank()) {
                decision.setUserVisibleStatus("正在查询业务数据");
            }
            return decision;
        }

        if (AgentAction.ASK_CLARIFY.equals(action)) {
            if (decision.getFinalAnswer() == null || decision.getFinalAnswer().isBlank()) {
                return AgentDecision.askClarify("我还需要你补充单号、申请号或更明确的问题。");
            }
            return decision;
        }

        if (AgentAction.FINAL_ANSWER.equals(action)) {
            if ((observations == null || observations.isEmpty()) && needsBusinessEvidence(userMessage)) {
                return fallbackDecision(userMessage, observations, previousState);
            }
            return decision;
        }

        return fallbackDecision(userMessage, observations, previousState);
    }

    private AgentDecision fallbackDecision(String userMessage,
                                           List<AgentObservation> observations,
                                           Map<String, Object> previousState) {
        if (observations != null && !observations.isEmpty()) {
            return AgentDecision.finalAnswer(null);
        }

        String text = userMessage == null ? "" : userMessage.trim().toLowerCase(Locale.ROOT);
        AgentDecision decision = new AgentDecision();

        AgentDecision referenceDecision = referenceDecision(text, previousState);
        if (referenceDecision != null) {
            return referenceDecision;
        }

        if (containsAny(text, "待办", "要处理", "审批", "有什么事", "今天要做")) {
            decision.setAction(AgentAction.CALL_TOOL);
            decision.setToolName("list_role_todos");
            decision.setUserVisibleStatus("正在查询你的业务待办");
            decision.setArguments(Map.of("limit", 10));
            return decision;
        }

        if (containsAny(text, "订单", "po")) {
            String orderNo = extractCode(userMessage, "PO");
            if (orderNo == null && containsReference(text)) {
                orderNo = lastArgument(previousState, "orderNo");
            }
            if (orderNo == null) {
                return AgentDecision.askClarify("请补充采购订单号，例如 PO202604211230001001。");
            }
            decision.setAction(AgentAction.CALL_TOOL);
            decision.setToolName("get_purchase_order_context");
            decision.setUserVisibleStatus("正在查询采购订单状态");
            decision.setArguments(Map.of("orderNo", orderNo));
            return decision;
        }

        if (containsAny(text, "采购申请", "申请单", "pr")) {
            String requestNo = extractCode(userMessage, "PR");
            if (requestNo == null && containsReference(text)) {
                requestNo = lastArgument(previousState, "requestNo");
            }
            if (requestNo == null) {
                return AgentDecision.askClarify("请补充采购申请单号，例如 PR202604211230001001。");
            }
            decision.setAction(AgentAction.CALL_TOOL);
            decision.setToolName("get_purchase_request_context");
            decision.setUserVisibleStatus("正在查询采购申请状态");
            decision.setArguments(Map.of("requestNo", requestNo));
            return decision;
        }

        if (containsAny(text, "低库存", "安全库存", "库存预警", "库存不足")) {
            decision.setAction(AgentAction.CALL_TOOL);
            decision.setToolName("list_inventory_alerts");
            decision.setUserVisibleStatus("正在查询库存预警");
            decision.setArguments(Map.of("stockStatus", "LOW", "limit", 10));
            return decision;
        }

        if (containsAny(text, "供应商资料", "供应商资质", "资质审核")) {
            decision.setAction(AgentAction.CALL_TOOL);
            decision.setToolName("get_supplier_profile_context");
            decision.setUserVisibleStatus("正在查询供应商资料状态");
            decision.setArguments(Map.of());
            return decision;
        }

        if (containsAny(text, "规则", "流程", "制度", "怎么流转", "状态")) {
            decision.setAction(AgentAction.CALL_TOOL);
            decision.setToolName("search_business_knowledge");
            decision.setUserVisibleStatus("正在检索业务规则");
            decision.setArguments(Map.of("query", userMessage, "topK", 4));
            return decision;
        }

        return AgentDecision.finalAnswer(null);
    }

    private AgentDecision referenceDecision(String text, Map<String, Object> previousState) {
        if (!containsReference(text) || previousState == null || previousState.isEmpty()) {
            return null;
        }
        String lastToolName = stringState(previousState, "lastToolName");
        Map<String, Object> arguments = mapState(previousState, "lastArguments");
        if ("get_purchase_order_context".equals(lastToolName) && hasAnyArgument(arguments, "orderNo", "orderId")) {
            AgentDecision decision = new AgentDecision();
            decision.setAction(AgentAction.CALL_TOOL);
            decision.setToolName("get_purchase_order_context");
            decision.setUserVisibleStatus("正在根据上次提到的订单重新查询最新状态");
            decision.setArguments(arguments);
            return decision;
        }
        if ("get_purchase_request_context".equals(lastToolName) && hasAnyArgument(arguments, "requestNo", "requestId")) {
            AgentDecision decision = new AgentDecision();
            decision.setAction(AgentAction.CALL_TOOL);
            decision.setToolName("get_purchase_request_context");
            decision.setUserVisibleStatus("正在根据上次提到的采购申请重新查询最新状态");
            decision.setArguments(arguments);
            return decision;
        }
        if ("list_role_todos".equals(lastToolName) && containsAny(text, "待办", "第二个", "第2个", "继续", "下一步")) {
            AgentDecision decision = new AgentDecision();
            decision.setAction(AgentAction.CALL_TOOL);
            decision.setToolName("list_role_todos");
            decision.setUserVisibleStatus("正在刷新你的业务待办");
            decision.setArguments(arguments == null || arguments.isEmpty() ? Map.of("limit", 10) : arguments);
            return decision;
        }
        return null;
    }

    private boolean containsReference(String text) {
        return containsAny(text, "刚才", "上次", "之前", "这个", "那个", "它", "继续", "下一步", "怎么办");
    }

    private boolean needsBusinessEvidence(String userMessage) {
        String text = userMessage == null ? "" : userMessage.trim().toLowerCase(Locale.ROOT);
        return containsAny(text,
                "待办", "审批", "订单", "采购申请", "申请单", "库存", "供应商",
                "到货", "入库", "安全库存", "流水", "规则", "流程", "状态", "po", "pr");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String extractCode(String message, String prefix) {
        if (message == null) {
            return null;
        }
        String upper = message.toUpperCase(Locale.ROOT);
        int index = upper.indexOf(prefix);
        if (index < 0) {
            return null;
        }
        StringBuilder code = new StringBuilder();
        for (int i = index; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                code.append(c);
                continue;
            }
            break;
        }
        return code.length() >= 4 ? code.toString() : null;
    }

    private String callChat(String system, String user, String fallbackAnswer) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return fallbackAnswer != null ? fallbackAnswer : "我在。你可以慢慢说，我会尽量用简单清楚的方式陪你聊。";
        }
        try {
            String answer = builder.build()
                    .prompt()
                    .system(system)
                    .user(user)
                    .call()
                    .content();
            if (answer == null || answer.isBlank()) {
                return fallbackAnswer;
            }
            return answer.trim();
        } catch (Exception ex) {
            return fallbackAnswer != null ? fallbackAnswer : "我在。你可以慢慢说，我会尽量用简单清楚的方式陪你聊。";
        }
    }

    private String buildDecisionSystemPrompt() {
        return """
                你是库存采购系统的 Agent 决策器，只输出 JSON，不输出解释。
                你可以选择：
                - CALL_TOOL：需要真实业务数据或规则检索时。
                - FINAL_ANSWER：普通聊天，或已有 Observation 足够回答时。
                - ASK_CLARIFY：缺少单号、参数或问题不明确时。

                白名单 Tool：
                1. list_role_todos：查询当前登录用户按角色对应的业务待办。
                2. get_purchase_order_context：查询采购订单上下文，参数 orderNo 或 orderId。
                3. get_purchase_request_context：查询采购申请上下文，参数 requestNo 或 requestId。
                4. list_inventory_alerts：查询库存预警，参数 stockStatus、limit。
                5. get_supplier_profile_context：查询供应商资料状态，供应商角色可不传参数，管理员可传 supplierId。
                6. search_business_knowledge：检索规则/制度/RAG，参数 query、bizIntent、topK。

                规则：
                - 不要生成 SQL。
                - 不要编造业务数据。
                - 普通闲聊、疲惫、吐槽、情绪表达时，直接 FINAL_ANSWER，不调用 Tool。
                - 实时业务状态优先业务 Tool。
                - 规则/流程/制度问题优先 search_business_knowledge。
                - 参数不足时 ASK_CLARIFY。
                - 最近会话上下文和上一轮状态只能用于理解“刚才那个”“它”“继续”等指代。
                - 如果用户追问的是订单、采购申请、库存、供应商、审批等实时业务状态，必须基于指代信息继续 CALL_TOOL 重新查询。

                JSON 格式：
                {"action":"CALL_TOOL|FINAL_ANSWER|ASK_CLARIFY","toolName":"白名单Tool或null","arguments":{},"userVisibleStatus":"给用户看的阶段状态","finalAnswer":"最终回答或澄清问题，CALL_TOOL时为null"}
                """;
    }

    private String buildDecisionUserPrompt(String userMessage,
                                           List<AgentObservation> observations,
                                           List<String> roleCodes,
                                           String recentConversationContext,
                                           Map<String, Object> previousState) {
        return "当前用户角色：" + roleCodes + "\n"
                + "最近会话上下文：\n" + defaultContext(recentConversationContext) + "\n\n"
                + "上一轮结构化状态：\n" + toJson(previousState) + "\n\n"
                + "用户输入：" + userMessage + "\n"
                + "已有 Observation：" + toJson(observations);
    }

    private String buildAnswerUserPrompt(String userMessage,
                                         String recentConversationContext,
                                         Map<String, Object> previousState,
                                         String observationJson) {
        StringBuilder builder = new StringBuilder();
        builder.append("最近会话上下文：\n")
                .append(defaultContext(recentConversationContext))
                .append("\n\n上一轮结构化状态：\n")
                .append(toJson(previousState))
                .append("\n\n当前用户问题：")
                .append(userMessage);
        if (observationJson != null) {
            builder.append("\n\nObservation：\n").append(observationJson);
        }
        return builder.toString();
    }

    private String defaultContext(String recentConversationContext) {
        if (recentConversationContext == null || recentConversationContext.isBlank()) {
            return "无";
        }
        return recentConversationContext;
    }

    private String lastArgument(Map<String, Object> previousState, String key) {
        if (previousState == null || previousState.isEmpty()) {
            return null;
        }
        Map<String, Object> arguments = mapState(previousState, "lastArguments");
        Object argument = arguments.get(key);
        if (argument == null) {
            return null;
        }
        String text = String.valueOf(argument).trim();
        return text.isEmpty() ? null : text;
    }

    private boolean hasAnyArgument(Map<String, Object> arguments, String... keys) {
        if (arguments == null || arguments.isEmpty()) {
            return false;
        }
        for (String key : keys) {
            Object value = arguments.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return true;
            }
        }
        return false;
    }

    private String stringState(Map<String, Object> state, String key) {
        if (state == null) {
            return null;
        }
        Object value = state.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Map<String, Object> mapState(Map<String, Object> state, String key) {
        if (state == null) {
            return Map.of();
        }
        Object value = state.get(key);
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        return raw.entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .collect(java.util.stream.Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        Map.Entry::getValue,
                        (left, right) -> right,
                        java.util.LinkedHashMap::new
                ));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }
}
