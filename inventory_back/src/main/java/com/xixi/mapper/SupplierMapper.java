package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.entity.Supplier;
import com.xixi.pojo.query.supplier.SupplierPageQuery;
import com.xixi.pojo.vo.supplier.SupplierVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SupplierMapper extends BaseMapper<Supplier>{

    public IPage<SupplierVO> getSupplierPage(IPage<SupplierVO> supplierPageVOIPage, @Param("q") SupplierPageQuery supplierPageQuery);
}
