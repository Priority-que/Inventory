package com.xixi.agent.dto;

import lombok.Data;

@Data
public class RagSearchRequest {
    private String query;

    private String bizIntent;

    private Integer topK;
}
