package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.entity.PurchaseRequestItem;
import com.xixi.pojo.query.purchase.PurchaseRequestItemQuery;
import com.xixi.pojo.vo.purchase.PurchaseRequestItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PurchaseRequestItemMapper extends BaseMapper<PurchaseRequestItem> {

    PurchaseRequestItemVO getPurchaseRequestItemById(Long id);
    List<PurchaseRequestItemVO> getPurchaseRequestItemListByRequestId(Long requestId);
    Integer getMaxSortNumberByRequestId(Long requestId);
    @Select("select * from purchase_request_item where request_id = #{requestId} and deleted =0 order by sort_number asc,id asc")
    List<PurchaseRequestItem> getPurchaseRequestItemByRequestId(Long requestId);
}
