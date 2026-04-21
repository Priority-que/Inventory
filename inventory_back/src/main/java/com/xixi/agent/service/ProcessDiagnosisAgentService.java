package com.xixi.agent.service;

import com.xixi.agent.dto.OrderDiagnosisRequest;
import com.xixi.agent.vo.OrderDiagnosisVO;
import com.xixi.agent.vo.OrderSnapshotVO;

public interface ProcessDiagnosisAgentService {
  OrderDiagnosisVO diagnose(OrderDiagnosisRequest request);
  OrderDiagnosisVO diagnoseRule(OrderSnapshotVO snapshot);
}

