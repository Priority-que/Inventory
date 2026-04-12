package com.xixi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xixi.entity.PurchaseRequest;
import com.xixi.mapper.PurchaseRequestMapper;
import com.xixi.pojo.dto.purchase.PurchaseRequestDTO;
import com.xixi.pojo.query.purchase.PurchaseRequestQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseRequestPageVO;
import com.xixi.pojo.vo.purchase.PurchaseRequestVO;
import com.xixi.service.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseRequestServiceImpl implements PurchaseRequestService {
    private final PurchaseRequestMapper purchaseRequestMapper;

    @Override
    public IPage<PurchaseRequestPageVO> getPurchaseRequestPage(PurchaseRequestQuery purchaseRequestQuery) {
        IPage<PurchaseRequestPageVO> purchaseRequestPageVoIPage = new Page<>(purchaseRequestQuery.getPageNum(),purchaseRequestQuery.getPageSize());
        IPage<PurchaseRequestPageVO> page = purchaseRequestMapper.getPurchaseRequestPage(purchaseRequestPageVoIPage,purchaseRequestQuery);
        return page;
    }

    @Override
    public PurchaseRequestVO getPurchaseRequestById(Integer id) {
        PurchaseRequestVO purchaseRequestVO = purchaseRequestMapper.getPurchaseRequestById(id);
        return purchaseRequestVO;
    }

    @Override
    public Result addPurchaseRequest(PurchaseRequestDTO purchaseRequestDTO) {
        PurchaseRequest purchaseRequest = BeanUtil.copyProperties(purchaseRequestDTO,PurchaseRequest.class);
        if(purchaseRequestMapper.insert(purchaseRequest)>0){
            return Result.success("添加采购申请主表信息成功");
        }
        return Result.error("添加采购申请主表信息成功");
    }

    @Override
    public Result updatePurchaseRequest(PurchaseRequestDTO purchaseRequestDTO) {
        PurchaseRequest purchaseRequest = BeanUtil.copyProperties(purchaseRequestDTO,PurchaseRequest.class);
        if(purchaseRequestMapper.updateById(purchaseRequest)>0){
            return Result.success("修改采购申请主表信息成功");
        }
        return Result.error("修改采购申请主表信息成功");
    }

    @Override
    public Result deletePurchaseRequest(List<Integer> ids) {
        if(purchaseRequestMapper.deleteByIds(ids)>0){
            return Result.success("删除采购申请主表信息成功");
        }
        return Result.error("删除采购申请主表信息成功");
    }
}
