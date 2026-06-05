package com.xixi.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "WarningScanVO", description = "WarningScanVO")
public class WarningScanVO {
    @Schema(description = "汇总信息")
    private String summary;

    @Schema(description = "明细列表")
    private List<WarningItemVO> items;

    @Schema(description = "AI分析摘要")
    private String aiSummary;
}

