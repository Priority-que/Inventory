package com.xixi.agent.service.impl;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.xixi.agent.dto.SupplierScoreRequest;
import com.xixi.agent.mapper.SupplierPerformanceMapper;
import com.xixi.agent.service.SupplierPerformanceAgentService;
import com.xixi.agent.vo.SupplierPerformanceMetricsVO;
import com.xixi.agent.vo.SupplierScoreVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SupplierPerformanceAgentServiceImpl implements SupplierPerformanceAgentService {
    private final SupplierPerformanceMapper supplierPerformanceMapper;

    @Qualifier("supplierPerformanceAgent")
    private final ReactAgent supplierPerformanceAgent;

    @Override
    public SupplierScoreVO scoreSupplier(SupplierScoreRequest request) {
        if (request.getSupplierId() == null) {
            SupplierScoreVO vo = new SupplierScoreVO();
            vo.setAnalysis("supplierId不能为空");
            vo.setSuggestion("请传入供应商ID。");
            return vo;
        }

        Integer days = request.getDays() == null ? 30 : request.getDays();
        SupplierPerformanceMetricsVO metrics =
                supplierPerformanceMapper.getSupplierPerformanceMetrics(request.getSupplierId(), days);

        if (metrics == null) {
            SupplierScoreVO vo = new SupplierScoreVO();
            vo.setSupplierId(request.getSupplierId());
            vo.setAnalysis("供应商不存在");
            vo.setSuggestion("请确认 supplierId 是否正确。");
            return vo;
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
            return vo;
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

        String threadId = request.getThreadId() == null || request.getThreadId().isBlank()
                ? "supplier-score-" + request.getSupplierId()
                : request.getThreadId();

        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        String prompt = """
                请分析供应商 %s 在最近 %s 天的履约表现。
                注意：该供应商已由后端 Java 查询确认存在，supplierId=%s，supplierName=%s。
                如果各项指标为 0，只能说明当前统计周期内履约数据不足，不能说供应商不存在。

                Java 规则已算出：
                总分：%s
                等级：%s
                确认及时率：%s
                到货完成率：%s
                入库完成率：%s
                异常到货率：%s

                请你调用工具 supplierPerformanceSnapshotTool 核对数据，并生成：
                1. 总体评价
                2. 主要优点
                3. 主要风险
                4. 合作建议

                严禁输出“供应商不存在”，除非工具明确返回 success=false 且 message=供应商不存在。
                """.formatted(
                metrics.getSupplierName(),
                days,
                metrics.getSupplierId(),
                metrics.getSupplierName(),
                totalScore,
                vo.getLevel(),
                vo.getConfirmRate(),
                vo.getArrivalCompletionRate(),
                vo.getInboundCompletionRate(),
                vo.getAbnormalArrivalRate()
        );

        try {
            AssistantMessage response = supplierPerformanceAgent.call(prompt, config);
            vo.setAnalysis(response.getText());
            vo.setSuggestion("建议结合 AI 分析结果决定后续合作策略。");
        } catch (Exception e) {
            vo.setAnalysis("已完成供应商履约评分，但 AI 分析摘要生成失败。");
            vo.setSuggestion("可先依据分数和指标进行人工判断。");
        }

        return vo;
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
