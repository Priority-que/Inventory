package com.xixi.agent.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.xixi.agent.vo.SupplierPerformanceMetricsVO;
import com.xixi.agent.vo.SupplierScoreVO;
import com.xixi.agent.workflow.state.WorkflowStateKeys;

import java.util.Map;

public class SupplierScoreRuleNode implements NodeAction {
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        SupplierPerformanceMetricsVO metrics =
                (SupplierPerformanceMetricsVO) state.value(WorkflowStateKeys.SUPPLIER_METRICS).orElse(null);
        if (metrics == null) {
            return Map.of(WorkflowStateKeys.ERROR_MESSAGE, "供应商指标为空");
        }

        SupplierScoreVO vo = new SupplierScoreVO();
        vo.setSupplierId(metrics.getSupplierId());
        vo.setSupplierName(metrics.getSupplierName());

        if (metrics.getTotalOrderCount() == null || metrics.getTotalOrderCount() == 0) {
            vo.setScore(0);
            vo.setLevel("数据不足");
            vo.setConfirmRate("0.00%");
            vo.setArrivalCompletionRate("0.00%");
            vo.setInboundCompletionRate("0.00%");
            vo.setAbnormalArrivalRate("0.00%");
            vo.setAnalysis("该供应商存在，但当前统计周期内暂无采购订单履约数据，暂无法形成有效履约评价。");
            vo.setSuggestion("建议扩大统计周期后重新分析，例如查看最近90天或180天。");
            return Map.of(WorkflowStateKeys.SUPPLIER_SCORE, vo);
        }

        double confirmRate = safeRate(metrics.getConfirmRate());
        double arrivalCompletionRate = safeRate(metrics.getArrivalCompletionRate());
        double inboundCompletionRate = safeRate(metrics.getInboundCompletionRate());
        double abnormalArrivalRate = safeRate(metrics.getAbnormalArrivalCount(), metrics.getTotalArrivalCount());

        int confirmScore = (int) Math.round(confirmRate * 20);
        int arrivalScore = (int) Math.round(arrivalCompletionRate * 30);
        int inboundScore = (int) Math.round(inboundCompletionRate * 20);
        int abnormalScore = (int) Math.round((1 - abnormalArrivalRate) * 20);
        int cancelScore = (int) Math.round((1 - safeRate(metrics.getCancelledOrderCount(), metrics.getTotalOrderCount())) * 10);

        int totalScore = confirmScore + arrivalScore + inboundScore + abnormalScore + cancelScore;
        totalScore = Math.max(0, Math.min(totalScore, 100));

        vo.setScore(totalScore);
        vo.setLevel(resolveLevel(totalScore));
        vo.setConfirmRate(formatRate(confirmRate));
        vo.setArrivalCompletionRate(formatRate(arrivalCompletionRate));
        vo.setInboundCompletionRate(formatRate(inboundCompletionRate));
        vo.setAbnormalArrivalRate(formatRate(abnormalArrivalRate));

        return Map.of(WorkflowStateKeys.SUPPLIER_SCORE, vo);
    }

    private double safeRate(Integer numerator, Integer denominator) {
        if (denominator == null || denominator == 0) {
            return 0D;
        }
        int n = numerator == null ? 0 : numerator;
        return (double) n / denominator;
    }

    private double safeRate(Double value) {
        return value == null ? 0D : value;
    }

    private String formatRate(double rate) {
        return String.format("%.2f%%", rate * 100);
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
}
