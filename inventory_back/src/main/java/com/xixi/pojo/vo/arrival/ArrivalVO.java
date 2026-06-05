package com.xixi.pojo.vo.arrival;

import io.swagger.v3.oas.annotations.media.Schema;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(name = "ArrivalVO", description = "ArrivalVO")
public class ArrivalVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "到货单主键ID")
    private Long id;

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

    @Schema(description = "到货日期")
    private LocalDate arrivalDate;

    @Schema(description = "到货数量")
    private BigDecimal arrivalNumber;

    @Schema(description = "合格数量")
    private BigDecimal qualifiedNumber;

    @Schema(description = "不合格数量")
    private BigDecimal unqualifiedNumber;

    @Schema(description = "到货状态，NORMAL正常到货，ABNORMAL异常到货")
    private String status;

    @Schema(description = "异常说明")
    private String abnormalNote;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "明细列表")
    private List<ArrivalItemVO> items;

    @TableLogic
    @Schema(description = "逻辑删除标识，0未删除，1已删除")
    private Integer deleted;
}

