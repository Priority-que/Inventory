# 《Python版 AI 问题聚焦与责任上下文最终落地手册》

> 基于现有文档：
>
> ```text
> document/Python版AI应用迁移傻瓜式落地手册.md
> document/Python版AI真正Agent工作流升级落地手册.md
> document/Python版AI自然对话与体验优化落地手册.md
> ```
>
> 本文档只解决一个核心问题：
>
> ```text
> 当前 Agent 能跑、能快、也能多轮追问，
> 但遇到“让哪个采购员跟进？为什么选他？”这类高价值业务问题时，
> 仍然像规则摘要器，而不像一个真正懂业务边界的 Agent。
> ```
>
> 这份文档给的是最终闭环方案，不是阶段性方案。
>
> 你可以按本文档一次性落地：
>
> ```text
> Java 后端补“责任上下文”
> Python Agent 做“问题聚焦”
> Python Agent 做“数据充分性判断”
> Python Agent 最后用人话表达
> ```

---

## 0. 先讲清楚这次到底解决什么

你现在遇到的现象不是一个普通 prompt 问题。

截图里的问题是：

```text
用户问：
让哪个采购员跟进？为什么选他？

当前回答：
这单目前卡在供应商发货 / 仓库到货登记阶段...
```

这个回答不是完全错，但它没有真正回答用户的焦点：

```text
1. 是哪个采购员？
2. 为什么选这个人？
3. 如果系统没有具体采购员，能不能明确说“只能判断到角色”？
```

所以这次不能继续做：

```text
多写几句 prompt
多加几个 if/else 模板
让模型自由发挥
```

这次要做一条完整链路：

```text
Java 给事实
Python 聚焦问题
Agent 判断事实够不够
最后用自然语言把知道什么、不知道什么、下一步怎么做说清楚
```

---

## 1. 这次方案的基本假设

按照 `$karpathy-guidelines`，先把假设说清楚。

本文档基于你当前项目结构：

```text
Java 后端：
D:\code\project\inventory\inventory_back

Python Agent：
D:\code\project\inventory\python_ai_workflow_service
```

并且基于当前已经存在的事实：

```text
1. purchase_order 表里已经有 purchaser_id
2. user 表里已经有 name、dept、phone
3. PurchaseOrderMapper.xml 已经可以 left join user u on po.purchaser_id = u.id
4. Python 侧已经有 /agent/workflow/execute
5. Python 侧已经有 ORDER_DIAGNOSIS / WARNING_SCAN / SUPPLIER_SCORE 工作流
6. 本文档先把“订单诊断 + 责任人追问”这一条链路打磨完整
```

为什么先聚焦订单诊断？

```text
因为这是最能体现 Agent 聪明感的一条链路：

用户不是只问“订单为什么没完成”，
而是会继续追问：

谁处理？
为什么是他？
下一步怎么做？
如果今天只能催一个人，催谁？
```

如果这条链路做顺，预警和供应商评分可以照同样模式扩展。

---

## 2. 最终验收标准

这次不要用“感觉好一点”作为验收标准。

最终必须满足下面这些标准。

### 2.1 有具体采购员时

用户问：

```json
{
  "message": "让哪个采购员跟进？为什么选他？",
  "threadId": "订单诊断后的 threadId"
}
```

如果 Java 后端返回：

```json
{
  "ownerUserName": "张三"
}
```

Agent 必须回答类似：

```text
建议让张三跟进。

原因是张三是这张采购订单的采购负责人，而当前问题卡在供应商发货前段。订单已经进入执行中，但还没有到货记录，仓库现在无法登记到货，所以第一责任点在采购侧。

下一步让张三先联系供应商确认发货时间，到货后再通知仓库登记到货。
```

### 2.2 没有具体采购员时

如果 Java 后端没有返回具体采购员，Agent 不能编造。

必须回答类似：

```text
现在还不能定位到具体某个采购员，只能判断到责任角色是采购侧。

为什么是采购侧：这单已经进入执行中，但目前没有到货记录；采购数量是 117，到货和入库都是 0。说明问题还卡在供应商发货前段，仓库暂时接不上处理。

下一步建议：先由采购侧联系供应商确认发货时间。要精确到某个采购员，需要后端返回这张订单的采购负责人。
```

### 2.3 不能暴露内部码

最终 `answer` 里不能出现：

```text
PURCHASER
WAREHOUSE
IN_PROGRESS
WAIT_CONFIRM
HIGH
MEDIUM
```

除非是在调试字段里，不能出现在用户可读回答里。

### 2.4 问什么答什么

用户问：

```text
让哪个采购员跟进？
```

不能输出完整订单诊断报告。

用户问：

```text
为什么选他？
```

不能只回答“请采购员跟进供应商发货”。

---

## 3. 最终链路总览

最终链路如下：

```text
用户问题
    ↓
Python /agent/workflow/execute
    ↓
intentClassify：识别业务意图
    ↓
entityExtract：抽取 orderNo
    ↓
answerPlan：识别 questionFocus
    ↓
routeDecision
    ↓
loadOrderContext：调用 Java /agent/context/order/{orderNo}
    ↓
orderRuleAnalyze：把 Java 上下文整理成诊断结果
    ↓
contextSelect：按 questionFocus 只挑必要事实
    ↓
businessAnswerGenerate：
        先判断数据是否足够
        再按“结论 -> 原因 -> 下一步 -> 信息边界”回答
    ↓
buildFinalResponse
```

这条链路里，职责边界非常明确：

```text
Java 后端：
    负责给出订单事实、负责人、责任来源、证据、下一步动作

Python Agent：
    负责判断用户问的是“谁 / 为什么 / 下一步 / 证据”
    负责判断当前事实能不能回答到这个粒度
    负责用自然语言表达
```

不要让 Python 猜具体采购员。

```text
具体负责人必须由 Java 后端基于 purchase_order.purchaser_id、user 表、流程任务或业务规则返回。
```

---

## 4. 本次新增和替换哪些文件

### 4.1 Java 后端新增文件

```text
inventory_back/src/main/java/com/xixi/agent/vo/AgentOrderContextRowVO.java
inventory_back/src/main/java/com/xixi/agent/vo/AgentOrderContextVO.java
inventory_back/src/main/java/com/xixi/agent/mapper/AgentOrderContextMapper.java
inventory_back/src/main/resources/com/xixi/mapper/AgentOrderContextMapper.xml
inventory_back/src/main/java/com/xixi/agent/service/AgentOrderContextService.java
inventory_back/src/main/java/com/xixi/agent/service/impl/AgentOrderContextServiceImpl.java
inventory_back/src/main/java/com/xixi/agent/controller/AgentContextController.java
```

### 4.2 Python 新增文件

```text
python_ai_workflow_service/app/schemas/order_context.py
python_ai_workflow_service/app/services/answer_humanizer.py
```

### 4.3 Python 替换文件

```text
python_ai_workflow_service/app/schemas/answer_plan.py
python_ai_workflow_service/app/schemas/diagnosis.py
python_ai_workflow_service/app/workflows/state.py
python_ai_workflow_service/app/workflows/nodes/intent_classify.py
python_ai_workflow_service/app/workflows/nodes/answer_plan.py
python_ai_workflow_service/app/workflows/nodes/load_order_context.py
python_ai_workflow_service/app/workflows/nodes/order_rule_analyze.py
python_ai_workflow_service/app/workflows/nodes/context_select.py
python_ai_workflow_service/app/workflows/nodes/business_answer_generate.py
```

### 4.4 Python 局部修改文件

```text
python_ai_workflow_service/app/clients/inventory_backend.py
python_ai_workflow_service/app/repositories/session_store.py
```

---

## 5. Java 后端完整代码

### 5.1 新增 VO：AgentOrderContextRowVO.java

路径：

```text
inventory_back/src/main/java/com/xixi/agent/vo/AgentOrderContextRowVO.java
```

完整代码：

```java
package com.xixi.agent.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AgentOrderContextRowVO {
    private Long orderId;
    private String orderNo;
    private String status;
    private Long supplierId;
    private String supplierName;
    private Long purchaserId;
    private String purchaserName;
    private String purchaserDept;
    private String purchaserPhone;
    private BigDecimal totalOrderNumber;
    private BigDecimal totalArriveNumber;
    private BigDecimal totalInboundNumber;
    private Integer arrivalCount;
    private Integer inboundCount;
}
```

---

### 5.2 新增 VO：AgentOrderContextVO.java

路径：

```text
inventory_back/src/main/java/com/xixi/agent/vo/AgentOrderContextVO.java
```

完整代码：

