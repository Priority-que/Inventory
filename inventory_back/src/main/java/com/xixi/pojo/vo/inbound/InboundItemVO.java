package com.xixi.pojo.vo.inbound;

import io.swagger.v3.oas.annotations.media.Schema;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(name = "InboundItemVO", description = "InboundItemVO")
public class InboundItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "入库明细主键ID")
    private Long id;

    @Schema(description = "入库单ID")
    private Long inboundId;

    @Schema(description = "到货明细ID")
    private Long arrivalItemId;

    @Schema(description = "采购订单明细ID")
    private Long orderItemId;

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "规格型号")
    private String specification;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "入库数量")
    private BigDecimal inboundNumber;

    @Schema(description = "排序号")
    private Integer sortNumber;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "逻辑删除标识，0未删除，1已删除")
    private Integer deleted;
}

