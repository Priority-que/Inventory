package com.xixi.agent.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.xixi.agent.service.ProcessDiagnosisAgentService;
import com.xixi.agent.vo.OrderDiagnosisVO;
import com.xixi.agent.vo.OrderSnapshotVO;
import com.xixi.agent.workflow.state.WorkflowStateKeys;
import lombok.RequiredArgsConstructor;

import java.util.Map;
@RequiredArgsConstructor
public class OrderRuleAnalyzeNode implements NodeAction {
    private final ProcessDiagnosisAgentService processDiagnosisAgentService;
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        OrderSnapshotVO snapshot = (OrderSnapshotVO) state.value(WorkflowStateKeys.ORDER_SNAPSHOT).orElse(null);
        if (snapshot == null) {
            return Map.of(WorkflowStateKeys.ERROR_MESSAGE, "订单快照为空");
        }
        OrderDiagnosisVO diagnosis = processDiagnosisAgentService.diagnoseRule(snapshot);
        return Map.of(WorkflowStateKeys.ORDER_DIAGNOSIS, diagnosis);
    }
}
