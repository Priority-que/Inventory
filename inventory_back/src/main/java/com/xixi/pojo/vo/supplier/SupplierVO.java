package com.xixi.pojo.vo.supplier;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SupplierVO {
    private static final long serialVersionUID = 1L;

    @TableId(type= IdType.AUTO)
    private Long id;

    private Long userId;

    private  String code;

    private  String name;

    private  String contactName;

    private  String contactPhone;

    private  String email;

    private  String address;

    private  String licenseNo;

    private  Integer fileRound;

    private  String status;

    private  String reviewNote;

    private  String remark;

    private LocalDateTime createTime;

    private  LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
