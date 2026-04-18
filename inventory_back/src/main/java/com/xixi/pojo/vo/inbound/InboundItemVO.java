package com.xixi.pojo.vo.inbound;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InboundItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long inboundId;

    private Long arrivalItemId;

    private Long orderItemId;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private String specification;

    private String unit;

    private BigDecimal inboundNumber;

    private Integer sortNumber;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
