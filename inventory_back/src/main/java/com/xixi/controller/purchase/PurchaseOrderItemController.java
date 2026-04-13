package com.xixi.controller.purchase;

import com.xixi.pojo.dto.purchase.PurchaseOrderItemDTO;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseOrderItemVO;
import com.xixi.service.PurchaseOrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/purchaseOrderItem")
public class PurchaseOrderItemController {
    private final PurchaseOrderItemService purchaseOrderItemService;
    @GetMapping("/getPurchaseOrderItemByOrderId/{orderId}")
    public Result getPurchaseOrderItemByOrderId(@PathVariable Long orderId){
        List<PurchaseOrderItemVO> list = purchaseOrderItemService.getPurchaseOrderItemByOrderId(orderId);
        return Result.success(list);
    }

    @GetMapping("/getPurchaseOrderItemById/{id}")
    public Result getPurchaseOrderItemById(@PathVariable Long id){
        PurchaseOrderItemVO purchaseOrderItemVO = purchaseOrderItemService.getPurchaseOrderItemById(id);
        return Result.success(purchaseOrderItemVO);
    }

    @PutMapping("/updatePurchaseOrderItem")
    public Result updatePurchaseOrderItem(@RequestBody PurchaseOrderItemDTO purchaseOrderItemDTO){
        return purchaseOrderItemService.updatePurchaseOrderItem(purchaseOrderItemDTO);
    }

}
