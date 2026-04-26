package com.xixi.agent.vo;

import lombok.Data;

@Data
public class RagImportResultVO {
    private String docCode;

    private String title;

    private String bizIntent;

    private Integer chunkCount;

    private String message;
}
