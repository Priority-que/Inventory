package com.xixi.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "WorkflowAgentResponse", description = "WorkflowAgentResponse")
public class WorkflowAgentResponse {
    @Schema(description = "会话ID")
    private Long sessionId;
    @Schema(description = "会话线程ID")
    private String threadId;
    @Schema(description = "识别到的业务意图")
    private String intent;
    @Schema(description = "AI回答内容")
    private String answer;
    @Schema(description = "当前业务阶段")
    private String currentStage;
    @Schema(description = "风险等级编码")
    private String riskLevel;
    @Schema(description = "建议负责人编码")
    private String suggestOwner;
    @Schema(description = "建议处理动作")
    private String suggestAction;
    @Schema(description = "判断依据列表")
    private List<String> evidence;
    @Schema(description = "响应数据")
    private Object data;
}

