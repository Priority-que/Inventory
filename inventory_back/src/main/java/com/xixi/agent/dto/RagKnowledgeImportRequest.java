package com.xixi.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(name = "RagKnowledgeImportRequest", description = "RagKnowledgeImportRequest")
public class RagKnowledgeImportRequest {
    /*
    * 文档编码，例如 ORDER_STATUS_RULE_V1
    * */
    @Schema(description = "文档编码")
    private String docCode;
    /*
    * 文档标题
    * */
    @Schema(description = "标题")
    private String title;
    /*
    * 文档类型，默认 BUSINESS_RULE
    * */
    @Schema(description = "文档类型")
    private String docType;
    /*
    * 适用意图，COMMON / BUSINESS_TODO / BUSINESS_QA / BUSINESS_KNOWLEDGE_QA
    * */
    @Schema(description = "业务意图")
    private String bizIntent;
    /*
    * 来源路径，例如 document/状态机设计.md
    * */
    @Schema(description = "来源路径")
    private String sourcePath;
    /*
    * 要导入的正文
    * */
    @Schema(description = "内容")
    private String content;
}

