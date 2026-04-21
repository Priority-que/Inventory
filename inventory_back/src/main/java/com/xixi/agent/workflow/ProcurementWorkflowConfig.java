package com.xixi.agent.workflow;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.xixi.agent.mapper.AgentQueryMapper;
import com.xixi.agent.mapper.AgentWarningMapper;
import com.xixi.agent.mapper.SupplierPerformanceMapper;
import com.xixi.agent.service.AgentRagService;
import com.xixi.agent.service.AgentSessionService;
import com.xixi.agent.service.ProcessDiagnosisAgentService;
import com.xixi.agent.workflow.node.*;
import com.xixi.agent.workflow.state.WorkflowIntent;
import com.xixi.agent.workflow.state.WorkflowStateKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class ProcurementWorkflowConfig {

    @Bean
    public StateGraph procurementWorkflowGraph(ChatClient.Builder chatClientBuilder,
                                               AgentQueryMapper agentQueryMapper,
                                               ProcessDiagnosisAgentService processDiagnosisAgentService,
                                               AgentWarningMapper agentWarningMapper,
                                               SupplierPerformanceMapper supplierPerformanceMapper,
                                               AgentSessionService agentSessionService,
                                               AgentRagService agentRagService,
                                               ObjectMapper objectMapper) throws Exception {
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put(WorkflowStateKeys.MESSAGE, new ReplaceStrategy());
            strategies.put(WorkflowStateKeys.THREAD_ID, new ReplaceStrategy());
            strategies.put(WorkflowStateKeys.NORMALIZED_MESSAGE, new ReplaceStrategy());
            strategies.put(WorkflowStateKeys.INTENT, new ReplaceStrategy());
            strategies.put(WorkflowStateKeys.ENTITY, new ReplaceStrategy());
            strategies.put(WorkflowStateKeys.NEED_RAG, new ReplaceStrategy());
            strategies.put(WorkflowStateKeys.RAG_DOCS, new ReplaceStrategy());
            strategies.put(WorkflowStateKeys.ORDER_SNAPSHOT, new ReplaceStrategy());
            strategies.put(WorkflowStateKeys.ORDER_DIAGNOSIS, new ReplaceStrategy());
            strategies.put(WorkflowStateKeys.WARNING_ITEMS, new ReplaceStrategy());
            strategies.put(WorkflowStateKeys.WARNING_ANALYSIS, new ReplaceStrategy());
            strategies.put(WorkflowStateKeys.WARNING_CONTEXT, new ReplaceStrategy());
            strategies.put(WorkflowStateKeys.SUPPLIER_METRICS, new ReplaceStrategy());
            strategies.put(WorkflowStateKeys.SUPPLIER_SCORE, new ReplaceStrategy());
            strategies.put(WorkflowStateKeys.LLM_ANSWER, new ReplaceStrategy());
            strategies.put(WorkflowStateKeys.GUARDRAIL_RESULT, new ReplaceStrategy());
            strategies.put(WorkflowStateKeys.FINAL_RESPONSE, new ReplaceStrategy());
            strategies.put(WorkflowStateKeys.ERROR_MESSAGE, new ReplaceStrategy());
            strategies.put("_route", new ReplaceStrategy());
            return strategies;
        };

        StateGraph graph = new StateGraph(keyStrategyFactory);

        graph.addNode("preprocessInput", node_async(new PreprocessInputNode()));
        graph.addNode("classifyIntent", com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async(
                new IntentClassifyNode(chatClientBuilder.build())));
        graph.addNode("extractEntities", node_async(new EntityExtractNode()));
        graph.addNode("retrieveKnowledge", node_async(new KnowledgeRetrieveNode(agentRagService)));
        graph.addNode("routeByIntent", node_async(new RouteDecisionNode()));
        graph.addNode("loadOrderContext", node_async(new LoadOrderContextNode(agentQueryMapper, agentSessionService, objectMapper)));
        graph.addNode("analyzeOrderByRules", node_async(new OrderRuleAnalyzeNode(processDiagnosisAgentService)));
        graph.addNode("generateBusinessAnswer", com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async(
                new BusinessAnswerGenerateNode(chatClientBuilder.build())));
        graph.addNode("guardrailValidate", node_async(new GuardrailValidateNode()));
        graph.addNode("buildFinalResponse", node_async(new BuildFinalResponseNode()));
        graph.addNode("loadWarningContext", node_async(new LoadWarningContextNode(agentWarningMapper, agentSessionService, objectMapper)));
        graph.addNode("analyzeWarningsByRules", node_async(new WarningRuleAnalyzeNode()));
        graph.addNode("loadSupplierContext", node_async(new LoadSupplierContextNode(supplierPerformanceMapper, agentSessionService, objectMapper)));
        graph.addNode("scoreSupplierByRules", node_async(new SupplierScoreRuleNode()));
        graph.addEdge(StateGraph.START, "preprocessInput");
        graph.addEdge("preprocessInput", "classifyIntent");
        graph.addEdge("classifyIntent", "extractEntities");
        graph.addEdge("extractEntities", "retrieveKnowledge");
        graph.addEdge("retrieveKnowledge", "routeByIntent");

        graph.addConditionalEdges(
                "routeByIntent",
                edge_async(state -> state.value("_route", WorkflowIntent.UNKNOWN.name()).toString()),
                Map.of(
                        WorkflowIntent.ORDER_DIAGNOSIS.name(), "loadOrderContext",
                        WorkflowIntent.WARNING_SCAN.name(), "loadWarningContext",
                        WorkflowIntent.SUPPLIER_SCORE.name(), "loadSupplierContext",
                        WorkflowIntent.KNOWLEDGE_QA.name(), "generateBusinessAnswer",
                        WorkflowIntent.UNKNOWN.name(), "generateBusinessAnswer"
                )
        );

        graph.addEdge("loadOrderContext", "analyzeOrderByRules");
        graph.addEdge("analyzeOrderByRules", "generateBusinessAnswer");
        graph.addEdge("loadWarningContext", "analyzeWarningsByRules");
        graph.addEdge("analyzeWarningsByRules", "generateBusinessAnswer");
        graph.addEdge("generateBusinessAnswer", "guardrailValidate");
        graph.addEdge("loadSupplierContext", "scoreSupplierByRules");
        graph.addEdge("scoreSupplierByRules", "generateBusinessAnswer");
        graph.addEdge("guardrailValidate", "buildFinalResponse");
        graph.addEdge("buildFinalResponse", StateGraph.END);

        return graph;
    }
}
