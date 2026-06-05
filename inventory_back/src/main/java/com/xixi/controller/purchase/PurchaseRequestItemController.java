package com.xixi.controller.purchase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.purchase.PurchaseRequestItemDTO;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseRequestItemVO;
import com.xixi.service.PurchaseRequestItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/purchaseRequestItem")
@RequiredArgsConstructor
@Tag(name = "采购申请明细管理", description = "采购申请明细管理接口")
public class PurchaseRequestItemController {
    private final PurchaseRequestItemService purchaseRequestItemService;
    @Operation(summary = "按申请查询采购申请明细", operationId = "getPurchaseRequestItemByRequestId")
    @GetMapping("/getPurchaseRequestItemByRequestId/{id}")
    public Result getPurchaseRequestItemByRequestId(@PathVariable Long id) {
        List<PurchaseRequestItemVO> purchaseRequestItemVOList = purchaseRequestItemService.getPurchaseRequestItemByRequestId(id);
        return Result.success(purchaseRequestItemVOList);
    }
    @Operation(summary = "查询采购申请明细详情", operationId = "getPurchaseRequestItemById")
    @GetMapping("/getPurchaseRequestItemById/{id}")
    public Result getPurchaseRequestItemById(@PathVariable Long id) {
        PurchaseRequestItemVO purchaseRequestItemVO = purchaseRequestItemService.getPurchaseRequestItemById(id);
        return Result.success(purchaseRequestItemVO);
    }
    @Operation(summary = "新增采购申请明细", operationId = "addPurchaseRequestItem")
    @PostMapping("/addPurchaseRequestItem")
    public Result addPurchaseRequestItem(@RequestBody PurchaseRequestItemDTO purchaseRequestItemDTO) {
        return purchaseRequestItemService.addPurchaseRequestItem(purchaseRequestItemDTO);
    }
    @Operation(summary = "更新采购申请明细", operationId = "updatePurchaseRequestItem")
    @PutMapping("/updatePurchaseRequestItem")
    public Result updatePurchaseRequestItem(@RequestBody PurchaseRequestItemDTO purchaseRequestItemDTO) {
        return purchaseRequestItemService.updatePurchaseRequestItem(purchaseRequestItemDTO);
    }
    @Operation(summary = "批量删除采购申请明细", operationId = "deletePurchaseRequestItem")
    @DeleteMapping("/deletePurchaseRequestItem/{ids}")
    public Result deletePurchaseRequestItem(@PathVariable List<Long> ids) {
        return purchaseRequestItemService.deletePurchaseRequestItem(ids);
    }
}

