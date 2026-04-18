package com.xixi.controller;

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
public class InboundController {
    private final InboundService inboundService;

    @GetMapping("/getInboundPage")
    public Result getInboundPage(InboundQuery inboundQuery) {
        IPage<InboundVO> page = inboundService.getInboundPage(inboundQuery);
        return Result.success(page);
    }

    @GetMapping("/getInboundById/{id}")
    public Result getInboundById(@PathVariable Long id) {
        InboundVO inboundVO = inboundService.getInboundById(id);
        return Result.success(inboundVO);
    }

    @PostMapping("/addInbound")
    public Result addInbound(@RequestBody InboundDTO inboundDTO) {
        return inboundService.addInbound(inboundDTO);
    }

    @PutMapping("/cancelInbound")
    public Result cancelInbound(@RequestBody InboundDTO inboundDTO) {
        return inboundService.cancelInbound(inboundDTO);
    }

    @PutMapping("/confirmInbound")
    public Result confirmInbound(@RequestBody InboundDTO inboundDTO) {
        return inboundService.confirmInbound(inboundDTO);
    }
}
