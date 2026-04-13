package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.entity.PurchaseRequestItem;
import com.xixi.pojo.query.purchase.PurchaseRequestItemQuery;
import com.xixi.pojo.vo.purchase.PurchaseRequestItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PurchaseRequestItemMapper extends BaseMapper<PurchaseRequestItem> {
    IPage<PurchaseRequestItemVO> getPurchaseRequestItemPage(IPage<PurchaseRequestItemVO> purchaseRequestPageVoIPage, @Param("q") PurchaseRequestItemQuery purchaseRequestItemQuery);

    PurchaseRequestItemVO getPurchaseRequestItemById(Integer id);

    Integer getMaxSortNumberByRequestId(Long requestId);
}
