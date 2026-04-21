package com.xixi.agent.service;

import com.xixi.agent.dto.WarningScanRequest;
import com.xixi.agent.vo.WarningScanVO;

public interface ProcurementWarningAgentService {
    WarningScanVO scanWarnings(WarningScanRequest request);
}
