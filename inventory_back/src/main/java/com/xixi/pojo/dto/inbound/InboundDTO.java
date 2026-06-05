package com.xixi.pojo.dto.inbound;

import io.swagger.v3.oas.annotations.media.Schema;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "InboundDTO", description = "InboundDTO")
public class InboundDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "入库单主键ID")
    private Long id;

    @Schema(description = "到货单ID")
    private Long arrivalId;

    @Schema(description = "备注")
    private String remark;
}

