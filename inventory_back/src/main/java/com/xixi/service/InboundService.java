package com.xixi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.inbound.InboundDTO;
import com.xixi.pojo.query.inbound.InboundQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.inbound.InboundVO;

public interface InboundService {
    IPage<InboundVO> getInboundPage(InboundQuery inboundQuery);

    InboundVO getInboundById(Long id);

    Result addInbound(InboundDTO inboundDTO);

    Result cancelInbound(InboundDTO inboundDTO);

    Result confirmInbound(InboundDTO inboundDTO);
}
