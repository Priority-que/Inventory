package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.entity.Warehouse;
import com.xixi.pojo.query.warehouse.WarehousePageQuery;
import com.xixi.pojo.vo.warehouse.WarehousePageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WarehouseMapper extends BaseMapper<Warehouse> {
    IPage<WarehousePageVO> getWarehousePage(IPage<WarehousePageVO> warehouseVOIPage, @Param("q") WarehousePageQuery warehousePageQuery);
}
