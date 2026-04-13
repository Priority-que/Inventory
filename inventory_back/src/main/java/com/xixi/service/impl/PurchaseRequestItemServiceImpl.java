package com.xixi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xixi.entity.PurchaseRequestItem;
import com.xixi.mapper.PurchaseRequestItemMapper;
import com.xixi.mapper.PurchaseRequestMapper;
import com.xixi.pojo.dto.purchase.PurchaseRequestItemDTO;
import com.xixi.pojo.query.purchase.PurchaseRequestItemQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseRequestItemVO;
import com.xixi.service.PurchaseRequestItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseRequestItemServiceImpl implements PurchaseRequestItemService {
    private final PurchaseRequestItemMapper purchaseRequestItemMapper;
    private final PurchaseRequestMapper purchaseRequestMapper;
    @Override
    public IPage<PurchaseRequestItemVO> getPurchaseRequestItemPage(PurchaseRequestItemQuery purchaseRequestItemQuery) {
        IPage<PurchaseRequestItemVO> purchaseRequestPageVoIPage = new Page<>(purchaseRequestItemQuery.getPageNum(),purchaseRequestItemQuery.getPageSize());
        IPage<PurchaseRequestItemVO> page = purchaseRequestItemMapper.getPurchaseRequestItemPage(purchaseRequestPageVoIPage,purchaseRequestItemQuery);
        return page;
    }

    @Override
    public PurchaseRequestItemVO getPurchaseRequestItemById(Integer id) {
        PurchaseRequestItemVO purchaseRequestItemVO = purchaseRequestItemMapper.getPurchaseRequestItemById(id);
        return purchaseRequestItemVO;
    }
    @Transactional
    @Override
    public Result addPurchaseRequestItem(PurchaseRequestItemDTO purchaseRequestItemDTO) {
        PurchaseRequestItem purchaseRequestItem = BeanUtil.copyProperties(purchaseRequestItemDTO,PurchaseRequestItem.class);
        if(purchaseRequestItem.getRequestId()==null){
            return Result.error("采购明细表请求Id为空，请重新确认采购明细表");
        }
        Long RequestId = purchaseRequestItem.getRequestId();
        /*
        如果同时插入新的明细表，加锁锁住采购申请表，保证A先能找到最大值3，插入4，B再找到新的最大值4,B再插入5，以此类推
         */
        Long lockId =purchaseRequestMapper.lockById(RequestId);
        if(lockId == null){
            return Result.error("采购申请不存在");
        }
        Integer maxSortNumber = purchaseRequestItemMapper.getMaxSortNumberByRequestId(RequestId);
        maxSortNumber=maxSortNumber== null?1:maxSortNumber+1;
        purchaseRequestItem.setSortNumber(maxSortNumber);
        if(purchaseRequestItemMapper.insert(purchaseRequestItem)>0){
            return Result.success("添加采购申请明细表成功！");
        }
        return Result.error("添加采购明细表失败！");
    }

    @Override
    public Result updatePurchaseRequestItem(PurchaseRequestItemDTO purchaseRequestItemDTO) {
        PurchaseRequestItem purchaseRequestItem = BeanUtil.copyProperties(purchaseRequestItemDTO,PurchaseRequestItem.class);
        if(purchaseRequestItemMapper.updateById(purchaseRequestItem)>0){
            return Result.success("修改采购申请明细表成功！");
        }
        return Result.error("修改采购申请明细表失败！");
    }

    @Override
    public Result deletePurchaseRequestItem(List<Integer> ids) {
        if(purchaseRequestItemMapper.deleteBatchIds(ids)>0){
            return Result.success("删除采购申请明细表成功！");
        }
        return Result.error("删除采购申请明细表失败！");
    }
}
