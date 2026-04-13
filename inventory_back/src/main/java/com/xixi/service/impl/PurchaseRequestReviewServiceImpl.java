package com.xixi.service.impl;

import com.xixi.entity.PurchaseRequestReview;
import com.xixi.mapper.PurchaseRequestReviewMapper;
import com.xixi.pojo.vo.purchase.PurchaseRequestReviewVO;
import com.xixi.service.PurchaseRequestReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseRequestReviewServiceImpl implements PurchaseRequestReviewService {
    private final PurchaseRequestReviewMapper purchaseRequestReviewMapper;
    @Override
    public List<PurchaseRequestReviewVO> getPurchaseRequestReviewByRequestId(Long requestId) {
        List<PurchaseRequestReviewVO> list = purchaseRequestReviewMapper.getPurchaseRequestReviewByRequestId(requestId);
        return list;
    }
    @Override
    public void saveReview(PurchaseRequestReview purchaseRequestReview){
        if (purchaseRequestReviewMapper.insert(purchaseRequestReview) <= 0) {
            throw new RuntimeException("保存采购申请审批历史失败");
        }
    }
}
