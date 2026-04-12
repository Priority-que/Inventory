package com.xixi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.supplier.SupplierDTO;
import com.xixi.pojo.query.supplier.SupplierPageQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.supplier.SupplierVO;

import java.util.List;

public interface SupplierService {
    IPage<SupplierVO> getSupplierPage(SupplierPageQuery supplierPageQuery);

    Result addSupplier(SupplierDTO supplierDTO);

    Result updateSupplier(SupplierDTO supplierDTO);

    Result deleteSupplier(List<Integer> ids);
}
