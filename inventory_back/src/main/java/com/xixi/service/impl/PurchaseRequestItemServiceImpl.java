package com.xixi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.xixi.annotation.OperLogRecord;
import com.xixi.entity.Material;
import com.xixi.entity.PurchaseRequest;
import com.xixi.entity.PurchaseRequestItem;
import com.xixi.mapper.MaterialMapper;
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

import static com.xixi.util.SecurityUtils.getCurrentUserId;
import static com.xixi.util.SecurityUtils.getCurrentUserRoleCodes;

@Service
@RequiredArgsConstructor
public class PurchaseRequestItemServiceImpl implements PurchaseRequestItemService {
    private final PurchaseRequestItemMapper purchaseRequestItemMapper;
    private final PurchaseRequestMapper purchaseRequestMapper;
    private final MaterialMapper materialMapper;

    @Override
    public PurchaseRequestItemVO getPurchaseRequestItemById(Long id) {
        PurchaseRequestItem purchaseRequestItem = purchaseRequestItemMapper.selectById(id);
        if (purchaseRequestItem == null) {
            return null;
        }
        PurchaseRequest purchaseRequest = purchaseRequestMapper.findPurchaseRequestById(purchaseRequestItem.getRequestId());
        if (purchaseRequest == null || !canViewPurchaseRequest(purchaseRequest)) {
            return null;
        }
        return purchaseRequestItemMapper.getPurchaseRequestItemById(id);
    }