```java
package com.xixi.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentOrderContextVO {
    private Boolean exists;
    private OrderInfo order;
    private StageInfo stage;
    private ResponsibilityInfo responsibility;
    private List<EvidenceItem> evidence = new ArrayList<>();
    private NextActionInfo nextAction;

    @Data
    public static class OrderInfo {
        private Long orderId;
        private String orderNo;
        private String status;
        private String statusName;
        private Long supplierId;
        private String supplierName;
        private BigDecimal totalOrderNumber;
        private BigDecimal totalArriveNumber;
        private BigDecimal totalInboundNumber;
        private Integer arrivalCount;
        private Integer inboundCount;
    }

    @Data
    public static class StageInfo {
        private String currentStage;
        private String blockReason;
        private String stageOwnerRole;
        private String stageOwnerRoleName;
    }

    @Data
    public static class ResponsibilityInfo {
        private String ownerRole;
        private String ownerRoleName;
        private Long ownerUserId;
        private String ownerUserName;
        private String ownerDeptName;
        private String ownerPhone;
        private String ownerSource;
        private String ownerReason;
    }

    @Data
    public static class EvidenceItem {
        private String type;
        private String label;
        private String value;
        private String explain;
    }

    @Data
    public static class NextActionInfo {
        private String actionOwnerRole;
        private String actionOwnerRoleName;
        private Long actionOwnerId;
        private String actionOwnerName;
        private String actionText;
    }
}
```

---

### 5.3 新增 Mapper：AgentOrderContextMapper.java

路径：

```text
inventory_back/src/main/java/com/xixi/agent/mapper/AgentOrderContextMapper.java
```

完整代码：

```java
package com.xixi.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xixi.agent.vo.AgentOrderContextRowVO;
import com.xixi.entity.PurchaseOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AgentOrderContextMapper extends BaseMapper<PurchaseOrder> {
    AgentOrderContextRowVO getOrderContextBaseByOrderNo(@Param("orderNo") String orderNo);
}
```

---

### 5.4 新增 XML：AgentOrderContextMapper.xml

路径：

```text
inventory_back/src/main/resources/com/xixi/mapper/AgentOrderContextMapper.xml
```

完整代码：

```xml
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.xixi.agent.mapper.AgentOrderContextMapper">
    <select id="getOrderContextBaseByOrderNo" resultType="com.xixi.agent.vo.AgentOrderContextRowVO">
        <!--
            给 Python Agent 使用的订单上下文基础查询。
            这里一次性返回订单、供应商、采购负责人、到货/入库聚合数据，
            避免 Python 侧为了回答“谁处理、为什么”再多次拼接口。
        -->
        select po.id as orderId,
               po.order_no as orderNo,
               po.status as status,
               po.supplier_id as supplierId,
               s.name as supplierName,
               po.purchaser_id as purchaserId,
               u.name as purchaserName,
               u.dept as purchaserDept,
               u.phone as purchaserPhone,
               coalesce(sum(poi.order_number), 0) as totalOrderNumber,
               greatest(
                   coalesce(sum(poi.arrived_number), 0),
                   coalesce((
                       select sum(a.arrival_number)
                       from arrival a
                       where a.order_id = po.id
                         and a.deleted = 0
                   ), 0)
               ) as totalArriveNumber,
               greatest(
                   coalesce(sum(poi.inbound_number), 0),
                   coalesce((
                       select sum(i.inbound_number)
                       from inbound i
                       left join arrival a on i.arrival_id = a.id
                       where a.order_id = po.id
                         and i.deleted = 0
                         and a.deleted = 0
                   ), 0)
               ) as totalInboundNumber,
               (
                   select count(1)
                   from arrival a
                   where a.order_id = po.id
                     and a.deleted = 0
               ) as arrivalCount,
               (
                   select count(1)
                   from inbound i
                   left join arrival a on i.arrival_id = a.id
                   where a.order_id = po.id
                     and i.deleted = 0
                     and a.deleted = 0
               ) as inboundCount
        from purchase_order po
        left join supplier s on po.supplier_id = s.id
        left join `user` u on po.purchaser_id = u.id
        left join purchase_order_item poi on po.id = poi.order_id and poi.deleted = 0
        where po.order_no = #{orderNo}
          and po.deleted = 0
        group by po.id,
                 po.order_no,
                 po.status,
                 po.supplier_id,
                 s.name,
                 po.purchaser_id,
                 u.name,
                 u.dept,
                 u.phone
    </select>
</mapper>
```

---

### 5.5 新增 Service：AgentOrderContextService.java

路径：

```text
inventory_back/src/main/java/com/xixi/agent/service/AgentOrderContextService.java
```

完整代码：

```java
package com.xixi.agent.service;

import com.xixi.agent.vo.AgentOrderContextVO;

public interface AgentOrderContextService {
    AgentOrderContextVO getOrderContext(String orderNo);
}
```

---

### 5.6 新增 ServiceImpl：AgentOrderContextServiceImpl.java

路径：

```text
inventory_back/src/main/java/com/xixi/agent/service/impl/AgentOrderContextServiceImpl.java
```

完整代码：

