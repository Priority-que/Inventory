package com.xixi.service.impl;

import com.xixi.annotation.OperLogRecord;
import com.xixi.entity.PurchaseOrder;
import com.xixi.entity.PurchaseOrderItem;
import com.xixi.mapper.PurchaseOrderItemMapper;
import com.xixi.mapper.PurchaseOrderMapper;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseOrderItemServiceIpml implements PurchaseOrderItemService {
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;

    @Override
    public List<PurchaseOrderItemVO> getPurchaseOrderItemByOrderId(Long orderId) {
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
        return purchaseOrderItemMapper.getPurchaseOrderItemById(id);
    }
}
