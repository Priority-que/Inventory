package com.xixi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.query.inventory.InventoryPageQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.inventory.InventoryPageVO;
import com.xixi.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory APIs")
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "Get inventory page", operationId = "getInventoryPage")
    @GetMapping("/getInventoryPage")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','PURCHASER','PURCHASE_MANAGER')")
    public Result getInventoryPage(InventoryPageQuery inventoryPageQuery) {
        IPage<InventoryPageVO> page = inventoryService.getInventoryPage(inventoryPageQuery);
        return Result.success(page);
    }
}