package com.xixi.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xixi.entity.Arrival;
import com.xixi.entity.Inbound;
import com.xixi.entity.InboundItem;
import com.xixi.entity.Inventory;
import com.xixi.entity.InventoryLog;
import com.xixi.mapper.ArrivalItemMapper;
import com.xixi.mapper.ArrivalMapper;
import com.xixi.mapper.InboundItemMapper;
import com.xixi.mapper.InboundMapper;
import com.xixi.mapper.InventoryLogMapper;
import com.xixi.mapper.InventoryMapper;
import com.xixi.mapper.PurchaseOrderItemMapper;
import com.xixi.mapper.PurchaseOrderMapper;
import com.xixi.pojo.dto.inbound.InboundDTO;
import com.xixi.pojo.query.inbound.InboundQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.arrival.ArrivalItemVO;
import com.xixi.pojo.vo.inbound.InboundItemVO;
import com.xixi.pojo.vo.inbound.InboundVO;
import com.xixi.pojo.vo.purchase.PurchaseOrderItemVO;
import com.xixi.service.InboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.xixi.util.SecurityUtils.getCurrentUserId;
import static com.xixi.util.SecurityUtils.getCurrentUsername;

@Service
@RequiredArgsConstructor
public class InboundServiceImpl implements InboundService {
    private final InboundMapper inboundMapper;
    private final InboundItemMapper inboundItemMapper;
    private final ArrivalMapper arrivalMapper;
    private final ArrivalItemMapper arrivalItemMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryLogMapper inventoryLogMapper;

    @Override
    public IPage<InboundVO> getInboundPage(InboundQuery inboundQuery) {
        IPage<InboundVO> page = new Page<>(inboundQuery.getPageNum(), inboundQuery.getPageSize());
        return inboundMapper.getInboundPage(page, inboundQuery);
    }

    @Override
    public InboundVO getInboundById(Long id) {
        InboundVO inboundVO = inboundMapper.getInboundById(id);
        if (inboundVO != null) {
            inboundVO.setItems(inboundItemMapper.getInboundItemByInboundId(id));
        }
        return inboundVO;
    }

