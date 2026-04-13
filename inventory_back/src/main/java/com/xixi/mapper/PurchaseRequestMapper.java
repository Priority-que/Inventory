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
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PurchaseRequestMapper extends BaseMapper<PurchaseRequest> {
    IPage<PurchaseRequestPageVO> getPurchaseRequestPage(IPage<PurchaseRequestPageVO> purchaseRequestPageVoIPage, @Param("q") PurchaseRequestQuery purchaseRequestQuery);
    @Select("select * from purchase_request where id = #{id} and deleted = 0")
    PurchaseRequestVO getPurchaseRequestById(Long id);
    Long lockById(Long id);
    @Update("update purchase_request set status = #{status} where id = #{id} and deleted =0")
    int updateStatusById(@Param("id") Long id, @Param("status") String status);
    @Select("select * from purchase_request where id = #{id} and deleted =0")
    PurchaseRequest findPurchaseRequestById(Long id);
}
