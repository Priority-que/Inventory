package com.xixi.agent.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.xixi.agent.vo.WarningItemVO;
import com.xixi.agent.vo.WarningScanVO;
import com.xixi.agent.vo.WarningSnapshotVO;
import com.xixi.agent.workflow.state.WorkflowStateKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class WarningRuleAnalyzeNode implements NodeAction {
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Map<String, List<WarningSnapshotVO>> context =
                (Map<String, List<WarningSnapshotVO>>) state.value(WorkflowStateKeys.WARNING_CONTEXT).orElse(null);
        if (context == null) {
            return Map.of(WorkflowStateKeys.ERROR_MESSAGE, "预警上下文为空");
        }

        List<WarningItemVO> items = new ArrayList<>();

        appendWarnings(items, context.get("waitConfirmOverdue"),
                "HIGH", "PURCHASE_ORDER", "采购订单待供应商确认超时", "订单长时间停留在 WAIT_CONFIRM 状态", "PURCHASER");
        appendWarnings(items, context.get("inProgressWithoutArrival"),
                "HIGH", "PURCHASE_ORDER", "采购订单执行中但无到货", "订单进入执行中后长时间没有到货记录", "PURCHASER");
        appendWarnings(items, context.get("partialArrivalStuck"),
                "MEDIUM", "PURCHASE_ORDER", "采购订单部分到货后停滞", "订单处于 PARTIAL_ARRIVAL 且长时间没有新到货", "PURCHASER");
        appendWarnings(items, context.get("arrivedWithoutInbound"),
                "HIGH", "ARRIVAL", "到货后未生成入库单", "到货记录存在，但仍未生成入库单", "WAREHOUSE");
        appendWarnings(items, context.get("pendingInboundOverdue"),
                "MEDIUM", "INBOUND", "待确认入库单超时", "入库单长时间处于 PENDING 状态", "WAREHOUSE");

        WarningScanVO vo = new WarningScanVO();
        vo.setItems(items);
        vo.setSummary("本次扫描共发现 " + items.size() + " 个执行风险。");

        return Map.of(WorkflowStateKeys.WARNING_ANALYSIS, vo);
    }
    private void appendWarnings(List<WarningItemVO> items,
                                List<WarningSnapshotVO> snapshots,
                                String riskLevel,
                                String bizType,
                                String problem,
                                String reason,
                                String owner) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        for (WarningSnapshotVO snapshot : snapshots) {
            WarningItemVO item = new WarningItemVO();
            item.setRiskLevel(riskLevel);
            item.setBizType(bizType);
            item.setBizId(snapshot.getBizId());
            item.setBizNo(snapshot.getBizNo());
            item.setProblem(problem);
            item.setReason(reason + "，已超时 " + snapshot.getOverdueDays() + " 天");
            item.setSuggestOwner(owner);
            item.setSuggestAction("请优先处理 " + snapshot.getBizNo());
            items.add(item);
        }
    }
}
