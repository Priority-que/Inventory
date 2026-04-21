package com.xixi.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xixi.agent.entity.AgentSession;
import com.xixi.agent.vo.AgentSessionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AgentSessionMapper extends BaseMapper<AgentSession> {

    @Select("select * from agent_session where thread_id = #{threadId} and deleted = 0")
    AgentSession getByThreadId(@Param("threadId") String threadId);

    @Select("""
            select * from agent_session
            where thread_id = #{threadId}
              and user_id = #{userId}
              and deleted = 0
            """)
    AgentSession getByThreadIdAndUserId(@Param("threadId") String threadId, @Param("userId") Long userId);

    List<AgentSessionVO> getSessionListByUserId(@Param("userId") Long userId);

    @Update("""
            update agent_session
            set current_intent = #{currentIntent},
                last_message_time = #{lastMessageTime}
            where id = #{id}
              and deleted = 0
            """)
    int updateRuntimeInfo(@Param("id") Long id,
                          @Param("currentIntent") String currentIntent,
                          @Param("lastMessageTime") LocalDateTime lastMessageTime);
}
