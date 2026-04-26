package com.xixi.agent.dto;

import lombok.Data;

@Data
public class RagKnowledgeImportRequest {
    /*
    * 文档编码，例如 ORDER_STATUS_RULE_V1
    * */
    private String docCode;
    /*
    * 文档标题
    * */
    private String title;
    /*
    * 文档类型，默认 BUSINESS_RULE
    * */
    private String docType;
    /*
    * 适用意图，COMMON / ORDER_DIAGNOSIS / WARNING_SCAN / SUPPLIER_SCORE / KNOWLEDGE_QA
    * */
    private String bizIntent;
    /*
    * 来源路径，例如 document/状态机设计.md
    * */
    private String sourcePath;
    /*
    * 要导入的正文
    * */
    private String content;
}
