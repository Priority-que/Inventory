package com.xixi.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xixi.mapper.InventoryMapper;
import com.xixi.pojo.query.inventory.InventoryPageQuery;
import com.xixi.pojo.vo.inventory.InventoryPageVO;
import com.xixi.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;

    @Override
    public IPage<InventoryPageVO> getInventoryPage(InventoryPageQuery inventoryPageQuery) {
        IPage<InventoryPageVO> page = new Page<>(
                inventoryPageQuery.getPageNum(),
                inventoryPageQuery.getPageSize()
        );
        return inventoryMapper.getInventoryPage(page, inventoryPageQuery);
    }
}