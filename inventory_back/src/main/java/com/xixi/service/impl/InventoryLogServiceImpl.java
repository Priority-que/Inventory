package com.xixi.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xixi.mapper.InventoryLogMapper;
import com.xixi.pojo.query.inventory.InventoryLogPageQuery;
import com.xixi.pojo.vo.inventory.InventoryLogPageVO;
import com.xixi.service.InventoryLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryLogServiceImpl implements InventoryLogService {

    private final InventoryLogMapper inventoryLogMapper;

    @Override
    public IPage<InventoryLogPageVO> getInventoryLogPage(InventoryLogPageQuery query) {
        IPage<InventoryLogPageVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        return inventoryLogMapper.getInventoryLogPage(page, query);
    }
}
