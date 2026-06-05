package com.xixi.controller.purchase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "采购申请管理", description = "采购申请管理接口")
public class PurchaseRequestController {
    private final PurchaseRequestService purchaseRequestService;
    @Operation(summary = "分页查询采购申请", operationId = "getPurchaseRequestPage")
    @GetMapping("/getPurchaseRequestPage")
    public Result getPurchaseRequestPage(PurchaseRequestQuery purchaseRequestQuery) {
        IPage<PurchaseRequestPageVO> page = purchaseRequestService.getPurchaseRequestPage(purchaseRequestQuery);
        return Result.success(page);
    }
    @Operation(summary = "分页查询我的已通过采购申请", operationId = "getMyApprovedPurchaseRequestPage")
    @GetMapping("/getMyApprovedPurchaseRequestPage")
    public Result getMyApprovedPurchaseRequestPage(PurchaseRequestQuery purchaseRequestQuery) {
        IPage<PurchaseRequestPageVO> page = purchaseRequestService.getMyApprovedPurchaseRequestPage(purchaseRequestQuery);
        return Result.success(page);
    }
    @Operation(summary = "查询采购申请详情", operationId = "getPurchaseRequestById")
    @GetMapping("/getPurchaseRequestById/{id}")
    public Result getPurchaseRequestById(@PathVariable Long id) {
        PurchaseRequestVO purchaseRequestVO = purchaseRequestService.getPurchaseRequestById(id);
        return Result.success(purchaseRequestVO);
    }
    @Operation(summary = "新增采购申请", operationId = "addPurchaseRequest")
    @PostMapping("/addPurchaseRequest")
    public Result addPurchaseRequest(@RequestBody PurchaseRequestDTO purchaseRequestDTO) {
        return purchaseRequestService.addPurchaseRequest(purchaseRequestDTO);
    }
    @Operation(summary = "更新采购申请", operationId = "updatePurchaseRequest")
    @PutMapping("/updatePurchaseRequest")
    public Result updatePurchaseRequest(@RequestBody PurchaseRequestDTO purchaseRequestDTO) {
        return purchaseRequestService.updatePurchaseRequest(purchaseRequestDTO);
    }
    @Operation(summary = "提交采购申请", operationId = "submitPurchaseRequest")
    @PutMapping("/submitPurchaseRequest")
    public Result submitPurchaseRequest(@RequestBody PurchaseRequestDTO purchaseRequestDTO){
        return purchaseRequestService.submitPurchaseRequest(purchaseRequestDTO);
    }
    @Operation(summary = "撤回采购申请", operationId = "withdrawPurchaseRequest")
    @PutMapping("/withdrawPurchaseRequest")
    public Result withdrawPurchaseRequest(@RequestBody PurchaseRequestDTO purchaseRequestDTO){
        return purchaseRequestService.withdrawPurchaseRequest(purchaseRequestDTO);
    }
    @Operation(summary = "审批通过采购申请", operationId = "approvePurchaseRequest")
    @PutMapping("/approvePurchaseRequest")
    public Result approvePurchaseRequest(@RequestBody PurchaseRequestDTO purchaseRequestDTO){
        return purchaseRequestService.approvePurchaseRequest(purchaseRequestDTO);
    }
    @Operation(summary = "驳回采购申请", operationId = "rejectPurchaseRequest")
    @PutMapping("/rejectPurchaseRequest")
    public Result rejectPurchaseRequest(@RequestBody PurchaseRequestDTO purchaseRequestDTO){
        return purchaseRequestService.rejectPurchaseRequest(purchaseRequestDTO);
    }
    @Operation(summary = "批量删除采购申请", operationId = "deletePurchaseRequest")
    @DeleteMapping("/deletePurchaseRequest/{ids}")
    public Result deletePurchaseRequest(@PathVariable List<Integer> ids) {
        return purchaseRequestService.deletePurchaseRequest(ids);
    }
}

