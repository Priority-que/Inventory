package com.xixi.service.impl;

import com.xixi.annotation.OperLogRecord;
import com.xixi.entity.Supplier;
import com.xixi.entity.SupplierFile;
import com.xixi.mapper.SupplierFileMapper;
import com.xixi.mapper.SupplierMapper;
import com.xixi.pojo.vo.Result;
import com.xixi.service.OssService;
import com.xixi.service.SupplierFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static com.xixi.util.SecurityUtils.getCurrentUserId;
import static com.xixi.util.SecurityUtils.getCurrentUserRoleCodes;

@Service
@RequiredArgsConstructor
public class SupplierFileServiceImpl implements SupplierFileService {
    private final SupplierFileMapper supplierFileMapper;
    private final SupplierMapper supplierMapper;
    private final OssService ossService;

    @Override
    @Transactional
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "供应商附件",
            operationType = "UPLOAD",
            operationDesc = "上传供应商附件",
            bizType = "SUPPLIER"
    )
    public Result uploadSupplierFile(Long supplierId, String fileType, MultipartFile file, String remark) {
        if (supplierId == null) {
            return Result.error("供应商id不能为空！");
        }
        if (fileType == null || fileType.isBlank()) {
            return Result.error("文件类型不能为空！");
        }
        if (!"BUSINESS_LICENSE".equals(fileType)
                && !"QUALIFICATION".equals(fileType)
                && !"BANK_LICENSE".equals(fileType)
                && !"OTHER".equals(fileType)) {
            return Result.error("文件类型不合法！");
        }
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空！");
        }

        Supplier supplier = supplierMapper.getSupplierById(supplierId);
        if (supplier == null) {
            return Result.error("供应商不存在！");
        }
        if (!canManageSupplier(supplier)) {
            return Result.error(403, "只能上传自己的供应商附件");
        }
        if (isPlainSupplier()
                && !"DRAFT".equals(supplier.getStatus())
                && !"REJECTED".equals(supplier.getStatus())) {
            return Result.error("当前供应商状态不允许上传附件");
        }

        Integer maxFileRound = supplierFileMapper.getMaxFileRound(supplierId, fileType);
        Integer newRound = maxFileRound == null ? 1 : maxFileRound + 1;

        String objectKey;
        try {
            objectKey = ossService.upload(file, "supplier");
        } catch (Exception ex) {
            return Result.error("上传OSS失败！");
        }

        supplierFileMapper.disableActiveFile(supplierId, fileType);

        SupplierFile supplierFile = new SupplierFile();
        supplierFile.setSupplierId(supplierId);
        supplierFile.setFileRound(newRound);
        supplierFile.setFileType(fileType);
        supplierFile.setFileName(file.getOriginalFilename());
        supplierFile.setFileUrl(objectKey);
        supplierFile.setFileSize(file.getSize());
        supplierFile.setMimeType(file.getContentType());
        supplierFile.setActiveFlag(1);
        supplierFile.setRemark(remark);
        supplierFile.setUploadTime(LocalDateTime.now());
        supplierFile.setDeleted(0);

        if (supplierFileMapper.insert(supplierFile) <= 0) {
            throw new RuntimeException("上传供应商附件失败！");
        }
        if (supplierMapper.updateFileRound(supplierId, newRound) <= 0) {
            throw new RuntimeException("更新供应商附件轮次失败！");
        }
        return Result.success("上传供应商附件成功！", supplierFile.getId());
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

    private boolean hasCurrentRole(String roleCode) {
        List<String> roleCodes = getCurrentUserRoleCodes();
        return roleCodes != null && roleCodes.contains(roleCode);
    }
}
