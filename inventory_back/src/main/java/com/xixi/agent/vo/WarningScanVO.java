package com.xixi.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WarningScanVO {
    private String summary;

    private List<WarningItemVO> items;

    private String aiSummary;
}
