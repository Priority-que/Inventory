package com.xixi.service;


import com.xixi.pojo.dto.purchase.PurchaseOrderItemDTO;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseOrderItemVO;

import java.util.List;

public interface PurchaseOrderItemService {
    List<PurchaseOrderItemVO> getPurchaseOrderItemByOrderId(Long orderId);

    Result updatePurchaseOrderItem(PurchaseOrderItemDTO purchaseOrderItemDTO);

    PurchaseOrderItemVO getPurchaseOrderItemById(Long id);
}
