package com.xixi.service;

import com.xixi.entity.PurchaseRequestReview;
import com.xixi.pojo.vo.purchase.PurchaseRequestReviewVO;

import java.util.List;

public interface PurchaseRequestReviewService {
    List<PurchaseRequestReviewVO> getPurchaseRequestReviewByRequestId(Long requestId);
    void saveReview(PurchaseRequestReview purchaseRequestReview);
}
