package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.entity.InventoryLog;
import com.xixi.pojo.query.inventory.InventoryLogPageQuery;
import com.xixi.pojo.vo.inventory.InventoryLogPageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InventoryLogMapper extends BaseMapper<InventoryLog> {

    IPage<InventoryLogPageVO> getInventoryLogPage(
            IPage<InventoryLogPageVO> page,
            @Param("q") InventoryLogPageQuery query
    );
}
