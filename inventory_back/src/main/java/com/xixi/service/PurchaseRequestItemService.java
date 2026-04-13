package com.xixi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.purchase.PurchaseRequestItemDTO;
import com.xixi.pojo.query.purchase.PurchaseRequestItemQuery;
import com.xixi.pojo.query.purchase.PurchaseRequestQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseRequestItemVO;
import com.xixi.pojo.vo.purchase.PurchaseRequestPageVO;

import java.util.List;

public interface PurchaseRequestItemService {
    IPage<PurchaseRequestItemVO> getPurchaseRequestItemPage(PurchaseRequestItemQuery purchaseRequestQuery);

    PurchaseRequestItemVO getPurchaseRequestItemById(Integer id);

    Result addPurchaseRequestItem(PurchaseRequestItemDTO purchaseRequestItemDTO);

    Result updatePurchaseRequestItem(PurchaseRequestItemDTO purchaseRequestItemDTO);

    Result deletePurchaseRequestItem(List<Integer> ids);
}
