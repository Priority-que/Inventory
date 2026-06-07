package com.xixi.controller;

import com.xixi.pojo.vo.Result;
import com.xixi.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "Statistics APIs")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "Get statistics summary", operationId = "getStatisticsSummary")
    @GetMapping("/summary")
    public Result getSummary() {
        return Result.success(statisticsService.getSummary());
    }
}
