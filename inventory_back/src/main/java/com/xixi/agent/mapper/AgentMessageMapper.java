package com.xixi.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xixi.agent.entity.AgentMessage;
import com.xixi.agent.vo.AgentMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentMessageMapper extends BaseMapper<AgentMessage> {
    List<AgentMessageVO> getMessagesByThreadId(@Param("threadId") String threadId);
}