```java
package com.xixi.agent.service.impl;

import com.xixi.agent.mapper.AgentOrderContextMapper;
import com.xixi.agent.service.AgentOrderContextService;
import com.xixi.agent.vo.AgentOrderContextRowVO;
import com.xixi.agent.vo.AgentOrderContextVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AgentOrderContextServiceImpl implements AgentOrderContextService {

    private static final String ROLE_PURCHASER = "PURCHASER";
    private static final String ROLE_WAREHOUSE = "WAREHOUSE";
    private static final String ROLE_NONE = "NONE";

    private final AgentOrderContextMapper agentOrderContextMapper;

    @Override
    public AgentOrderContextVO getOrderContext(String orderNo) {
        AgentOrderContextVO context = new AgentOrderContextVO();
        if (orderNo == null || orderNo.trim().isEmpty()) {
            context.setExists(false);
            return context;
        }

        AgentOrderContextRowVO row = agentOrderContextMapper.getOrderContextBaseByOrderNo(orderNo.trim());
        if (row == null) {
            context.setExists(false);
            return context;
        }

        context.setExists(true);
        context.setOrder(buildOrderInfo(row));
        fillStageResponsibilityAndAction(context, row);
        fillEvidence(context, row);
        return context;
    }

    private AgentOrderContextVO.OrderInfo buildOrderInfo(AgentOrderContextRowVO row) {
        AgentOrderContextVO.OrderInfo order = new AgentOrderContextVO.OrderInfo();
        order.setOrderId(row.getOrderId());
        order.setOrderNo(row.getOrderNo());
        order.setStatus(row.getStatus());
        order.setStatusName(statusName(row.getStatus()));
        order.setSupplierId(row.getSupplierId());
        order.setSupplierName(row.getSupplierName());
        order.setTotalOrderNumber(safe(row.getTotalOrderNumber()));
        order.setTotalArriveNumber(safe(row.getTotalArriveNumber()));
        order.setTotalInboundNumber(safe(row.getTotalInboundNumber()));
        order.setArrivalCount(row.getArrivalCount() == null ? 0 : row.getArrivalCount());
        order.setInboundCount(row.getInboundCount() == null ? 0 : row.getInboundCount());
        return order;
    }

    private void fillStageResponsibilityAndAction(AgentOrderContextVO context, AgentOrderContextRowVO row) {
        BigDecimal totalOrder = safe(row.getTotalOrderNumber());
        BigDecimal totalArrive = safe(row.getTotalArriveNumber());
        BigDecimal totalInbound = safe(row.getTotalInboundNumber());
        String status = row.getStatus();

        if ("WAIT_CONFIRM".equals(status)) {
            fillContext(
                    context,
                    row,
                    "供应商确认阶段",
                    "采购订单仍处于待供应商确认状态。",
                    ROLE_PURCHASER,
                    "当前需要采购侧跟进供应商确认订单和预计交期。",
                    "请采购负责人联系供应商确认订单，并同步预计交期。"
            );
            return;
        }

        if ("IN_PROGRESS".equals(status) && totalArrive.compareTo(BigDecimal.ZERO) == 0) {
            fillContext(
                    context,
                    row,
                    "供应商发货 / 仓库到货登记阶段",
                    "订单已进入执行中，但目前还没有到货记录。",
                    ROLE_PURCHASER,
                    "当前问题卡在供应商发货前段，仓库暂时没有可登记的到货对象，所以第一责任点在采购侧。",
                    "请采购负责人先联系供应商确认发货时间，到货后再通知仓库登记到货。"
            );
            return;
        }

        if ("PARTIAL_ARRIVAL".equals(status) && totalArrive.compareTo(totalOrder) < 0) {
            fillContext(
                    context,
                    row,
                    "剩余到货阶段",
                    "订单已有部分到货，但仍有剩余采购数量未到货。",
                    ROLE_PURCHASER,
                    "当前还缺剩余到货，核心动作仍然是采购侧推动供应商补齐发货。",
                    "请采购负责人催促供应商补齐剩余到货。"
            );
            return;
        }

        if ("PARTIAL_ARRIVAL".equals(status)
                && totalArrive.compareTo(totalOrder) >= 0
                && totalInbound.compareTo(totalOrder) < 0) {
            fillContext(
                    context,
                    row,
                    "入库确认阶段",
                    "订单已全部到货，但仍有部分数量未确认入库。",
                    ROLE_WAREHOUSE,
                    "当前货物已经到达，问题转到仓库入库确认环节，所以第一责任点在仓库侧。",
                    "请仓库侧检查待确认入库单并执行确认入库。"
            );
            return;
        }

        if ("COMPLETED".equals(status)) {
            fillContext(
                    context,
                    row,
                    "流程已完成",
                    "采购订单已完成，无阻塞。",
                    ROLE_NONE,
                    "订单已经完成，不需要继续指定处理人。",
                    "无需处理。"
            );
            return;
        }

        if ("CLOSED".equals(status) || "CANCELLED".equals(status)) {
            fillContext(
                    context,
                    row,
                    "流程已终止",
                    "采购订单已关闭或取消。",
                    ROLE_PURCHASER,
                    "订单已经终止，如需继续采购，应由采购侧重新发起或创建新订单。",
                    "如需继续采购，请重新发起采购申请或创建新订单。"
            );
            return;
        }

        fillContext(
                context,
                row,
                "未知阶段",
                "当前状态无法根据规则判断阻塞点。",
                ROLE_PURCHASER,
                "系统无法自动判断责任点，建议先由采购侧人工核对订单状态和明细数据。",
                "请采购负责人人工检查订单状态、到货记录和入库记录。"
        );
    }

    private void fillContext(
            AgentOrderContextVO context,
            AgentOrderContextRowVO row,
            String currentStage,
            String blockReason,
            String ownerRole,
            String ownerReason,
            String actionText
    ) {
        AgentOrderContextVO.StageInfo stage = new AgentOrderContextVO.StageInfo();
        stage.setCurrentStage(currentStage);
        stage.setBlockReason(blockReason);
        stage.setStageOwnerRole(ownerRole);
        stage.setStageOwnerRoleName(roleName(ownerRole));
        context.setStage(stage);

        AgentOrderContextVO.ResponsibilityInfo responsibility = new AgentOrderContextVO.ResponsibilityInfo();
        responsibility.setOwnerRole(ownerRole);
        responsibility.setOwnerRoleName(roleName(ownerRole));
        responsibility.setOwnerReason(ownerReason);

        if (ROLE_PURCHASER.equals(ownerRole) && row.getPurchaserId() != null) {
            responsibility.setOwnerUserId(row.getPurchaserId());
            responsibility.setOwnerUserName(row.getPurchaserName());
            responsibility.setOwnerDeptName(row.getPurchaserDept());
            responsibility.setOwnerPhone(row.getPurchaserPhone());
            responsibility.setOwnerSource("PURCHASE_ORDER_PURCHASER");
        } else {
            responsibility.setOwnerSource("ROLE_ONLY");
        }

        context.setResponsibility(responsibility);

        AgentOrderContextVO.NextActionInfo nextAction = new AgentOrderContextVO.NextActionInfo();
        nextAction.setActionOwnerRole(ownerRole);
        nextAction.setActionOwnerRoleName(roleName(ownerRole));
        nextAction.setActionText(actionText);
        if (ROLE_PURCHASER.equals(ownerRole) && row.getPurchaserId() != null) {
            nextAction.setActionOwnerId(row.getPurchaserId());
            nextAction.setActionOwnerName(row.getPurchaserName());
        }
        context.setNextAction(nextAction);
    }

    private void fillEvidence(AgentOrderContextVO context, AgentOrderContextRowVO row) {
        context.getEvidence().add(evidence(
                "ORDER_STATUS",
                "订单状态",
                statusName(row.getStatus()),
                "订单当前状态决定了流程所处节点。"
        ));
        context.getEvidence().add(evidence(
                "ORDER_QTY",
                "采购数量",
                safe(row.getTotalOrderNumber()).toPlainString(),
                "采购数量用于判断到货和入库是否完成。"
        ));
        context.getEvidence().add(evidence(
                "ARRIVAL_QTY",
                "到货数量",
                safe(row.getTotalArriveNumber()).toPlainString(),
                "如果到货数量为 0，说明仓库暂时没有可登记的到货对象。"
        ));
        context.getEvidence().add(evidence(
                "INBOUND_QTY",
                "入库数量",
                safe(row.getTotalInboundNumber()).toPlainString(),
                "入库数量用于判断是否已经完成仓库入库确认。"
        ));
        context.getEvidence().add(evidence(
                "ARRIVAL_COUNT",
                "到货次数",
                String.valueOf(row.getArrivalCount() == null ? 0 : row.getArrivalCount()),
                "到货次数可以辅助判断是否已经有到货记录。"
        ));
        context.getEvidence().add(evidence(
                "INBOUND_COUNT",
                "入库次数",
                String.valueOf(row.getInboundCount() == null ? 0 : row.getInboundCount()),
                "入库次数可以辅助判断是否已经进入入库确认。"
        ));
    }

    private AgentOrderContextVO.EvidenceItem evidence(String type, String label, String value, String explain) {
        AgentOrderContextVO.EvidenceItem item = new AgentOrderContextVO.EvidenceItem();
        item.setType(type);
        item.setLabel(label);
        item.setValue(value);
        item.setExplain(explain);
        return item;
    }

    private String roleName(String role) {
        if (ROLE_PURCHASER.equals(role)) {
            return "采购员";
        }
        if (ROLE_WAREHOUSE.equals(role)) {
            return "仓库收货员";
        }
        if (ROLE_NONE.equals(role)) {
            return "无需处理";
        }
        return "待确认责任方";
    }

    private String statusName(String status) {
        if ("WAIT_CONFIRM".equals(status)) {
            return "待供应商确认";
        }
        if ("IN_PROGRESS".equals(status)) {
            return "执行中";
        }
        if ("PARTIAL_ARRIVAL".equals(status)) {
            return "部分到货";
        }
        if ("COMPLETED".equals(status)) {
            return "已完成";
        }
        if ("CLOSED".equals(status)) {
            return "已关闭";
        }
        if ("CANCELLED".equals(status)) {
            return "已取消";
        }
        return status == null ? "未知状态" : status;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
```

---

### 5.7 新增 Controller：AgentContextController.java

路径：

```text
inventory_back/src/main/java/com/xixi/agent/controller/AgentContextController.java
```

完整代码：

```java
package com.xixi.agent.controller;

import com.xixi.agent.service.AgentOrderContextService;
import com.xixi.pojo.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent/context")
@RequiredArgsConstructor
public class AgentContextController {

    private final AgentOrderContextService agentOrderContextService;

    @GetMapping("/order/{orderNo}")
    public Result getOrderContext(@PathVariable String orderNo) {
        return Result.success(agentOrderContextService.getOrderContext(orderNo));
    }
}
```

---

## 6. Python 侧完整代码

### 6.1 新增 schema：app/schemas/order_context.py

路径：

```text
python_ai_workflow_service/app/schemas/order_context.py
```

完整代码：

```python
from decimal import Decimal

from pydantic import Field

from app.schemas.common import ApiModel


class OrderContextOrder(ApiModel):
    order_id: int | None = Field(default=None, alias="orderId")
    order_no: str | None = Field(default=None, alias="orderNo")
    status: str | None = None
    status_name: str | None = Field(default=None, alias="statusName")
    supplier_id: int | None = Field(default=None, alias="supplierId")
    supplier_name: str | None = Field(default=None, alias="supplierName")
    total_order_number: Decimal = Field(default=Decimal("0"), alias="totalOrderNumber")
    total_arrive_number: Decimal = Field(default=Decimal("0"), alias="totalArriveNumber")
    total_inbound_number: Decimal = Field(default=Decimal("0"), alias="totalInboundNumber")
    arrival_count: int = Field(default=0, alias="arrivalCount")
    inbound_count: int = Field(default=0, alias="inboundCount")


class OrderContextStage(ApiModel):
    current_stage: str | None = Field(default=None, alias="currentStage")
    block_reason: str | None = Field(default=None, alias="blockReason")
    stage_owner_role: str | None = Field(default=None, alias="stageOwnerRole")
    stage_owner_role_name: str | None = Field(default=None, alias="stageOwnerRoleName")


class OrderContextResponsibility(ApiModel):
    owner_role: str | None = Field(default=None, alias="ownerRole")
    owner_role_name: str | None = Field(default=None, alias="ownerRoleName")
    owner_user_id: int | None = Field(default=None, alias="ownerUserId")
    owner_user_name: str | None = Field(default=None, alias="ownerUserName")
    owner_dept_name: str | None = Field(default=None, alias="ownerDeptName")
    owner_phone: str | None = Field(default=None, alias="ownerPhone")
    owner_source: str | None = Field(default=None, alias="ownerSource")
    owner_reason: str | None = Field(default=None, alias="ownerReason")


class OrderContextEvidenceItem(ApiModel):
    type: str | None = None
    label: str | None = None
    value: str | None = None
    explain: str | None = None


class OrderContextNextAction(ApiModel):
    action_owner_role: str | None = Field(default=None, alias="actionOwnerRole")
    action_owner_role_name: str | None = Field(default=None, alias="actionOwnerRoleName")
    action_owner_id: int | None = Field(default=None, alias="actionOwnerId")
    action_owner_name: str | None = Field(default=None, alias="actionOwnerName")
    action_text: str | None = Field(default=None, alias="actionText")


class AgentOrderContextVO(ApiModel):
    exists: bool = False
    order: OrderContextOrder | None = None
    stage: OrderContextStage | None = None
    responsibility: OrderContextResponsibility | None = None
    evidence: list[OrderContextEvidenceItem] = Field(default_factory=list)
    next_action: OrderContextNextAction | None = Field(default=None, alias="nextAction")
```

