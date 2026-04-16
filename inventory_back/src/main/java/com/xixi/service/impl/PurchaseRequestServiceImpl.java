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
        PurchaseRequest purchaseRequest = BeanUtil.copyProperties(purchaseRequestDTO, PurchaseRequest.class);
        String requestNo = generateRequestNo(purchaseRequest.getApplicantId());
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
        if (purchaseRequestDTO.getId() == null) {
            return Result.error("申请主表Id不能为空");
        }
        PurchaseRequest purchaseRequest = purchaseRequestMapper.findPurchaseRequestById(purchaseRequestDTO.getId());
        if (purchaseRequest == null) {
            return Result.error("申请单不存在");
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
        // TODO 后续这里实现登录认证信息之后改成当前用户信息
        purchaseRequestReview.setOperatorId(purchaseRequest.getApplicantId());
        purchaseRequestReview.setOperatorName("坤坤");
        purchaseRequestReview.setOperateTime(LocalDateTime.now());
        purchaseRequestReviewService.saveReview(purchaseRequestReview);
        return Result.success("提交申请单成功！");
    }

    @Transactional
    @Override
    public Result withdrawPurchaseRequest(PurchaseRequestDTO purchaseRequestDTO) {
        if (purchaseRequestDTO.getId() == null) {
            return Result.error("申请主表Id不能为空");
        }
        PurchaseRequest purchaseRequest = purchaseRequestMapper.findPurchaseRequestById(purchaseRequestDTO.getId());
        if (purchaseRequest == null) {
            return Result.error("申请单不存在");
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
        // TODO 后续这里实现登录认证信息之后改成当前用户信息
        purchaseRequestReview.setOperatorId(purchaseRequest.getApplicantId());
        purchaseRequestReview.setOperatorName("坤坤");
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
        //TODO 审核ID还没有完善，后续登录认证完成再开发
        purchaseRequest.setReviewUserId(11L);
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
        //TODO 审核ID还没有完善，后续登录认证完成再开发
        purchaseRequestReview.setOperatorId(purchaseRequest.getReviewUserId());
        purchaseRequestReview.setOperatorName("鸡哥");
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
        //TODO 审核ID还没有完善，后续登录认证完成再开发
        purchaseRequest.setReviewUserId(11L);
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
        //TODO 审核ID还没有完善，后续登录认证完成再开发
        purchaseRequestReview.setOperatorId(purchaseRequest.getReviewUserId());
        purchaseRequestReview.setOperatorName("鸡哥");
        purchaseRequestReview.setOperateNote(purchaseRequestDTO.getReviewNote());
        purchaseRequestReview.setOperateTime(LocalDateTime.now());
        purchaseRequestReviewService.saveReview(purchaseRequestReview);
        return Result.success("审核采购申请单成功");
    }

    private String generateRequestNo(Long requestId) {
        return "PR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + requestId;
    }
}
