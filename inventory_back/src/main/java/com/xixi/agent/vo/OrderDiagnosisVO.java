package com.xixi.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/*
输出订单快照诊断的信息
* */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDiagnosisVO {
    private String orderNo;
    /*
    *当前状态
    * */
    private String currentStage;
    /*
    * 出错原因
    * */
    private String blockReason;
    /*
    * 订单数据
    * */
    private List<String> evidence;
    /*
    *建议者
    * */
    private String suggestOwner;
    /*
    建议行动
    * */
    private String suggestAction;
    /*
    * AI总结
    * */
    private String aiSummary;
}
