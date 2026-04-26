package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xixi.entity.InboundItem;
import com.xixi.pojo.vo.inbound.InboundItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InboundItemMapper extends BaseMapper<InboundItem> {
    List<InboundItemVO> getInboundItemByInboundId(@Param("inboundId") Long inboundId);
}
