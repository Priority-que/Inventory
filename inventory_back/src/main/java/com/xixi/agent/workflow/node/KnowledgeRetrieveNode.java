package com.xixi.agent.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.xixi.agent.service.AgentRagService;
import com.xixi.agent.vo.RagSearchResultVO;
import com.xixi.agent.workflow.state.WorkflowStateKeys;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class KnowledgeRetrieveNode implements NodeAction {
    private final AgentRagService agentRagService;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String intent = state.value(WorkflowStateKeys.INTENT, "UNKNOWN").toString();
        String message = state.value(WorkflowStateKeys.NORMALIZED_MESSAGE, "").toString();

        String docs;
        try {
            List<RagSearchResultVO> hits = agentRagService.search(message, intent, 4);
            docs = buildRagDocs(hits, intent);
        } catch (Exception e) {
            // RAG 不应该阻断主业务链路。Redis 或向量检索异常时，使用兜底规则继续执行 Workflow。
            docs = fallbackDocs(intent);
        }

        return Map.of(WorkflowStateKeys.RAG_DOCS, docs);
    }

    private String buildRagDocs(List<RagSearchResultVO> hits, String intent) {
        if (hits == null || hits.isEmpty()) {
            return fallbackDocs(intent);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("以下内容来自 Redis Stack 知识库检索结果，只能作为业务规则参考，不能替代数据库实时业务数据：\n\n");

        int index = 1;
        for (RagSearchResultVO hit : hits) {
            builder.append("【资料").append(index).append("】")
                    .append(hit.getTitle())
                    .append("，相似度：")
                    .append(hit.getScore() == null ? "N/A" : String.format("%.4f", hit.getScore()))
                    .append("\n");
            builder.append(hit.getContent()).append("\n\n");
            index++;
        }

        return builder.toString();
    }

    private String fallbackDocs(String intent) {
        return switch (intent) {
            case "ORDER_DIAGNOSIS" -> "采购订单状态规则：WAIT_CONFIRM 待确认，IN_PROGRESS 执行中，PARTIAL_ARRIVAL 部分到货，COMPLETED 已完成。";
            case "WARNING_SCAN" -> "采购执行预警规则：待确认超时、到货停滞、待入库超时均应进入预警列表。";
            case "SUPPLIER_SCORE" -> "供应商评分规则：确认及时率、到货完成率、入库完成率、异常到货率共同影响评分。";
            default -> "";
        };
    }
}
