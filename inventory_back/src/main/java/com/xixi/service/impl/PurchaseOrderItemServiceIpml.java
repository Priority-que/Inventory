package com.xixi.service.impl;

import com.xixi.annotation.OperLogRecord;
import com.xixi.entity.PurchaseOrder;
import com.xixi.entity.PurchaseOrderItem;
import com.xixi.entity.Supplier;
import com.xixi.mapper.PurchaseOrderItemMapper;
import com.xixi.mapper.PurchaseOrderMapper;
import com.xixi.mapper.SupplierMapper;
import com.xixi.pojo.dto.purchase.PurchaseOrderItemDTO;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseOrderItemVO;
import com.xixi.service.PurchaseOrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

import static com.xixi.util.SecurityUtils.getCurrentUserId;
import static com.xixi.util.SecurityUtils.getCurrentUserRoleCodes;

@Service
@RequiredArgsConstructor
public class PurchaseOrderItemServiceIpml implements PurchaseOrderItemService {
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final SupplierMapper supplierMapper;

    @Override
    public List<PurchaseOrderItemVO> getPurchaseOrderItemByOrderId(Long orderId) {
        PurchaseOrder purchaseOrder = purchaseOrderMapper.selectById(orderId);
        if (purchaseOrder == null || !canAccessPurchaseOrder(purchaseOrder)) {
            return Collections.emptyList();
        }
        return purchaseOrderItemMapper.getPurchaseOrderItemByOrderId(orderId);
    }

    @Transactional
    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "采购订单",
            operationType = "UPDATE",
            operationDesc = "修改采购订单明细",
            bizType = "PURCHASE_ORDER_ITEM"
    )
    public Result updatePurchaseOrderItem(PurchaseOrderItemDTO purchaseOrderItemDTO) {
        if (purchaseOrderItemDTO.getId() == null) {
            return Result.error("采购订单明细表Id不能为空！");
        }
        PurchaseOrderItem purchaseOrderItem = purchaseOrderItemMapper.findPurchaseOrderItemById(purchaseOrderItemDTO.getId());
        if (purchaseOrderItem == null) {
            return Result.error("采购订单明细表不存在！");
        }
        PurchaseOrder purchaseOrder = purchaseOrderMapper.getPurchaseOrderByOrderId(purchaseOrderItem.getOrderId());
        if (purchaseOrder == null) {
            return Result.error("采购订单不存在！");
        }
        if (hasCurrentRole("SUPPLIER")) {
            return Result.error("供应商不能修改采购订单明细！");
        }
        if (!canManagePurchaserOrder(purchaseOrder.getPurchaserId())) {
            return Result.error("只能维护自己的采购订单明细！");
        }
        if (!"WAIT_CONFIRM".equals(purchaseOrder.getStatus())) {
            return Result.error("采购订单状态不是待确认！");
        }
        if (purchaseOrderItemDTO.getUnitPrice() == null
                || purchaseOrderItemDTO.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("采购订单明细单价不能小于0！");
        }
        PurchaseOrderItem updateItem = new PurchaseOrderItem();
        updateItem.setOrderId(purchaseOrderItem.getOrderId());
        updateItem.setUnitPrice(purchaseOrderItemDTO.getUnitPrice());
        BigDecimal lineAmount = purchaseOrderItem.getOrderNumber()
                .multiply(purchaseOrderItemDTO.getUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);
        updateItem.setLineAmount(lineAmount);
        updateItem.setRemark(purchaseOrderItemDTO.getRemark());
        updateItem.setId(purchaseOrderItem.getId());
        purchaseOrderItemMapper.updateById(updateItem);

        List<PurchaseOrderItemVO> list = purchaseOrderItemMapper.getPurchaseOrderItemByOrderId(purchaseOrder.getId());
        BigDecimal totalAmount = new BigDecimal(BigInteger.ZERO);
        for (PurchaseOrderItemVO purchaseOrderItemVO : list) {
            totalAmount = totalAmount.add(purchaseOrderItemVO.getLineAmount());
        }
        purchaseOrder.setTotalAmount(totalAmount);
        purchaseOrderMapper.updateById(purchaseOrder);
        return Result.success("修改采购订单明细成功！");
    }

    @Override
    public PurchaseOrderItemVO getPurchaseOrderItemById(Long id) {
        PurchaseOrderItem purchaseOrderItem = purchaseOrderItemMapper.findPurchaseOrderItemById(id);
        if (purchaseOrderItem == null) {
            return null;
        }
        PurchaseOrder purchaseOrder = purchaseOrderMapper.selectById(purchaseOrderItem.getOrderId());
        if (purchaseOrder == null || !canAccessPurchaseOrder(purchaseOrder)) {
            return null;
        }
        return purchaseOrderItemMapper.getPurchaseOrderItemById(id);
    }

    private boolean canAccessPurchaseOrder(PurchaseOrder purchaseOrder) {
        if (hasCurrentRole("SUPPLIER")) {
            return canAccessSupplierOrder(purchaseOrder.getSupplierId());
        }
        if (isPlainPurchaser()) {
            Long currentUserId = getCurrentUserId();
            return currentUserId != null && currentUserId.equals(purchaseOrder.getPurchaserId());
        }
        return true;
    }

    private boolean canManagePurchaserOrder(Long purchaserId) {
        if (isPlainPurchaser()) {
            Long currentUserId = getCurrentUserId();
            return currentUserId != null && currentUserId.equals(purchaserId);
        }
        return true;
    }

    private boolean canAccessSupplierOrder(Long supplierId) {
        if (!hasCurrentRole("SUPPLIER")) {
            return true;
        }
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }
        Supplier supplier = supplierMapper.getSupplierByUserId(currentUserId);
        return supplier != null && supplier.getId().equals(supplierId);
    }

    private boolean isPlainPurchaser() {
        List<String> roleCodes = getCurrentUserRoleCodes();
        return roleCodes != null
                && roleCodes.contains("PURCHASER")
                && !roleCodes.contains("ADMIN")
                && !roleCodes.contains("PURCHASE_MANAGER");
    }

    private boolean hasCurrentRole(String roleCode) {
        List<String> roleCodes = getCurrentUserRoleCodes();
        return roleCodes != null && roleCodes.contains(roleCode);
    }
}
