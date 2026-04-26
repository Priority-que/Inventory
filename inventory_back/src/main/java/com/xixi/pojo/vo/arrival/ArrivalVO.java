package com.xixi.pojo.vo.arrival;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArrivalVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String arrivalNo;

    private Long orderId;

    private String orderNo;

    private Long warehouseId;

    private String warehouseName;

    private LocalDate arrivalDate;

    private BigDecimal arrivalNumber;

    private BigDecimal qualifiedNumber;

    private BigDecimal unqualifiedNumber;

    private String status;

    private String abnormalNote;

    private Long operatorId;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<ArrivalItemVO> items;

    @TableLogic
    private Integer deleted;
}