---

### 6.2 替换 schema：app/schemas/answer_plan.py

路径：

```text
python_ai_workflow_service/app/schemas/answer_plan.py
```

完整代码：

```python
from typing import Any

from pydantic import Field

from app.schemas.common import ApiModel


class AnswerPlan(ApiModel):
    interaction_type: str = Field(default="BUSINESS", alias="interactionType")
    intent: str
    question_focus: str = Field(default="FULL_DIAGNOSIS", alias="questionFocus")
    turn_type: str = Field(default="FIRST_TURN", alias="turnType")
    answer_mode: str = Field(default="FULL_ANALYSIS", alias="answerMode")
    focus: str | None = None
    target_biz_no: str | None = Field(default=None, alias="targetBizNo")
    target_order_no: str | None = Field(default=None, alias="targetOrderNo")
    target_supplier_id: int | None = Field(default=None, alias="targetSupplierId")
    owner: str | None = None
    risk_level: str | None = Field(default=None, alias="riskLevel")
    risk_type: str | None = Field(default=None, alias="riskType")
    needs_refresh: bool = Field(default=True, alias="needsRefresh")
    use_llm: bool = Field(default=True, alias="useLlm")
    max_context_items: int = Field(default=10, alias="maxContextItems")


class SelectedContext(ApiModel):
    interaction_type: str = Field(default="BUSINESS", alias="interactionType")
    intent: str
    question_focus: str = Field(default="FULL_DIAGNOSIS", alias="questionFocus")
    answer_mode: str = Field(alias="answerMode")
    summary: str | None = None
    facts: dict[str, Any] = Field(default_factory=dict)
    items: list[dict[str, Any]] = Field(default_factory=list)
    instruction: str | None = None
```

---

### 6.3 替换 schema：app/schemas/diagnosis.py

路径：

```text
python_ai_workflow_service/app/schemas/diagnosis.py
```

完整代码：

```python
from decimal import Decimal
from typing import Any

from pydantic import Field

from app.schemas.common import ApiModel


class OrderSnapshotVO(ApiModel):
    order_id: int | None = Field(default=None, alias="orderId")
    order_no: str | None = Field(default=None, alias="orderNo")
    status: str | None = None
    status_name: str | None = Field(default=None, alias="statusName")
    supplier_id: int | None = Field(default=None, alias="supplierId")
    supplier_name: str | None = Field(default=None, alias="supplierName")
    total_order_number: Decimal = Field(default=Decimal("0"), alias="totalOrderNumber")
    total_arrive_number: Decimal = Field(default=Decimal("0"), alias="totalArriveNumber")
    total_inbound_number: Decimal = Field(default=Decimal("0"), alias="totalInboundNumber")
    arrival_count: int = Field(default=0, alias="arrivalCount")
    inbound_count: int = Field(default=0, alias="inboundCount")


class OrderDiagnosisVO(ApiModel):
    order_no: str | None = Field(default=None, alias="orderNo")
    current_stage: str | None = Field(default=None, alias="currentStage")
    block_reason: str | None = Field(default=None, alias="blockReason")
    evidence: list[str] = Field(default_factory=list)
    suggest_owner: str | None = Field(default=None, alias="suggestOwner")
    suggest_action: str | None = Field(default=None, alias="suggestAction")
    responsibility: dict[str, Any] | None = None
    next_action: dict[str, Any] | None = Field(default=None, alias="nextAction")
    ai_summary: str | None = Field(default=None, alias="aiSummary")
```

---

### 6.4 替换 state：app/workflows/state.py

路径：

```text
python_ai_workflow_service/app/workflows/state.py
```

完整代码：

```python
from enum import Enum
from typing import Any, TypedDict

from pydantic import Field

from app.schemas.common import ApiModel


class WorkflowIntent(str, Enum):
    ORDER_DIAGNOSIS = "ORDER_DIAGNOSIS"
    WARNING_SCAN = "WARNING_SCAN"
    SUPPLIER_SCORE = "SUPPLIER_SCORE"
    KNOWLEDGE_QA = "KNOWLEDGE_QA"
    UNKNOWN = "UNKNOWN"


class InteractionType(str, Enum):
    BUSINESS = "BUSINESS"
    SOCIAL = "SOCIAL"
    META = "META"
    CLARIFY = "CLARIFY"


class WorkflowStateKeys:
    MESSAGE = "message"
    THREAD_ID = "threadId"
    AUTHORIZATION = "authorization"
    USER_ID = "userId"
    NORMALIZED_MESSAGE = "normalizedMessage"

    INTENT = "intent"
    ACTIVE_INTENT = "activeIntent"
    INTERACTION_TYPE = "interactionType"

    ENTITY = "entity"
    RAG_DOCS = "ragDocs"

    ORDER_CONTEXT = "orderContext"
    ORDER_SNAPSHOT = "orderSnapshot"
    ORDER_DIAGNOSIS = "orderDiagnosis"

    WARNING_CONTEXT = "warningContext"
    WARNING_ANALYSIS = "warningAnalysis"

    SUPPLIER_METRICS = "supplierMetrics"
    SUPPLIER_SCORE = "supplierScore"

    ANSWER_PLAN = "answerPlan"
    SELECTED_CONTEXT = "selectedContext"

    LLM_ANSWER = "llmAnswer"
    GUARDRAIL_RESULT = "guardrailResult"
    FINAL_RESPONSE = "finalResponse"
    ERROR_MESSAGE = "errorMessage"
    ROUTE = "_route"


class WorkflowEntity(ApiModel):
    order_no: str | None = Field(default=None, alias="orderNo")
    supplier_id: int | None = Field(default=None, alias="supplierId")
    days: int | None = None
    material_code: str | None = Field(default=None, alias="materialCode")
    warehouse_id: int | None = Field(default=None, alias="warehouseId")


class WorkflowGraphState(TypedDict, total=False):
    message: str
    threadId: str
    authorization: str
    userId: int
    normalizedMessage: str

    intent: str
    activeIntent: str
    interactionType: str

    entity: dict[str, Any]
    ragDocs: str

    orderContext: dict[str, Any]
    orderSnapshot: dict[str, Any]
    orderDiagnosis: dict[str, Any]

    warningContext: dict[str, Any]
    warningAnalysis: dict[str, Any]

    supplierMetrics: dict[str, Any]
    supplierScore: dict[str, Any]

    answerPlan: dict[str, Any]
    selectedContext: dict[str, Any]

    llmAnswer: str
    guardrailResult: str
    finalResponse: dict[str, Any]
    errorMessage: str
    _route: str
```

---

### 6.5 局部修改 client：app/clients/inventory_backend.py

路径：

```text
python_ai_workflow_service/app/clients/inventory_backend.py
```

在 `InventoryBackendClient` 类里新增这个方法：

```python
    async def get_agent_order_context(self, order_no: str, authorization: str) -> dict[str, Any] | None:
        data = await self._request(
            "GET",
            f"/agent/context/order/{order_no}",
            authorization,
        )
        return data if isinstance(data, dict) else None
```

放置位置建议：

```text
get_purchase_order_by_order_no 方法后面
```

---

### 6.6 替换节点：app/workflows/nodes/intent_classify.py

路径：

```text
python_ai_workflow_service/app/workflows/nodes/intent_classify.py
```

完整代码：

