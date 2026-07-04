package com.xixi.controller.purchase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.purchase.PurchaseOrderDTO;
import com.xixi.pojo.query.purchase.PurchaseOrderQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseOrderVO;
import com.xixi.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/purchaseOrder")
@RequiredArgsConstructor
@Tag(name = "采购订单管理", description = "采购订单管理接口")
public class PurchaseOrderController {
    private final PurchaseOrderService purchaseOrderService;
    @Operation(summary = "分页查询采购订单", operationId = "getPurchaseOrderPage")
    @GetMapping("/getPurchaseOrderPage")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASER','WAREHOUSE','SUPPLIER')")
    public Result getPurchaseOrderPage(PurchaseOrderQuery purchaseOrderQuery) {
        IPage<PurchaseOrderVO> page = purchaseOrderService.getPurchaseOrderPage(purchaseOrderQuery);
        return Result.success(page);
    }
    @Operation(summary = "供应商分页查询自己的采购订单", operationId = "getSupplierPurchaseOrderPage")
    @GetMapping("/getSupplierPurchaseOrderPage")
    @PreAuthorize("hasRole('SUPPLIER')")
    public Result getSupplierPurchaseOrderPage(PurchaseOrderQuery purchaseOrderQuery) {
        return purchaseOrderService.getSupplierPurchaseOrderPage(purchaseOrderQuery);
    }
    @Operation(summary = "查询采购订单详情", operationId = "getPurchaseOrderById")
    @GetMapping("/getPurchaseOrderById/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASER','WAREHOUSE','SUPPLIER')")
    public Result getPurchaseOrderById(@PathVariable Long id) {
        PurchaseOrderVO purchaseOrderVO = purchaseOrderService.getPurchaseOrderById(id);
        return Result.success(purchaseOrderVO);
    }
    @Operation(summary = "新增采购订单", operationId = "addPurchaseOrder")
    @PostMapping("/addPurchaseOrder")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASER')")
    public Result addPurchaseOrder(@RequestBody PurchaseOrderDTO purchaseOrderDTO) {
        return purchaseOrderService.addPurchaseOrder(purchaseOrderDTO);
    }
    @Operation(summary = "更新采购订单", operationId = "updatePurchaseOrder")
    @PutMapping("/updatePurchaseOrder")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASER')")
    public Result updatePurchaseOrder(@RequestBody PurchaseOrderDTO purchaseOrderDTO) {
        return purchaseOrderService.updatePurchaseOrder(purchaseOrderDTO);
    }
    @Operation(summary = "取消采购订单", operationId = "cancelPurchaseOrder")
    @PutMapping("/cancelPurchaseOrder")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASER')")
    public Result cancelPurchaseOrder(@RequestBody PurchaseOrderDTO purchaseOrderDTO) {
        return purchaseOrderService.cancelPurchaseOrder(purchaseOrderDTO);
    }
    @Operation(summary = "供应商确认采购订单", operationId = "confirmPurchaseOrder")
    @PutMapping("/confirmPurchaseOrder")
    @PreAuthorize("hasRole('SUPPLIER')")
    public Result confirmPurchaseOrder(@RequestBody PurchaseOrderDTO purchaseOrderDTO) {
        return purchaseOrderService.confirmPurchaseOrder(purchaseOrderDTO);
    }
    @Operation(summary = "关闭采购订单", operationId = "closePurchaseOrder")
    @PutMapping("/closePurchaseOrder")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASER')")
    public Result closePurchaseOrder(@RequestBody PurchaseOrderDTO purchaseOrderDTO) {
        return purchaseOrderService.closePurchaseOrder(purchaseOrderDTO);
    }
}

