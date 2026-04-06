package com.xixi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xixi.entity.Warehouse;
import com.xixi.mapper.WarehouseMapper;
import com.xixi.pojo.dto.warehouse.WarehouseDTO;
import com.xixi.pojo.query.warehouse.WarehousePageQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.warehouse.WarehouseVO;
import com.xixi.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {
    private final WarehouseMapper warehouseMapper;

    @Override
    public IPage<WarehouseVO> getWarehousePage(WarehousePageQuery warehousePageQuery) {
        IPage<WarehouseVO> warehouseVOIPage =new Page<>(warehousePageQuery.getPageNum(), warehousePageQuery.getPageSize());
        IPage<WarehouseVO> page = warehouseMapper.getWarehousePage(warehouseVOIPage,warehousePageQuery);
        return page;
    }

    @Override
    public Result addWarehouse(WarehouseDTO warehouseDTO) {
        Warehouse warehouse = BeanUtil.copyProperties(warehouseDTO, Warehouse.class);
        if(warehouseMapper.insert(warehouse)>0){
            return Result.success("添加仓库成功！");
        }
        return Result.error("添加仓库失败！");
    }

    @Override
    public Result updateWarehouse(WarehouseDTO warehouseDTO) {
        Warehouse warehouse = BeanUtil.copyProperties(warehouseDTO, Warehouse.class);
        if(warehouseMapper.updateById(warehouse)>0){
            return Result.success("修改仓库信息成功！");
        }
        return Result.error("修改仓库信息失败！");
    }

    @Override
    public Result deleteWarehouse(List<Integer> ids) {
        if(warehouseMapper.deleteBatchIds(ids)>0){
            return Result.success("删除仓库成功！");
        }
        return Result.error("删除仓库失败!");
    }
}
