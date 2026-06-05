package com.xixi.pojo.dto.material;

import io.swagger.v3.oas.annotations.media.Schema;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(name = "MaterialDTO", description = "MaterialDTO")
public class MaterialDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "物料主键ID")
    private Long id;

    @Schema(description = "物料编码")
    private String code;

    @Schema(description = "物料名称")
    private String name;

    @Schema(description = "规格型号")
    private String specification;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "物料分类")
    private String categoryName;

    @Schema(description = "安全库存")
    private BigDecimal safetyNumber;

    @Schema(description = "库存上限")
    private BigDecimal upperNumber;

    @Schema(description = "物料状态，ENABLED启用，DISABLED禁用")
    private String status;

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

