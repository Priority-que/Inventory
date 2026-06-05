package com.xixi.pojo.dto.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(name = "WarehouseDTO", description = "WarehouseDTO")
public class WarehouseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "仓库主键ID")
    private Long id;

    @Schema(description = "仓库编码")
    private String code;

    @Schema(description = "仓库名称")
    private String name;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "仓库负责人")
    private String managerName;

    @Schema(description = "仓库负责人电话")
    private String managerPhone;

    @Schema(description = "仓库状态，ENABLED启用，DISABLED禁用")
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

