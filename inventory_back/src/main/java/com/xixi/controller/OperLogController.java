package com.xixi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.query.log.OperLogPageQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.log.OperLogPageVO;
import com.xixi.service.OperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
@Tag(name = "操作日志管理", description = "操作日志管理接口")
public class OperLogController {
    private final OperLogService operLogService;

    @Operation(summary = "分页查询操作日志", operationId = "getOperLogPage")
    @GetMapping("/getOperLogPage")
    public Result getOperLogPage(OperLogPageQuery operLogPageQuery){
        IPage<OperLogPageVO> iPage= operLogService.getOperLogPage(operLogPageQuery);
        return Result.success(iPage);
    }

    @Operation(summary = "查询操作日志详情", operationId = "getOperLogById")
    @GetMapping("/getOperLogById/{id}")
    public Result getOperLogById(@PathVariable  Long id){
        return operLogService.getOperLogById(id);
    }
}

