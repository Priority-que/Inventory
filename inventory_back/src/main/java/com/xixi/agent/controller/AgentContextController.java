package com.xixi.agent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.xixi.agent.service.AgentBusinessContextService;
import com.xixi.pojo.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent/context")
@RequiredArgsConstructor
@Tag(name = "AI业务上下文", description = "AI业务上下文接口")
public class AgentContextController {

    private final AgentBusinessContextService agentBusinessContextService;

    @Operation(summary = "查询订单上下文", operationId = "getOrderContext")
    @GetMapping("/order/{orderNo}")
    public Result getOrderContext(@PathVariable String orderNo) {
        return Result.success(agentBusinessContextService.getOrderContext(orderNo));
    }

    @Operation(summary = "扫描预警上下文", operationId = "scanWarningContext")
    @GetMapping("/warnings")
    public Result scanWarningContext(@RequestParam(required = false) Integer days) {
        return Result.success(agentBusinessContextService.scanWarningContext(days));
    }

    @Operation(summary = "查询供应商上下文", operationId = "getSupplierContext")
    @GetMapping("/supplier/{supplierId}")
    public Result getSupplierContext(
            @PathVariable Long supplierId,
            @RequestParam(required = false) Integer days
    ) {
        return Result.success(agentBusinessContextService.getSupplierContext(supplierId, days));
    }
}

