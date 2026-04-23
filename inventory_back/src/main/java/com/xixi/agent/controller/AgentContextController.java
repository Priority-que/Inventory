package com.xixi.agent.controller;

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
public class AgentContextController {

    private final AgentBusinessContextService agentBusinessContextService;

    @GetMapping("/order/{orderNo}")
    public Result getOrderContext(@PathVariable String orderNo) {
        return Result.success(agentBusinessContextService.getOrderContext(orderNo));
    }

    @GetMapping("/warnings")
    public Result scanWarningContext(@RequestParam(required = false) Integer days) {
        return Result.success(agentBusinessContextService.scanWarningContext(days));
    }

    @GetMapping("/supplier/{supplierId}")
    public Result getSupplierContext(
            @PathVariable Long supplierId,
            @RequestParam(required = false) Integer days
    ) {
        return Result.success(agentBusinessContextService.getSupplierContext(supplierId, days));
    }
}
