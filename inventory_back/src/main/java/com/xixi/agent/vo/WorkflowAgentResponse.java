package com.xixi.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowAgentResponse {
    private Long sessionId;
    private String threadId;
    private String intent;
    private String answer;
    private String currentStage;
    private String riskLevel;
    private String suggestOwner;
    private String suggestAction;
    private List<String> evidence;
    private Object data;
}
