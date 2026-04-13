package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.entity.PurchaseOrder;
import com.xixi.pojo.query.purchase.PurchaseOrderQuery;
import com.xixi.pojo.vo.purchase.PurchaseOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {
    IPage<PurchaseOrderVO> getPurchaseOrderPage(IPage<PurchaseOrderVO> page,@Param("q") PurchaseOrderQuery purchaseOrderQuery);

    PurchaseOrderVO getPurchaseOrderById(Long id);

    @Select("select count(1) from purchase_order where request_id = #{requestId} and deleted = 0")
    Integer countByRequestId(Long requestId);
    @Select("select * from purchase_order where id = #{orderId}")
    PurchaseOrder getPurchaseOrderByOrderId(Long orderId);
}
