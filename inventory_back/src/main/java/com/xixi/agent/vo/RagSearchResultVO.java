package com.xixi.agent.vo;

import lombok.Data;

@Data
public class RagSearchResultVO {
    private String id;

    private String docCode;

    private String title;

    private String docType;

    private String bizIntent;

    private String sourcePath;

    private Integer chunkNo;

    private String content;

    private Double score;
}
