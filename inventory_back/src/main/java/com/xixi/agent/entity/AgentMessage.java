package com.xixi.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("agent_message")
public class AgentMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private String threadId;

    private String messageRole;

    private String messageType;

    private String content;

    private String nodeName;

    private String toolName;

    private String toolRequestJson;

    private String toolResponseJson;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
