package com.xixi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.inbound.InboundDTO;
import com.xixi.pojo.query.inbound.InboundQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.inbound.InboundVO;
import com.xixi.service.InboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inbound")
@RequiredArgsConstructor
@Tag(name = "入库管理", description = "入库管理接口")
public class InboundController {
    private final InboundService inboundService;

    @Operation(summary = "分页查询入库单", operationId = "getInboundPage")
    @GetMapping("/getInboundPage")
    public Result getInboundPage(InboundQuery inboundQuery) {
        IPage<InboundVO> page = inboundService.getInboundPage(inboundQuery);
        return Result.success(page);
    }

    @Operation(summary = "查询入库单详情", operationId = "getInboundById")
    @GetMapping("/getInboundById/{id}")
    public Result getInboundById(@PathVariable Long id) {
        InboundVO inboundVO = inboundService.getInboundById(id);
        return Result.success(inboundVO);
    }

    @Operation(summary = "新增入库单", operationId = "addInbound")
    @PostMapping("/addInbound")
    public Result addInbound(@RequestBody InboundDTO inboundDTO) {
        return inboundService.addInbound(inboundDTO);
    }

    @Operation(summary = "取消入库单", operationId = "cancelInbound")
    @PutMapping("/cancelInbound")
    public Result cancelInbound(@RequestBody InboundDTO inboundDTO) {
        return inboundService.cancelInbound(inboundDTO);
    }

    @Operation(summary = "确认入库", operationId = "confirmInbound")
    @PutMapping("/confirmInbound")
    public Result confirmInbound(@RequestBody InboundDTO inboundDTO) {
        return inboundService.confirmInbound(inboundDTO);
    }
}