```python
import re

from app.clients.llm_client import LLMClient
from app.workflows.prompts import INTENT_CLASSIFY_PROMPT
from app.workflows.state import InteractionType, WorkflowIntent, WorkflowStateKeys


class IntentClassifyNode:
    ORDER_NO_PATTERN = re.compile(r"\bPO\d+\b", re.IGNORECASE)
    SUPPLIER_ID_PATTERN = re.compile(r"供应商\s*(\d+)")

    def __init__(self, llm_client: LLMClient):
        self.llm_client = llm_client

    async def __call__(self, state: dict) -> dict:
        message = str(state.get(WorkflowStateKeys.NORMALIZED_MESSAGE, "")).strip()
        active_intent = self._resolve_active_intent(state)

        interaction_type = self._classify_interaction_type(message, active_intent)
        if interaction_type != InteractionType.BUSINESS.value:
            return {
                WorkflowStateKeys.INTERACTION_TYPE: interaction_type,
                WorkflowStateKeys.INTENT: WorkflowIntent.UNKNOWN.value,
            }

        rule_intent = self._rule_business_intent(message, active_intent)
        if rule_intent is not None:
            return {
                WorkflowStateKeys.INTERACTION_TYPE: InteractionType.BUSINESS.value,
                WorkflowStateKeys.INTENT: rule_intent,
                WorkflowStateKeys.ACTIVE_INTENT: rule_intent,
            }

        prompt = (
            INTENT_CLASSIFY_PROMPT
            .replace("{previousIntent}", active_intent)
            .replace("{message}", message)
        )
        intent_text = await self.llm_client.chat_text(
            "你是意图分类器，只输出意图编码。",
            prompt,
            temperature=0.0,
        )
        intent = self._parse_intent(intent_text)
        if intent == WorkflowIntent.UNKNOWN.value and active_intent != WorkflowIntent.UNKNOWN.value:
            intent = active_intent

        if intent == WorkflowIntent.UNKNOWN.value:
            return {
                WorkflowStateKeys.INTERACTION_TYPE: InteractionType.CLARIFY.value,
                WorkflowStateKeys.INTENT: WorkflowIntent.UNKNOWN.value,
            }

        return {
            WorkflowStateKeys.INTERACTION_TYPE: InteractionType.BUSINESS.value,
            WorkflowStateKeys.INTENT: intent,
            WorkflowStateKeys.ACTIVE_INTENT: intent,
        }

    def _resolve_active_intent(self, state: dict) -> str:
        active_intent = str(state.get(WorkflowStateKeys.ACTIVE_INTENT, "")).strip()
        if active_intent in {
            WorkflowIntent.ORDER_DIAGNOSIS.value,
            WorkflowIntent.WARNING_SCAN.value,
            WorkflowIntent.SUPPLIER_SCORE.value,
            WorkflowIntent.KNOWLEDGE_QA.value,
        }:
            return active_intent

        current_intent = str(state.get(WorkflowStateKeys.INTENT, WorkflowIntent.UNKNOWN.value))
        if current_intent in {
            WorkflowIntent.ORDER_DIAGNOSIS.value,
            WorkflowIntent.WARNING_SCAN.value,
            WorkflowIntent.SUPPLIER_SCORE.value,
            WorkflowIntent.KNOWLEDGE_QA.value,
        }:
            return current_intent

        return WorkflowIntent.UNKNOWN.value

    def _parse_intent(self, text: str | None) -> str:
        if not text:
            return WorkflowIntent.UNKNOWN.value
        value = text.strip()
        for intent in WorkflowIntent:
            if intent.value in value:
                return intent.value
        return WorkflowIntent.UNKNOWN.value

    def _classify_interaction_type(self, message: str, active_intent: str) -> str:
        if not message:
            return InteractionType.CLARIFY.value

        if self._is_social_message(message):
            return InteractionType.SOCIAL.value

        if self._is_meta_message(message):
            return InteractionType.META.value

        if active_intent != WorkflowIntent.UNKNOWN.value and self._looks_like_business_follow_up(message):
            return InteractionType.BUSINESS.value

        if self.ORDER_NO_PATTERN.search(message) or self.SUPPLIER_ID_PATTERN.search(message):
            return InteractionType.BUSINESS.value

        if self._contains_any(
            message,
            ["订单", "采购单", "采购订单", "风险", "预警", "扫描", "供应商", "履约", "到货", "入库", "规则", "状态流转"],
        ):
            return InteractionType.BUSINESS.value

        return InteractionType.CLARIFY.value

    def _rule_business_intent(self, message: str, active_intent: str) -> str | None:
        text = (message or "").strip()

        if active_intent != WorkflowIntent.UNKNOWN.value and self._looks_like_business_follow_up(text):
            return active_intent

        if self.ORDER_NO_PATTERN.search(text):
            return WorkflowIntent.ORDER_DIAGNOSIS.value

        if self.SUPPLIER_ID_PATTERN.search(text) or (
            "供应商" in text and self._contains_any(text, ["履约", "评分", "分数", "等级", "合作", "表现", "分析"])
        ):
            return WorkflowIntent.SUPPLIER_SCORE.value

        if self._contains_any(text, ["风险", "预警", "扫描", "优先处理", "高风险", "中风险", "低风险"]):
            return WorkflowIntent.WARNING_SCAN.value

        if self._contains_any(text, ["规则", "状态流转", "为什么不能", "为什么无法", "怎么操作", "是什么意思"]):
            return WorkflowIntent.KNOWLEDGE_QA.value

        if self._contains_any(text, ["订单", "采购单", "采购订单"]) and self._contains_any(
            text,
            ["为什么", "没完成", "卡在哪", "下一步", "谁处理", "怎么处理", "怎么办", "到货", "入库"],
        ):
            return WorkflowIntent.ORDER_DIAGNOSIS.value

        return None

    def _is_social_message(self, text: str) -> bool:
        if text in {"你好", "您好", "hi", "hello", "在吗", "收到", "好的", "好", "谢谢", "谢谢你", "辛苦了", "ok", "OK"}:
            return True
        return self._contains_any(text, ["你好", "您好", "在吗", "收到", "好的", "谢谢", "辛苦了"])

    def _is_meta_message(self, text: str) -> bool:
        return self._contains_any(
            text,
            ["你是谁", "你是什么", "你是什么模型", "你用的什么模型", "底层模型", "你会做什么", "你能做什么", "你有什么能力"],
        )

    def _looks_like_business_follow_up(self, text: str) -> bool:
        return self._contains_any(
            text,
            ["为什么", "原因", "下一步", "怎么处理", "怎么办", "谁处理", "哪个", "哪位", "采购员", "为什么选", "依据"],
        )

    def _contains_any(self, text: str, words: list[str]) -> bool:
        return any(word in (text or "") for word in words)
```

---

### 6.7 替换节点：app/workflows/nodes/answer_plan.py

路径：

```text
python_ai_workflow_service/app/workflows/nodes/answer_plan.py
```

完整代码：

```python
import re

from app.schemas.answer_plan import AnswerPlan
from app.workflows.state import InteractionType, WorkflowIntent, WorkflowStateKeys


BIZ_NO_PATTERN = re.compile(r"(PO|AR|IN)\d+", re.IGNORECASE)


class AnswerPlanNode:
    async def __call__(self, state: dict) -> dict:
        interaction_type = str(state.get(WorkflowStateKeys.INTERACTION_TYPE, InteractionType.BUSINESS.value))
        intent = str(state.get(WorkflowStateKeys.INTENT, WorkflowIntent.UNKNOWN.value))
        message = str(state.get(WorkflowStateKeys.MESSAGE, ""))

        plan = self._build_plan(state, interaction_type, intent, message)
        return {WorkflowStateKeys.ANSWER_PLAN: plan.model_dump(by_alias=True)}

    def _build_plan(self, state: dict, interaction_type: str, intent: str, message: str) -> AnswerPlan:
        if interaction_type != InteractionType.BUSINESS.value:
            return AnswerPlan(
                interactionType=interaction_type,
                intent=WorkflowIntent.UNKNOWN.value,
                questionFocus=interaction_type,
                answerMode=interaction_type,
                needsRefresh=False,
                useLlm=False,
                maxContextItems=0,
            )

        question_focus = self._classify_question_focus(intent, message)
        target_biz_no = self._extract_biz_no(message)
        use_llm = question_focus in {"FULL_DIAGNOSIS", "SUMMARY", "KNOWLEDGE_ANSWER"}

        return AnswerPlan(
            interactionType=InteractionType.BUSINESS.value,
            intent=intent,
            questionFocus=question_focus,
            turnType="FOLLOW_UP" if self._has_reusable_result(state, intent) else "FIRST_TURN",
            answerMode="FOCUSED_ANSWER" if not use_llm else "FULL_ANALYSIS",
            focus=question_focus.lower(),
            targetBizNo=target_biz_no,
            targetOrderNo=self._extract_order_no(message),
            targetSupplierId=self._extract_supplier_id(message),
            needsRefresh=self._needs_refresh(state, intent, message),
            useLlm=use_llm,
            maxContextItems=5,
        )

    def _classify_question_focus(self, intent: str, message: str) -> str:
        text = message or ""

        if intent == WorkflowIntent.ORDER_DIAGNOSIS.value:
            asks_owner = self._contains_any(text, ["谁处理", "谁来", "哪个采购员", "哪位采购员", "谁跟进", "哪个人", "负责人"])
            asks_reason = self._contains_any(text, ["为什么", "原因", "为什么选", "凭什么", "依据"])
            if asks_owner and asks_reason:
                return "OWNER_REASON"
            if asks_owner:
                return "OWNER"
            if self._contains_any(text, ["下一步", "怎么处理", "怎么办", "怎么解决"]):
                return "NEXT_ACTION"
            if asks_reason or self._contains_any(text, ["卡在哪", "没完成"]):
                return "CAUSE"
            if self._contains_any(text, ["证据", "依据", "根据什么"]):
                return "EVIDENCE"
            return "FULL_DIAGNOSIS"

        if intent == WorkflowIntent.WARNING_SCAN.value:
            if self._contains_any(text, ["最严重", "优先", "先处理"]):
                return "TOP_RISK"
            return "SUMMARY"

        if intent == WorkflowIntent.SUPPLIER_SCORE.value:
            if self._contains_any(text, ["分数", "意味着", "等级"]):
                return "SCORE_MEANING"
            if self._contains_any(text, ["合作", "继续", "建议"]):
                return "COOP_ADVICE"
            return "FULL_ANALYSIS"

        if intent == WorkflowIntent.KNOWLEDGE_QA.value:
            return "KNOWLEDGE_ANSWER"

        return "CLARIFY"

    def _needs_refresh(self, state: dict, intent: str, message: str) -> bool:
        if not self._has_reusable_result(state, intent):
            return True
        if self._contains_any(message, ["重新", "刷新", "重新扫描", "再扫描", "最新"]):
            return True
        return False

    def _has_reusable_result(self, state: dict, intent: str) -> bool:
        if intent == WorkflowIntent.ORDER_DIAGNOSIS.value:
            return bool(state.get(WorkflowStateKeys.ORDER_DIAGNOSIS))
        if intent == WorkflowIntent.WARNING_SCAN.value:
            return bool(state.get(WorkflowStateKeys.WARNING_ANALYSIS))
        if intent == WorkflowIntent.SUPPLIER_SCORE.value:
            return bool(state.get(WorkflowStateKeys.SUPPLIER_SCORE))
        return False

    def _extract_biz_no(self, text: str) -> str | None:
        match = BIZ_NO_PATTERN.search(text or "")
        return match.group(0).upper() if match else None

    def _extract_order_no(self, text: str) -> str | None:
        biz_no = self._extract_biz_no(text)
        return biz_no if biz_no and biz_no.startswith("PO") else None

    def _extract_supplier_id(self, text: str) -> int | None:
        match = re.search(r"供应商\s*(\d+)", text or "")
        return int(match.group(1)) if match else None

    def _contains_any(self, text: str, words: list[str]) -> bool:
        return any(word in (text or "") for word in words)
```

