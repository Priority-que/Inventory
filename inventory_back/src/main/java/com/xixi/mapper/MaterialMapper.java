package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.entity.Material;
import com.xixi.pojo.query.material.MaterialPageQuery;
import com.xixi.pojo.vo.material.MaterialPageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MaterialMapper extends BaseMapper<Material> {
    IPage<MaterialPageVO> getMaterialPage(IPage<MaterialPageVO> materialPageVoIPage, @Param("q") MaterialPageQuery materialPageQuery);
}
