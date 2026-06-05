package com.xixi.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "RagImportResultVO", description = "RagImportResultVO")
public class RagImportResultVO {
    @Schema(description = "文档编码")
    private String docCode;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "业务意图")
    private String bizIntent;

    @Schema(description = "文档分片数量")
    private Integer chunkCount;

    @Schema(description = "用户消息内容")
    private String message;
}

