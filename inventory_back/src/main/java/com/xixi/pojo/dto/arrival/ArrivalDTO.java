package com.xixi.pojo.dto.arrival;

import io.swagger.v3.oas.annotations.media.Schema;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(name = "ArrivalDTO", description = "ArrivalDTO")
public class ArrivalDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "到货单主键ID")
    private Long id;

    @Schema(description = "采购订单ID")
    private Long orderId;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "到货日期")
    private LocalDate arrivalDate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "明细列表")
    private List<ArrivalItemDTO> items;

    @TableLogic
    @Schema(description = "逻辑删除标识，0未删除，1已删除")
    private Integer deleted;
}

