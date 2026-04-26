package com.xixi.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xixi.agent.vo.AgentOrderContextRowVO;
import com.xixi.entity.PurchaseOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AgentOrderContextMapper extends BaseMapper<PurchaseOrder> {
    AgentOrderContextRowVO getOrderContextBaseByOrderNo(@Param("orderNo") String orderNo);
}
