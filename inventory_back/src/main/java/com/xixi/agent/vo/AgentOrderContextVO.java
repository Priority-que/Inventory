package com.xixi.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "AgentOrderContextVO", description = "AgentOrderContextVO")
public class AgentOrderContextVO {
    @Schema(description = "是否存在对应业务数据")
    private Boolean exists;
    @Schema(description = "订单信息")
    private OrderInfo order;
    @Schema(description = "阶段信息")
    private StageInfo stage;
    @Schema(description = "责任信息")
    private ResponsibilityInfo responsibility;
    @Schema(description = "判断依据列表")
    private List<EvidenceItem> evidence = new ArrayList<>();
    @Schema(description = "下一步建议动作")
    private NextActionInfo nextAction;

    @Data
    public static class OrderInfo {
        @Schema(description = "采购订单ID")
        private Long orderId;
        @Schema(description = "采购订单号")
        private String orderNo;
        @Schema(description = "业务状态")
        private String status;
        @Schema(description = "状态名称")
        private String statusName;
        @Schema(description = "供应商ID")
        private Long supplierId;
        @Schema(description = "供应商名称")
        private String supplierName;
        @Schema(description = "采购总数量")
        private BigDecimal totalOrderNumber;
        @Schema(description = "到货总数量")
        private BigDecimal totalArriveNumber;
        @Schema(description = "入库总数量")
        private BigDecimal totalInboundNumber;
        @Schema(description = "到货次数")
        private Integer arrivalCount;
        @Schema(description = "入库次数")
        private Integer inboundCount;
    }

    @Data
    public static class StageInfo {
        @Schema(description = "当前业务阶段")
        private String currentStage;
        @Schema(description = "阻塞原因")
        private String blockReason;
        @Schema(description = "当前阶段责任角色编码")
        private String stageOwnerRole;
        @Schema(description = "当前阶段责任角色名称")
        private String stageOwnerRoleName;
    }

    @Data
    public static class ResponsibilityInfo {
        @Schema(description = "责任角色编码")
        private String ownerRole;
        @Schema(description = "责任角色名称")
        private String ownerRoleName;
        @Schema(description = "责任人用户ID")
        private Long ownerUserId;
        @Schema(description = "责任人姓名")
        private String ownerUserName;
        @Schema(description = "责任人部门")
        private String ownerDeptName;
        @Schema(description = "责任人联系电话")
        private String ownerPhone;
        @Schema(description = "责任来源")
        private String ownerSource;
        @Schema(description = "责任判定原因")
        private String ownerReason;
    }

    @Data
    public static class EvidenceItem {
        @Schema(description = "类型")
        private String type;
        @Schema(description = "展示标签")
        private String label;
        @Schema(description = "值")
        private String value;
        @Schema(description = "说明")
        private String explain;
    }

    @Data
    public static class NextActionInfo {
        @Schema(description = "建议处理角色编码")
        private String actionOwnerRole;
        @Schema(description = "建议处理角色名称")
        private String actionOwnerRoleName;
        @Schema(description = "建议处理人ID")
        private Long actionOwnerId;
        @Schema(description = "建议处理人姓名")
        private String actionOwnerName;
        @Schema(description = "建议处理动作")
        private String actionText;
    }
}

