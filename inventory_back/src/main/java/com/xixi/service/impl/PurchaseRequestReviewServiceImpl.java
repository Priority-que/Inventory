package com.xixi.service.impl;

import com.xixi.entity.PurchaseRequestReview;
import com.xixi.entity.PurchaseRequest;
import com.xixi.mapper.PurchaseRequestMapper;
import com.xixi.mapper.PurchaseRequestReviewMapper;
import com.xixi.pojo.vo.purchase.PurchaseRequestReviewVO;
import com.xixi.service.PurchaseRequestReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.xixi.util.SecurityUtils.getCurrentUserId;
import static com.xixi.util.SecurityUtils.getCurrentUserRoleCodes;

@Service
@RequiredArgsConstructor
public class PurchaseRequestReviewServiceImpl implements PurchaseRequestReviewService {
    private final PurchaseRequestReviewMapper purchaseRequestReviewMapper;
    private final PurchaseRequestMapper purchaseRequestMapper;

    @Override
    public List<PurchaseRequestReviewVO> getPurchaseRequestReviewByRequestId(Long requestId) {
        PurchaseRequest purchaseRequest = purchaseRequestMapper.findPurchaseRequestById(requestId);
        if (purchaseRequest == null || !canViewPurchaseRequest(purchaseRequest)) {
            return List.of();
        }
        return purchaseRequestReviewMapper.getPurchaseRequestReviewByRequestId(requestId);
    }

    @Override
    public void saveReview(PurchaseRequestReview purchaseRequestReview){
        if (purchaseRequestReviewMapper.insert(purchaseRequestReview) <= 0) {
            throw new RuntimeException("保存采购申请审批历史失败");
        }
    }

    private boolean canViewPurchaseRequest(PurchaseRequest purchaseRequest) {
        if (isPlainPurchaser()) {
            Long currentUserId = getCurrentUserId();
            return currentUserId != null && currentUserId.equals(purchaseRequest.getApplicantId());
        }
        return true;
    }

    private boolean isPlainPurchaser() {
        List<String> roleCodes = getCurrentUserRoleCodes();
        return roleCodes != null
                && roleCodes.contains("PURCHASER")
                && !roleCodes.contains("ADMIN")
                && !roleCodes.contains("PURCHASE_MANAGER");
    }
}
