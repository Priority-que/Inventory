package com.xixi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.purchase.PurchaseOrderDTO;
import com.xixi.pojo.query.purchase.PurchaseOrderQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseOrderVO;

public interface PurchaseOrderService {
    IPage<PurchaseOrderVO> getPurchaseOrderPage(PurchaseOrderQuery purchaseOrderQuery);

    PurchaseOrderVO getPurchaseOrderById(Long id);

    Result addPurchaseOrder(PurchaseOrderDTO purchaseOrderDTO);

    Result updatePurchaseOrder(PurchaseOrderDTO purchaseOrderDTO);

    Result cancelPurchaseOrder(PurchaseOrderDTO purchaseOrderDTO);

    Result closePurchaseOrder(PurchaseOrderDTO purchaseOrderDTO);
}
