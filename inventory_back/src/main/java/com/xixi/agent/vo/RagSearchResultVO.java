package com.xixi.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "RagSearchResultVO", description = "RagSearchResultVO")
public class RagSearchResultVO {
    @Schema(description = "RAG知识库主键ID")
    private String id;

    @Schema(description = "文档编码")
    private String docCode;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "文档类型")
    private String docType;

    @Schema(description = "业务意图")
    private String bizIntent;

    @Schema(description = "来源路径")
    private String sourcePath;

    @Schema(description = "文档分片序号")
    private Integer chunkNo;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "评分信息")
    private Double score;
}

