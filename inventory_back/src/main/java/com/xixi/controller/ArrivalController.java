package com.xixi.controller;

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
public class ArrivalController {
    private final ArrivalService arrivalService;
    @GetMapping("/getArrivalPage")
    public Result getArrivalPage(ArrivalQuery  arrivalQuery) {
        IPage<ArrivalVO> iPage = arrivalService.getArrivalPage(arrivalQuery);
        return Result.success(iPage);
    }
    @GetMapping({ "/getArrivalById/{arrivalId}"})
    public Result getArrivalById(@PathVariable("arrivalId") Long arrivalId) {
        ArrivalVO arrivalVO = arrivalService.getArrivalById(arrivalId);
        return Result.success(arrivalVO);
    }
    @PostMapping("/addArrival")
    public Result addArrival(@RequestBody ArrivalDTO arrivalDTO) {
        return arrivalService.addArrival(arrivalDTO);
    }
}
