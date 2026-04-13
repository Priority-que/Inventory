package com.xixi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.xixi.entity.PurchaseRequest;
import com.xixi.entity.PurchaseRequestItem;
import com.xixi.mapper.PurchaseRequestItemMapper;
import com.xixi.mapper.PurchaseRequestMapper;
import com.xixi.pojo.dto.purchase.PurchaseRequestItemDTO;
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
    public PurchaseRequestItemVO getPurchaseRequestItemById(Long id) {
        PurchaseRequestItemVO purchaseRequestItemVO = purchaseRequestItemMapper.getPurchaseRequestItemById(id);
        return purchaseRequestItemVO;
    }

    @Transactional
    @Override
    public Result addPurchaseRequestItem(PurchaseRequestItemDTO purchaseRequestItemDTO) {
        PurchaseRequestItem purchaseRequestItem = BeanUtil.copyProperties(purchaseRequestItemDTO, PurchaseRequestItem.class);
        if (purchaseRequestItem.getRequestId() == null) {
            return Result.error("采购明细表申请Id为空，请重新确认采购明细表");
        }
        if (purchaseRequestItem.getMaterialId() == null) {
            return Result.error("采购明细表物料Id为空，请重新确认采购明细表");
        }
        if (purchaseRequestItem.getRequestNumber() == null || purchaseRequestItem.getRequestNumber().signum() <= 0) {
            return Result.error("采购明细表申请数量必须大于0");
        }
        Long requestId = purchaseRequestItem.getRequestId();
        Long lockId = purchaseRequestMapper.lockById(requestId);
        if (lockId == null) {
            return Result.error("采购申请不存在");
        }
        PurchaseRequest purchaseRequest = purchaseRequestMapper.findPurchaseRequestById(requestId);
        if (purchaseRequest == null) {
            return Result.error("采购申请不存在");
        }
        if (!"DRAFT".equals(purchaseRequest.getStatus()) && !"REJECTED".equals(purchaseRequest.getStatus())) {
            return Result.error("当前采购申请状态不允许维护明细");
        }
        Integer maxSortNumber = purchaseRequestItemMapper.getMaxSortNumberByRequestId(requestId);
        maxSortNumber = maxSortNumber == null ? 1 : maxSortNumber + 1;
        purchaseRequestItem.setSortNumber(maxSortNumber);
        if (purchaseRequestItemMapper.insert(purchaseRequestItem) > 0) {
            return Result.success("添加采购申请明细表成功！");
        }
        return Result.error("添加采购明细表失败！");
    }

    @Transactional
    @Override
    public Result updatePurchaseRequestItem(PurchaseRequestItemDTO purchaseRequestItemDTO) {
        if (purchaseRequestItemDTO.getId() == null) {
            return Result.error("采购申请明细Id不能为空");
        }
        PurchaseRequestItem oldPurchaseRequestItem = purchaseRequestItemMapper.selectById(purchaseRequestItemDTO.getId());
        if (oldPurchaseRequestItem == null) {
            return Result.error("采购申请明细不存在");
        }
        PurchaseRequest purchaseRequest = purchaseRequestMapper.findPurchaseRequestById(oldPurchaseRequestItem.getRequestId());
        if (purchaseRequest == null) {
            return Result.error("采购申请不存在");
        }
        if (!"DRAFT".equals(purchaseRequest.getStatus()) && !"REJECTED".equals(purchaseRequest.getStatus())) {
            return Result.error("当前采购申请状态不允许维护明细");
        }
        if (purchaseRequestItemDTO.getRequestNumber() != null && purchaseRequestItemDTO.getRequestNumber().signum() <= 0) {
            return Result.error("采购申请明细申请数量必须大于0");
        }
        PurchaseRequestItem purchaseRequestItem = new PurchaseRequestItem();
        purchaseRequestItem.setId(oldPurchaseRequestItem.getId());
        purchaseRequestItem.setMaterialId(purchaseRequestItemDTO.getMaterialId());
        purchaseRequestItem.setMaterialCode(purchaseRequestItemDTO.getMaterialCode());
        purchaseRequestItem.setMaterialName(purchaseRequestItemDTO.getMaterialName());
        purchaseRequestItem.setSpecification(purchaseRequestItemDTO.getSpecification());
        purchaseRequestItem.setUnit(purchaseRequestItemDTO.getUnit());
        purchaseRequestItem.setRequestNumber(purchaseRequestItemDTO.getRequestNumber());
        purchaseRequestItem.setRemark(purchaseRequestItemDTO.getRemark());
        if (purchaseRequestItemMapper.updateById(purchaseRequestItem) > 0) {
            return Result.success("修改采购申请明细表成功！");
        }
        return Result.error("修改采购申请明细表失败！");
    }

    @Transactional
    @Override
    public Result deletePurchaseRequestItem(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("采购申请明细Id不能为空");
        }
        for (Long id : ids) {
            PurchaseRequestItem purchaseRequestItem = purchaseRequestItemMapper.selectById(id);
            if (purchaseRequestItem == null) {
                return Result.error("采购申请明细不存在");
            }
            PurchaseRequest purchaseRequest = purchaseRequestMapper.findPurchaseRequestById(purchaseRequestItem.getRequestId());
            if (purchaseRequest == null) {
                return Result.error("采购申请不存在");
            }
            if (!"DRAFT".equals(purchaseRequest.getStatus()) && !"REJECTED".equals(purchaseRequest.getStatus())) {
                return Result.error("当前采购申请状态不允许删除明细");
            }
        }
        if (purchaseRequestItemMapper.deleteBatchIds(ids) > 0) {
            return Result.success("删除采购申请明细表成功！");
        }
        return Result.error("删除采购申请明细表失败！");
    }

    @Override
    public List<PurchaseRequestItemVO> getPurchaseRequestItemByRequestId(Long id) {
        List<PurchaseRequestItemVO> list = purchaseRequestItemMapper.getPurchaseRequestItemListByRequestId(id);
        return list;
    }
}
