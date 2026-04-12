package com.xixi.pojo.dto.supplier;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

@Data
public class SupplierDTO {
    private  static  final long serialVersionUID = 1L;

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

    private  String remark;

    @TableLogic
    private Integer deleted;
}
