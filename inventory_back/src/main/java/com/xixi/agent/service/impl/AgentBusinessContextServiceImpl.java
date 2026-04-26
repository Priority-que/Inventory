package com.xixi.agent.service.impl;

import com.xixi.agent.mapper.AgentOrderContextMapper;
import com.xixi.agent.mapper.AgentWarningMapper;
import com.xixi.agent.mapper.SupplierPerformanceMapper;
import com.xixi.agent.service.AgentBusinessContextService;
import com.xixi.agent.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentBusinessContextServiceImpl implements AgentBusinessContextService {

    private static final String ROLE_PURCHASER = "PURCHASER";
    private static final String ROLE_WAREHOUSE = "WAREHOUSE";
    private static final String ROLE_SUPPLIER = "SUPPLIER";
    private static final String ROLE_NONE = "NONE";

    private final AgentOrderContextMapper agentOrderContextMapper;
    private final AgentWarningMapper agentWarningMapper;
    private final SupplierPerformanceMapper supplierPerformanceMapper;

    @Override
    public AgentOrderContextVO getOrderContext(String orderNo) {
        AgentOrderContextVO context = new AgentOrderContextVO();
        if (orderNo == null || orderNo.trim().isEmpty()) {
            context.setExists(false);
            return context;
        }

        AgentOrderContextRowVO row = agentOrderContextMapper.getOrderContextBaseByOrderNo(orderNo.trim());
        if (row == null) {
            context.setExists(false);
            return context;
        }

        context.setExists(true);
        context.setOrder(buildOrderInfo(row));
        fillOrderStageResponsibilityAndAction(context, row);
        fillOrderEvidence(context, row);
        return context;
    }

    @Override
    public AgentWarningContextVO scanWarningContext(Integer days) {
        int realDays = days == null ? 7 : days;
        List<AgentWarningContextVO.WarningItem> items = new ArrayList<>();

        appendWarnings(
                items,
                agentWarningMapper.getWaitConfirmOverdueOrders(realDays),
                "HIGH",
                "PURCHASE_ORDER",
                "采购订单待供应商确认超时",
                "订单长时间停留在待供应商确认状态",
                ROLE_PURCHASER
        );
        appendWarnings(
                items,
                agentWarningMapper.getInProgressWithoutArrivalOrders(realDays),
                "HIGH",
                "PURCHASE_ORDER",
                "采购订单执行中但无到货",
                "订单进入执行中后长时间没有到货记录",
                ROLE_PURCHASER
        );
        appendWarnings(
                items,
                agentWarningMapper.getPartialArrivalStuckOrders(realDays),
                "MEDIUM",
                "PURCHASE_ORDER",
                "采购订单部分到货后停滞",
                "订单处于部分到货状态且长时间没有新到货",
                ROLE_PURCHASER
        );
        appendWarnings(
                items,
                agentWarningMapper.getArrivedWithoutInboundRecords(realDays),
                "HIGH",
                "ARRIVAL",
                "到货后未生成入库单",
                "到货记录已经存在，但仍未生成入库单",
                ROLE_WAREHOUSE
        );
        appendWarnings(
                items,
                agentWarningMapper.getPendingInboundOverdueRecords(realDays),
                "MEDIUM",
                "INBOUND",
                "待确认入库单超时",
                "入库单长时间处于待入库状态",
                ROLE_WAREHOUSE
        );

        items.sort(Comparator.comparing(AgentWarningContextVO.WarningItem::getPriorityScore).reversed());

        AgentWarningContextVO context = new AgentWarningContextVO();
        context.setDays(realDays);
        context.setItems(items);
        context.setTopItems(items.stream().limit(10).collect(Collectors.toList()));
        context.setSummary(buildWarningSummary(items));
        context.setOwnerStats(buildOwnerStats(items));
        context.setRiskTypeStats(buildRiskTypeStats(items));
        return context;
    }

    @Override
    public AgentSupplierContextVO getSupplierContext(Long supplierId, Integer days) {
        int realDays = days == null ? 30 : days;
        AgentSupplierContextVO context = new AgentSupplierContextVO();
        context.setDays(realDays);

        if (supplierId == null) {
            context.setExists(false);
            context.setAnalysisSummary("supplierId 不能为空。");
            context.setSuggestion("请传入供应商ID。");
            return context;
        }

        SupplierPerformanceMetricsVO metrics = supplierPerformanceMapper.getSupplierPerformanceMetrics(supplierId, realDays);
        if (metrics == null) {
            context.setExists(false);
            context.setAnalysisSummary("供应商不存在。");
            context.setSuggestion("请确认 supplierId 是否正确。");
            return context;
        }

        context.setExists(true);
        context.setSupplier(buildSupplierInfo(metrics));
        context.setMetrics(buildSupplierMetrics(metrics));
        fillSupplierScoreAndAdvice(context, metrics);
        return context;
    }

    private AgentOrderContextVO.OrderInfo buildOrderInfo(AgentOrderContextRowVO row) {
        AgentOrderContextVO.OrderInfo order = new AgentOrderContextVO.OrderInfo();
        order.setOrderId(row.getOrderId());
        order.setOrderNo(row.getOrderNo());
        order.setStatus(row.getStatus());
        order.setStatusName(statusName(row.getStatus()));
        order.setSupplierId(row.getSupplierId());
        order.setSupplierName(row.getSupplierName());
        order.setTotalOrderNumber(safe(row.getTotalOrderNumber()));
        order.setTotalArriveNumber(safe(row.getTotalArriveNumber()));
        order.setTotalInboundNumber(safe(row.getTotalInboundNumber()));
        order.setArrivalCount(row.getArrivalCount() == null ? 0 : row.getArrivalCount());
        order.setInboundCount(row.getInboundCount() == null ? 0 : row.getInboundCount());
        return order;
    }

    private void fillOrderStageResponsibilityAndAction(AgentOrderContextVO context, AgentOrderContextRowVO row) {
        BigDecimal totalOrder = safe(row.getTotalOrderNumber());
        BigDecimal totalArrive = safe(row.getTotalArriveNumber());
        BigDecimal totalInbound = safe(row.getTotalInboundNumber());
        String status = row.getStatus();

        if ("WAIT_CONFIRM".equals(status)) {
            fillOrderContext(context, row, "供应商确认阶段", "采购订单仍处于待供应商确认状态。", ROLE_PURCHASER,
                    "当前需要采购侧跟进供应商确认订单和预计交期。", "请采购负责人联系供应商确认订单，并同步预计交期。");
            return;
        }

        if ("IN_PROGRESS".equals(status) && totalArrive.compareTo(BigDecimal.ZERO) == 0) {
            fillOrderContext(context, row, "供应商发货 / 仓库到货登记阶段", "订单已进入执行中，但目前还没有到货记录。", ROLE_PURCHASER,
                    "当前问题卡在供应商发货前段，仓库暂时没有可登记的到货对象，所以第一责任点在采购侧。", "请采购负责人先联系供应商确认发货时间，到货后再通知仓库登记到货。");
            return;
        }

        if ("PARTIAL_ARRIVAL".equals(status) && totalArrive.compareTo(totalOrder) < 0) {
            fillOrderContext(context, row, "剩余到货阶段", "订单已有部分到货，但仍有剩余采购数量未到货。", ROLE_PURCHASER,
                    "当前还缺剩余到货，核心动作仍然是采购侧推动供应商补齐发货。", "请采购负责人催促供应商补齐剩余到货。");
            return;
        }

        if ("PARTIAL_ARRIVAL".equals(status) && totalArrive.compareTo(totalOrder) >= 0 && totalInbound.compareTo(totalOrder) < 0) {
            fillOrderContext(context, row, "入库确认阶段", "订单已全部到货，但仍有部分数量未确认入库。", ROLE_WAREHOUSE,
                    "当前货物已经到达，问题转到仓库入库确认环节，所以第一责任点在仓库侧。", "请仓库侧检查待确认入库单并执行确认入库。");
            return;
        }

        if ("COMPLETED".equals(status)) {
            fillOrderContext(context, row, "流程已完成", "采购订单已完成，无阻塞。", ROLE_NONE,
                    "订单已经完成，不需要继续指定处理人。", "无需处理。");
            return;
        }

        fillOrderContext(context, row, "未知阶段", "当前状态无法根据规则判断阻塞点。", ROLE_PURCHASER,
                "系统无法自动判断责任点，建议先由采购侧人工核对订单状态和明细数据。", "请采购负责人人工检查订单状态、到货记录和入库记录。");
    }

    private void fillOrderContext(AgentOrderContextVO context, AgentOrderContextRowVO row, String currentStage,
                                  String blockReason, String ownerRole, String ownerReason, String actionText) {
        AgentOrderContextVO.StageInfo stage = new AgentOrderContextVO.StageInfo();
        stage.setCurrentStage(currentStage);
        stage.setBlockReason(blockReason);
        stage.setStageOwnerRole(ownerRole);
        stage.setStageOwnerRoleName(roleName(ownerRole));
        context.setStage(stage);

        AgentOrderContextVO.ResponsibilityInfo responsibility = new AgentOrderContextVO.ResponsibilityInfo();
        responsibility.setOwnerRole(ownerRole);
        responsibility.setOwnerRoleName(roleName(ownerRole));
        responsibility.setOwnerReason(ownerReason);
        if (ROLE_PURCHASER.equals(ownerRole) && row.getPurchaserId() != null) {
            responsibility.setOwnerUserId(row.getPurchaserId());
            responsibility.setOwnerUserName(row.getPurchaserName());
            responsibility.setOwnerDeptName(row.getPurchaserDept());
            responsibility.setOwnerPhone(row.getPurchaserPhone());
            responsibility.setOwnerSource("PURCHASE_ORDER_PURCHASER");
        } else {
            responsibility.setOwnerSource("ROLE_ONLY");
        }
        context.setResponsibility(responsibility);

        AgentOrderContextVO.NextActionInfo nextAction = new AgentOrderContextVO.NextActionInfo();
        nextAction.setActionOwnerRole(ownerRole);
        nextAction.setActionOwnerRoleName(roleName(ownerRole));
        nextAction.setActionText(actionText);
        if (ROLE_PURCHASER.equals(ownerRole) && row.getPurchaserId() != null) {
            nextAction.setActionOwnerId(row.getPurchaserId());
            nextAction.setActionOwnerName(row.getPurchaserName());
        }
        context.setNextAction(nextAction);
    }

    private void fillOrderEvidence(AgentOrderContextVO context, AgentOrderContextRowVO row) {
        context.getEvidence().add(evidence("ORDER_STATUS", "订单状态", statusName(row.getStatus()), "订单当前状态决定了流程所处节点。"));
        context.getEvidence().add(evidence("ORDER_QTY", "采购数量", safe(row.getTotalOrderNumber()).toPlainString(), "采购数量用于判断到货和入库是否完成。"));
        context.getEvidence().add(evidence("ARRIVAL_QTY", "到货数量", safe(row.getTotalArriveNumber()).toPlainString(), "如果到货数量为 0，说明仓库暂时没有可登记的到货对象。"));
        context.getEvidence().add(evidence("INBOUND_QTY", "入库数量", safe(row.getTotalInboundNumber()).toPlainString(), "入库数量用于判断是否已经完成仓库入库确认。"));
    }

    private void appendWarnings(List<AgentWarningContextVO.WarningItem> items, List<WarningSnapshotVO> snapshots,
                                String riskLevel, String bizType, String problem, String reason, String owner) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        for (WarningSnapshotVO snapshot : snapshots) {
            AgentWarningContextVO.WarningItem item = new AgentWarningContextVO.WarningItem();
            item.setRiskLevel(riskLevel);
            item.setRiskLevelName(riskLevelName(riskLevel));
            item.setBizType(bizType);
            item.setBizTypeName(bizTypeName(bizType));
            item.setBizId(snapshot.getBizId());
            item.setBizNo(snapshot.getBizNo());
            item.setStatus(snapshot.getStatus());
            item.setStatusName(statusName(snapshot.getStatus()));
            item.setProblem(problem);
            item.setReason(reason + "，已超时 " + snapshot.getOverdueDays() + " 天。");
            item.setOverdueDays(snapshot.getOverdueDays());
            item.setSuggestOwner(owner);
            item.setSuggestOwnerName(roleName(owner));
            item.setSuggestAction(buildWarningAction(snapshot.getBizNo(), owner, problem));
            item.setSupplierId(snapshot.getSupplierId());
            item.setSupplierName(snapshot.getSupplierName());
            item.setWarehouseId(snapshot.getWarehouseId());
            item.setWarehouseName(snapshot.getWarehouseName());
            item.setPriorityScore(priorityScore(riskLevel, snapshot.getOverdueDays()));
            item.setPriorityReason(buildPriorityReason(riskLevel, snapshot.getOverdueDays(), problem));
            items.add(item);
        }
    }

    private AgentWarningContextVO.WarningSummary buildWarningSummary(List<AgentWarningContextVO.WarningItem> items) {
        AgentWarningContextVO.WarningSummary summary = new AgentWarningContextVO.WarningSummary();
        int high = (int) items.stream().filter(item -> "HIGH".equals(item.getRiskLevel())).count();
        int medium = (int) items.stream().filter(item -> "MEDIUM".equals(item.getRiskLevel())).count();
        int low = (int) items.stream().filter(item -> "LOW".equals(item.getRiskLevel())).count();
        summary.setTotalCount(items.size());
        summary.setHighCount(high);
        summary.setMediumCount(medium);
        summary.setLowCount(low);
        summary.setSummaryText("本次扫描共发现 " + items.size() + " 个执行风险，其中高风险 " + high + " 个，中风险 " + medium + " 个。");
        return summary;
    }

    private List<AgentWarningContextVO.OwnerStat> buildOwnerStats(List<AgentWarningContextVO.WarningItem> items) {
        Map<String, Long> grouped = items.stream().collect(Collectors.groupingBy(AgentWarningContextVO.WarningItem::getSuggestOwner, Collectors.counting()));
        List<AgentWarningContextVO.OwnerStat> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : grouped.entrySet()) {
            AgentWarningContextVO.OwnerStat stat = new AgentWarningContextVO.OwnerStat();
            stat.setOwnerRole(entry.getKey());
            stat.setOwnerRoleName(roleName(entry.getKey()));
            stat.setCount(entry.getValue().intValue());
            result.add(stat);
        }
        return result;
    }

    private List<AgentWarningContextVO.RiskTypeStat> buildRiskTypeStats(List<AgentWarningContextVO.WarningItem> items) {
        Map<String, Long> grouped = items.stream().collect(Collectors.groupingBy(AgentWarningContextVO.WarningItem::getProblem, Collectors.counting()));
        List<AgentWarningContextVO.RiskTypeStat> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : grouped.entrySet()) {
            AgentWarningContextVO.RiskTypeStat stat = new AgentWarningContextVO.RiskTypeStat();
            stat.setProblem(entry.getKey());
            stat.setCount(entry.getValue().intValue());
            result.add(stat);
        }
        result.sort(Comparator.comparing(AgentWarningContextVO.RiskTypeStat::getCount).reversed());
        return result;
    }

    private AgentSupplierContextVO.SupplierInfo buildSupplierInfo(SupplierPerformanceMetricsVO metrics) {
        AgentSupplierContextVO.SupplierInfo supplier = new AgentSupplierContextVO.SupplierInfo();
        supplier.setSupplierId(metrics.getSupplierId());
        supplier.setSupplierName(metrics.getSupplierName());
        return supplier;
    }

    private AgentSupplierContextVO.Metrics buildSupplierMetrics(SupplierPerformanceMetricsVO metrics) {
        AgentSupplierContextVO.Metrics result = new AgentSupplierContextVO.Metrics();
        result.setTotalOrderCount(nullToZero(metrics.getTotalOrderCount()));
        result.setCompletedOrderCount(nullToZero(metrics.getCompletedOrderCount()));
        result.setCancelledOrderCount(nullToZero(metrics.getCancelledOrderCount()));
        result.setAbnormalArrivalCount(nullToZero(metrics.getAbnormalArrivalCount()));
        result.setTotalArrivalCount(nullToZero(metrics.getTotalArrivalCount()));
        result.setConfirmRate(formatRate(safeRate(metrics.getConfirmRate())));
        result.setArrivalCompletionRate(formatRate(safeRate(metrics.getArrivalCompletionRate())));
        result.setInboundCompletionRate(formatRate(safeRate(metrics.getInboundCompletionRate())));
        result.setAbnormalArrivalRate(formatRate(safeRate(metrics.getAbnormalArrivalCount(), metrics.getTotalArrivalCount())));
        return result;
    }

    private void fillSupplierScoreAndAdvice(AgentSupplierContextVO context, SupplierPerformanceMetricsVO metrics) {
        int totalOrderCount = nullToZero(metrics.getTotalOrderCount());
        if (totalOrderCount == 0) {
            AgentSupplierContextVO.Score score = new AgentSupplierContextVO.Score();
            score.setTotalScore(0);
            score.setLevel("数据不足");
            score.setLevelExplain("当前统计周期内没有采购订单履约数据，不能形成有效评分。");
            context.setScore(score);
            context.setAnalysisSummary("该供应商存在，但当前统计周期内暂无采购订单履约数据。");
            context.setSuggestion("建议扩大统计周期后重新分析，例如查看最近90天或180天。");
            return;
        }

        double confirmRate = safeRate(metrics.getConfirmRate());
        double arrivalRate = safeRate(metrics.getArrivalCompletionRate());
        double inboundRate = safeRate(metrics.getInboundCompletionRate());
        double abnormalRate = safeRate(metrics.getAbnormalArrivalCount(), metrics.getTotalArrivalCount());
        double cancelRate = safeRate(metrics.getCancelledOrderCount(), metrics.getTotalOrderCount());

        int confirmScore = (int) Math.round(confirmRate * 20);
        int arrivalScore = (int) Math.round(arrivalRate * 30);
        int inboundScore = (int) Math.round(inboundRate * 20);
        int abnormalScore = (int) Math.round((1 - abnormalRate) * 20);
        int cancelScore = (int) Math.round((1 - cancelRate) * 10);
        int totalScore = Math.max(0, Math.min(100, confirmScore + arrivalScore + inboundScore + abnormalScore + cancelScore));

        AgentSupplierContextVO.Score score = new AgentSupplierContextVO.Score();
        score.setTotalScore(totalScore);
        score.setLevel(resolveLevel(totalScore));
        score.setLevelExplain(resolveLevelExplain(totalScore));
        context.setScore(score);

        addScoreBreakdown(context, "CONFIRM_RATE", "确认及时率", 20, confirmScore, formatRate(confirmRate), "供应商是否及时确认采购订单。");
        addScoreBreakdown(context, "ARRIVAL_COMPLETION_RATE", "到货完成率", 30, arrivalScore, formatRate(arrivalRate), "供应商是否按订单完成到货。");
        addScoreBreakdown(context, "INBOUND_COMPLETION_RATE", "入库完成率", 20, inboundScore, formatRate(inboundRate), "到货后是否顺利完成入库。");
        addScoreBreakdown(context, "ABNORMAL_ARRIVAL_RATE", "异常到货率", 20, abnormalScore, formatRate(abnormalRate), "异常到货越低得分越高。");
        addScoreBreakdown(context, "CANCEL_RATE", "取消/关闭订单率", 10, cancelScore, formatRate(cancelRate), "取消或关闭订单越少得分越高。");

        fillWeakMetrics(context);
        context.setAnalysisSummary("该供应商综合评分 " + totalScore + " 分，等级为" + score.getLevel() + "。");
        context.setSuggestion(buildSupplierSuggestion(context));
    }

    private void addScoreBreakdown(AgentSupplierContextVO context, String code, String name, int maxScore, int actualScore, String value, String explain) {
        AgentSupplierContextVO.ScoreBreakdown item = new AgentSupplierContextVO.ScoreBreakdown();
        item.setMetricCode(code);
        item.setMetricName(name);
        item.setMaxScore(maxScore);
        item.setActualScore(actualScore);
        item.setValue(value);
        item.setExplain(explain);
        context.getScoreBreakdown().add(item);
    }

    private void fillWeakMetrics(AgentSupplierContextVO context) {
        for (AgentSupplierContextVO.ScoreBreakdown item : context.getScoreBreakdown()) {
            if (item.getActualScore() < Math.round(item.getMaxScore() * 0.7)) {
                AgentSupplierContextVO.WeakMetric weak = new AgentSupplierContextVO.WeakMetric();
                weak.setMetricCode(item.getMetricCode());
                weak.setMetricName(item.getMetricName());
                weak.setValue(item.getValue());
                weak.setReason(item.getMetricName() + "得分偏低，只拿到 " + item.getActualScore() + "/" + item.getMaxScore() + " 分。");
                weak.setSuggestion("建议重点跟进" + item.getMetricName() + "，先把该指标提升到稳定水平。");
                context.getWeakMetrics().add(weak);
            }
        }
    }

    private String buildSupplierSuggestion(AgentSupplierContextVO context) {
        if (context.getWeakMetrics().isEmpty()) {
            return "该供应商当前主要指标较稳定，可保持合作并持续监控。";
        }
        String names = context.getWeakMetrics().stream().map(AgentSupplierContextVO.WeakMetric::getMetricName).collect(Collectors.joining("、"));
        return "建议重点关注 " + names + "，后续合作中可以增加过程跟踪和到货节点确认。";
    }

    private AgentOrderContextVO.EvidenceItem evidence(String type, String label, String value, String explain) {
        AgentOrderContextVO.EvidenceItem item = new AgentOrderContextVO.EvidenceItem();
        item.setType(type);
        item.setLabel(label);
        item.setValue(value);
        item.setExplain(explain);
        return item;
    }

    private String buildWarningAction(String bizNo, String owner, String problem) {
        if (ROLE_WAREHOUSE.equals(owner)) {
            return "请仓库侧优先处理 " + bizNo + "，重点核对：" + problem + "。";
        }
        return "请采购侧优先处理 " + bizNo + "，重点核对：" + problem + "。";
    }

    private Integer priorityScore(String riskLevel, Integer overdueDays) {
        int riskScore = "HIGH".equals(riskLevel) ? 100 : "MEDIUM".equals(riskLevel) ? 60 : 30;
        int overdueScore = Math.min(overdueDays == null ? 0 : overdueDays, 30);
        return riskScore + overdueScore;
    }

    private String buildPriorityReason(String riskLevel, Integer overdueDays, String problem) {
        return riskLevelName(riskLevel) + "，且已超时 " + (overdueDays == null ? 0 : overdueDays) + " 天，风险类型为“" + problem + "”。";
    }

    private String roleName(String role) {
        if (ROLE_PURCHASER.equals(role)) {
            return "采购侧";
        }
        if (ROLE_WAREHOUSE.equals(role)) {
            return "仓库侧";
        }
        if (ROLE_SUPPLIER.equals(role)) {
            return "供应商";
        }
        if (ROLE_NONE.equals(role)) {
            return "无需处理";
        }
        return "待确认责任方";
    }

    private String riskLevelName(String riskLevel) {
        if ("HIGH".equals(riskLevel)) {
            return "高风险";
        }
        if ("MEDIUM".equals(riskLevel)) {
            return "中风险";
        }
        if ("LOW".equals(riskLevel)) {
            return "低风险";
        }
        return "未知风险";
    }

    private String bizTypeName(String bizType) {
        if ("PURCHASE_ORDER".equals(bizType)) {
            return "采购订单";
        }
        if ("ARRIVAL".equals(bizType)) {
            return "到货单";
        }
        if ("INBOUND".equals(bizType)) {
            return "入库单";
        }
        return bizType;
    }

    private String statusName(String status) {
        if ("WAIT_CONFIRM".equals(status)) {
            return "待供应商确认";
        }
        if ("IN_PROGRESS".equals(status)) {
            return "执行中";
        }
        if ("PARTIAL_ARRIVAL".equals(status)) {
            return "部分到货";
        }
        if ("COMPLETED".equals(status)) {
            return "已完成";
        }
        if ("PENDING".equals(status)) {
            return "待入库";
        }
        if ("CLOSED".equals(status)) {
            return "已关闭";
        }
        if ("CANCELLED".equals(status)) {
            return "已取消";
        }
        return status == null ? "未知状态" : status;
    }

    private String resolveLevel(int score) {
        if (score >= 90) {
            return "优秀";
        }
        if (score >= 75) {
            return "良好";
        }
        if (score >= 60) {
            return "一般";
        }
        return "较差";
    }

    private String resolveLevelExplain(int score) {
        if (score >= 90) {
            return "核心履约指标表现稳定，适合作为优先合作供应商。";
        }
        if (score >= 75) {
            return "整体表现较好，但仍需关注个别履约指标。";
        }
        if (score >= 60) {
            return "履约表现一般，存在需要重点跟进的短板。";
        }
        return "履约风险较高，不建议放任合作，需要加强管控。";
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeRate(Double value) {
        return value == null ? 0D : value;
    }

    private double safeRate(Integer numerator, Integer denominator) {
        if (denominator == null || denominator == 0) {
            return 0D;
        }
        return (double) nullToZero(numerator) / denominator;
    }

    private String formatRate(double rate) {
        return String.format("%.2f%%", rate * 100);
    }
}
