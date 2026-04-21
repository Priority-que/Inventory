package com.xixi.agent.mapper;

import com.xixi.agent.vo.WarningSnapshotVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentWarningMapper {
    List<WarningSnapshotVO> getWaitConfirmOverdueOrders(@Param("days") Integer days);
    List<WarningSnapshotVO> getInProgressWithoutArrivalOrders(@Param("days") Integer days);
    List<WarningSnapshotVO> getPartialArrivalStuckOrders(@Param("days") Integer days);
    List<WarningSnapshotVO> getArrivedWithoutInboundRecords(@Param("days") Integer days);
    List<WarningSnapshotVO> getPendingInboundOverdueRecords(@Param("days") Integer days);

}
