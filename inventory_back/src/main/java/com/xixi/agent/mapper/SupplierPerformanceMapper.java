package com.xixi.agent.mapper;

import com.xixi.agent.vo.SupplierPerformanceMetricsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SupplierPerformanceMapper {
    SupplierPerformanceMetricsVO getSupplierPerformanceMetrics(@Param("supplierId") Long supplierId, @Param("days") Integer days);
}
