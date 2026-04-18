package com.xixi.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xixi.entity.Arrival;
import com.xixi.entity.ArrivalItem;
import com.xixi.entity.PurchaseOrder;
import com.xixi.entity.Warehouse;
import com.xixi.mapper.ArrivalItemMapper;
import com.xixi.mapper.ArrivalMapper;
import com.xixi.mapper.PurchaseOrderItemMapper;
import com.xixi.mapper.PurchaseOrderMapper;
import com.xixi.mapper.WarehouseMapper;
import com.xixi.pojo.dto.arrival.ArrivalDTO;
import com.xixi.pojo.dto.arrival.ArrivalItemDTO;
import com.xixi.pojo.query.arrival.ArrivalQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.arrival.ArrivalVO;
import com.xixi.pojo.vo.purchase.PurchaseOrderItemVO;
import com.xixi.service.ArrivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.xixi.util.SecurityUtils.getCurrentUserId;

@Service
@RequiredArgsConstructor
public class ArrivalServiceImpl implements ArrivalService {
    private final ArrivalMapper arrivalMapper;
    private final ArrivalItemMapper arrivalItemMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final WarehouseMapper warehouseMapper;

    @Override
    public IPage<ArrivalVO> getArrivalPage(ArrivalQuery arrivalQuery) {
        IPage<ArrivalVO> iPage = new Page<>(arrivalQuery.getPageNum(),arrivalQuery.getPageSize());
        IPage<ArrivalVO> page = arrivalMapper.getArrivalPage(iPage,arrivalQuery);
        return page;
    }

    @Override
    public ArrivalVO getArrivalById(Long arrivalId) {
        ArrivalVO arrivalVO = arrivalMapper.getArrivalById(arrivalId);
        if (arrivalVO != null) {
            arrivalVO.setItems(arrivalItemMapper.getArrivalItemByArrivalId(arrivalId));
        }
        return arrivalVO;
    }

