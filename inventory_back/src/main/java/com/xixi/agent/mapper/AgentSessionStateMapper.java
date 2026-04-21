package com.xixi.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xixi.agent.entity.AgentSessionState;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AgentSessionStateMapper extends BaseMapper<AgentSessionState> {

    @Select("select * from agent_session_state where thread_id = #{threadId} and deleted = 0")
    AgentSessionState getByThreadId(@Param("threadId") String threadId);
}
