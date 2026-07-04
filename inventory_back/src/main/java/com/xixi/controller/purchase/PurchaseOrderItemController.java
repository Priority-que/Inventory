package com.xixi.controller.purchase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.xixi.pojo.dto.purchase.PurchaseOrderItemDTO;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseOrderItemVO;
import com.xixi.service.PurchaseOrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/purchaseOrderItem")
@Tag(name = "采购订单明细管理", description = "采购订单明细管理接口")
public class PurchaseOrderItemController {
    private final PurchaseOrderItemService purchaseOrderItemService;
    @Operation(summary = "按订单查询采购订单明细", operationId = "getPurchaseOrderItemByOrderId")
    @GetMapping("/getPurchaseOrderItemByOrderId/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASER','WAREHOUSE','SUPPLIER')")
    public Result getPurchaseOrderItemByOrderId(@PathVariable Long orderId){
        List<PurchaseOrderItemVO> list = purchaseOrderItemService.getPurchaseOrderItemByOrderId(orderId);
        return Result.success(list);
    }

    @Operation(summary = "查询采购订单明细详情", operationId = "getPurchaseOrderItemById")
    @GetMapping("/getPurchaseOrderItemById/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASER','WAREHOUSE','SUPPLIER')")
    public Result getPurchaseOrderItemById(@PathVariable Long id){
        PurchaseOrderItemVO purchaseOrderItemVO = purchaseOrderItemService.getPurchaseOrderItemById(id);
        return Result.success(purchaseOrderItemVO);
    }

    @Operation(summary = "更新采购订单明细", operationId = "updatePurchaseOrderItem")
    @PutMapping("/updatePurchaseOrderItem")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASER')")
    public Result updatePurchaseOrderItem(@RequestBody PurchaseOrderItemDTO purchaseOrderItemDTO){
        return purchaseOrderItemService.updatePurchaseOrderItem(purchaseOrderItemDTO);
    }

}

