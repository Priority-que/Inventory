package com.xixi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xixi.annotation.OperLogRecord;
import com.xixi.entity.PurchaseRequest;
import com.xixi.entity.PurchaseRequestItem;
import com.xixi.entity.PurchaseRequestReview;
import com.xixi.mapper.PurchaseRequestItemMapper;
import com.xixi.mapper.PurchaseRequestMapper;
import com.xixi.pojo.dto.purchase.PurchaseRequestDTO;
import com.xixi.pojo.query.purchase.PurchaseRequestQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.purchase.PurchaseRequestPageVO;
import com.xixi.pojo.vo.purchase.PurchaseRequestVO;
import com.xixi.service.PurchaseRequestReviewService;
import com.xixi.service.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.xixi.util.SecurityUtils.getCurrentUserId;
import static com.xixi.util.SecurityUtils.getCurrentUsername;

@Service
@RequiredArgsConstructor
public class PurchaseRequestServiceImpl implements PurchaseRequestService {
    private final PurchaseRequestMapper purchaseRequestMapper;
    private final PurchaseRequestItemMapper purchaseRequestItemMapper;
    private final PurchaseRequestReviewService purchaseRequestReviewService;

    @Override
    public IPage<PurchaseRequestPageVO> getPurchaseRequestPage(PurchaseRequestQuery purchaseRequestQuery) {
        IPage<PurchaseRequestPageVO> page = new Page<>(purchaseRequestQuery.getPageNum(), purchaseRequestQuery.getPageSize());
        return purchaseRequestMapper.getPurchaseRequestPage(page, purchaseRequestQuery);
    }

