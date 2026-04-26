package com.xixi.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentWarningContextVO {
    private Integer days;
    private WarningSummary summary;
    private List<WarningItem> items = new ArrayList<>();
    private List<WarningItem> topItems = new ArrayList<>();
    private List<OwnerStat> ownerStats = new ArrayList<>();
    private List<RiskTypeStat> riskTypeStats = new ArrayList<>();

    @Data
    public static class WarningSummary {
        private Integer totalCount;
        private Integer highCount;
        private Integer mediumCount;
        private Integer lowCount;
        private String summaryText;
    }

    @Data
    public static class WarningItem {
        private String riskLevel;
        private String riskLevelName;
        private String bizType;
        private String bizTypeName;
        private Long bizId;
        private String bizNo;
        private String status;
        private String statusName;
        private String problem;
        private String reason;
        private Integer overdueDays;
        private Integer priorityScore;
        private String priorityReason;
        private String suggestOwner;
        private String suggestOwnerName;
        private String suggestAction;
        private Long supplierId;
        private String supplierName;
        private Long warehouseId;
        private String warehouseName;
    }

    @Data
    public static class OwnerStat {
        private String ownerRole;
        private String ownerRoleName;
        private Integer count;
    }

    @Data
    public static class RiskTypeStat {
        private String problem;
        private Integer count;
    }
}
