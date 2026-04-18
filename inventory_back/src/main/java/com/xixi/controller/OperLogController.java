package com.xixi.controller;


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
public class OperLogController {
    private final OperLogService operLogService;

    @GetMapping("/getOperLogPage")
    public Result getOperLogPage(OperLogPageQuery operLogPageQuery){
        IPage<OperLogPageVO> iPage= operLogService.getOperLogPage(operLogPageQuery);
        return Result.success(iPage);
    }

    @GetMapping("/getOperLogById/{id}")
    public Result getOperLogById(@PathVariable  Long id){
        return operLogService.getOperLogById(id);
    }
}