    @Override
    public PurchaseRequestVO getPurchaseRequestById(Long id) {
        return purchaseRequestMapper.getPurchaseRequestById(id);
    }

    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "采购申请",
            operationType = "CREATE",
            operationDesc = "新增采购申请",
            bizType = "PURCHASE_REQUEST"
    )
    public Result addPurchaseRequest(PurchaseRequestDTO purchaseRequestDTO) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return Result.error("当前登录用户不存在");
        }
        PurchaseRequest purchaseRequest = BeanUtil.copyProperties(purchaseRequestDTO, PurchaseRequest.class);
        purchaseRequest.setApplicantId(currentUserId);
        purchaseRequest.setRequestNo(generateRequestNo(currentUserId));
        purchaseRequest.setStatus("DRAFT");
        if (purchaseRequestMapper.insert(purchaseRequest) > 0) {
            purchaseRequestDTO.setId(purchaseRequest.getId());
            return Result.success("添加采购申请成功");
        }
        return Result.error("添加采购申请失败");
    }

    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "采购申请",
            operationType = "UPDATE",
            operationDesc = "修改采购申请",
            bizType = "PURCHASE_REQUEST"
    )
    public Result updatePurchaseRequest(PurchaseRequestDTO purchaseRequestDTO) {
        PurchaseRequest purchaseRequest = new PurchaseRequest();
        purchaseRequest.setId(purchaseRequestDTO.getId());
        purchaseRequest.setTitle(purchaseRequestDTO.getTitle());
        purchaseRequest.setDept(purchaseRequestDTO.getDept());
        purchaseRequest.setExpectedDate(purchaseRequestDTO.getExpectedDate());
        purchaseRequest.setRemark(purchaseRequestDTO.getRemark());
        if (purchaseRequestMapper.updateById(purchaseRequest) > 0) {
            return Result.success("修改采购申请成功");
        }
        return Result.error("修改采购申请失败");
    }

    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "采购申请",
            operationType = "DELETE",
            operationDesc = "删除采购申请",
            bizType = "PURCHASE_REQUEST"
    )
    public Result deletePurchaseRequest(List<Integer> ids) {
        if (purchaseRequestMapper.deleteByIds(ids) > 0) {
            return Result.success("删除采购申请成功");
        }
        return Result.error("删除采购申请失败");
    }

    @Transactional
    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "采购申请",
            operationType = "SUBMIT",
            operationDesc = "提交采购申请",
            bizType = "PURCHASE_REQUEST"
    )
    public Result submitPurchaseRequest(PurchaseRequestDTO purchaseRequestDTO) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return Result.error("当前登录用户不存在");
        }
        if (purchaseRequestDTO.getId() == null) {
            return Result.error("申请主表Id不能为空");
        }
        PurchaseRequest purchaseRequest = purchaseRequestMapper.findPurchaseRequestById(purchaseRequestDTO.getId());
        if (purchaseRequest == null) {
            return Result.error("申请单不存在");
        }
        if (!currentUserId.equals(purchaseRequest.getApplicantId())) {
            return Result.error("当前用户不是申请人，不能提交该申请单");
        }
        if (!"DRAFT".equals(purchaseRequest.getStatus()) && !"REJECTED".equals(purchaseRequest.getStatus())) {
            return Result.error("申请单状态必须为草稿或已驳回");
        }

        List<PurchaseRequestItem> items = purchaseRequestItemMapper.getPurchaseRequestItemByRequestId(purchaseRequestDTO.getId());
        if (items == null || items.isEmpty()) {
            return Result.error("申请单没有明细！");
        }

        String fromStatus = purchaseRequest.getStatus();
        purchaseRequest.setStatus("PENDING_APPROVAL");
        purchaseRequest.setSubmitTime(LocalDateTime.now());
        if (purchaseRequestMapper.updateById(purchaseRequest) <= 0) {
            return Result.error("提交申请单失败！");
        }

        PurchaseRequestReview review = new PurchaseRequestReview();
        if ("DRAFT".equals(fromStatus)) {
            review.setActionType("SUBMIT");
            review.setOperateNote("提交采购申请");
        }
        if ("REJECTED".equals(fromStatus)) {
            review.setActionType("RESUBMIT");
            review.setOperateNote("驳回后重新提交采购申请");
        }
        review.setRequestId(purchaseRequest.getId());
        review.setFromStatus(fromStatus);
        review.setToStatus("PENDING_APPROVAL");
        review.setOperatorId(getCurrentUserId());
        review.setOperatorName(getCurrentUsername());
        review.setOperateTime(LocalDateTime.now());
        purchaseRequestReviewService.saveReview(review);
        return Result.success("提交申请单成功！");
    }

    @Transactional
    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "采购申请",
            operationType = "WITHDRAW",
            operationDesc = "撤回采购申请",
            bizType = "PURCHASE_REQUEST"
    )
    public Result withdrawPurchaseRequest(PurchaseRequestDTO purchaseRequestDTO) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return Result.error("当前登录用户不存在");
        }
        if (purchaseRequestDTO.getId() == null) {
            return Result.error("申请主表Id不能为空");
        }
        PurchaseRequest purchaseRequest = purchaseRequestMapper.findPurchaseRequestById(purchaseRequestDTO.getId());
        if (purchaseRequest == null) {
            return Result.error("申请单不存在");
        }
        if (!currentUserId.equals(purchaseRequest.getApplicantId())) {
            return Result.error("当前用户不是申请人，不能撤回该申请单");
        }
        if (!"PENDING_APPROVAL".equals(purchaseRequest.getStatus())) {
            return Result.error("申请单状态不是待审核，不能撤回");
        }

        String fromStatus = "PENDING_APPROVAL";
        purchaseRequest.setStatus("WITHDRAWN");
        if (purchaseRequestMapper.updateById(purchaseRequest) <= 0) {
            return Result.error("撤回申请单失败！");
        }

        PurchaseRequestReview review = new PurchaseRequestReview();
        review.setRequestId(purchaseRequest.getId());
        review.setActionType("WITHDRAW");
        review.setFromStatus(fromStatus);
        review.setToStatus(purchaseRequest.getStatus());
        review.setOperateNote("申请人主动撤回");
        review.setOperatorId(getCurrentUserId());
        review.setOperatorName(getCurrentUsername());
        review.setOperateTime(LocalDateTime.now());
        purchaseRequestReviewService.saveReview(review);
        return Result.success("撤回申请单成功！");
    }

    @Transactional
    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "采购申请",
            operationType = "APPROVE",
            operationDesc = "审核通过采购申请",
            bizType = "PURCHASE_REQUEST"
    )
    public Result approvePurchaseRequest(PurchaseRequestDTO purchaseRequestDTO) {
        if (purchaseRequestDTO.getId() == null) {
            return Result.error("采购申请单Id不能为空！");
        }
        PurchaseRequest purchaseRequest = purchaseRequestMapper.findPurchaseRequestById(purchaseRequestDTO.getId());
        if (purchaseRequest == null) {
            return Result.error("采购申请单不存在！");
        }
        if (!"PENDING_APPROVAL".equals(purchaseRequest.getStatus())) {
            return Result.error("采购申请单状态不是审核中！");
        }
        if (purchaseRequestDTO.getReviewNote() == null) {
            return Result.error("审核意见为空");
        }

        purchaseRequest.setStatus("APPROVED");
        purchaseRequest.setReviewUserId(getCurrentUserId());
        purchaseRequest.setReviewTime(LocalDateTime.now());
        purchaseRequest.setReviewNote(purchaseRequestDTO.getReviewNote());
        if (purchaseRequestMapper.updateById(purchaseRequest) <= 0) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("审核采购申请单失败");
        }

        PurchaseRequestReview review = new PurchaseRequestReview();
        review.setRequestId(purchaseRequest.getId());
        review.setActionType("APPROVE");
        review.setFromStatus("PENDING_APPROVAL");
        review.setToStatus("APPROVED");
        review.setOperatorId(purchaseRequest.getReviewUserId());
        review.setOperatorName(getCurrentUsername());
        review.setOperateNote(purchaseRequestDTO.getReviewNote());
        review.setOperateTime(LocalDateTime.now());
        purchaseRequestReviewService.saveReview(review);
        return Result.success("审核采购申请单成功");
    }

    @Transactional
    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "采购申请",
            operationType = "REJECT",
            operationDesc = "驳回采购申请",
            bizType = "PURCHASE_REQUEST"
    )
    public Result rejectPurchaseRequest(PurchaseRequestDTO purchaseRequestDTO) {
        if (purchaseRequestDTO.getId() == null) {
            return Result.error("采购申请单Id不能为空！");
        }
        PurchaseRequest purchaseRequest = purchaseRequestMapper.findPurchaseRequestById(purchaseRequestDTO.getId());
        if (purchaseRequest == null) {
            return Result.error("采购申请单不存在！");
        }
        if (!"PENDING_APPROVAL".equals(purchaseRequest.getStatus())) {
            return Result.error("采购申请单状态不是审核中！");
        }

        purchaseRequest.setStatus("REJECTED");
        purchaseRequest.setReviewUserId(getCurrentUserId());
        purchaseRequest.setReviewTime(LocalDateTime.now());
        purchaseRequest.setReviewNote(purchaseRequestDTO.getReviewNote());
        if (purchaseRequestMapper.updateById(purchaseRequest) <= 0) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("审核采购申请单失败");
        }

        PurchaseRequestReview review = new PurchaseRequestReview();
        review.setRequestId(purchaseRequest.getId());
        review.setActionType("REJECT");
        review.setFromStatus("PENDING_APPROVAL");
        review.setToStatus("REJECTED");
        review.setOperatorId(purchaseRequest.getReviewUserId());
        review.setOperatorName(getCurrentUsername());
        review.setOperateNote(purchaseRequestDTO.getReviewNote());
        review.setOperateTime(LocalDateTime.now());
        purchaseRequestReviewService.saveReview(review);
        return Result.success("审核采购申请单成功");
    }

    private String generateRequestNo(Long requestId) {
        return "PR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + requestId;
    }
}
