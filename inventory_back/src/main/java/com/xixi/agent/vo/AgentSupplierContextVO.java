package com.xixi.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "AgentSupplierContextVO", description = "AgentSupplierContextVO")
public class AgentSupplierContextVO {
    @Schema(description = "是否存在对应业务数据")
    private Boolean exists;
    @Schema(description = "统计天数")
    private Integer days;
    @Schema(description = "供应商信息")
    private SupplierInfo supplier;
    @Schema(description = "指标数据")
    private Metrics metrics;
    @Schema(description = "评分信息")
    private Score score;
    @Schema(description = "评分明细")
    private List<ScoreBreakdown> scoreBreakdown = new ArrayList<>();
    @Schema(description = "薄弱指标列表")
    private List<WeakMetric> weakMetrics = new ArrayList<>();
    @Schema(description = "分析摘要")
    private String analysisSummary;
    @Schema(description = "建议")
    private String suggestion;

    @Data
    public static class SupplierInfo {
        @Schema(description = "供应商ID")
        private Long supplierId;
        @Schema(description = "供应商名称")
        private String supplierName;
    }

    @Data
    public static class Metrics {
        @Schema(description = "订单总数")
        private Integer totalOrderCount;
        @Schema(description = "已完成订单数")
        private Integer completedOrderCount;
        @Schema(description = "已取消订单数")
        private Integer cancelledOrderCount;
        @Schema(description = "异常到货次数")
        private Integer abnormalArrivalCount;
        @Schema(description = "到货总次数")
        private Integer totalArrivalCount;
        @Schema(description = "确认率")
        private String confirmRate;
        @Schema(description = "到货完成率")
        private String arrivalCompletionRate;
        @Schema(description = "入库完成率")
        private String inboundCompletionRate;
        @Schema(description = "异常到货率")
        private String abnormalArrivalRate;
    }

    @Data
    public static class Score {
        @Schema(description = "总分")
        private Integer totalScore;
        @Schema(description = "等级")
        private String level;
        @Schema(description = "等级说明")
        private String levelExplain;
    }

    @Data
    public static class ScoreBreakdown {
        @Schema(description = "指标编码")
        private String metricCode;
        @Schema(description = "指标名称")
        private String metricName;
        @Schema(description = "满分")
        private Integer maxScore;
        @Schema(description = "实际得分")
        private Integer actualScore;
        @Schema(description = "值")
        private String value;
        @Schema(description = "说明")
        private String explain;
    }

    @Data
    public static class WeakMetric {
        @Schema(description = "指标编码")
        private String metricCode;
        @Schema(description = "指标名称")
        private String metricName;
        @Schema(description = "值")
        private String value;
        @Schema(description = "原因说明")
        private String reason;
        @Schema(description = "建议")
        private String suggestion;
    }
}

