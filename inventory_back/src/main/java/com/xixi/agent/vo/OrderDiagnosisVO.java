package com.xixi.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/*
输出订单快照诊断的信息
* */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "OrderDiagnosisVO", description = "OrderDiagnosisVO")
public class OrderDiagnosisVO {
    @Schema(description = "采购订单号")
    private String orderNo;
    /*
    *当前状态
    * */
    @Schema(description = "当前业务阶段")
    private String currentStage;
    /*
    * 出错原因
    * */
    @Schema(description = "阻塞原因")
    private String blockReason;
    /*
    * 订单数据
    * */
    @Schema(description = "判断依据列表")
    private List<String> evidence;
    /*
    *建议者
    * */
    @Schema(description = "建议负责人编码")
    private String suggestOwner;
    /*
    建议行动
    * */
    @Schema(description = "建议处理动作")
    private String suggestAction;
    /*
    * AI总结
    * */
    @Schema(description = "AI分析摘要")
    private String aiSummary;
}

