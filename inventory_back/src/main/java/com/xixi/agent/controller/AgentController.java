package com.xixi.agent.controller;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.xixi.agent.dto.*;
import com.xixi.agent.service.ProcessDiagnosisAgentService;
import com.xixi.agent.service.PythonWorkflowProxyService;
import com.xixi.agent.service.ProcurementWarningAgentService;
import com.xixi.agent.service.SupplierPerformanceAgentService;
import com.xixi.pojo.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {
    private final ReactAgent inventoryChatAgent;
    private final ProcessDiagnosisAgentService processDiagnosisAgentService;
    private final ProcurementWarningAgentService procurementWarningAgentService;
    private final SupplierPerformanceAgentService supplierPerformanceAgentService;
    private final PythonWorkflowProxyService pythonWorkflowProxyService;

    @PostMapping("/chat")
    public Result chat(@RequestBody AgentChatRequest agentChatRequest) throws GraphRunnerException {
        String threadId = agentChatRequest.getThreadId();
        if(threadId == null || threadId.isEmpty()){
            threadId = "default - thread";
        }
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();
        AssistantMessage response = inventoryChatAgent.call(agentChatRequest.getMessage(),config);
        return Result.success(response.getText());
    }
    @PostMapping("/diagnose/order")
    public Result diagnoseOrder(@RequestBody OrderDiagnosisRequest request) {
        return Result.success(processDiagnosisAgentService.diagnose(request));
    }
    @PostMapping("/warning/scan")
    public Result scanWarnings(@RequestBody WarningScanRequest request) {
        return Result.success(procurementWarningAgentService.scanWarnings(request));
    }
    @PostMapping("/supplier/score")
    public Result scoreSupplier(@RequestBody SupplierScoreRequest request) {
        return Result.success(supplierPerformanceAgentService.scoreSupplier(request));
    }
    @PostMapping("/workflow/execute")
    public ResponseEntity<String> execute(@RequestBody WorkflowAgentRequest request,
                                          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                          String authorization) {
        return pythonWorkflowProxyService.executeWorkflow(request, authorization);
    }
}
