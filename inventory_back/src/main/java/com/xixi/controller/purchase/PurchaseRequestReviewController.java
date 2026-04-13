package com.xixi.controller.purchase;

import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseRequestReviewVO;
import com.xixi.service.PurchaseRequestReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/purchaseRequestReview")
@RequiredArgsConstructor
public class PurchaseRequestReviewController {
    private final PurchaseRequestReviewService purchaseRequestReviewService;
    @GetMapping("/getPurchaseRequestReviewByRequestId/{requestId}")
    public Result getPurchaseRequestReviewByRequestId(@PathVariable Long requestId) {
        List<PurchaseRequestReviewVO> list = purchaseRequestReviewService.getPurchaseRequestReviewByRequestId(requestId);
        return Result.success(list);
    }
}
