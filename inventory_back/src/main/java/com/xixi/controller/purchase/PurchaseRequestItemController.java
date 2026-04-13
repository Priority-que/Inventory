package com.xixi.controller.purchase;

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
public class PurchaseRequestItemController {
    private final PurchaseRequestItemService purchaseRequestItemService;
    @GetMapping("/getPurchaseRequestItemByRequestId/{id}")
    public Result getPurchaseRequestItemByRequestId(@PathVariable Long id) {
        List<PurchaseRequestItemVO> purchaseRequestItemVOList = purchaseRequestItemService.getPurchaseRequestItemByRequestId(id);
        return Result.success(purchaseRequestItemVOList);
    }
    @GetMapping("/getPurchaseRequestItemById/{id}")
    public Result getPurchaseRequestItemById(@PathVariable Long id) {
        PurchaseRequestItemVO purchaseRequestItemVO = purchaseRequestItemService.getPurchaseRequestItemById(id);
        return Result.success(purchaseRequestItemVO);
    }
    @PostMapping("/addPurchaseRequestItem")
    public Result addPurchaseRequestItem(@RequestBody PurchaseRequestItemDTO purchaseRequestItemDTO) {
        return purchaseRequestItemService.addPurchaseRequestItem(purchaseRequestItemDTO);
    }
    @PutMapping("/updatePurchaseRequestItem")
    public Result updatePurchaseRequestItem(@RequestBody PurchaseRequestItemDTO purchaseRequestItemDTO) {
        return purchaseRequestItemService.updatePurchaseRequestItem(purchaseRequestItemDTO);
    }
    @DeleteMapping("/deletePurchaseRequestItem/{ids}")
    public Result deletePurchaseRequestItem(@PathVariable List<Long> ids) {
        return purchaseRequestItemService.deletePurchaseRequestItem(ids);
    }
}
