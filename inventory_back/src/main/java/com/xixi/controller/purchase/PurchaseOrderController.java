package com.xixi.controller.purchase;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.purchase.PurchaseOrderDTO;
import com.xixi.pojo.query.purchase.PurchaseOrderQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseOrderVO;
import com.xixi.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/purchaseOrder")
@RequiredArgsConstructor
public class PurchaseOrderController {
    private final PurchaseOrderService purchaseOrderService;
    @GetMapping("/getPurchaseOrderPage")
    public Result getPurchaseOrderPage(PurchaseOrderQuery purchaseOrderQuery) {
        IPage<PurchaseOrderVO> page = purchaseOrderService.getPurchaseOrderPage(purchaseOrderQuery);
        return Result.success(page);
    }
    @GetMapping("/getPurchaseOrderById/{id}")
    public Result getPurchaseOrderById(@PathVariable Long id) {
        PurchaseOrderVO purchaseOrderVO = purchaseOrderService.getPurchaseOrderById(id);
        return Result.success(purchaseOrderVO);
    }
    @PostMapping("/addPurchaseOrder")
    public Result addPurchaseOrder(@RequestBody PurchaseOrderDTO purchaseOrderDTO) {
        return purchaseOrderService.addPurchaseOrder(purchaseOrderDTO);
    }
    @PutMapping("/updatePurchaseOrder")
    public Result updatePurchaseOrder(@RequestBody PurchaseOrderDTO purchaseOrderDTO) {
        return purchaseOrderService.updatePurchaseOrder(purchaseOrderDTO);
    }
    @PutMapping("/cancelPurchaseOrder")
    public Result cancelPurchaseOrder(@RequestBody PurchaseOrderDTO purchaseOrderDTO) {
        return purchaseOrderService.cancelPurchaseOrder(purchaseOrderDTO);
    }
    @PutMapping("/closePurchaseOrder")
    public Result closePurchaseOrder(@RequestBody PurchaseOrderDTO purchaseOrderDTO) {
        return purchaseOrderService.closePurchaseOrder(purchaseOrderDTO);
    }
}
