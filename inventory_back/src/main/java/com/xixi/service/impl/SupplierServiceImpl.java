package com.xixi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xixi.entity.Supplier;
import com.xixi.mapper.SupplierMapper;
import com.xixi.pojo.dto.supplier.SupplierDTO;
import com.xixi.pojo.query.supplier.SupplierPageQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.supplier.SupplierVO;
import com.xixi.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {
    private final SupplierMapper supplierMapper;

    @Override
    public IPage<SupplierVO> getSupplierPage(SupplierPageQuery supplierPageQuery){
        IPage<SupplierVO> supplierVOIPage =new Page<>(supplierPageQuery.getPageNum(),supplierPageQuery.getPageSize());
        IPage<SupplierVO> page = supplierMapper.getSupplierPage(supplierVOIPage,supplierPageQuery);
        return page;
    }
    @Override
    public Result addSupplier(SupplierDTO supplierDTO){
        Supplier supplier= BeanUtil.copyProperties(supplierDTO,Supplier.class);
        if(supplierMapper.insert(supplier)>0){
            return Result.success("添加供应商成功！");
        }
        return  Result.success("添加供应商失败！");
    }

    @Override
    public Result updateSupplier(SupplierDTO supplierDTO) {
        Supplier supplier= BeanUtil.copyProperties(supplierDTO,Supplier.class);
        if(supplierMapper.updateById(supplier)>0){
            return Result.success("修改供应商信息成功！");
        }
        return Result.error("修改供应商信息失败！");
    }


    @Override
    public Result deleteSupplier(List<Integer> ids){
        if(supplierMapper.deleteByIds(ids)>0){
            return Result.success("删除供应商成功！");
        }
        return Result.error("删除供应商失败!");
    }
}
