package com.xixi.controller.purchase;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.purchase.PurchaseRequestDTO;
import com.xixi.pojo.query.purchase.PurchaseRequestQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseRequestPageVO;
import com.xixi.pojo.vo.purchase.PurchaseRequestVO;
import com.xixi.service.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/purchaseRequest")
public class PurchaseRequestController {
    private final PurchaseRequestService purchaseRequestService;
    @GetMapping("/getPurchaseRequestPage")
    public Result getPurchaseRequestPage(PurchaseRequestQuery purchaseRequestQuery) {
        IPage<PurchaseRequestPageVO> page = purchaseRequestService.getPurchaseRequestPage(purchaseRequestQuery);
        return Result.success(page);
    }
    @GetMapping("/getPurchaseRequestById/{id}")
    public Result getPurchaseRequestById(@PathVariable Integer id) {
        PurchaseRequestVO purchaseRequestVO = purchaseRequestService.getPurchaseRequestById(id);
        return Result.success(purchaseRequestVO);
    }
    @PostMapping("/addPurchaseRequest")
    public Result addPurchaseRequest(@RequestBody PurchaseRequestDTO purchaseRequestDTO) {
        return purchaseRequestService.addPurchaseRequest(purchaseRequestDTO);
    }
    @PutMapping("/updatePurchaseRequest")
    public Result updatePurchaseRequest(@RequestBody PurchaseRequestDTO purchaseRequestDTO) {
        return purchaseRequestService.updatePurchaseRequest(purchaseRequestDTO);
    }
    @DeleteMapping("/deletePurchaseRequest/{ids}")
    public Result deletePurchaseRequest(@PathVariable List<Integer> ids) {
        return purchaseRequestService.deletePurchaseRequest(ids);
    }
}
