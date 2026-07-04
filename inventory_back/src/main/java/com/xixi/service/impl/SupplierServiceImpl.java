package com.xixi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xixi.annotation.OperLogRecord;
import com.xixi.entity.Supplier;
import com.xixi.mapper.SupplierFileMapper;
import com.xixi.mapper.SupplierMapper;
import com.xixi.pojo.dto.supplier.SupplierDTO;
import com.xixi.pojo.dto.supplier.SupplierReviewDTO;
import com.xixi.pojo.query.supplier.SupplierPageQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.supplier.SupplierVO;
import com.xixi.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.xixi.util.SecurityUtils.getCurrentUserId;
import static com.xixi.util.SecurityUtils.getCurrentUserRoleCodes;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {
    private final SupplierMapper supplierMapper;
    private final SupplierFileMapper supplierFileMapper;

    @Override
    public IPage<SupplierVO> getSupplierPage(SupplierPageQuery supplierPageQuery) {
        if (isPlainSupplier()) {
            Long currentUserId = getCurrentUserId();
            supplierPageQuery.setUserId(currentUserId == null ? -1L : currentUserId);
        } else if (isPlainPurchaser()) {
            supplierPageQuery.setStatus("ACTIVE");
        }
        IPage<SupplierVO> supplierVOIPage = new Page<>(supplierPageQuery.getPageNum(), supplierPageQuery.getPageSize());
        return supplierMapper.getSupplierPage(supplierVOIPage, supplierPageQuery);
    }

    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "供应商管理",
            operationType = "CREATE",
            operationDesc = "新增供应商",
            bizType = "SUPPLIER"
    )
    public Result addSupplier(SupplierDTO supplierDTO) {
        if (!hasCurrentRole("ADMIN")) {
            return Result.error(403, "只有管理员可以新增供应商");
        }
        if (supplierDTO.getUserId() == null) {
            return Result.error("绑定用户不能为空");
        }
        if (isBlank(supplierDTO.getCode())) {
            return Result.error("供应商编码不能为空");
        }
        if (isBlank(supplierDTO.getName())) {
            return Result.error("供应商名称不能为空");
        }
        Supplier supplier = BeanUtil.copyProperties(supplierDTO, Supplier.class);
        supplier.setStatus("DRAFT");
        supplier.setFileRound(1);
        if (supplierMapper.insert(supplier) > 0) {
            supplierDTO.setId(supplier.getId());
            return Result.success("添加供应商成功！");
        }
        return Result.error("添加供应商失败！");
    }

    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "供应商管理",
            operationType = "UPDATE",
            operationDesc = "修改供应商",
            bizType = "SUPPLIER"
    )
    public Result updateSupplier(SupplierDTO supplierDTO) {
        if (supplierDTO.getId() == null) {
            return Result.error("供应商ID不能为空");
        }
        Supplier oldSupplier = supplierMapper.getSupplierById(supplierDTO.getId());
        if (oldSupplier == null) {
            return Result.error("供应商不存在");
        }
        if (!canManageSupplier(oldSupplier)) {
            return Result.error(403, "只能维护自己的供应商资料");
        }
        if (isPlainSupplier()
                && !"DRAFT".equals(oldSupplier.getStatus())
                && !"REJECTED".equals(oldSupplier.getStatus())) {
            return Result.error("当前供应商状态不允许修改资料");
        }

        Supplier supplier = new Supplier();
        supplier.setId(oldSupplier.getId());
        if (hasCurrentRole("ADMIN")) {
            supplier.setUserId(supplierDTO.getUserId());
            supplier.setCode(supplierDTO.getCode());
        }
        supplier.setName(supplierDTO.getName());
        supplier.setContactName(supplierDTO.getContactName());
        supplier.setContactPhone(supplierDTO.getContactPhone());
        supplier.setEmail(supplierDTO.getEmail());
        supplier.setAddress(supplierDTO.getAddress());
        supplier.setLicenseNo(supplierDTO.getLicenseNo());
        supplier.setRemark(supplierDTO.getRemark());
        if (supplierMapper.updateById(supplier) > 0) {
            return Result.success("修改供应商信息成功！");
        }
        return Result.error("修改供应商信息失败！");
    }

    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "供应商管理",
            operationType = "DELETE",
            operationDesc = "删除供应商",
            bizType = "SUPPLIER"
    )
    public Result deleteSupplier(List<Integer> ids) {
        if (!hasCurrentRole("ADMIN")) {
            return Result.error(403, "只有管理员可以删除供应商");
        }
        if (ids == null || ids.isEmpty()) {
            return Result.error("供应商ID不能为空");
        }
        if (supplierMapper.deleteByIds(ids) > 0) {
            return Result.success("删除供应商成功！");
        }
        return Result.error("删除供应商失败！");
    }

    @Override
    @Transactional
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "供应商资质",
            operationType = "SUBMIT",
            operationDesc = "提交供应商资质审核",
            bizType = "SUPPLIER"
    )
    public Result submitSupplierReview(SupplierReviewDTO supplierReviewDTO) {
        Supplier supplier = getSupplierForOperate(supplierReviewDTO);
        if (supplier == null) {
            return Result.error("供应商不存在");
        }
        if (!canManageSupplier(supplier)) {
            return Result.error(403, "只能提交自己的供应商资质");
        }
        if (!"DRAFT".equals(supplier.getStatus()) && !"REJECTED".equals(supplier.getStatus())) {
            return Result.error("只有草稿或已驳回状态允许提交审核");
        }
        Integer activeFileCount = supplierFileMapper.countActiveFile(supplier.getId());
        if (activeFileCount == null || activeFileCount <= 0) {
            return Result.error("请先上传供应商资质附件");
        }
        Supplier update = new Supplier();
        if (supplierMapper.submitReview(supplier.getId(), LocalDateTime.now()) > 0) {
            return Result.success("提交供应商资质审核成功");
        }
        return Result.error("提交供应商资质审核失败");
    }

    @Override
    @Transactional
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "供应商资质",
            operationType = "APPROVE",
            operationDesc = "审核通过供应商资质",
            bizType = "SUPPLIER"
    )
    public Result approveSupplier(SupplierReviewDTO supplierReviewDTO) {
        return reviewSupplier(supplierReviewDTO, "ACTIVE", "审核通过");
    }

    @Override
    @Transactional
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "供应商资质",
            operationType = "REJECT",
            operationDesc = "驳回供应商资质",
            bizType = "SUPPLIER"
    )
    public Result rejectSupplier(SupplierReviewDTO supplierReviewDTO) {
        if (supplierReviewDTO == null || isBlank(supplierReviewDTO.getReviewNote())) {
            return Result.error("驳回原因不能为空");
        }
        return reviewSupplier(supplierReviewDTO, "REJECTED", supplierReviewDTO.getReviewNote());
    }

    @Override
    @Transactional
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "供应商管理",
            operationType = "DISABLE",
            operationDesc = "停用供应商",
            bizType = "SUPPLIER"
    )
    public Result disableSupplier(SupplierReviewDTO supplierReviewDTO) {
        if (!hasCurrentRole("ADMIN")) {
            return Result.error(403, "只有管理员可以停用供应商");
        }
        Supplier supplier = getSupplierForOperate(supplierReviewDTO);
        if (supplier == null) {
            return Result.error("供应商不存在");
        }
        if ("DISABLED".equals(supplier.getStatus())) {
            return Result.error("供应商已停用");
        }
        if (supplierMapper.updateReviewStatus(
                supplier.getId(),
                "DISABLED",
                LocalDateTime.now(),
                getCurrentUserId(),
                defaultText(supplierReviewDTO.getReviewNote(), "管理员停用供应商")) > 0) {
            return Result.success("停用供应商成功");
        }
        return Result.error("停用供应商失败");
    }

    private Result reviewSupplier(SupplierReviewDTO supplierReviewDTO, String nextStatus, String defaultNote) {
        if (!hasCurrentRole("ADMIN")) {
            return Result.error(403, "只有管理员可以审核供应商");
        }
        Supplier supplier = getSupplierForOperate(supplierReviewDTO);
        if (supplier == null) {
            return Result.error("供应商不存在");
        }
        if (!"PENDING".equals(supplier.getStatus())) {
            return Result.error("只有待审核状态允许审核");
        }
        if (supplierMapper.updateReviewStatus(
                supplier.getId(),
                nextStatus,
                LocalDateTime.now(),
                getCurrentUserId(),
                defaultText(supplierReviewDTO.getReviewNote(), defaultNote)) > 0) {
            return Result.success("供应商审核操作成功");
        }
        return Result.error("供应商审核操作失败");
    }

    private Supplier getSupplierForOperate(SupplierReviewDTO supplierReviewDTO) {
        if (supplierReviewDTO == null || supplierReviewDTO.getId() == null) {
            return null;
        }
        return supplierMapper.getSupplierById(supplierReviewDTO.getId());
    }

    private boolean canManageSupplier(Supplier supplier) {
        if (hasCurrentRole("ADMIN")) {
            return true;
        }
        if (!hasCurrentRole("SUPPLIER")) {
            return false;
        }
        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(supplier.getUserId());
    }

    private boolean isPlainSupplier() {
        List<String> roleCodes = getCurrentUserRoleCodes();
        return roleCodes != null
                && roleCodes.contains("SUPPLIER")
                && !roleCodes.contains("ADMIN");
    }

    private boolean isPlainPurchaser() {
        List<String> roleCodes = getCurrentUserRoleCodes();
        return roleCodes != null
                && roleCodes.contains("PURCHASER")
                && !roleCodes.contains("ADMIN");
    }

    private boolean hasCurrentRole(String roleCode) {
        List<String> roleCodes = getCurrentUserRoleCodes();
        return roleCodes != null && roleCodes.contains(roleCode);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String defaultText(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }
}
