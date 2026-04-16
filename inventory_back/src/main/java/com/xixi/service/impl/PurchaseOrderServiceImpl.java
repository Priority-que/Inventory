package com.xixi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xixi.entity.PurchaseOrder;
import com.xixi.entity.PurchaseOrderItem;
import com.xixi.entity.PurchaseRequestItem;
import com.xixi.entity.Supplier;
import com.xixi.mapper.PurchaseOrderItemMapper;
import com.xixi.mapper.PurchaseOrderMapper;
import com.xixi.mapper.PurchaseRequestItemMapper;
import com.xixi.mapper.PurchaseRequestMapper;
import com.xixi.mapper.SupplierMapper;
import com.xixi.pojo.dto.purchase.PurchaseOrderDTO;
import com.xixi.pojo.dto.purchase.PurchaseOrderItemCreateDTO;
import com.xixi.pojo.query.purchase.PurchaseOrderQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseOrderVO;
import com.xixi.pojo.vo.purchase.PurchaseRequestVO;
import com.xixi.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseRequestMapper purchaseRequestMapper;
    private final SupplierMapper supplierMapper;
    private final PurchaseRequestItemMapper purchaseRequestItemMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;

    @Override
    public IPage<PurchaseOrderVO> getPurchaseOrderPage(PurchaseOrderQuery purchaseOrderQuery) {
        IPage<PurchaseOrderVO> page = new Page<>(purchaseOrderQuery.getPageNum(), purchaseOrderQuery.getPageSize());
        IPage<PurchaseOrderVO> result = purchaseOrderMapper.getPurchaseOrderPage(page, purchaseOrderQuery);
        return result;
    }

    @Override
    public PurchaseOrderVO getPurchaseOrderById(Long id) {
        PurchaseOrderVO purchaseOrderVO = purchaseOrderMapper.getPurchaseOrderById(id);
        return purchaseOrderVO;
    }

    @Transactional
    @Override
    public Result addPurchaseOrder(PurchaseOrderDTO purchaseOrderDTO) {
        PurchaseOrder purchaseOrder = BeanUtil.copyProperties(purchaseOrderDTO, PurchaseOrder.class);
        if (purchaseOrder.getRequestId() == null) {
            return Result.error("采购申请Id为空");
        }
        if (purchaseOrder.getSupplierId() == null) {
            return Result.error("供应商Id为空");
        }
        if (purchaseOrder.getPurchaserId() == null) {
            return Result.error("采购员Id为空");
        }
        if (purchaseOrder.getPlanDate() == null) {
            return Result.error("计划交期为空");
        }
        if (purchaseOrderDTO.getItems() == null || purchaseOrderDTO.getItems().isEmpty()) {
            return Result.error("采购订单明细不能为空");
        }

        Long requestId = purchaseOrder.getRequestId();
        Long lockId = purchaseRequestMapper.lockById(requestId);
        if (lockId == null) {
            return Result.error("采购申请单不存在");
        }

        Integer count = purchaseOrderMapper.countByRequestId(requestId);
        if (count != null && count > 0) {
            return Result.error("该采购申请已生成采购订单");
        }

        PurchaseRequestVO purchaseRequest = purchaseRequestMapper.getPurchaseRequestById(requestId);
        if (purchaseRequest == null) {
            return Result.error("采购申请单不存在");
        }
        if (!"APPROVED".equals(purchaseRequest.getStatus())) {
            return Result.error("采购申请单未通过审核");
        }

        Supplier supplier = supplierMapper.getSupplierById(purchaseOrder.getSupplierId());
        if (supplier == null) {
            return Result.error("供应商不存在");
        }
        if (!"ACTIVE".equals(supplier.getStatus())) {
            return Result.error("供应商状态异常");
        }

        List<PurchaseRequestItem> purchaseRequestItems = purchaseRequestItemMapper.getPurchaseRequestItemByRequestId(requestId);
        if (purchaseRequestItems == null || purchaseRequestItems.isEmpty()) {
            return Result.error("采购申请明细不能为空");
        }

        Map<Long, PurchaseRequestItem> requestItemMap = new HashMap<>();
        for (PurchaseRequestItem purchaseRequestItem : purchaseRequestItems) {
            requestItemMap.put(purchaseRequestItem.getId(), purchaseRequestItem);
        }

        Map<Long, PurchaseOrderItemCreateDTO> dtoItemMap = new HashMap<>();
        for (PurchaseOrderItemCreateDTO item : purchaseOrderDTO.getItems()) {
            if (item.getRequestItemId() == null) {
                return Result.error("订单明细requestItemId不能为空");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                return Result.error("订单明细单价不能为空且不能小于0");
            }
            if (!requestItemMap.containsKey(item.getRequestItemId())) {
                return Result.error("订单明细来源申请明细不存在");
            }
            dtoItemMap.put(item.getRequestItemId(), item);
        }

        if (dtoItemMap.size() != purchaseRequestItems.size()) {
            return Result.error("订单明细必须与采购申请明细一致");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PurchaseRequestItem purchaseRequestItem : purchaseRequestItems) {
            PurchaseOrderItemCreateDTO item = dtoItemMap.get(purchaseRequestItem.getId());
            BigDecimal lineAmount = purchaseRequestItem.getRequestNumber()
                    .multiply(item.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(lineAmount);
        }

        purchaseOrder.setOrderNo(generateOrderNo(requestId));
        purchaseOrder.setStatus("WAIT_CONFIRM");
        purchaseOrder.setTotalAmount(totalAmount);
        if (purchaseOrderMapper.insert(purchaseOrder) <= 0) {
            return Result.error("添加采购订单主表失败");
        }

        for (PurchaseRequestItem purchaseRequestItem : purchaseRequestItems) {
            PurchaseOrderItemCreateDTO item = dtoItemMap.get(purchaseRequestItem.getId());
            PurchaseOrderItem purchaseOrderItem = new PurchaseOrderItem();
            purchaseOrderItem.setOrderId(purchaseOrder.getId());
            purchaseOrderItem.setRequestItemId(purchaseRequestItem.getId());
            purchaseOrderItem.setMaterialId(purchaseRequestItem.getMaterialId());
            purchaseOrderItem.setMaterialCode(purchaseRequestItem.getMaterialCode());
            purchaseOrderItem.setMaterialName(purchaseRequestItem.getMaterialName());
            purchaseOrderItem.setSpecification(purchaseRequestItem.getSpecification());
            purchaseOrderItem.setUnit(purchaseRequestItem.getUnit());
            purchaseOrderItem.setOrderNumber(purchaseRequestItem.getRequestNumber());
            purchaseOrderItem.setUnitPrice(item.getUnitPrice().setScale(2, RoundingMode.HALF_UP));
            purchaseOrderItem.setLineAmount(purchaseRequestItem.getRequestNumber()
                    .multiply(item.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP));
            purchaseOrderItem.setArrivedNumber(BigDecimal.ZERO);
            purchaseOrderItem.setInboundNumber(BigDecimal.ZERO);
            purchaseOrderItem.setSortNumber(purchaseRequestItem.getSortNumber());
            purchaseOrderItem.setRemark(item.getRemark());
            if (purchaseOrderItemMapper.insert(purchaseOrderItem) <= 0) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.error("添加采购订单明细失败");
            }
        }

        if (purchaseRequestMapper.updateStatusById(requestId, "ORDER_CREATED") <= 0) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("回写采购申请状态失败");
        }
        return Result.success("添加采购订单主表成功");
    }

    @Override
    public Result updatePurchaseOrder(PurchaseOrderDTO purchaseOrderDTO) {
        if (purchaseOrderDTO.getId() == null) {
            return Result.error("采购订单ID不能为空");
        }
        PurchaseOrder purchaseOrderStatus = purchaseOrderMapper.selectById(purchaseOrderDTO.getId());
        if (purchaseOrderStatus == null) {
            return Result.error("采购订单不存在");
        }
        if (!"WAIT_CONFIRM".equals(purchaseOrderStatus.getStatus())) {
            return Result.error("采购订单状态错误");
        }

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(purchaseOrderDTO.getId());
        purchaseOrder.setPlanDate(purchaseOrderDTO.getPlanDate());
        purchaseOrder.setRemark(purchaseOrderDTO.getRemark());
        if (purchaseOrderMapper.updateById(purchaseOrder) > 0) {
            return Result.success("修改采购订单主表基本信息成功");
        }
        return Result.error("修改采购订单基本信息主表失败");
    }

    @Transactional
    @Override
    public Result cancelPurchaseOrder(PurchaseOrderDTO purchaseOrderDTO) {
        if (purchaseOrderDTO.getId() == null) {
            return Result.error("采购订单ID不能为空");
        }
        PurchaseOrder purchaseOrderStatus = purchaseOrderMapper.selectById(purchaseOrderDTO.getId());
        if (purchaseOrderStatus == null) {
            return Result.error("采购订单不存在");
        }
        if (!"WAIT_CONFIRM".equals(purchaseOrderStatus.getStatus())) {
            return Result.error("采购订单状态错误");
        }

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(purchaseOrderDTO.getId());
        purchaseOrder.setStatus("CANCELLED");
        if (purchaseOrderMapper.updateById(purchaseOrder) <= 0) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("取消采购订单失败");
        }
        if (purchaseRequestMapper.updateStatusById(purchaseOrderStatus.getRequestId(), "APPROVED") <= 0) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("回写采购申请状态失败");
        }
        return Result.success("取消采购订单成功");
    }

    @Transactional
    @Override
    public Result closePurchaseOrder(PurchaseOrderDTO purchaseOrderDTO) {
        if (purchaseOrderDTO.getId() == null) {
            return Result.error("采购订单ID不能为空");
        }
        if (purchaseOrderDTO.getCloseReason() == null || purchaseOrderDTO.getCloseReason().trim().isEmpty()) {
            return Result.error("关闭原因不能为空");
        }
        PurchaseOrder purchaseOrderStatus = purchaseOrderMapper.selectById(purchaseOrderDTO.getId());
        if (purchaseOrderStatus == null) {
            return Result.error("采购订单不存在");
        }
        if (!"IN_PROGRESS".equals(purchaseOrderStatus.getStatus())
                && !"PARTIAL_ARRIVAL".equals(purchaseOrderStatus.getStatus())) {
            return Result.error("采购订单状态错误");
        }

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(purchaseOrderDTO.getId());
        purchaseOrder.setStatus("CLOSED");
        purchaseOrder.setCloseTime(LocalDateTime.now());
        purchaseOrder.setCloseReason(purchaseOrderDTO.getCloseReason());
        if (purchaseOrderMapper.updateById(purchaseOrder) > 0) {
            return Result.success("关闭采购订单成功");
        }
        return Result.error("关闭采购订单失败");
    }

    private String generateOrderNo(Long requestId) {
        return "PO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + requestId;
    }
}
