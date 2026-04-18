package com.xixi.pojo.vo.inbound;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InboundVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String inboundNo;

    private Long arrivalId;

    private String arrivalNo;

    private Long orderId;

    private String orderNo;

    private Long warehouseId;

    private String warehouseName;

    private BigDecimal inboundNumber;

    private String status;

    private Long operatorId;

    private LocalDateTime inboundTime;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<InboundItemVO> items;

    @TableLogic
    private Integer deleted;
}
