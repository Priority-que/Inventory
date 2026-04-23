package com.xixi.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentSupplierContextVO {
    private Boolean exists;
    private Integer days;
    private SupplierInfo supplier;
    private Metrics metrics;
    private Score score;
    private List<ScoreBreakdown> scoreBreakdown = new ArrayList<>();
    private List<WeakMetric> weakMetrics = new ArrayList<>();
    private String analysisSummary;
    private String suggestion;

    @Data
    public static class SupplierInfo {
        private Long supplierId;
        private String supplierName;
    }

    @Data
    public static class Metrics {
        private Integer totalOrderCount;
        private Integer completedOrderCount;
        private Integer cancelledOrderCount;
        private Integer abnormalArrivalCount;
        private Integer totalArrivalCount;
        private String confirmRate;
        private String arrivalCompletionRate;
        private String inboundCompletionRate;
        private String abnormalArrivalRate;
    }

    @Data
    public static class Score {
        private Integer totalScore;
        private String level;
        private String levelExplain;
    }

    @Data
    public static class ScoreBreakdown {
        private String metricCode;
        private String metricName;
        private Integer maxScore;
        private Integer actualScore;
        private String value;
        private String explain;
    }

    @Data
    public static class WeakMetric {
        private String metricCode;
        private String metricName;
        private String value;
        private String reason;
        private String suggestion;
    }
}
