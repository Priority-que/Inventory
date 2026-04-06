package com.xixi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.entity.Warehouse;
import com.xixi.pojo.dto.warehouse.WarehouseDTO;
import com.xixi.pojo.query.warehouse.WarehousePageQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.warehouse.WarehouseVO;
import com.xixi.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouse")
@RequiredArgsConstructor
public class WarehouseController {
    private final WarehouseService warehouseService;
    @GetMapping("/getWarehousePage")
    public Result getWarehousePage(WarehousePageQuery warehousePageQuery) {
        IPage<WarehouseVO> iPage = warehouseService.getWarehousePage(warehousePageQuery);
        return Result.success(iPage);
    }
    @PostMapping("/addWarehouse")
    public Result addWarehouse(@RequestBody WarehouseDTO warehouseDTO) {
        return warehouseService.addWarehouse(warehouseDTO);
    }
    @PutMapping("/updateWarehouse")
    public Result updateWarehouse(@RequestBody WarehouseDTO warehouseDTO) {
        return warehouseService.updateWarehouse(warehouseDTO);
    }
    @DeleteMapping("/deleteWarehouse/{ids}")
    public Result deleteWarehouse(@PathVariable List<Integer> ids) {
        return warehouseService.deleteWarehouse(ids);
    }
}
