package com.xixi.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.query.material.MaterialPageQuery;
import com.xixi.pojo.query.supplier.SupplierPageQuery;
import com.xixi.pojo.query.warehouse.WarehousePageQuery;
import com.xixi.pojo.vo.material.MaterialPageVO;
import com.xixi.pojo.vo.supplier.SupplierVO;
import com.xixi.pojo.vo.warehouse.WarehouseVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public class SupplierMapper {

    IPage<SupplierVO> getSupplierPage(IPage<SupplierVO> supplierVOIPage,@Param("q") SupplierPageQuery supplierPageQuery);
}
