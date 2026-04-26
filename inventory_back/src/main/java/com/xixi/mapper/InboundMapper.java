package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.entity.Inbound;
import com.xixi.pojo.query.inbound.InboundQuery;
import com.xixi.pojo.vo.inbound.InboundVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface InboundMapper extends BaseMapper<Inbound> {
    IPage<InboundVO> getInboundPage(IPage<InboundVO> page, @Param("q") InboundQuery inboundQuery);

    InboundVO getInboundById(Long id);

    Long lockById(Long id);

    Integer countByArrivalId(Long arrivalId);

    int completeInbound(@Param("id") Long id, @Param("inboundTime") LocalDateTime inboundTime);
}
