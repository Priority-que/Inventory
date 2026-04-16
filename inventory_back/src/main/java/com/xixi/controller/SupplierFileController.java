package com.xixi.controller;


import com.xixi.pojo.vo.Result;
import com.xixi.service.SupplierFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/supplierFile")
@RequiredArgsConstructor
public class SupplierFileController {
    private final SupplierFileService supplierFileService;

    @PostMapping("/uploadSupplierFile")
    public Result uploadSupplierFile(@RequestParam(value = "supplierId", required = false) Long supplierId,
                                     @RequestParam(value = "fileType", required = false) String fileType,
                                     @RequestParam(value = "remark", required = false) String remark,
                                     @RequestParam(value = "file", required = false)MultipartFile file){
        return supplierFileService.uploadSupplierFile(supplierId,fileType,file,remark);
    }
}
