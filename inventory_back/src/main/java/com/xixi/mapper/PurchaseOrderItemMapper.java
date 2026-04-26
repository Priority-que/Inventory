package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xixi.entity.PurchaseOrderItem;
import com.xixi.pojo.vo.purchase.PurchaseOrderItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PurchaseOrderItemMapper extends BaseMapper<PurchaseOrderItem> {
    List<PurchaseOrderItemVO> getPurchaseOrderItemByOrderId(@Param("orderId") Long orderId);
    @Select("select * from purchase_order_item where id = #{id} and deleted = 0")
    PurchaseOrderItemVO getPurchaseOrderItemById(Long id);
    @Select("select * from purchase_order_item where id = #{id} and deleted = 0")
    PurchaseOrderItem findPurchaseOrderItemById(@Param("id") Long id);

    @Update("update purchase_order_item set arrived_number = arrived_number + #{arrivalNumber} where id = #{id} and deleted = 0")
    int increaseArrivedNumber(@Param("id") Long id, @Param("arrivalNumber") java.math.BigDecimal arrivalNumber);

    @Update("update purchase_order_item set inbound_number = inbound_number + #{inboundNumber} where id = #{id} and deleted = 0")
    int increaseInboundNumber(@Param("id") Long id, @Param("inboundNumber") java.math.BigDecimal inboundNumber);
}
