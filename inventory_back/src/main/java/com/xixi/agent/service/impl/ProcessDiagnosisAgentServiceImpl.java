package com.xixi.agent.service.impl;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.xixi.agent.dto.OrderDiagnosisRequest;
import com.xixi.agent.mapper.AgentQueryMapper;
import com.xixi.agent.service.ProcessDiagnosisAgentService;
import com.xixi.agent.vo.OrderDiagnosisVO;
import com.xixi.agent.vo.OrderSnapshotVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessDiagnosisAgentServiceImpl implements ProcessDiagnosisAgentService {
    private final AgentQueryMapper agentQueryMapper;
    @Qualifier("processDiagnosisAgent")
    private final ReactAgent processDiagnosisAgent;
    public OrderDiagnosisVO diagnose(OrderDiagnosisRequest request) {
        if(request.getOrderNo() == null){
            OrderDiagnosisVO vo = new OrderDiagnosisVO();
            vo.setBlockReason("订单号不能为空");
            vo.setSuggestAction("请输入采购订单号，例如 PO20260419. ");
            return vo;
        }
        String orderNo = request.getOrderNo();
        OrderSnapshotVO snapshot = agentQueryMapper.getOrderSnapshotByOrderNo(orderNo);
        if (snapshot == null){
            OrderDiagnosisVO vo = new OrderDiagnosisVO();
            vo.setOrderNo(orderNo);
            vo.setCurrentStage("无法诊断");
            vo.setBlockReason("采购订单不存在");
            vo.setSuggestOwner("PURCHASER");
            vo.setSuggestAction("请确认订单号是否输入错误");
            return vo;
        }
        OrderDiagnosisVO ruleResult = diagnoseRule(snapshot);
        String threadId = request.getThreadId();
        if (threadId == null){
            threadId = "diagnose-"+orderNo;
        }
        RunnableConfig config = RunnableConfig
                .builder()
                .threadId(threadId)
                .build();
        String prompt = """
                请诊断采购订单 %s 的流程阻塞情况。
                Java 规则已经给出初步判断：
                当前阶段：%s
                阻塞原因：%s
                建议处理角色：%s
                建议动作：%s

                请你调用工具 getPurchaseOrderSnapshot 核对订单快照，然后基于事实生成一段简洁的中文解释。
                不要编造工具中没有返回的数据。
                """.formatted(
                orderNo,
                ruleResult.getCurrentStage(),
                ruleResult.getBlockReason(),
                ruleResult.getSuggestOwner(),
                ruleResult.getSuggestAction()
        );
        try {
            AssistantMessage response = processDiagnosisAgent.call(prompt,config);
            ruleResult.setAiSummary(response.getText());
        } catch (Exception e) {
            ruleResult.setAiSummary(null);
        }
        return  ruleResult;
    }
    public OrderDiagnosisVO diagnoseRule(OrderSnapshotVO snapshot){
        OrderDiagnosisVO vo = new OrderDiagnosisVO();
        vo.setOrderNo(snapshot.getOrderNo());
        BigDecimal totalOrder = safe(snapshot.getTotalOrderNumber());
        BigDecimal totalArrived =safe(snapshot.getTotalArriveNumber());
        BigDecimal totalInbound =safe(snapshot.getTotalInboundNumber());
        List<String> evidence = new ArrayList<>();
        evidence.add("订单状态为 "+ snapshot.getStatus());
        evidence.add("采购总数量为 "+ totalOrder);
        evidence.add("已到货数量 "+totalArrived);
        evidence.add("已入库数量 "+totalInbound);
        evidence.add("到货次数 "+snapshot.getArrivalCount());
        evidence.add("入库次数 "+snapshot.getInboundCount());
        vo.setEvidence(evidence);
        if("WAIT_CONFIRM".equals(snapshot.getStatus())){
            vo.setCurrentStage("供应商确认阶段");
            vo.setBlockReason("采购订单仍处于供应商确认状态。");
            vo.setSuggestOwner("PURCHASER");
            vo.setSuggestAction("请采购员跟进供应商确认订单并反馈预计交期。");
            return vo;
        }
        if("IN_PROGRESS".equals(snapshot.getStatus())&&totalArrived.compareTo(BigDecimal.ZERO) == 0){
            vo.setCurrentStage("供应商发货 / 仓库到货登记阶段");
            vo.setBlockReason("订单已进入执行中，但目前还没有到货记录。");
            vo.setSuggestOwner("PURCHASER");
            vo.setSuggestAction("请采购员跟进供应商发货，仓库岗收到货后登记到货。");
            return vo;
        }

        if("PARTIAL_ARRIVAL".equals(snapshot.getStatus())&&totalArrived.compareTo(totalOrder)<0){
            vo.setCurrentStage("剩余到货阶段");
            vo.setBlockReason("订单已有部分到货，但仍有剩余采购数量未到货。");
            vo.setSuggestOwner("PURCHASER");
            vo.setSuggestAction("请采购员催促供应商补齐剩余到货。");
            return vo;
        }

        if("PARTIAL_ARRIVAL".equals(snapshot.getStatus())
                && totalArrived.compareTo(totalOrder) >= 0
                && totalInbound.compareTo(totalOrder) < 0){
            vo.setCurrentStage("入库确认阶段");
            vo.setBlockReason("订单已全部到货，但仍有部分数量未确认入库。");
            vo.setSuggestOwner("WAREHOUSE");
            vo.setSuggestAction("请仓库岗检查待确认入库单并执行确认入库。");
            return vo;
        }

        if("COMPLETED".equals(snapshot.getStatus())){
            vo.setCurrentStage("流程已完成");
            vo.setBlockReason("采购订单已完成，无阻塞。");
            vo.setSuggestOwner("NONE");
            vo.setSuggestAction("无需处理。");
            return vo;
        }

        if("CLOSED".equals(snapshot.getStatus()) || "CANCELLED".equals(snapshot.getStatus())){
            vo.setCurrentStage("流程已终止");
            vo.setBlockReason("采购订单已关闭或取消。");
            vo.setSuggestOwner("PURCHASER");
            vo.setSuggestAction("如需继续采购，请重新发起采购申请或创建新订单。");
            return vo;
        }

        vo.setCurrentStage("未知阶段");
        vo.setBlockReason("当前状态无法根据规则判断阻塞点。");
        vo.setSuggestOwner("PURCHASER");
        vo.setSuggestAction("请采购员人工检查订单状态和明细数据。");
        return vo;
    }
    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

