package com.xixi.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xixi.annotation.OperLogRecord;
import com.xixi.entity.Inventory;
import com.xixi.entity.InventoryLog;
import com.xixi.mapper.InventoryLogMapper;
import com.xixi.mapper.InventoryMapper;
import com.xixi.pojo.dto.inventory.InventoryAdjustDTO;
import com.xixi.pojo.query.inventory.InventoryPageQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.inventory.InventoryPageVO;
import com.xixi.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.xixi.util.SecurityUtils.getCurrentName;
import static com.xixi.util.SecurityUtils.getCurrentUserId;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;
    private final InventoryLogMapper inventoryLogMapper;

    @Override
    public IPage<InventoryPageVO> getInventoryPage(InventoryPageQuery inventoryPageQuery) {
        IPage<InventoryPageVO> page = new Page<>(
                inventoryPageQuery.getPageNum(),
                inventoryPageQuery.getPageSize()
        );
        return inventoryMapper.getInventoryPage(page, inventoryPageQuery);
    }

    @Override
    @Transactional
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "库存台账",
            operationType = "ADJUST",
            operationDesc = "调整库存",
            bizType = "INVENTORY"
    )
    public Result adjustInventory(InventoryAdjustDTO inventoryAdjustDTO) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return Result.error("当前登录用户不存在");
        }
        if (inventoryAdjustDTO == null || inventoryAdjustDTO.getInventoryId() == null) {
            return Result.error("库存台账ID不能为空");
        }
        if (inventoryAdjustDTO.getChangeNumber() == null
                || inventoryAdjustDTO.getChangeNumber().compareTo(BigDecimal.ZERO) == 0) {
            return Result.error("调整数量不能为空且不能为0");
        }
        if (inventoryAdjustDTO.getReason() == null || inventoryAdjustDTO.getReason().trim().isEmpty()) {
            return Result.error("调整原因不能为空");
        }

        Inventory inventory = inventoryMapper.lockById(inventoryAdjustDTO.getInventoryId());
        if (inventory == null) {
            return Result.error("库存台账不存在");
        }
        BigDecimal beforeNumber = inventory.getCurrentNumber() == null ? BigDecimal.ZERO : inventory.getCurrentNumber();
        BigDecimal afterNumber = beforeNumber.add(inventoryAdjustDTO.getChangeNumber());
        if (afterNumber.compareTo(BigDecimal.ZERO) < 0) {
            return Result.error("调整后库存不能小于0");
        }

        if (inventoryMapper.adjustCurrentNumber(inventory.getId(), inventoryAdjustDTO.getChangeNumber()) <= 0) {
            return Result.error("调整库存失败");
        }

        InventoryLog inventoryLog = new InventoryLog();
        inventoryLog.setLogNo(generateInventoryLogNo(inventory.getId()));
        inventoryLog.setInventoryId(inventory.getId());
        inventoryLog.setMaterialId(inventory.getMaterialId());
        inventoryLog.setWarehouseId(inventory.getWarehouseId());
        inventoryLog.setBizType("ADJUST");
        inventoryLog.setBizId(inventory.getId());
        inventoryLog.setBeforeNumber(beforeNumber);
        inventoryLog.setChangeNumber(inventoryAdjustDTO.getChangeNumber());
        inventoryLog.setAfterNumber(afterNumber);
        inventoryLog.setOperatorId(currentUserId);
        inventoryLog.setOperatorName(getCurrentName());
        inventoryLog.setRemark("库存调整：" + inventoryAdjustDTO.getReason().trim());
        inventoryLog.setOperateTime(LocalDateTime.now());
        if (inventoryLogMapper.insert(inventoryLog) <= 0) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("新增库存流水失败");
        }
        return Result.success("库存调整成功");
    }

    private String generateInventoryLogNo(Long inventoryId) {
        return "IL" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + "ADJ" + inventoryId;
    }
}
