package com.xixi.agent.service.impl;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.xixi.agent.dto.WarningScanRequest;
import com.xixi.agent.mapper.AgentWarningMapper;
import com.xixi.agent.service.ProcurementWarningAgentService;
import com.xixi.agent.vo.WarningItemVO;
import com.xixi.agent.vo.WarningScanVO;
import com.xixi.agent.vo.WarningSnapshotVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcurementWarningAgentServiceImpl implements ProcurementWarningAgentService {
    private final AgentWarningMapper agentWarningMapper;
    @Qualifier("procurementWarningAgent")
    private final ReactAgent procurementWarningAgent;
    @Override
    public WarningScanVO scanWarnings(WarningScanRequest request) {
        Integer days = request.getDays();
        if (days == null) {
            days = 7;
        }
        List<WarningItemVO> items = new ArrayList<>();
        appendWarnings(items, agentWarningMapper.getWaitConfirmOverdueOrders(days),
                "HIGH", "PURCHASE_ORDER", "采购订单待供应商确认超时", "订单长时间停留在 WAIT_CONFIRM 状态", "SUPPLIER");
        appendWarnings(items, agentWarningMapper.getInProgressWithoutArrivalOrders(days),
                "HIGH", "PURCHASE_ORDER", "采购订单执行中但无到货", "订单进入执行中后长时间没有到货记录", "SUPPLIER");
        appendWarnings(items, agentWarningMapper.getPartialArrivalStuckOrders(days),
                "MEDIUM", "PURCHASE_ORDER", "采购订单部分到货后停滞", "订单处于 PARTIAL_ARRIVAL 且长时间没有新到货", "SUPPLIER");
        appendWarnings(items, agentWarningMapper.getArrivedWithoutInboundRecords(days),
                "HIGH", "ARRIVAL", "到货后未生成入库单", "到货记录存在，但仍未生成入库单", "WAREHOUSE");
        appendWarnings(items, agentWarningMapper.getPendingInboundOverdueRecords(days),
                "MEDIUM", "INBOUND", "待确认入库单超时", "入库单长时间处于 PENDING 状态", "WAREHOUSE");
        WarningScanVO vo = new WarningScanVO();
        vo.setItems(items);
        vo.setSummary("本次扫描共发现" + items.size() +"个执行风险");
        String threadId = request.getThreadId();
        if (threadId == null) {
            threadId = "warning-scan-"+days;
        }
        RunnableConfig config = RunnableConfig
                .builder()
                .threadId(threadId)
                .build();
        String prompt = """
                我已经通过 Java 规则扫描出采购执行风险列表，共 %s 条。
                请你基于工具 procurementWarningSnapshotTool 和下列风险列表，生成一段简洁中文总结。
                需要包含：
                1. 高风险数量
                2. 中风险数量
                3. 最优先处理的风险类型
                4. 建议先由哪个角色处理
                """.formatted(items.size());
        try {
            AssistantMessage response = procurementWarningAgent.call(prompt,config);
            vo.setAiSummary(response.getText());
        }catch (Exception e){
            vo.setAiSummary(null);
        }
        return vo;
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
