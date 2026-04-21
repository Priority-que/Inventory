package com.xixi.agent.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.xixi.agent.vo.OrderDiagnosisVO;
import com.xixi.agent.vo.SupplierScoreVO;
import com.xixi.agent.vo.WarningScanVO;
import com.xixi.agent.vo.WorkflowAgentResponse;
import com.xixi.agent.workflow.state.WorkflowStateKeys;

import java.util.Map;

public class BuildFinalResponseNode implements NodeAction{
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        WorkflowAgentResponse response = new WorkflowAgentResponse();
        String intent = state.value(WorkflowStateKeys.INTENT, "UNKNOWN").toString();
        response.setIntent(intent);
        response.setAnswer(state.value(WorkflowStateKeys.LLM_ANSWER, "").toString());

        if ("ORDER_DIAGNOSIS".equals(intent)) {
            OrderDiagnosisVO diagnosis = (OrderDiagnosisVO) state.value(WorkflowStateKeys.ORDER_DIAGNOSIS).orElse(null);
            if (diagnosis != null) {
                response.setData(diagnosis);
            }
        } else if ("WARNING_SCAN".equals(intent)) {
            WarningScanVO warningScanVO = (WarningScanVO) state.value(WorkflowStateKeys.WARNING_ANALYSIS).orElse(null);
            if (warningScanVO != null) {
                response.setData(warningScanVO);
            }
        } else if ("SUPPLIER_SCORE".equals(intent)) {
            SupplierScoreVO supplierScoreVO = (SupplierScoreVO) state.value(WorkflowStateKeys.SUPPLIER_SCORE).orElse(null);
            if (supplierScoreVO != null) {
                response.setData(supplierScoreVO);
            }
        }
        return Map.of(WorkflowStateKeys.FINAL_RESPONSE, response);
    }
}
