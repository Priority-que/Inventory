package com.xixi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
        IPage<PurchaseRequestPageVO> purchaseRequestPageVoIPage = new Page<>(purchaseRequestQuery.getPageNum(), purchaseRequestQuery.getPageSize());
        IPage<PurchaseRequestPageVO> page = purchaseRequestMapper.getPurchaseRequestPage(purchaseRequestPageVoIPage, purchaseRequestQuery);
        return page;
    }

    @Override
    public PurchaseRequestVO getPurchaseRequestById(Long id) {
        PurchaseRequestVO purchaseRequestVO = purchaseRequestMapper.getPurchaseRequestById(id);
        return purchaseRequestVO;
    }

    @Override
    public Result addPurchaseRequest(PurchaseRequestDTO purchaseRequestDTO) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return Result.error("当前登录用户不存在");
        }
        PurchaseRequest purchaseRequest = BeanUtil.copyProperties(purchaseRequestDTO, PurchaseRequest.class);
        purchaseRequest.setApplicantId(currentUserId);
        String requestNo = generateRequestNo(currentUserId);
        purchaseRequest.setRequestNo(requestNo);
        purchaseRequest.setStatus("DRAFT");
        if (purchaseRequestMapper.insert(purchaseRequest) > 0) {
            return Result.success("添加采购申请主表信息成功");
        }
        return Result.error("添加采购申请主表信息成功");
    }

    @Override
    public Result updatePurchaseRequest(PurchaseRequestDTO purchaseRequestDTO) {
        PurchaseRequest purchaseRequest = new PurchaseRequest();
        purchaseRequest.setId(purchaseRequestDTO.getId());
        purchaseRequest.setTitle(purchaseRequestDTO.getTitle());
        purchaseRequest.setDept(purchaseRequestDTO.getDept());
        purchaseRequest.setExpectedDate(purchaseRequestDTO.getExpectedDate());
        purchaseRequest.setRemark(purchaseRequestDTO.getRemark());
        if (purchaseRequestMapper.updateById(purchaseRequest) > 0) {
            return Result.success("修改采购申请主表信息成功");
        }
        return Result.error("修改采购申请主表信息成功");
    }

    @Override
    public Result deletePurchaseRequest(List<Integer> ids) {
        if (purchaseRequestMapper.deleteByIds(ids) > 0) {
            return Result.success("删除采购申请主表信息成功");
        }
        return Result.error("删除采购申请主表信息成功");
    }

    @Transactional
    @Override
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
            return Result.error("申请单状态必须为草稿或者被拒绝");
        }
        List<PurchaseRequestItem> list = purchaseRequestItemMapper.getPurchaseRequestItemByRequestId(purchaseRequestDTO.getId());
        if (list == null || list.isEmpty()) {
            return Result.error("申请单没有明细！");
        }
        String fromStatus = purchaseRequest.getStatus();
        purchaseRequest.setStatus("PENDING_APPROVAL");
        purchaseRequest.setSubmitTime(LocalDateTime.now());
        if (purchaseRequestMapper.updateById(purchaseRequest) <= 0) {
            return Result.error("提交申请单失败！");
        }

        PurchaseRequestReview purchaseRequestReview = new PurchaseRequestReview();
        if ("DRAFT".equals(fromStatus)) {
            purchaseRequestReview.setActionType("SUBMIT");
            purchaseRequestReview.setOperateNote("提交采购申请");
        }
        if ("REJECTED".equals(fromStatus)) {
            purchaseRequestReview.setActionType("RESUBMIT");
            purchaseRequestReview.setOperateNote("驳回后重新提交采购申请");
        }
        purchaseRequestReview.setRequestId(purchaseRequest.getId());
        purchaseRequestReview.setFromStatus(fromStatus);
        purchaseRequestReview.setToStatus("PENDING_APPROVAL");
        purchaseRequestReview.setOperatorId(getCurrentUserId());
        purchaseRequestReview.setOperatorName(getCurrentUsername());
        purchaseRequestReview.setOperateTime(LocalDateTime.now());
        purchaseRequestReviewService.saveReview(purchaseRequestReview);
        return Result.success("提交申请单成功！");
    }

    @Transactional
    @Override
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
            return Result.error("申请单状态不是待审核状态，不能撤回");
        }
        String fromStatus = "PENDING_APPROVAL";
        purchaseRequest.setStatus("WITHDRAWN");
        if (purchaseRequestMapper.updateById(purchaseRequest) <= 0) {
            return Result.error("撤回申请单失败！");
        }

        PurchaseRequestReview purchaseRequestReview = new PurchaseRequestReview();
        purchaseRequestReview.setRequestId(purchaseRequest.getId());
        purchaseRequestReview.setActionType("WITHDRAW");
        purchaseRequestReview.setFromStatus(fromStatus);
        purchaseRequestReview.setToStatus(purchaseRequest.getStatus());
        purchaseRequestReview.setOperateNote("申请人主动撤回");
        purchaseRequestReview.setOperatorId(getCurrentUserId());
        purchaseRequestReview.setOperatorName(getCurrentUsername());
        purchaseRequestReview.setOperateTime(LocalDateTime.now());
        purchaseRequestReviewService.saveReview(purchaseRequestReview);
        return Result.success("撤回申请单成功！");
    }
    @Transactional
    @Override
    public Result approvePurchaseRequest(PurchaseRequestDTO purchaseRequestDTO) {
        if(purchaseRequestDTO.getId() == null){
            return Result.error("采购申请单Id不能为空！");
        }
        PurchaseRequest purchaseRequest = purchaseRequestMapper.findPurchaseRequestById(purchaseRequestDTO.getId());
        if(purchaseRequest == null) {
            return Result.error("采购申请单不存在！");
        }
        if(!"PENDING_APPROVAL".equals(purchaseRequest.getStatus())) {
            return Result.error("采购申请单状态不是审核中！");
        }
        if(purchaseRequestDTO.getReviewNote()== null){
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
        PurchaseRequestReview purchaseRequestReview = new PurchaseRequestReview();
        purchaseRequestReview.setRequestId(purchaseRequest.getId());
        purchaseRequestReview.setActionType("APPROVE");
        purchaseRequestReview.setFromStatus("PENDING_APPROVAL");
        purchaseRequestReview.setToStatus("APPROVED");
        purchaseRequestReview.setOperatorId(purchaseRequest.getReviewUserId());
        purchaseRequestReview.setOperatorName(getCurrentUsername());
        purchaseRequestReview.setOperateNote(purchaseRequestDTO.getReviewNote());
        purchaseRequestReview.setOperateTime(LocalDateTime.now());
        purchaseRequestReviewService.saveReview(purchaseRequestReview);
        return Result.success("审核采购申请单成功");
    }
    @Transactional
    @Override
    public Result rejectPurchaseRequest(PurchaseRequestDTO purchaseRequestDTO) {
        if(purchaseRequestDTO.getId() == null){
            return Result.error("采购申请单Id不能为空！");
        }
        PurchaseRequest purchaseRequest = purchaseRequestMapper.findPurchaseRequestById(purchaseRequestDTO.getId());
        if(purchaseRequest == null) {
            return Result.error("采购申请单不存在！");
        }
        if(!"PENDING_APPROVAL".equals(purchaseRequest.getStatus())) {
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
        PurchaseRequestReview purchaseRequestReview = new PurchaseRequestReview();
        purchaseRequestReview.setRequestId(purchaseRequest.getId());
        purchaseRequestReview.setActionType("REJECT");
        purchaseRequestReview.setFromStatus("PENDING_APPROVAL");
        purchaseRequestReview.setToStatus("REJECTED");
        purchaseRequestReview.setOperatorId(purchaseRequest.getReviewUserId());
        purchaseRequestReview.setOperatorName(getCurrentUsername());
        purchaseRequestReview.setOperateNote(purchaseRequestDTO.getReviewNote());
        purchaseRequestReview.setOperateTime(LocalDateTime.now());
        purchaseRequestReviewService.saveReview(purchaseRequestReview);
        return Result.success("审核采购申请单成功");
    }

    private String generateRequestNo(Long requestId) {
        return "PR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + requestId;
    }
}