---

### 6.8 替换节点：app/workflows/nodes/load_order_context.py

路径：

```text
python_ai_workflow_service/app/workflows/nodes/load_order_context.py
```

完整代码：

```python
import json
from typing import Any

from app.clients.inventory_backend import InventoryBackendClient
from app.repositories.session_store import SessionStore
from app.schemas.order_context import AgentOrderContextVO
from app.workflows.state import WorkflowStateKeys


class LoadOrderContextNode:
    def __init__(self, backend: InventoryBackendClient, session_store: SessionStore):
        self.backend = backend
        self.session_store = session_store

    async def __call__(self, state: dict) -> dict:
        entity = dict(state.get(WorkflowStateKeys.ENTITY, {}) or {})
        thread_id = str(state.get(WorkflowStateKeys.THREAD_ID, ""))
        authorization = str(state.get(WorkflowStateKeys.AUTHORIZATION, ""))
        order_no = entity.get("orderNo")

        if not order_no:
            response = {"success": False, "message": "未识别采购订单号"}
            self.session_store.save_tool_message(thread_id, "loadOrderContext", self._json(entity), self._json(response))
            return {WorkflowStateKeys.ERROR_MESSAGE: "未识别采购订单号"}

        raw_context = await self.backend.get_agent_order_context(str(order_no), authorization)
        if not raw_context:
            response = {"success": False, "message": "订单上下文为空", "orderNo": order_no}
            self.session_store.save_tool_message(thread_id, "loadOrderContext", self._json(entity), self._json(response))
            return {WorkflowStateKeys.ERROR_MESSAGE: "订单上下文为空"}

        context = AgentOrderContextVO(**raw_context)
        if not context.exists or context.order is None:
            response = {"success": False, "message": "采购订单号不存在", "orderNo": order_no}
            self.session_store.save_tool_message(thread_id, "loadOrderContext", self._json(entity), self._json(response))
            return {WorkflowStateKeys.ERROR_MESSAGE: "采购订单号不存在"}

        context_dict = context.model_dump(by_alias=True)
        order_snapshot = self._build_order_snapshot(context_dict)

        payload = {"success": True, "data": context_dict}
        self.session_store.save_tool_message(thread_id, "loadOrderContext", self._json(entity), self._json(payload))
        return {
            WorkflowStateKeys.ORDER_CONTEXT: context_dict,
            WorkflowStateKeys.ORDER_SNAPSHOT: order_snapshot,
        }

    def _build_order_snapshot(self, context: dict[str, Any]) -> dict[str, Any]:
        order = dict(context.get("order") or {})
        return {
            "orderId": order.get("orderId"),
            "orderNo": order.get("orderNo"),
            "status": order.get("status"),
            "statusName": order.get("statusName"),
            "supplierId": order.get("supplierId"),
            "supplierName": order.get("supplierName"),
            "totalOrderNumber": order.get("totalOrderNumber", 0),
            "totalArriveNumber": order.get("totalArriveNumber", 0),
            "totalInboundNumber": order.get("totalInboundNumber", 0),
            "arrivalCount": order.get("arrivalCount", 0),
            "inboundCount": order.get("inboundCount", 0),
        }

    def _json(self, value: Any) -> str:
        return json.dumps(value, ensure_ascii=False, default=str)
```

---

### 6.9 替换节点：app/workflows/nodes/order_rule_analyze.py

路径：

```text
python_ai_workflow_service/app/workflows/nodes/order_rule_analyze.py
```

完整代码：

```python
from decimal import Decimal

from app.schemas.diagnosis import OrderDiagnosisVO
from app.workflows.state import WorkflowStateKeys


class OrderRuleAnalyzeNode:
    async def __call__(self, state: dict) -> dict:
        order_context = dict(state.get(WorkflowStateKeys.ORDER_CONTEXT, {}) or {})
        if order_context:
            return {WorkflowStateKeys.ORDER_DIAGNOSIS: self._diagnosis_from_context(order_context)}

        snapshot = dict(state.get(WorkflowStateKeys.ORDER_SNAPSHOT, {}) or {})
        if not snapshot:
            return {WorkflowStateKeys.ERROR_MESSAGE: "订单快照为空"}

        return {WorkflowStateKeys.ORDER_DIAGNOSIS: self._diagnosis_from_snapshot(snapshot)}

    def _diagnosis_from_context(self, context: dict) -> dict:
        order = dict(context.get("order") or {})
        stage = dict(context.get("stage") or {})
        responsibility = dict(context.get("responsibility") or {})
        next_action = dict(context.get("nextAction") or {})
        evidence_items = list(context.get("evidence") or [])

        evidence = []
        for item in evidence_items:
            label = item.get("label")
            value = item.get("value")
            explain = item.get("explain")
            if label and value and explain:
                evidence.append(f"{label}为 {value}，{explain}")

        result = OrderDiagnosisVO(
            orderNo=order.get("orderNo"),
            currentStage=stage.get("currentStage"),
            blockReason=stage.get("blockReason"),
            evidence=evidence,
            suggestOwner=responsibility.get("ownerRole"),
            suggestAction=next_action.get("actionText"),
            responsibility=responsibility,
            nextAction=next_action,
        )
        return result.model_dump(by_alias=True)

    def _diagnosis_from_snapshot(self, snapshot: dict) -> dict:
        total_order = Decimal(str(snapshot.get("totalOrderNumber", 0)))
        total_arrive = Decimal(str(snapshot.get("totalArriveNumber", 0)))
        total_inbound = Decimal(str(snapshot.get("totalInboundNumber", 0)))

        evidence = [
            f"订单状态为 {snapshot.get('status')}",
            f"采购总数量为 {total_order}",
            f"已到货数量 {total_arrive}",
            f"已入库数量 {total_inbound}",
            f"到货次数 {snapshot.get('arrivalCount', 0)}",
            f"入库次数 {snapshot.get('inboundCount', 0)}",
        ]

        status = snapshot.get("status")
        base = {
            "orderNo": snapshot.get("orderNo"),
            "evidence": evidence,
        }

        if status == "WAIT_CONFIRM":
            result = OrderDiagnosisVO(
                **base,
                currentStage="供应商确认阶段",
                blockReason="采购订单仍处于供应商确认状态。",
                suggestOwner="PURCHASER",
                suggestAction="请采购员跟进供应商确认订单并反馈预计交期。",
            )
        elif status == "IN_PROGRESS" and total_arrive == 0:
            result = OrderDiagnosisVO(
                **base,
                currentStage="供应商发货 / 仓库到货登记阶段",
                blockReason="订单已进入执行中，但目前还没有到货记录。",
                suggestOwner="PURCHASER",
                suggestAction="请采购员跟进供应商发货，仓库岗收到货后登记到货。",
            )
        elif status == "PARTIAL_ARRIVAL" and total_arrive < total_order:
            result = OrderDiagnosisVO(
                **base,
                currentStage="剩余到货阶段",
                blockReason="订单已有部分到货，但仍有剩余采购数量未到货。",
                suggestOwner="PURCHASER",
                suggestAction="请采购员催促供应商补齐剩余到货。",
            )
        elif status == "PARTIAL_ARRIVAL" and total_arrive >= total_order and total_inbound < total_order:
            result = OrderDiagnosisVO(
                **base,
                currentStage="入库确认阶段",
                blockReason="订单已全部到货，但仍有部分数量未确认入库。",
                suggestOwner="WAREHOUSE",
                suggestAction="请仓库岗检查待确认入库单并执行确认入库。",
            )
        elif status == "COMPLETED":
            result = OrderDiagnosisVO(
                **base,
                currentStage="流程已完成",
                blockReason="采购订单已完成，无阻塞。",
                suggestOwner="NONE",
                suggestAction="无需处理。",
            )
        elif status in ("CLOSED", "CANCELLED"):
            result = OrderDiagnosisVO(
                **base,
                currentStage="流程已终止",
                blockReason="采购订单已关闭或取消。",
                suggestOwner="PURCHASER",
                suggestAction="如需继续采购，请重新发起采购申请或创建新订单。",
            )
        else:
            result = OrderDiagnosisVO(
                **base,
                currentStage="未知阶段",
                blockReason="当前状态无法根据规则判断阻塞点。",
                suggestOwner="PURCHASER",
                suggestAction="请采购员人工检查订单状态和明细数据。",
            )

        return result.model_dump(by_alias=True)
```

