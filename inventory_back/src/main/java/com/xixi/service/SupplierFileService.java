package com.xixi.service;

import com.xixi.pojo.vo.Result;
import org.springframework.web.multipart.MultipartFile;

public interface SupplierFileService {

    Result uploadSupplierFile(Long supplierId, String fileType, MultipartFile file, String remark);
}