    @Transactional
    @Override
    public Result addInbound(InboundDTO inboundDTO) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return Result.error("当前登录用户不存在");
        }
        if (inboundDTO.getArrivalId() == null) {
            return Result.error("到货单Id不能为空");
        }

        Long lockId = arrivalMapper.lockById(inboundDTO.getArrivalId());
        if (lockId == null) {
            return Result.error("到货单不存在");
        }

        Arrival arrival = arrivalMapper.selectById(inboundDTO.getArrivalId());
        if (arrival == null) {
            return Result.error("到货单不存在");
        }
        Integer count = inboundMapper.countByArrivalId(inboundDTO.getArrivalId());
        if (count != null && count > 0) {
            return Result.error("该到货单已生成入库单");
        }

        List<ArrivalItemVO> arrivalItems = arrivalItemMapper.getArrivalItemByArrivalId(arrival.getId());
        if (arrivalItems == null || arrivalItems.isEmpty()) {
            return Result.error("到货明细不能为空");
        }

        BigDecimal totalInboundNumber = BigDecimal.ZERO;
        for (ArrivalItemVO arrivalItem : arrivalItems) {
            BigDecimal qualifiedNumber = arrivalItem.getQualifiedNumber() == null
                    ? BigDecimal.ZERO : arrivalItem.getQualifiedNumber();
            if (qualifiedNumber.compareTo(BigDecimal.ZERO) > 0) {
                totalInboundNumber = totalInboundNumber.add(qualifiedNumber);
            }
        }
        if (totalInboundNumber.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("到货单无可入库合格数量");
        }

        Inbound inbound = new Inbound();
        inbound.setInboundNo(generateInboundNo(arrival.getId()));
        inbound.setArrivalId(arrival.getId());
        inbound.setWarehouseId(arrival.getWarehouseId());
        inbound.setInboundNumber(totalInboundNumber);
        inbound.setStatus("PENDING");
        inbound.setOperatorId(currentUserId);
        inbound.setRemark(inboundDTO.getRemark());
        if (inboundMapper.insert(inbound) <= 0) {
            return Result.error("新增入库主表失败");
        }

        for (ArrivalItemVO arrivalItem : arrivalItems) {
            BigDecimal qualifiedNumber = arrivalItem.getQualifiedNumber() == null
                    ? BigDecimal.ZERO : arrivalItem.getQualifiedNumber();
            if (qualifiedNumber.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            InboundItem inboundItem = new InboundItem();
            inboundItem.setInboundId(inbound.getId());
            inboundItem.setArrivalItemId(arrivalItem.getId());
            inboundItem.setMaterialId(arrivalItem.getMaterialId());
            inboundItem.setMaterialCode(arrivalItem.getMaterialCode());
            inboundItem.setMaterialName(arrivalItem.getMaterialName());
            inboundItem.setSpecification(arrivalItem.getSpecification());
            inboundItem.setUnit(arrivalItem.getUnit());
            inboundItem.setInboundNumber(qualifiedNumber);
            inboundItem.setSortNumber(arrivalItem.getSortNumber());
            inboundItem.setRemark(arrivalItem.getRemark());
            if (inboundItemMapper.insert(inboundItem) <= 0) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.error("新增入库明细失败");
            }
        }
        return Result.success("新增入库单成功");
    }

    @Transactional
    @Override
    public Result cancelInbound(InboundDTO inboundDTO) {
        if (inboundDTO.getId() == null) {
            return Result.error("入库单Id不能为空");
        }
        Inbound inbound = inboundMapper.selectById(inboundDTO.getId());
        if (inbound == null) {
            return Result.error("入库单不存在");
        }
        if (!"PENDING".equals(inbound.getStatus())) {
            return Result.error("只有待确认入库单允许取消");
        }
        Inbound updateInbound = new Inbound();
        updateInbound.setId(inbound.getId());
        updateInbound.setStatus("CANCELLED");
        updateInbound.setRemark(inboundDTO.getRemark());
        if (inboundMapper.updateById(updateInbound) > 0) {
            return Result.success("取消入库单成功");
        }
        return Result.error("取消入库单失败");
    }

    @Transactional
    @Override
    public Result confirmInbound(InboundDTO inboundDTO) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return Result.error("当前登录用户不存在");
        }
        if (inboundDTO.getId() == null) {
            return Result.error("入库单Id不能为空");
        }

        Long lockId = inboundMapper.lockById(inboundDTO.getId());
        if (lockId == null) {
            return Result.error("入库单不存在");
        }

        Inbound inbound = inboundMapper.selectById(inboundDTO.getId());
        if (inbound == null) {
            return Result.error("入库单不存在");
        }
        if (!"PENDING".equals(inbound.getStatus())) {
            return Result.error("当前入库单状态不允许确认入库");
        }

        Arrival arrival = arrivalMapper.selectById(inbound.getArrivalId());
        if (arrival == null) {
            return Result.error("来源到货单不存在");
        }

        List<InboundItemVO> inboundItems = inboundItemMapper.getInboundItemByInboundId(inbound.getId());
        if (inboundItems == null || inboundItems.isEmpty()) {
            return Result.error("入库明细不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        if (inboundMapper.completeInbound(inbound.getId(), now) <= 0) {
            return Result.error("确认入库失败");
        }

        // 只有确认入库后才允许回写订单入库数量、库存台账和库存流水。
        for (InboundItemVO inboundItem : inboundItems) {
            if (inboundItem.getOrderItemId() == null) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.error("来源采购订单明细不存在");
            }
            if (purchaseOrderItemMapper.increaseInboundNumber(
                    inboundItem.getOrderItemId(), inboundItem.getInboundNumber()) <= 0) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.error("回写采购订单明细已入库数量失败");
            }

            Inventory inventory = inventoryMapper.lockByMaterialAndWarehouse(
                    inboundItem.getMaterialId(), inbound.getWarehouseId());
            BigDecimal beforeNumber = BigDecimal.ZERO;
            BigDecimal afterNumber = inboundItem.getInboundNumber();
            if (inventory == null) {
                inventory = new Inventory();
                inventory.setMaterialId(inboundItem.getMaterialId());
                inventory.setWarehouseId(inbound.getWarehouseId());
                inventory.setCurrentNumber(afterNumber);
                inventory.setLastInboundTime(now);
                if (inventoryMapper.insert(inventory) <= 0) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return Result.error("新增库存台账失败");
                }
            } else {
                beforeNumber = inventory.getCurrentNumber() == null ? BigDecimal.ZERO : inventory.getCurrentNumber();
                afterNumber = beforeNumber.add(inboundItem.getInboundNumber());
                if (inventoryMapper.increaseCurrentNumber(inventory.getId(), inboundItem.getInboundNumber(), now) <= 0) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return Result.error("更新库存台账失败");
                }
            }

            InventoryLog inventoryLog = new InventoryLog();
            inventoryLog.setLogNo(generateInventoryLogNo(inbound.getId(), inboundItem.getId()));
            inventoryLog.setInventoryId(inventory.getId());
            inventoryLog.setMaterialId(inboundItem.getMaterialId());
            inventoryLog.setWarehouseId(inbound.getWarehouseId());
            inventoryLog.setBizType("INBOUND");
            inventoryLog.setBizId(inbound.getId());
            inventoryLog.setBeforeNumber(beforeNumber);
            inventoryLog.setChangeNumber(inboundItem.getInboundNumber());
            inventoryLog.setAfterNumber(afterNumber);
            inventoryLog.setOperatorId(currentUserId);
            inventoryLog.setOperatorName(getCurrentUsername());
            inventoryLog.setRemark("由入库单 " + inbound.getInboundNo() + " 触发的库存变更");
            inventoryLog.setOperateTime(now);
            if (inventoryLogMapper.insert(inventoryLog) <= 0) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.error("新增库存流水失败");
            }
        }

        updatePurchaseOrderStatus(arrival.getOrderId());
        return Result.success("确认入库成功");
    }

    private void updatePurchaseOrderStatus(Long orderId) {
        List<PurchaseOrderItemVO> orderItems = purchaseOrderItemMapper.getPurchaseOrderItemByOrderId(orderId);
        if (orderItems == null || orderItems.isEmpty()) {
            purchaseOrderMapper.updateStatusById(orderId, "PARTIAL_ARRIVAL");
            return;
        }
        boolean completed = true;
        // 所有订单明细都完成入库后，采购订单才进入 COMPLETED。
        for (PurchaseOrderItemVO orderItem : orderItems) {
            BigDecimal inboundNumber = orderItem.getInboundNumber() == null ? BigDecimal.ZERO : orderItem.getInboundNumber();
            if (inboundNumber.compareTo(orderItem.getOrderNumber()) < 0) {
                completed = false;
                break;
            }
        }
        purchaseOrderMapper.updateStatusById(orderId, completed ? "COMPLETED" : "PARTIAL_ARRIVAL");
    }

    private String generateInboundNo(Long arrivalId) {
        return "IN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + arrivalId;
    }

    private String generateInventoryLogNo(Long inboundId, Long inboundItemId) {
        return "IL" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + inboundId + inboundItemId;
    }
}