---

### 6.10 新增服务：app/services/answer_humanizer.py

路径：

```text
python_ai_workflow_service/app/services/answer_humanizer.py
```

完整代码：

```python
from typing import Any


class AnswerHumanizer:
    ROLE_LABELS = {
        "PURCHASER": "采购侧",
        "WAREHOUSE": "仓库侧",
        "SUPPLIER": "供应商",
        "ADMIN": "系统管理员",
        "NONE": "无需处理",
        "UNKNOWN": "待确认责任方",
    }

    ROLE_PERSON_LABELS = {
        "PURCHASER": "采购员",
        "WAREHOUSE": "仓库收货员",
        "SUPPLIER": "供应商联系人",
        "NONE": "无需处理",
    }

    STATUS_LABELS = {
        "WAIT_CONFIRM": "待供应商确认",
        "IN_PROGRESS": "执行中",
        "PARTIAL_ARRIVAL": "部分到货",
        "COMPLETED": "已完成",
        "CLOSED": "已关闭",
        "CANCELLED": "已取消",
        "PENDING": "待入库",
    }

    RISK_LABELS = {
        "HIGH": "高风险",
        "MEDIUM": "中风险",
        "LOW": "低风险",
    }

    def role_side(self, code: str | None) -> str:
        if not code:
            return "待确认责任方"
        return self.ROLE_LABELS.get(code, code)

    def role_person(self, code: str | None) -> str:
        if not code:
            return "责任人"
        return self.ROLE_PERSON_LABELS.get(code, code)

    def normalize_text(self, text: str | None) -> str:
        if not text:
            return ""
        result = text
        replacements: dict[str, str] = {}
        replacements.update(self.ROLE_LABELS)
        replacements.update(self.STATUS_LABELS)
        replacements.update(self.RISK_LABELS)
        for raw, label in replacements.items():
            result = result.replace(raw, label)
        return result

    def evidence_text(self, evidence: list[Any]) -> str:
        if not evidence:
            return "当前没有足够证据。"
        normalized = [self.normalize_text(str(item)) for item in evidence if item]
        return "；".join(normalized)
```

---

### 6.11 替换节点：app/workflows/nodes/context_select.py

路径：

```text
python_ai_workflow_service/app/workflows/nodes/context_select.py
```

完整代码只展示订单诊断关键链路；预警和供应商可保留你当前已有逻辑。

如果你希望一次性替换文件，可以用下面这份：

```python
from app.schemas.answer_plan import SelectedContext
from app.workflows.state import InteractionType, WorkflowIntent, WorkflowStateKeys


class ContextSelectNode:
    async def __call__(self, state: dict) -> dict:
        interaction_type = str(state.get(WorkflowStateKeys.INTERACTION_TYPE, InteractionType.BUSINESS.value))
        intent = str(state.get(WorkflowStateKeys.INTENT, WorkflowIntent.UNKNOWN.value))
        plan = dict(state.get(WorkflowStateKeys.ANSWER_PLAN, {}) or {})

        if interaction_type != InteractionType.BUSINESS.value:
            selected = SelectedContext(
                interactionType=interaction_type,
                intent=WorkflowIntent.UNKNOWN.value,
                questionFocus=plan.get("questionFocus", interaction_type),
                answerMode=plan.get("answerMode", interaction_type),
                summary="非业务问题",
                facts={},
                instruction="简短自然回答，不进入业务分析。",
            )
        elif intent == WorkflowIntent.ORDER_DIAGNOSIS.value:
            selected = self._select_order_context(state, plan)
        elif intent == WorkflowIntent.KNOWLEDGE_QA.value:
            selected = SelectedContext(
                interactionType=interaction_type,
                intent=intent,
                questionFocus=plan.get("questionFocus", "KNOWLEDGE_ANSWER"),
                answerMode=plan.get("answerMode", "FULL_ANALYSIS"),
                summary="知识库问答",
                facts={"ragDocs": state.get(WorkflowStateKeys.RAG_DOCS, "")},
                instruction="基于知识库片段回答用户问题。",
            )
        else:
            selected = SelectedContext(
                interactionType=interaction_type,
                intent=intent,
                questionFocus=plan.get("questionFocus", "FULL_ANALYSIS"),
                answerMode=plan.get("answerMode", "FULL_ANALYSIS"),
                summary="当前业务暂未接入问题聚焦回答",
                facts={},
                instruction="提示用户当前问题暂不支持。",
            )

        return {WorkflowStateKeys.SELECTED_CONTEXT: selected.model_dump(by_alias=True)}

    def _select_order_context(self, state: dict, plan: dict) -> SelectedContext:
        diagnosis = dict(state.get(WorkflowStateKeys.ORDER_DIAGNOSIS, {}) or {})
        snapshot = dict(state.get(WorkflowStateKeys.ORDER_SNAPSHOT, {}) or {})
        question_focus = plan.get("questionFocus", "FULL_DIAGNOSIS")

        responsibility = dict(diagnosis.get("responsibility") or {})
        next_action = dict(diagnosis.get("nextAction") or {})

        common_facts = {
            "orderNo": diagnosis.get("orderNo") or snapshot.get("orderNo"),
            "currentStage": diagnosis.get("currentStage"),
            "blockReason": diagnosis.get("blockReason"),
            "responsibility": responsibility,
            "nextAction": next_action,
            "evidence": diagnosis.get("evidence", []),
            "snapshot": snapshot,
        }

        if question_focus in {"OWNER", "OWNER_REASON"}:
            instruction = "回答责任人和选择理由。若没有具体人，只能说明责任角色，不能编造人名。"
            facts = common_facts
        elif question_focus == "NEXT_ACTION":
            instruction = "只回答下一步由谁处理、怎么处理。"
            facts = common_facts
        elif question_focus == "CAUSE":
            instruction = "解释为什么没完成、卡在哪。"
            facts = common_facts
        elif question_focus == "EVIDENCE":
            instruction = "只解释判断依据。"
            facts = common_facts
        else:
            instruction = "给出完整但简洁的订单诊断。"
            facts = common_facts

        return SelectedContext(
            interactionType=InteractionType.BUSINESS.value,
            intent=WorkflowIntent.ORDER_DIAGNOSIS.value,
            questionFocus=question_focus,
            answerMode=plan.get("answerMode", "FULL_ANALYSIS"),
            summary=diagnosis.get("blockReason"),
            facts=facts,
            instruction=instruction,
        )
```

---

### 6.12 替换节点：app/workflows/nodes/business_answer_generate.py

路径：

```text
python_ai_workflow_service/app/workflows/nodes/business_answer_generate.py
```

完整代码：

