package com.xixi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.xixi.pojo.vo.Result;
import com.xixi.service.SupplierFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/supplierFile")
@RequiredArgsConstructor
@Tag(name = "供应商附件管理", description = "供应商附件管理接口")
public class SupplierFileController {
    private final SupplierFileService supplierFileService;

    @Operation(summary = "上传供应商附件", operationId = "uploadSupplierFile")
    @PostMapping("/uploadSupplierFile")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPLIER')")
    public Result uploadSupplierFile(@RequestParam(value = "supplierId", required = false) Long supplierId,
                                     @RequestParam(value = "fileType", required = false) String fileType,
                                     @RequestParam(value = "remark", required = false) String remark,
                                     @RequestParam(value = "file", required = false)MultipartFile file){
        return supplierFileService.uploadSupplierFile(supplierId,fileType,file,remark);
    }
}

