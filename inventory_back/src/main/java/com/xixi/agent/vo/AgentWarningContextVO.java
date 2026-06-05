package com.xixi.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "AgentWarningContextVO", description = "AgentWarningContextVO")
public class AgentWarningContextVO {
    @Schema(description = "统计天数")
    private Integer days;
    @Schema(description = "汇总信息")
    private WarningSummary summary;
    @Schema(description = "明细列表")
    private List<WarningItem> items = new ArrayList<>();
    @Schema(description = "高优先级预警列表")
    private List<WarningItem> topItems = new ArrayList<>();
    @Schema(description = "责任人统计列表")
    private List<OwnerStat> ownerStats = new ArrayList<>();
    @Schema(description = "风险类型统计列表")
    private List<RiskTypeStat> riskTypeStats = new ArrayList<>();

    @Data
    public static class WarningSummary {
        @Schema(description = "总数量")
        private Integer totalCount;
        @Schema(description = "高风险数量")
        private Integer highCount;
        @Schema(description = "中风险数量")
        private Integer mediumCount;
        @Schema(description = "低风险数量")
        private Integer lowCount;
        @Schema(description = "汇总说明")
        private String summaryText;
    }

    @Data
    public static class WarningItem {
        @Schema(description = "风险等级编码")
        private String riskLevel;
        @Schema(description = "风险等级名称")
        private String riskLevelName;
        @Schema(description = "业务类型编码")
        private String bizType;
        @Schema(description = "业务类型名称")
        private String bizTypeName;
        @Schema(description = "业务单据ID")
        private Long bizId;
        @Schema(description = "业务单号")
        private String bizNo;
        @Schema(description = "业务状态")
        private String status;
        @Schema(description = "状态名称")
        private String statusName;
        @Schema(description = "问题描述")
        private String problem;
        @Schema(description = "原因说明")
        private String reason;
        @Schema(description = "超期天数")
        private Integer overdueDays;
        @Schema(description = "优先级分数")
        private Integer priorityScore;
        @Schema(description = "优先级原因")
        private String priorityReason;
        @Schema(description = "建议负责人编码")
        private String suggestOwner;
        @Schema(description = "建议负责人名称")
        private String suggestOwnerName;
        @Schema(description = "建议处理动作")
        private String suggestAction;
        @Schema(description = "供应商ID")
        private Long supplierId;
        @Schema(description = "供应商名称")
        private String supplierName;
        @Schema(description = "仓库ID")
        private Long warehouseId;
        @Schema(description = "仓库名称")
        private String warehouseName;
    }

    @Data
    public static class OwnerStat {
        @Schema(description = "责任角色编码")
        private String ownerRole;
        @Schema(description = "责任角色名称")
        private String ownerRoleName;
        @Schema(description = "数量")
        private Integer count;
    }

    @Data
    public static class RiskTypeStat {
        @Schema(description = "问题描述")
        private String problem;
        @Schema(description = "数量")
        private Integer count;
    }
}

