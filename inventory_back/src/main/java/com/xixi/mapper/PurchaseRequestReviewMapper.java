package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xixi.entity.PurchaseRequestReview;
import com.xixi.pojo.vo.purchase.PurchaseRequestReviewVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PurchaseRequestReviewMapper extends BaseMapper<PurchaseRequestReview> {
    List<PurchaseRequestReviewVO> getPurchaseRequestReviewByRequestId(Long requestId);
}
