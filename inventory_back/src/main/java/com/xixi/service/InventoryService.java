package com.xixi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.query.inventory.InventoryPageQuery;
import com.xixi.pojo.vo.inventory.InventoryPageVO;

public interface InventoryService {

    IPage<InventoryPageVO> getInventoryPage(InventoryPageQuery inventoryPageQuery);
}