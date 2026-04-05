package com.xixi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("supplier_file")
public class SupplierFile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long supplierId;

    private Integer fileRound;

    private String fileType;

    private String fileName;

    private String fileUrl;

    private Long fileSize;

    private String mimeType;

    private Integer activeFlag;

    private String remark;

    private LocalDateTime uploadTime;

    @TableLogic
    private Integer deleted;
}
