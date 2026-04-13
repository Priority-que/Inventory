package com.xixi.controller.purchase;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.purchase.PurchaseRequestItemDTO;
import com.xixi.pojo.query.purchase.PurchaseRequestItemQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseRequestItemVO;
import com.xixi.pojo.vo.purchase.PurchaseRequestPageVO;
import com.xixi.service.PurchaseRequestItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/purchaseRequestItem")
@RequiredArgsConstructor
public class PurchaseRequestItemController {
    private final PurchaseRequestItemService purchaseRequestItemService;
    @GetMapping("/getPurchaseRequestItemPage")
    public Result getPurchaseRequestItemPage(PurchaseRequestItemQuery purchaseRequestItemQuery) {
        IPage<PurchaseRequestItemVO> page = purchaseRequestItemService.getPurchaseRequestItemPage(purchaseRequestItemQuery);
        return Result.success(page);
    }
    @GetMapping("/getPurchaseRequestItemById/{id}")
    public Result getPurchaseRequestItemById(@PathVariable Integer id) {
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
    public Result deletePurchaseRequestItem(@PathVariable List<Integer> ids) {
        return purchaseRequestItemService.deletePurchaseRequestItem(ids);
    }
}
