package com.xixi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.warehouse.WarehouseDTO;
import com.xixi.pojo.query.warehouse.WarehousePageQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.warehouse.WarehousePageVO;
import com.xixi.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouse")
@RequiredArgsConstructor
@Tag(name = "仓库管理", description = "仓库管理接口")
public class WarehouseController {
    private final WarehouseService warehouseService;
    @Operation(summary = "分页查询仓库", operationId = "getWarehousePage")
    @GetMapping("/getWarehousePage")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public Result getWarehousePage(WarehousePageQuery warehousePageQuery) {
        IPage<WarehousePageVO> iPage = warehouseService.getWarehousePage(warehousePageQuery);
        return Result.success(iPage);
    }
    @Operation(summary = "新增仓库", operationId = "addWarehouse")
    @PostMapping("/addWarehouse")
    @PreAuthorize("hasRole('ADMIN')")
    public Result addWarehouse(@RequestBody WarehouseDTO warehouseDTO) {
        return warehouseService.addWarehouse(warehouseDTO);
    }
    @Operation(summary = "更新仓库", operationId = "updateWarehouse")
    @PutMapping("/updateWarehouse")
    @PreAuthorize("hasRole('ADMIN')")
    public Result updateWarehouse(@RequestBody WarehouseDTO warehouseDTO) {
        return warehouseService.updateWarehouse(warehouseDTO);
    }
    @Operation(summary = "批量删除仓库", operationId = "deleteWarehouse")
    @DeleteMapping("/deleteWarehouse/{ids}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result deleteWarehouse(@PathVariable List<Integer> ids) {
        return warehouseService.deleteWarehouse(ids);
    }
}

