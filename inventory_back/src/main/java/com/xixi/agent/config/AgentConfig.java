package com.xixi.agent.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.xixi.agent.dto.PurchaseOrderSnapshotRequest;
import com.xixi.agent.dto.SupplierScoreRequest;
import com.xixi.agent.dto.WarningScanRequest;
import com.xixi.agent.tool.ProcurementWarningSnapshotTool;
import com.xixi.agent.tool.PurchaseOrderSnapshotTool;
import com.xixi.agent.tool.SupplierPerformanceSnapshotTool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {
    @Bean
    public ReactAgent inventoryChatAgent(ChatModel chatModel) {
        return ReactAgent.builder()
                .name("inventory_chat_agent")
                .model(chatModel)
                .systemPrompt("你是供应商协同采购入库系统的智能助手。\n" +
                        "    当前阶段只允许回答系统相关问题，不允许编造业务数据。\n" +
                        "     如果用户询问具体订单、库存、供应商信息，但你没有工具数据，请说明需要接入业务工具。")
                .saver(new MemorySaver())
                .build();
    }
    @Bean
    public ReactAgent processDiagnosisAgent(ChatModel chatModel, PurchaseOrderSnapshotTool tool) {
        ToolCallback orderSnapshotTool = FunctionToolCallback
                .builder("getPurchaseOrderSnapshot",tool)
                .description("根据采购订单号查询采购订单执行快照，包括订单状态、采购总数量、已到货数量、已入库数量、到货次数、入库次数")
                .inputType(PurchaseOrderSnapshotRequest.class)
                .build();
        ModelCallLimitHook limitHook = ModelCallLimitHook.builder()
                .runLimit(5)
                .exitBehavior(ModelCallLimitHook.ExitBehavior.ERROR)
                .build();
        return ReactAgent.builder()
                .name("process_diagnosis_agent")
                .model(chatModel)
                .systemPrompt("""
                        你是供应商协同采购入库系统的流程阻塞诊断助手。
                        你必须先调用工具 getPurchaseOrderSnapshot 查询订单执行快照，再进行回答。
                        你只能基于工具返回的数据进行分析，不允许编造订单、数量、供应商、仓库信息。
                        你的任务是解释采购订单当前卡在哪个环节、为什么卡住、下一步应该由哪个角色处理。
                        输出内容必须包含：当前阶段、阻塞原因、证据、建议处理角色、建议动作。
                        如果工具返回采购订单不存在，请直接说明订单不存在。
                        """)
                .tools(orderSnapshotTool)
                .hooks(limitHook)
                .saver(new MemorySaver())
                .build();
    }
    @Bean
    public ReactAgent procurementWarningAgent(ChatModel chatModel,
                                              ProcurementWarningSnapshotTool tool) {
        ToolCallback warningTool = FunctionToolCallback
                .builder("procurementWarningSnapshotTool", tool)
                .description("查询采购执行预警扫描结果，包括待确认超时、执行中无到货、部分到货停滞、已到货未入库、待确认入库超时等风险")
                .inputType(WarningScanRequest.class)
                .build();

        ModelCallLimitHook limitHook = ModelCallLimitHook.builder()
                .runLimit(5)
                .exitBehavior(ModelCallLimitHook.ExitBehavior.ERROR)
                .build();

        return ReactAgent.builder()
                .name("procurement_warning_agent")
                .model(chatModel)
                .systemPrompt("""
                    你是采购执行预警助手。
                    你必须先调用工具 procurementWarningSnapshotTool 获取结构化预警数据，再生成总结。
                    你不能编造风险，不允许新增不存在的订单、到货单、入库单。
                    输出应聚焦：风险数量、风险等级、优先处理建议、建议处理角色。
                    """)
                .tools(warningTool)
                .hooks(limitHook)
                .saver(new MemorySaver())
                .build();
    }
    @Bean
    public ReactAgent supplierPerformanceAgent(ChatModel chatModel,
                                               SupplierPerformanceSnapshotTool tool) {
        ToolCallback supplierTool = FunctionToolCallback
                .builder("supplierPerformanceSnapshotTool", tool)
                .description("根据供应商ID和统计周期查询供应商履约指标，包括订单数量、完成数量、取消数量、异常到货数量等")
                .inputType(SupplierScoreRequest.class)
                .build();

        ModelCallLimitHook limitHook = ModelCallLimitHook.builder()
                .runLimit(5)
                .exitBehavior(ModelCallLimitHook.ExitBehavior.ERROR)
                .build();

        return ReactAgent.builder()
                .name("supplier_performance_agent")
                .model(chatModel)
                .systemPrompt("""
                    你是供应商履约评分助手。
                    你必须先调用工具 supplierPerformanceSnapshotTool 获取供应商履约指标，再生成分析。
                    你不能编造订单数量、异常数量、评分结果。
                    输出需要聚焦：总体评价、优势、风险、合作建议。
                    """)
                .tools(supplierTool)
                .hooks(limitHook)
                .saver(new MemorySaver())
                .build();
    }
}