```python
import json

from app.clients.llm_client import LLMClient
from app.services.answer_humanizer import AnswerHumanizer
from app.workflows.state import InteractionType, WorkflowStateKeys


class BusinessAnswerGenerateNode:
    def __init__(self, llm_client: LLMClient):
        self.llm_client = llm_client
        self.humanizer = AnswerHumanizer()

    async def __call__(self, state: dict) -> dict:
        message = str(state.get(WorkflowStateKeys.MESSAGE, ""))
        error_message = str(state.get(WorkflowStateKeys.ERROR_MESSAGE, ""))
        if error_message:
            return {WorkflowStateKeys.LLM_ANSWER: error_message}

        answer_plan = dict(state.get(WorkflowStateKeys.ANSWER_PLAN, {}) or {})
        selected_context = dict(state.get(WorkflowStateKeys.SELECTED_CONTEXT, {}) or {})
        interaction_type = str(selected_context.get("interactionType", InteractionType.BUSINESS.value))

        if interaction_type != InteractionType.BUSINESS.value:
            return {WorkflowStateKeys.LLM_ANSWER: self._answer_non_business(message, interaction_type)}

        if not answer_plan.get("useLlm", True):
            return {WorkflowStateKeys.LLM_ANSWER: self._focused_answer(selected_context)}

        prompt = self._build_prompt(message, answer_plan, selected_context)
        try:
            answer = await self.llm_client.chat_text(
                "你是采购入库协同业务 Agent。你必须基于给定上下文回答，不能编造事实。",
                prompt,
            )
        except Exception:
            answer = self._focused_answer(selected_context)

        return {WorkflowStateKeys.LLM_ANSWER: self.humanizer.normalize_text(answer)}

    def _answer_non_business(self, message: str, interaction_type: str) -> str:
        if interaction_type == InteractionType.SOCIAL.value:
            return "好，后面你把订单号、供应商ID或扫描范围发给我，我继续帮你看。"
        if interaction_type == InteractionType.META.value:
            return "我是采购入库协同业务 Agent，主要帮你看订单卡点、采购执行风险和供应商履约表现。"
        return "我需要一个更具体的业务对象，比如订单号、供应商ID，或者“扫描最近7天采购风险”。"

    def _focused_answer(self, selected_context: dict) -> str:
        intent = selected_context.get("intent")
        question_focus = selected_context.get("questionFocus")
        facts = selected_context.get("facts") or {}

        if intent == "ORDER_DIAGNOSIS":
            return self._order_answer(question_focus, facts)

        return selected_context.get("summary") or "当前问题暂时无法生成明确回答。"

    def _order_answer(self, question_focus: str, facts: dict) -> str:
        order_no = facts.get("orderNo") or "这张订单"
        current_stage = self.humanizer.normalize_text(facts.get("currentStage"))
        block_reason = self.humanizer.normalize_text(facts.get("blockReason"))
        evidence = facts.get("evidence") or []
        snapshot = facts.get("snapshot") or {}
        responsibility = facts.get("responsibility") or {}
        next_action = facts.get("nextAction") or {}

        owner_role = responsibility.get("ownerRole")
        owner_side = responsibility.get("ownerRoleName") or self.humanizer.role_side(owner_role)
        owner_person = responsibility.get("ownerUserName")
        owner_reason = self.humanizer.normalize_text(responsibility.get("ownerReason"))
        action_text = self.humanizer.normalize_text(next_action.get("actionText"))

        if question_focus == "OWNER":
            if owner_person:
                return f"{order_no} 建议先让 {owner_person} 跟进。{owner_person} 是这张订单当前匹配到的{owner_side}。"
            return (
                f"{order_no} 现在还不能定位到具体某个人，只能判断到责任角色是{owner_side}。"
                f"如果要精确到人，需要后端返回这张订单的负责人姓名或账号。"
            )

        if question_focus == "OWNER_REASON":
            if owner_person:
                return (
                    f"建议让 {owner_person} 跟进。\n\n"
                    f"原因是：{owner_reason or f'{owner_person} 是这张订单当前匹配到的负责人'}。"
                    f"从业务节点看，订单现在处在“{current_stage}”，{block_reason}\n\n"
                    f"下一步：{action_text}"
                )
            return (
                f"现在还不能定位到具体某个{self.humanizer.role_person(owner_role)}，只能判断到责任角色是{owner_side}。\n\n"
                f"为什么是{owner_side}：{owner_reason or block_reason}。"
                f"从当前数据看，订单状态是{self.humanizer.normalize_text(snapshot.get('status'))}，"
                f"采购数量是 {snapshot.get('totalOrderNumber', 0)}，"
                f"到货数量是 {snapshot.get('totalArriveNumber', 0)}，"
                f"入库数量是 {snapshot.get('totalInboundNumber', 0)}。\n\n"
                f"下一步建议：{action_text}。如果要精确到具体人，需要后端返回这张订单的负责人姓名或账号。"
            )

        if question_focus == "NEXT_ACTION":
            if owner_person:
                return f"这单下一步先让 {owner_person} 处理。具体动作是：{action_text}"
            return f"这单下一步先由{owner_side}处理。具体动作是：{action_text}"

        if question_focus == "CAUSE":
            return (
                f"{order_no} 现在卡在“{current_stage}”。"
                f"直接原因是：{block_reason}"
            )

        if question_focus == "EVIDENCE":
            return f"我这样判断的依据是：{self.humanizer.evidence_text(evidence)}。"

        return (
            f"{order_no} 当前卡在“{current_stage}”。"
            f"原因是：{block_reason}"
            f"建议动作：{action_text}"
        )

    def _build_prompt(self, message: str, answer_plan: dict, selected_context: dict) -> str:
        return f"""
你是采购入库协同业务 Agent。

回答规则：
1. 先回答用户当前问题，不要重复完整报告。
2. 如果上下文没有具体负责人，必须明确说只能判断到责任角色，不能编造人名。
3. 不要直接输出内部码值，例如 PURCHASER、WAREHOUSE、IN_PROGRESS。
4. 回答按“结论 -> 原因 -> 下一步 -> 信息边界”组织。

用户问题：
{message}

回答计划：
{json.dumps(answer_plan, ensure_ascii=False, default=str)}

上下文：
{json.dumps(selected_context, ensure_ascii=False, default=str)}
""".strip()
```

---

### 6.13 session_store.py 局部修改

路径：

```text
python_ai_workflow_service/app/repositories/session_store.py
```

把 `load_state_by_thread_id` 里的 `restorable_keys` 改成：

```python
restorable_keys = [
    WorkflowStateKeys.INTENT,
    WorkflowStateKeys.ACTIVE_INTENT,
    WorkflowStateKeys.ENTITY,
    WorkflowStateKeys.ORDER_CONTEXT,
    WorkflowStateKeys.ORDER_SNAPSHOT,
    WorkflowStateKeys.ORDER_DIAGNOSIS,
    WorkflowStateKeys.WARNING_ANALYSIS,
    WorkflowStateKeys.SUPPLIER_METRICS,
    WorkflowStateKeys.SUPPLIER_SCORE,
    WorkflowStateKeys.ANSWER_PLAN,
    WorkflowStateKeys.SELECTED_CONTEXT,
]
```

原因：

```text
如果不保存 ORDER_CONTEXT 和 ACTIVE_INTENT，
用户中间问一句“你是什么模型”或“好的”，
下一轮再问“那让谁处理”，Agent 可能接不上订单上下文。
```

---

## 7. 测试步骤

### 7.1 先测 Java 后端

登录后请求：

```http
GET http://localhost:8080/agent/context/order/PO2026040022
Authorization: Bearer <token>
```

必须看到：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "exists": true,
    "order": {
      "orderNo": "PO2026040022",
      "status": "IN_PROGRESS",
      "statusName": "执行中"
    },
    "responsibility": {
      "ownerRole": "PURCHASER",
      "ownerRoleName": "采购员",
      "ownerUserId": 1,
      "ownerUserName": "某个采购员"
    }
  }
}
```

如果 `ownerUserName` 是 `null`，也可以，但 Python 必须说明不能定位到具体人。

### 7.2 再测订单首问

```json
{
  "message": "帮我分析 PO2026040022 为什么还没完成"
}
```

预期：

```text
能返回订单卡在哪、为什么、建议动作。
```

### 7.3 再测责任人追问

```json
{
  "message": "让哪个采购员跟进？为什么选他？",
  "threadId": "上一步返回的 threadId"
}
```

如果 Java 有具体人：

```text
必须回答具体人名。
```

如果 Java 没具体人：

```text
必须说只能定位到采购侧，不能定位到具体采购员。
```

### 7.4 再测下一步

```json
{
  "message": "下一步谁来解决这个问题，怎么解决？",
  "threadId": "同一个 threadId"
}
```

预期：

```text
只回答下一步，不重复完整报告。
```

### 7.5 检查内部码值

最终 `answer` 里不应该出现：

```text
PURCHASER
WAREHOUSE
IN_PROGRESS
WAIT_CONFIRM
```

---

## 8. 这套方案为什么是最终闭环

因为它不再试图让模型猜。

它把责任拆清楚了：

```text
Java：
    订单事实
    负责人
    责任来源
    证据
    下一步动作

Python：
    用户问题焦点
    数据够不够
    上下文选择
    自然表达
```

所以当用户问：

```text
哪个采购员？
```

系统不会再装作知道。

它会根据事实回答：

```text
有具体人 -> 说具体人
没具体人 -> 明确说只能定位到角色
```

这才是一个业务 Agent 该有的可靠性。

---

## 9. 最重要的一句话

```text
Agent 的聪明感，不是来自更花哨的语言，
而是来自它知道用户问的焦点、知道当前事实够不够、知道不能编造。
```

这条链路落地后，订单诊断的核心体验会从：

```text
规则摘要器
```

变成：

```text
能判断责任、能解释原因、能说明边界的业务 Agent
```

