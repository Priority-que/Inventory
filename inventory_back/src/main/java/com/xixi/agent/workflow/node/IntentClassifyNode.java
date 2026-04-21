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
        String message = state.value(WorkflowStateKeys.NORMALIZED_MESSAGE,"").toString();
        String previousIntent = state.value(WorkflowStateKeys.INTENT, "UNKNOWN").toString();
        PromptTemplate template = new PromptTemplate(WorkflowPrompts.INTENT_CLASSIFY_PROMPT);
        String intentText = chatClient.prompt()
                .user(user -> user
                        .text(template.getTemplate())
                        .param("previousIntent", previousIntent)
                        .param("message",message))
                .call()
                .content();
        WorkflowIntent intent = parseIntent(intentText);
        if ((intent == WorkflowIntent.UNKNOWN || intent == WorkflowIntent.KNOWLEDGE_QA)
                && !"UNKNOWN".equals(previousIntent)) {
            intent = parseIntent(previousIntent);
        }
        return Map.of(WorkflowStateKeys.INTENT,intent.name());
    }
    private WorkflowIntent parseIntent(String text) {
        if (text == null) {
            return WorkflowIntent.UNKNOWN;
        }
        String value = text.trim();
        for(WorkflowIntent intent : WorkflowIntent.values()) {
            if(value.contains(intent.name())) {
                return intent;
            }
        }
        return WorkflowIntent.UNKNOWN;
    }
}