    @Transactional
    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "采购申请",
            operationType = "CREATE",
            operationDesc = "新增采购申请明细",
            bizType = "PURCHASE_REQUEST_ITEM"
    )
    public Result addPurchaseRequestItem(PurchaseRequestItemDTO purchaseRequestItemDTO) {
        PurchaseRequestItem purchaseRequestItem = BeanUtil.copyProperties(purchaseRequestItemDTO, PurchaseRequestItem.class);
        if (purchaseRequestItem.getRequestId() == null) {
            return Result.error("采购申请明细所属申请Id不能为空！");
        }
        if (purchaseRequestItem.getMaterialId() == null) {
            return Result.error("采购申请明细物料Id不能为空！");
        }
        Material material = materialMapper.selectById(purchaseRequestItem.getMaterialId());
        if (material == null) {
            return Result.error("物料不存在！");
        }
        if (!"ENABLED".equals(material.getStatus())) {
            return Result.error("物料已禁用，不能加入采购申请！");
        }
        if (purchaseRequestItem.getRequestNumber() == null || purchaseRequestItem.getRequestNumber().signum() <= 0) {
            return Result.error("采购申请数量必须大于0！");
        }
        Long requestId = purchaseRequestItem.getRequestId();
        Long lockId = purchaseRequestMapper.lockById(requestId);
        if (lockId == null) {
            return Result.error("采购申请不存在！");
        }
        PurchaseRequest purchaseRequest = purchaseRequestMapper.findPurchaseRequestById(requestId);
        if (purchaseRequest == null) {
            return Result.error("采购申请不存在！");
        }
        if (!canManagePurchaseRequest(purchaseRequest)) {
            return Result.error("只能维护自己的采购申请明细！");
        }
        if (!"DRAFT".equals(purchaseRequest.getStatus()) && !"REJECTED".equals(purchaseRequest.getStatus())) {
            return Result.error("当前采购申请状态不允许维护明细！");
        }
        Integer maxSortNumber = purchaseRequestItemMapper.getMaxSortNumberByRequestId(requestId);
        maxSortNumber = maxSortNumber == null ? 1 : maxSortNumber + 1;
        fillMaterialSnapshot(purchaseRequestItem, material);
        purchaseRequestItem.setSortNumber(maxSortNumber);
        if (purchaseRequestItemMapper.insert(purchaseRequestItem) > 0) {
            purchaseRequestItemDTO.setId(purchaseRequestItem.getId());
            return Result.success("添加采购申请明细成功！");
        }
        return Result.error("添加采购申请明细失败！");
    }

    @Transactional
    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "采购申请",
            operationType = "UPDATE",
            operationDesc = "修改采购申请明细",
            bizType = "PURCHASE_REQUEST_ITEM"
    )
    public Result updatePurchaseRequestItem(PurchaseRequestItemDTO purchaseRequestItemDTO) {
        if (purchaseRequestItemDTO.getId() == null) {
            return Result.error("采购申请明细Id不能为空！");
        }
        PurchaseRequestItem oldPurchaseRequestItem = purchaseRequestItemMapper.selectById(purchaseRequestItemDTO.getId());
        if (oldPurchaseRequestItem == null) {
            return Result.error("采购申请明细不存在！");
        }
        PurchaseRequest purchaseRequest = purchaseRequestMapper.findPurchaseRequestById(oldPurchaseRequestItem.getRequestId());
        if (purchaseRequest == null) {
            return Result.error("采购申请不存在！");
        }
        if (!canManagePurchaseRequest(purchaseRequest)) {
            return Result.error("只能维护自己的采购申请明细！");
        }
        if (!"DRAFT".equals(purchaseRequest.getStatus()) && !"REJECTED".equals(purchaseRequest.getStatus())) {
            return Result.error("当前采购申请状态不允许维护明细！");
        }
        if (purchaseRequestItemDTO.getRequestNumber() != null && purchaseRequestItemDTO.getRequestNumber().signum() <= 0) {
            return Result.error("采购申请明细申请数量必须大于0！");
        }
        Long materialId = purchaseRequestItemDTO.getMaterialId() == null
                ? oldPurchaseRequestItem.getMaterialId()
                : purchaseRequestItemDTO.getMaterialId();
        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            return Result.error("物料不存在！");
        }
        if (!"ENABLED".equals(material.getStatus())) {
            return Result.error("物料已禁用，不能加入采购申请！");
        }
        PurchaseRequestItem purchaseRequestItem = new PurchaseRequestItem();
        purchaseRequestItem.setId(oldPurchaseRequestItem.getId());
        fillMaterialSnapshot(purchaseRequestItem, material);
        purchaseRequestItem.setRequestNumber(purchaseRequestItemDTO.getRequestNumber());
        purchaseRequestItem.setRemark(purchaseRequestItemDTO.getRemark());
        if (purchaseRequestItemMapper.updateById(purchaseRequestItem) > 0) {
            return Result.success("修改采购申请明细成功！");
        }
        return Result.error("修改采购申请明细失败！");
    }

    @Transactional
    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "采购申请",
            operationType = "DELETE",
            operationDesc = "删除采购申请明细",
            bizType = "PURCHASE_REQUEST_ITEM"
    )
    public Result deletePurchaseRequestItem(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("采购申请明细Id不能为空！");
        }
        for (Long id : ids) {
            PurchaseRequestItem purchaseRequestItem = purchaseRequestItemMapper.selectById(id);
            if (purchaseRequestItem == null) {
                return Result.error("采购申请明细不存在！");
            }
            PurchaseRequest purchaseRequest = purchaseRequestMapper.findPurchaseRequestById(purchaseRequestItem.getRequestId());
            if (purchaseRequest == null) {
                return Result.error("采购申请不存在！");
            }
            if (!canManagePurchaseRequest(purchaseRequest)) {
                return Result.error("只能删除自己的采购申请明细！");
            }
            if (!"DRAFT".equals(purchaseRequest.getStatus()) && !"REJECTED".equals(purchaseRequest.getStatus())) {
                return Result.error("当前采购申请状态不允许删除明细！");
            }
        }
        if (purchaseRequestItemMapper.deleteBatchIds(ids) > 0) {
            return Result.success("删除采购申请明细成功！");
        }
        return Result.error("删除采购申请明细失败！");
    }

    @Override
    public List<PurchaseRequestItemVO> getPurchaseRequestItemByRequestId(Long id) {
        PurchaseRequest purchaseRequest = purchaseRequestMapper.findPurchaseRequestById(id);
        if (purchaseRequest == null || !canViewPurchaseRequest(purchaseRequest)) {
            return List.of();
        }
        return purchaseRequestItemMapper.getPurchaseRequestItemListByRequestId(id);
    }

    private void fillMaterialSnapshot(PurchaseRequestItem purchaseRequestItem, Material material) {
        purchaseRequestItem.setMaterialId(material.getId());
        purchaseRequestItem.setMaterialCode(material.getCode());
        purchaseRequestItem.setMaterialName(material.getName());
        purchaseRequestItem.setSpecification(material.getSpecification());
        purchaseRequestItem.setUnit(material.getUnit());
    }

    private boolean canManagePurchaseRequest(PurchaseRequest purchaseRequest) {
        if (!isPlainPurchaser()) {
            return true;
        }
        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(purchaseRequest.getApplicantId());
    }

    private boolean canViewPurchaseRequest(PurchaseRequest purchaseRequest) {
        return canManagePurchaseRequest(purchaseRequest);
    }

    private boolean isPlainPurchaser() {
        List<String> roleCodes = getCurrentUserRoleCodes();
        return roleCodes != null
                && roleCodes.contains("PURCHASER")
                && !roleCodes.contains("ADMIN")
                && !roleCodes.contains("PURCHASE_MANAGER");
    }
}
