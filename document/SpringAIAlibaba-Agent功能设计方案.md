# Spring AI Alibaba Agent 功能设计方案

## 1. 文档目标

本文档用于规划在当前“供应商协同采购入库系统”中接入 Spring AI Alibaba Agent Framework 的完整落地流程。

目标不是把系统改成一个普通聊天机器人，而是让 AI Agent 能够基于采购、到货、入库、库存、供应商等业务数据，完成以下三类能力：

1. 采购执行预警 Agent
2. 流程阻塞诊断 Agent
3. 供应商履约评分 Agent

整体原则：

- 业务数据由后端 Service / Mapper 查询，不让大模型直接写 SQL。
- 业务判断由 Java 规则和状态机完成，不完全交给大模型自由判断。
- 大模型主要负责总结、解释、归纳、生成建议。
- 第一版 Agent 只读，不允许直接创建订单、确认入库、修改库存。

## 2. 推荐技术路线

当前项目是 Spring Boot 后端，因此建议直接走 Java 体系：

```text
Spring Boot 业务系统
    ↓
Spring AI Alibaba Agent Framework
    ↓
Agent Tool 调用现有业务 Service / Mapper
    ↓
Java 规则计算风险、阻塞点、评分
    ↓
LLM 生成自然语言解释和建议
    ↓
Controller 返回给前端或 Apifox
```

不建议第一版引入：

- Python Agent 服务
- 代码沙箱
- 让模型执行任意 SQL
- 让模型自动改数据库
- 复杂多 Agent 自主协作
- 大规模向量数据库

推荐第一版使用：

- Spring AI Alibaba Agent Framework
- DashScope 模型接入
- Tool Calling
- ReactAgent 或简单 Assistant Agent
- 少量 Memory
- Java 规则引擎式判断
- 后续再扩展 Workflow / Graph

官方参考：

