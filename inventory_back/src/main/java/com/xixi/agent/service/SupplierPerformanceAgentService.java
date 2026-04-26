package com.xixi.agent.service;

import com.xixi.agent.dto.SupplierScoreRequest;
import com.xixi.agent.vo.SupplierScoreVO;

public interface SupplierPerformanceAgentService {
    SupplierScoreVO scoreSupplier(SupplierScoreRequest request);
}