    @Transactional
    @Override
    public Result addArrival(ArrivalDTO arrivalDTO) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return Result.error("当前登录用户不存在");
        }
        if (arrivalDTO.getOrderId() == null) {
            return Result.error("采购订单Id不能为空");
        }
        if (arrivalDTO.getWarehouseId() == null) {
            return Result.error("仓库Id不能为空");
        }
        if (arrivalDTO.getArrivalDate() == null) {
            return Result.error("到货日期不能为空");
        }
        if (arrivalDTO.getItems() == null || arrivalDTO.getItems().isEmpty()) {
            return Result.error("到货明细不能为空");
        }

        Long lockId = purchaseOrderMapper.lockById(arrivalDTO.getOrderId());
        if (lockId == null) {
            return Result.error("采购订单不存在");
        }

        PurchaseOrder purchaseOrder = purchaseOrderMapper.selectById(arrivalDTO.getOrderId());
        if (purchaseOrder == null) {
            return Result.error("采购订单不存在");
        }
        if (!"IN_PROGRESS".equals(purchaseOrder.getStatus())
                && !"PARTIAL_ARRIVAL".equals(purchaseOrder.getStatus())) {
            return Result.error("当前采购订单状态不允许登记到货");
        }

        Warehouse warehouse = warehouseMapper.selectById(arrivalDTO.getWarehouseId());
        if (warehouse == null) {
            return Result.error("仓库不存在");
        }
        if (!"ENABLED".equals(warehouse.getStatus())) {
            return Result.error("仓库状态异常");
        }

        List<PurchaseOrderItemVO> purchaseOrderItems =
                purchaseOrderItemMapper.getPurchaseOrderItemByOrderId(arrivalDTO.getOrderId());
        if (purchaseOrderItems == null || purchaseOrderItems.isEmpty()) {
            return Result.error("采购订单明细不能为空");
        }

        Map<Long, PurchaseOrderItemVO> orderItemMap = new HashMap<>();
        for (PurchaseOrderItemVO purchaseOrderItem : purchaseOrderItems) {
            orderItemMap.put(purchaseOrderItem.getId(), purchaseOrderItem);
        }

        Set<Long> orderItemIdSet = new HashSet<>();
        BigDecimal totalArrivalNumber = BigDecimal.ZERO;
        BigDecimal totalQualifiedNumber = BigDecimal.ZERO;
        BigDecimal totalUnqualifiedNumber = BigDecimal.ZERO;
        List<String> abnormalNotes = new ArrayList<>();

        for (ArrivalItemDTO itemDTO : arrivalDTO.getItems()) {
            if (itemDTO.getOrderItemId() == null) {
                return Result.error("到货明细orderItemId不能为空");
            }
            if (!orderItemIdSet.add(itemDTO.getOrderItemId())) {
                return Result.error("到货明细不能重复选择同一采购订单明细");
            }
            PurchaseOrderItemVO orderItem = orderItemMap.get(itemDTO.getOrderItemId());
            if (orderItem == null) {
                return Result.error("到货明细来源采购订单明细不存在");
            }
            if (itemDTO.getArrivalNumber() == null || itemDTO.getArrivalNumber().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error("到货数量必须大于0");
            }
            if (itemDTO.getQualifiedNumber() == null || itemDTO.getQualifiedNumber().compareTo(BigDecimal.ZERO) < 0) {
                return Result.error("合格数量不能为空且不能小于0");
            }
            if (itemDTO.getUnqualifiedNumber() == null
                    || itemDTO.getUnqualifiedNumber().compareTo(BigDecimal.ZERO) < 0) {
                return Result.error("不合格数量不能为空且不能小于0");
            }
            BigDecimal sumNumber = itemDTO.getQualifiedNumber().add(itemDTO.getUnqualifiedNumber());
            if (sumNumber.compareTo(itemDTO.getArrivalNumber()) != 0) {
                return Result.error("合格数量与不合格数量之和必须等于到货数量");
            }
            BigDecimal arrivedNumber = orderItem.getArrivedNumber() == null ? BigDecimal.ZERO : orderItem.getArrivedNumber();
            BigDecimal remainArrivalNumber = orderItem.getOrderNumber().subtract(arrivedNumber);
            if (itemDTO.getArrivalNumber().compareTo(remainArrivalNumber) > 0) {
                return Result.error("本次到货数量超过剩余可到货数量");
            }

            totalArrivalNumber = totalArrivalNumber.add(itemDTO.getArrivalNumber());
            totalQualifiedNumber = totalQualifiedNumber.add(itemDTO.getQualifiedNumber());
            totalUnqualifiedNumber = totalUnqualifiedNumber.add(itemDTO.getUnqualifiedNumber());
            if (itemDTO.getUnqualifiedNumber().compareTo(BigDecimal.ZERO) > 0 || hasText(itemDTO.getAbnormalNote())) {
                abnormalNotes.add(defaultAbnormalNote(itemDTO.getAbnormalNote()));
            }
        }

        Arrival arrival = new Arrival();
        arrival.setArrivalNo(generateArrivalNo(arrivalDTO.getOrderId()));
        arrival.setOrderId(arrivalDTO.getOrderId());
        arrival.setWarehouseId(arrivalDTO.getWarehouseId());
        arrival.setArrivalDate(arrivalDTO.getArrivalDate());
        arrival.setArrivalNumber(totalArrivalNumber);
        arrival.setQualifiedNumber(totalQualifiedNumber);
        arrival.setUnqualifiedNumber(totalUnqualifiedNumber);
        arrival.setStatus(abnormalNotes.isEmpty() ? "NORMAL" : "ABNORMAL");
        arrival.setAbnormalNote(buildAbnormalNote(abnormalNotes));
        arrival.setOperatorId(currentUserId);
        arrival.setRemark(arrivalDTO.getRemark());
        if (arrivalMapper.insert(arrival) <= 0) {
            return Result.error("新增到货主表失败");
        }

        for (ArrivalItemDTO itemDTO : arrivalDTO.getItems()) {
            PurchaseOrderItemVO orderItem = orderItemMap.get(itemDTO.getOrderItemId());
            ArrivalItem arrivalItem = new ArrivalItem();
            arrivalItem.setArrivalId(arrival.getId());
            arrivalItem.setOrderItemId(orderItem.getId());
            arrivalItem.setMaterialId(orderItem.getMaterialId());
            arrivalItem.setMaterialCode(orderItem.getMaterialCode());
            arrivalItem.setMaterialName(orderItem.getMaterialName());
            arrivalItem.setSpecification(orderItem.getSpecification());
            arrivalItem.setUnit(orderItem.getUnit());
            arrivalItem.setArrivalNumber(itemDTO.getArrivalNumber());
            arrivalItem.setQualifiedNumber(itemDTO.getQualifiedNumber());
            arrivalItem.setUnqualifiedNumber(itemDTO.getUnqualifiedNumber());
            arrivalItem.setAbnormalNote(itemDTO.getAbnormalNote());
            arrivalItem.setSortNumber(orderItem.getSortNumber());
            arrivalItem.setRemark(itemDTO.getRemark());
            if (arrivalItemMapper.insert(arrivalItem) <= 0) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.error("新增到货明细失败");
            }
            if (purchaseOrderItemMapper.increaseArrivedNumber(orderItem.getId(), itemDTO.getArrivalNumber()) <= 0) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.error("回写采购订单明细已到货数量失败");
            }
        }

        if (!"PARTIAL_ARRIVAL".equals(purchaseOrder.getStatus())
                && purchaseOrderMapper.updateStatusById(purchaseOrder.getId(), "PARTIAL_ARRIVAL") <= 0) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("回写采购订单状态失败");
        }
        return Result.success("新增到货单成功");
    }

    private String generateArrivalNo(Long orderId) {
        return "AR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + orderId;
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private String defaultAbnormalNote(String abnormalNote) {
        if (hasText(abnormalNote)) {
            return abnormalNote.trim();
        }
        return "到货存在异常";
    }

    private String buildAbnormalNote(List<String> abnormalNotes) {
        if (abnormalNotes == null || abnormalNotes.isEmpty()) {
            return null;
        }
        String note = String.join("；", abnormalNotes);
        if (note.length() > 255) {
            return note.substring(0, 255);
        }
        return note;
    }
}
