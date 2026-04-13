package com.xixi.service;

import com.xixi.pojo.dto.purchase.PurchaseRequestItemDTO;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseRequestItemVO;

import java.util.List;

public interface PurchaseRequestItemService {

    PurchaseRequestItemVO getPurchaseRequestItemById(Long id);

    Result addPurchaseRequestItem(PurchaseRequestItemDTO purchaseRequestItemDTO);

    Result updatePurchaseRequestItem(PurchaseRequestItemDTO purchaseRequestItemDTO);

    Result deletePurchaseRequestItem(List<Long> ids);

    List<PurchaseRequestItemVO> getPurchaseRequestItemByRequestId(Long id);
}
