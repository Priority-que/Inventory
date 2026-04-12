package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.entity.PurchaseRequest;
import com.xixi.pojo.query.purchase.PurchaseRequestQuery;
import com.xixi.pojo.vo.purchase.PurchaseRequestPageVO;
import com.xixi.pojo.vo.purchase.PurchaseRequestVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PurchaseRequestMapper extends BaseMapper<PurchaseRequest> {
    IPage<PurchaseRequestPageVO> getPurchaseRequestPage(IPage<PurchaseRequestPageVO> purchaseRequestPageVoIPage, @Param("q") PurchaseRequestQuery purchaseRequestQuery);
    @Select("select * from purchase_request where id = #{id}")
    PurchaseRequestVO getPurchaseRequestById(Integer id);
}
