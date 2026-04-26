package com.xixi.agent.mapper;

import com.xixi.agent.vo.OrderSnapshotVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AgentQueryMapper {
    OrderSnapshotVO getOrderSnapshotByOrderNo(@Param("orderNo") String orderNo);
}
