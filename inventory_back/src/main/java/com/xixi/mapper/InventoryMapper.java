package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.entity.Inventory;
import com.xixi.pojo.query.inventory.InventoryPageQuery;
import com.xixi.pojo.vo.inventory.InventoryPageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    IPage<InventoryPageVO> getInventoryPage(
            IPage<InventoryPageVO> page,
            @Param("q") InventoryPageQuery query
    );

    Inventory lockByMaterialAndWarehouse(@Param("materialId") Long materialId,
                                         @Param("warehouseId") Long warehouseId);

    int increaseCurrentNumber(@Param("id") Long id,
                              @Param("changeNumber") BigDecimal changeNumber,
                              @Param("lastInboundTime") LocalDateTime lastInboundTime);
}
