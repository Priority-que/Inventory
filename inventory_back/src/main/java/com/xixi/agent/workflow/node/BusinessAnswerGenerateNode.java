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
        String intent = state.value(WorkflowStateKeys.INTENT,"UNKNOWN").toString();
        String message = state.value(WorkflowStateKeys.MESSAGE,"").toString();
        String ragDocs = state.value(WorkflowStateKeys.RAG_DOCS,"").toString();
        String errorMessage = state.value(WorkflowStateKeys.ERROR_MESSAGE,"").toString();
        if (errorMessage != null && !errorMessage.isBlank()) {
            return Map.of(WorkflowStateKeys.LLM_ANSWER, errorMessage);
        }
        String prompt;
        if("ORDER_DIAGNOSIS".equals(intent)){
            Object snapshot = state.value(WorkflowStateKeys.ORDER_SNAPSHOT).orElse(null);
            Object diagnosis = state.value(WorkflowStateKeys.ORDER_DIAGNOSIS).orElse(null);
            prompt = WorkflowPrompts.ORDER_BUSINESS_PROMPT
                    .replace("{message}", message)
                    .replace("{orderSnapshot}",String.valueOf(snapshot))
                    .replace("{orderDiagnosis}",String.valueOf(diagnosis))
                    .replace("{ragDocs}",ragDocs);
        }else if ("WARNING_SCAN".equals(intent)){
            Object warningAnalysis = state.value(WorkflowStateKeys.WARNING_ANALYSIS).orElse(null);
            prompt = WorkflowPrompts.WARNING_BUSINESS_PROMPT
                    .replace("{message}", message)
                    .replace("{warningItems}", String.valueOf(warningAnalysis))
                    .replace("{ragDocs}", ragDocs);
        }else if ("SUPPLIER_SCORE".equals(intent)){
            Object metrics = state.value(WorkflowStateKeys.SUPPLIER_METRICS).orElse(null);
            Object score = state.value(WorkflowStateKeys.SUPPLIER_SCORE).orElse(null);
            prompt = WorkflowPrompts.SUPPLIER_BUSINESS_PROMPT
                    .replace("{message}", message)
                    .replace("{supplierMetrics}", String.valueOf(metrics))
                    .replace("{supplierScore}", String.valueOf(score))
                    .replace("{ragDocs}", ragDocs);
        }else{
            prompt = "用户问题无法识别，请提示用户补充订单号、供应商ID或扫描范围。";
        }
        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        return Map.of(WorkflowStateKeys.LLM_ANSWER,answer);
    }
}
