package com.xixi.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentOrderContextVO {
    private Boolean exists;
    private OrderInfo order;
    private StageInfo stage;
    private ResponsibilityInfo responsibility;
    private List<EvidenceItem> evidence = new ArrayList<>();
    private NextActionInfo nextAction;

    @Data
    public static class OrderInfo {
        private Long orderId;
        private String orderNo;
        private String status;
        private String statusName;
        private Long supplierId;
        private String supplierName;
        private BigDecimal totalOrderNumber;
        private BigDecimal totalArriveNumber;
        private BigDecimal totalInboundNumber;
        private Integer arrivalCount;
        private Integer inboundCount;
    }

    @Data
    public static class StageInfo {
        private String currentStage;
        private String blockReason;
        private String stageOwnerRole;
        private String stageOwnerRoleName;
    }

    @Data
    public static class ResponsibilityInfo {
        private String ownerRole;
        private String ownerRoleName;
        private Long ownerUserId;
        private String ownerUserName;
        private String ownerDeptName;
        private String ownerPhone;
        private String ownerSource;
        private String ownerReason;
    }

    @Data
    public static class EvidenceItem {
        private String type;
        private String label;
        private String value;
        private String explain;
    }

    @Data
    public static class NextActionInfo {
        private String actionOwnerRole;
        private String actionOwnerRoleName;
        private Long actionOwnerId;
        private String actionOwnerName;
        private String actionText;
    }
}
