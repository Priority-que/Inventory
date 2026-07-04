package com.xixi.controller.purchase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseRequestReviewVO;
import com.xixi.service.PurchaseRequestReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/purchaseRequestReview")
@RequiredArgsConstructor
@Tag(name = "采购审批记录管理", description = "采购审批记录管理接口")
public class PurchaseRequestReviewController {
    private final PurchaseRequestReviewService purchaseRequestReviewService;
    @Operation(summary = "按申请查询采购审批记录", operationId = "getPurchaseRequestReviewByRequestId")
    @GetMapping("/getPurchaseRequestReviewByRequestId/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASER','PURCHASE_MANAGER')")
    public Result getPurchaseRequestReviewByRequestId(@PathVariable Long requestId) {
        List<PurchaseRequestReviewVO> list = purchaseRequestReviewService.getPurchaseRequestReviewByRequestId(requestId);
        return Result.success(list);
    }
}

