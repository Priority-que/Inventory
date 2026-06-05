package com.xixi.pojo.vo.inbound;

import io.swagger.v3.oas.annotations.media.Schema;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(name = "InboundVO", description = "InboundVO")
public class InboundVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "入库单主键ID")
    private Long id;

    @Schema(description = "入库单号")
    private String inboundNo;

    @Schema(description = "到货单ID")
    private Long arrivalId;

    @Schema(description = "到货单号")
    private String arrivalNo;

    @Schema(description = "采购订单ID")
    private Long orderId;

    @Schema(description = "采购订单号")
    private String orderNo;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "入库数量")
    private BigDecimal inboundNumber;

    @Schema(description = "入库状态，PENDING待确认，COMPLETED已完成，CANCELLED已取消，ABNORMAL异常")
    private String status;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "入库时间")
    private LocalDateTime inboundTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "明细列表")
    private List<InboundItemVO> items;

    @TableLogic
    @Schema(description = "逻辑删除标识，0未删除，1已删除")
    private Integer deleted;
}

