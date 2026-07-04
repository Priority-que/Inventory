package com.xixi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.query.inventory.InventoryLogPageQuery;
import com.xixi.pojo.vo.inventory.InventoryLogPageVO;

public interface InventoryLogService {

    IPage<InventoryLogPageVO> getInventoryLogPage(InventoryLogPageQuery query);
}
