package com.xixi.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "RagSearchRequest", description = "RagSearchRequest")
public class RagSearchRequest {
    @Schema(description = "检索关键词")
    private String query;

    @Schema(description = "业务意图")
    private String bizIntent;

    @Schema(description = "返回条数")
    private Integer topK;
}

