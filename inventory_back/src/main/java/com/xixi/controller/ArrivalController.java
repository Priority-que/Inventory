package com.xixi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.arrival.ArrivalDTO;
import com.xixi.pojo.query.arrival.ArrivalQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.arrival.ArrivalVO;
import com.xixi.service.ArrivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/arrival")
@RequiredArgsConstructor
@Tag(name = "到货登记管理", description = "到货登记管理接口")
public class ArrivalController {
    private final ArrivalService arrivalService;
    @Operation(summary = "分页查询到货登记", operationId = "getArrivalPage")
    @GetMapping("/getArrivalPage")
    public Result getArrivalPage(ArrivalQuery  arrivalQuery) {
        IPage<ArrivalVO> iPage = arrivalService.getArrivalPage(arrivalQuery);
        return Result.success(iPage);
    }
    @Operation(summary = "查询到货登记详情", operationId = "getArrivalById")
    @GetMapping({ "/getArrivalById/{arrivalId}"})
    public Result getArrivalById(@PathVariable("arrivalId") Long arrivalId) {
        ArrivalVO arrivalVO = arrivalService.getArrivalById(arrivalId);
        return Result.success(arrivalVO);
    }
    @Operation(summary = "新增到货登记", operationId = "addArrival")
    @PostMapping("/addArrival")
    public Result addArrival(@RequestBody ArrivalDTO arrivalDTO) {
        return arrivalService.addArrival(arrivalDTO);
    }
}

