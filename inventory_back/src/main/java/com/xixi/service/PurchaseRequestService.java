package com.xixi.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.purchase.PurchaseRequestDTO;
import com.xixi.pojo.query.purchase.PurchaseRequestQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseRequestPageVO;
import com.xixi.pojo.vo.purchase.PurchaseRequestVO;

import java.util.List;

public interface PurchaseRequestService {
    IPage<PurchaseRequestPageVO> getPurchaseRequestPage(PurchaseRequestQuery purchaseRequestQuery);

    PurchaseRequestVO getPurchaseRequestById(Long id);

    Result addPurchaseRequest(PurchaseRequestDTO purchaseRequestDTO);

    Result updatePurchaseRequest(PurchaseRequestDTO purchaseRequestDTO);

    Result deletePurchaseRequest(List<Integer> ids);

    Result submitPurchaseRequest(PurchaseRequestDTO purchaseRequestDTO);

    Result withdrawPurchaseRequest(PurchaseRequestDTO purchaseRequestDTO);
}