- [Spring AI Alibaba Quick Start](https://java2ai.com/en/docs/quick-start/)
- [Spring AI Alibaba Agents](https://java2ai.com/en/docs/frameworks/agent-framework/tutorials/agents)
- [Spring AI Alibaba Tools](https://java2ai.com/en/docs/frameworks/agent-framework/tutorials/tools)
- [Spring AI Alibaba Workflow](https://java2ai.com/en/docs/frameworks/agent-framework/advanced/workflow/)
- [Spring AI Alibaba GitHub](https://github.com/alibaba/spring-ai-alibaba)

## 3. 推荐包结构

建议在后端新增一个 `agent` 模块包：

```text
com.xixi.agent
├── controller
│   └── AgentController.java
├── service
│   ├── ProcessDiagnosisAgentService.java
│   ├── ProcurementWarningAgentService.java
│   └── SupplierPerformanceAgentService.java
├── tool
│   ├── PurchaseOrderAgentTool.java
│   ├── ArrivalAgentTool.java
│   ├── InboundAgentTool.java
│   ├── InventoryAgentTool.java
│   └── SupplierAgentTool.java
├── dto
│   ├── AgentChatRequest.java
│   ├── OrderDiagnosisRequest.java
│   ├── WarningScanRequest.java
│   └── SupplierScoreRequest.java
├── vo
│   ├── AgentResponseVO.java
│   ├── OrderDiagnosisVO.java
│   ├── WarningScanVO.java
│   ├── WarningItemVO.java
│   └── SupplierScoreVO.java
└── config
    └── AgentConfig.java
```

第一版可以先不做复杂 Agent 类，采用：

```text
Controller -> AgentService -> AgentTool / 业务 Service -> LLM 总结
```

等完整跑通后，再逐步升级成 Spring AI Alibaba 的 Agent / Workflow / Graph 形式。

## 4. 接入 Spring AI Alibaba 的基础步骤

### 4.1 添加 Maven 依赖

参考官方 Quick Start，可先添加：

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-agent-framework</artifactId>
    <version>1.1.2.0</version>
</dependency>

<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
    <version>1.1.2.1</version>
</dependency>
```

注意：

- 实际版本需要和当前 Spring Boot、Spring AI 版本兼容。
- 如果依赖冲突，应优先参考 Spring AI Alibaba 官方示例项目。

### 4.2 配置 API Key

`application-dev.yml` 示例：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY}
```

本地环境变量示例：

```powershell
$env:AI_DASHSCOPE_API_KEY="你的百炼API_KEY"
```

### 4.3 先验证模型连通性

先做一个最小接口：

```text
POST /agent/chat
```

请求：

```json
{
  "message": "你好，你是谁？",
  "threadId": "test-001"
}
```

预期：

```json
{
  "code": 200,
  "msg": "success",
  "data": "我是采购执行智能助手..."
}
```

这一步只验证：

- Maven 依赖可用
- API Key 正确
- 模型可以正常返回

不要一开始就接采购业务。

## 5. Agent Tool 设计原则

Agent 的核心价值不是让大模型直接访问数据库，而是让模型调用受控的业务工具。

### 5.1 Tool 的边界

推荐做只读 Tool：

```text
查询采购订单
查询订单明细
查询到货记录
查询入库记录
查询库存
查询供应商履约数据
```

第一版不要做写操作 Tool：

```text
创建采购订单
登记到货
创建入库单
确认入库
删除数据
修改库存
```

### 5.2 推荐 Tool 列表

#### PurchaseOrderAgentTool

```text
getOrderByNo(orderNo)
getOrderItems(orderNo)
getOrderExecutionSnapshot(orderNo)
```

#### ArrivalAgentTool

```text
getArrivalsByOrderNo(orderNo)
getUnInboundArrivals()
getAbnormalArrivals(days)
```

#### InboundAgentTool

```text
getInboundsByOrderNo(orderNo)
getPendingInbounds(days)
```

#### InventoryAgentTool

```text
getLowStockMaterials()
getMaterialInventory(materialCode)
```

#### SupplierAgentTool

```text
getSupplierById(supplierId)
getSupplierOrders(supplierId, days)
getSupplierPerformanceMetrics(supplierId, days)
```

### 5.3 第一个推荐 Tool

建议第一个只做：

```text
getPurchaseOrderSnapshot(orderNo)
```

返回结构：

```json
{
  "orderNo": "PO2026040011",
  "status": "PARTIAL_ARRIVAL",
  "totalOrderNumber": 96.000,
  "totalArrivedNumber": 96.000,
  "totalInboundNumber": 64.000,
  "arrivalCount": 2,
  "inboundCount": 1
}
```

这个 Tool 足够支撑第一个完整 Agent：流程阻塞诊断 Agent。

## 6. Agent 一：流程阻塞诊断 Agent

### 6.1 功能定位

用户输入采购订单号，Agent 自动判断：

- 当前流程走到哪一步
- 是否阻塞
- 阻塞原因是什么
- 下一步应该谁处理
- 建议动作是什么

示例问题：

```text
PO2026040011 为什么还没完成？
帮我诊断这张订单卡在哪一步。
```

### 6.2 接口设计

```text
POST /agent/diagnose/order
```

请求：

```json
{
  "orderNo": "PO2026040011",
  "threadId": "user-1-order-diagnose"
}
```

响应：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "orderNo": "PO2026040011",
    "currentStage": "入库确认阶段",
    "blockReason": "订单已全部到货，但仍有部分数量未确认入库。",
    "evidence": [
      "订单状态为 PARTIAL_ARRIVAL",
      "总采购数量 96.000",
      "已到货数量 96.000",
      "已入库数量 64.000"
    ],
    "suggestOwner": "WAREHOUSE",
    "suggestAction": "请仓库岗检查待确认入库单并执行确认入库。"
  }
}
```

### 6.3 推荐业务流程

```text
接收订单号
    ↓
查询采购订单
    ↓
查询采购订单明细
    ↓
查询到货记录
    ↓
查询入库记录
    ↓
Java 规则判断阻塞阶段
    ↓
LLM 生成解释和建议
    ↓
返回诊断结果
```

### 6.4 阻塞规则

建议用 Java 代码判断，不完全交给大模型：

```text
WAIT_CONFIRM
=> 阻塞在供应商确认

IN_PROGRESS 且到货数量 = 0
=> 阻塞在供应商发货 / 仓库登记到货

PARTIAL_ARRIVAL 且已到货数量 < 采购数量
=> 阻塞在剩余到货

PARTIAL_ARRIVAL 且已到货数量 >= 采购数量 且已入库数量 < 采购数量
=> 阻塞在入库确认

COMPLETED
=> 已完成，无阻塞

CLOSED / CANCELLED
=> 流程已终止
```

### 6.5 Prompt 示例

```text
你是供应商协同采购入库系统的流程阻塞诊断助手。
你只能基于工具返回的数据进行分析，不允许编造订单、数量、供应商、仓库信息。
你的任务是解释采购订单当前卡在哪个环节、为什么卡住、下一步应该由哪个角色处理。
输出必须包含：
1. 当前阶段
2. 阻塞原因
3. 证据
4. 建议处理角色
5. 建议动作
如果工具返回的数据不足，请明确说明数据不足。
```

## 7. Agent 二：采购执行预警 Agent

### 7.1 功能定位

采购执行预警 Agent 不是等用户提问，而是主动扫描业务数据，发现执行风险。

适合做成：

```text
手动触发扫描
定时任务扫描
首页工作台预警列表
```

### 7.2 接口设计

```text
POST /agent/warning/scan
```

请求：

```json
{
  "days": 7,
  "threadId": "warning-scan-001"
}
```

响应：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "summary": "本次扫描发现 6 个执行风险，其中高风险 2 个，中风险 4 个。",
    "items": [
      {
        "riskLevel": "HIGH",
        "bizType": "PURCHASE_ORDER",
        "bizNo": "PO2026040011",
        "problem": "已到货但未完成入库",
        "reason": "订单全部到货，但仍存在待确认入库数量。",
        "suggestOwner": "WAREHOUSE",
        "suggestAction": "请仓库岗优先处理待确认入库单。"
      }
    ]
  }
}
```

### 7.3 第一版预警规则

建议先实现 5 条：

```text
1. WAIT_CONFIRM 超过 1 天
2. IN_PROGRESS 超过 3 天无到货
3. PARTIAL_ARRIVAL 超过 3 天无新增到货
4. 到货单已创建但未生成入库单
5. 入库单 PENDING 超过 1 天
```

后续补充库存查询后，再加：

```text
6. 当前库存低于安全库存
```

### 7.4 推荐业务流程

```text
接收扫描范围
    ↓
查询待确认订单
    ↓
查询执行中订单
    ↓
查询部分到货订单
    ↓
查询已到货未入库记录
    ↓
查询待确认入库单
    ↓
Java 规则生成预警列表
    ↓
LLM 生成预警摘要和处理优先级
    ↓
返回预警结果
```

### 7.5 Prompt 示例

```text
你是采购执行预警助手。
你会收到系统扫描出的采购执行风险列表。
请根据风险等级、业务类型和阻塞原因，生成一段简洁的风险摘要，并给出处理优先级建议。
不要编造列表中不存在的风险。
不要要求用户执行系统不支持的操作。
```

## 8. Agent 三：供应商履约评分 Agent

### 8.1 功能定位

供应商履约评分 Agent 用于生成供应商履约报告。

它适合回答：

```text
分析供应商 001 最近 30 天履约情况
生成本月供应商履约排行榜
这个供应商是否适合继续合作？
```

### 8.2 接口设计

```text
POST /agent/supplier/score
```

请求：

```json
{
  "supplierId": 1,
  "days": 30,
  "threadId": "supplier-score-1"
}
```

响应：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "supplierId": 1,
    "supplierName": "供应商001",
    "score": 86,
    "level": "良好",
    "metrics": {
      "confirmRate": "90%",
      "arrivalCompletionRate": "88%",
      "inboundCompletionRate": "85%",
      "abnormalArrivalRate": "6%"
    },
    "analysis": "该供应商整体履约稳定，订单确认较及时，但存在少量异常到货。",
    "suggestion": "建议继续合作，同时关注包装和分批到货周期。"
  }
}
```

### 8.3 推荐统计指标

第一版指标：

```text
订单确认及时率
到货完成率
入库完成率
异常到货率
关闭 / 取消订单比例
```

### 8.4 第一版评分公式

建议简单可解释：

```text
总分 100

订单确认及时率：20 分
到货完成率：30 分
入库完成率：20 分
异常到货控制：20 分
关闭 / 取消订单控制：10 分
```

示例公式：

```text
confirmScore = confirmRate * 20
arrivalScore = arrivalCompletionRate * 30
inboundScore = inboundCompletionRate * 20
abnormalScore = (1 - abnormalRate) * 20
cancelScore = (1 - cancelRate) * 10

totalScore = confirmScore + arrivalScore + inboundScore + abnormalScore + cancelScore
```

### 8.5 推荐业务流程

```text
接收 supplierId 和统计周期
    ↓
查询供应商资料
    ↓
查询供应商采购订单
    ↓
查询订单到货情况
    ↓
查询订单入库情况
    ↓
Java 计算指标和总分
    ↓
LLM 生成履约评价和建议
    ↓
返回评分报告
```

### 8.6 Prompt 示例

```text
你是供应商履约分析助手。
你会收到供应商的履约指标和系统计算出的评分。
请基于这些指标生成简洁、客观、可解释的履约评价。
不要修改评分，不要编造不存在的订单或异常。
输出应包含：总体评价、主要优势、主要风险、合作建议。
```

## 9. RAG、Graph、Workflow 的使用建议

### 9.1 RAG 用在哪里

RAG 不适合查实时业务数据。

实时数据应该走 Tool：

```text
采购订单
到货记录
入库记录
库存数据
供应商履约数据
```

RAG 更适合放静态业务知识：

```text
V3 基线文档
状态机设计
供应商审核规则
系统操作手册
常见异常处理规范
```

示例问题：

```text
为什么已到货不能直接改库存？
```

Agent 可以从 RAG 文档中找到规则：

```text
库存只允许由确认入库动作更新。
```

### 9.2 Graph 用在哪里

Graph 适合固定诊断流程：

```text
订单诊断 Graph
采购预警扫描 Graph
供应商评分 Graph
```

订单诊断 Graph 示例：

```text
start
  ↓
loadOrder
  ↓
loadOrderItems
  ↓
loadArrivalSummary
  ↓
loadInboundSummary
  ↓
judgeBlockStage
  ↓
generateAdvice
  ↓
end
```

### 9.3 Workflow 用在哪里

你的业务流程明确：

```text
采购申请 -> 采购订单 -> 到货 -> 入库 -> 库存
```

因此更适合固定 Workflow，而不是完全开放式 Agent。

第一版可以先用 ReactAgent 跑通 Tool Calling。  
第二版再把稳定流程改造成 Workflow / Graph。

## 10. 推荐开发顺序

### 第 1 步：模型接入跑通

目标：

```text
POST /agent/chat 能正常返回内容
```

### 第 2 步：第一个 Tool 跑通

目标：

```text
getPurchaseOrderSnapshot("PO2026040011")
```

能够返回订单执行快照。

### 第 3 步：ReactAgent 调用 Tool

目标：

```text
用户问：PO2026040011 现在怎么样？
Agent 调用 Tool 后回答。
```

### 第 4 步：流程阻塞诊断 Agent

目标：

```text
POST /agent/diagnose/order
```

返回当前阶段、阻塞原因、证据和下一步建议。

### 第 5 步：采购执行预警 Agent

目标：

```text
POST /agent/warning/scan
```

返回风险列表和 AI 摘要。

### 第 6 步：供应商履约评分 Agent

目标：

```text
POST /agent/supplier/score
```

返回评分、指标、评价和建议。

### 第 7 步：加入记忆

使用 `threadId` 支持上下文：

```text
用户第一次问 PO2026040011
第二次问：那下一步谁处理？
Agent 能知道仍然围绕这张订单。
```

### 第 8 步：加入安全边界

必须限制：

```text
只读 Tool
最大工具调用次数
最大模型调用次数
禁止模型生成 SQL 并执行
禁止 Agent 调用确认入库、删除、修改库存等写接口
```

## 11. 第一版最小可运行闭环

如果只想先做一个完整可跑通版本，建议只做：

```text
Spring AI Alibaba 接入
    ↓
ReactAgent 创建成功
    ↓
PurchaseOrderAgentTool
    ↓
POST /agent/diagnose/order
    ↓
输入 orderNo
    ↓
返回当前阶段、阻塞原因、下一步建议
```

这个闭环跑通后，再扩展：

```text
POST /agent/warning/scan
POST /agent/supplier/score
```

## 12. 安全与边界

### 12.1 不允许 Agent 直接写业务数据

第一版 Agent 只做：

```text
查询
分析
预警
评分
建议
```

不要做：

```text
自动创建采购单
自动确认入库
自动更新库存
自动删除数据
```

### 12.2 不允许 Agent 直接写 SQL

所有数据访问必须通过：

```text
Mapper
Service
预定义 Tool
```

### 12.3 业务判断不能完全交给模型

以下内容建议 Java 代码确定：

```text
当前阶段
阻塞规则
风险等级
供应商评分
是否超时
是否异常
```

模型只负责：

```text
总结
解释
生成建议
组织表达
```

### 12.4 Tool 返回必须结构化

Tool 不要返回大段自然语言，应返回结构化对象：

```json
{
  "orderNo": "PO2026040011",
  "status": "PARTIAL_ARRIVAL",
  "totalOrderNumber": 96.000,
  "totalArrivedNumber": 96.000,
  "totalInboundNumber": 64.000
}
```

这样模型更不容易胡说。

## 13. 推荐简历表述

可以写：

```text
基于 Spring AI Alibaba Agent Framework 构建采购执行智能体，将采购订单、到货、入库、库存等领域 Service 封装为 Agent Tools，结合业务状态机规则和 LLM 总结能力，实现订单阻塞诊断、采购执行预警和供应商履约评分。
```

更技术一点：

```text
设计采购执行 Agent 架构，将采购订单、到货、入库、库存、供应商履约等数据封装为只读 Tool，通过 Workflow 编排订单诊断流程，并结合 RAG 注入业务状态机文档，实现可解释的采购执行预警与供应商履约分析。
```

## 14. 结论

当前项目最适合的 Agent 路线是：

```text
Spring AI Alibaba Agent Framework
+ Tool Calling
+ Java 业务规则
+ 少量 Memory
+ 后续 Workflow / Graph
```

最推荐先做：

```text
流程阻塞诊断 Agent
```

原因：

- 输入简单，只需要订单号。
- 复用现有采购、到货、入库业务数据。
- 最容易跑通 Tool Calling。
- 可以为采购执行预警和供应商履约评分复用大部分 Tool。

完成第一个 Agent 后，再扩展：

```text
采购执行预警 Agent
供应商履约评分 Agent
```

这样开发节奏最稳，也最符合当前项目阶段。
