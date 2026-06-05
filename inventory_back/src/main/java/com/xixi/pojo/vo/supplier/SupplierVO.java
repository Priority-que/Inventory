package com.xixi.pojo.vo.supplier;

import io.swagger.v3.oas.annotations.media.Schema;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "SupplierVO", description = "SupplierVO")
public class SupplierVO {
    private static final long serialVersionUID = 1L;

    @TableId(type= IdType.AUTO)
    @Schema(description = "供应商主键ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "供应商编码")
    private  String code;

    @Schema(description = "供应商名称")
    private  String name;

    @Schema(description = "联系人姓名")
    private  String contactName;

    @Schema(description = "联系人电话")
    private  String contactPhone;

    @Schema(description = "邮箱")
    private  String email;

    @Schema(description = "地址")
    private  String address;

    @Schema(description = "营业执照号")
    private  String licenseNo;

    @Schema(description = "附件轮次")
    private  Integer fileRound;

    @Schema(description = "供应商状态，PENDING待审核，REJECTED驳回，ACTIVE启用，DISABLED停用")
    private  String status;

    @Schema(description = "审批意见")
    private  String reviewNote;

    @Schema(description = "备注")
    private  String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private  LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "逻辑删除标识，0未删除，1已删除")
    private Integer deleted;
}

