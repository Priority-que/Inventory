package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xixi.entity.SupplierFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SupplierFileMapper extends BaseMapper<SupplierFile> {

    Integer getMaxFileRound(@Param("supplierId") Long supplierId,@Param("fileType")String fileType);

    Integer disableActiveFile(@Param("supplierId")Long supplierId,@Param("fileType")String fileType);
}
