package com.xixi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.warehouse.WarehouseDTO;
import com.xixi.pojo.query.warehouse.WarehousePageQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.warehouse.WarehouseVO;

import java.util.List;

public interface WarehouseService {
    IPage<WarehouseVO> getWarehousePage(WarehousePageQuery warehousePageQuery);

    Result addWarehouse(WarehouseDTO warehouseDTO);

    Result updateWarehouse(WarehouseDTO warehouseDTO);

    Result deleteWarehouse(List<Integer> ids);
}
