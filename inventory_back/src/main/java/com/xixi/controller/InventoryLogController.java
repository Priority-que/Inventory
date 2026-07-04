package com.xixi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.query.inventory.InventoryLogPageQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.inventory.InventoryLogPageVO;
import com.xixi.service.InventoryLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventoryLog")
@RequiredArgsConstructor
@Tag(name = "库存流水", description = "库存流水查询接口")
public class InventoryLogController {

    private final InventoryLogService inventoryLogService;

    @Operation(summary = "分页查询库存流水", operationId = "getInventoryLogPage")
    @GetMapping("/getInventoryLogPage")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','PURCHASER','PURCHASE_MANAGER')")
    public Result getInventoryLogPage(InventoryLogPageQuery query) {
        IPage<InventoryLogPageVO> page = inventoryLogService.getInventoryLogPage(query);
        return Result.success(page);
    }
}
