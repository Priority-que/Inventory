package com.xixi.agent.service;

import com.xixi.agent.vo.AgentOrderContextVO;
import com.xixi.agent.vo.AgentSupplierContextVO;
import com.xixi.agent.vo.AgentWarningContextVO;

public interface AgentBusinessContextService {
    AgentOrderContextVO getOrderContext(String orderNo);

    AgentWarningContextVO scanWarningContext(Integer days);

    AgentSupplierContextVO getSupplierContext(Long supplierId, Integer days);
}
