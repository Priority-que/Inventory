package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.entity.Warehouse;
import com.xixi.pojo.query.warehouse.WarehousePageQuery;
import com.xixi.pojo.vo.warehouse.WarehouseVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WarehouseMapper extends BaseMapper<Warehouse> {
    IPage<WarehouseVO> getWarehousePage(IPage<WarehouseVO> warehouseVOIPage,@Param("q") WarehousePageQuery warehousePageQuery);
}
