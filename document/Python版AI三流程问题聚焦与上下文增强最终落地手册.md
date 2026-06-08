# 《Python版 AI 三流程问题聚焦与上下文增强最终落地手册》

> 本文档替代并扩展：
>
> ```text
> document/Python版AI问题聚焦与责任上下文最终落地手册.md
> ```
>
> 上一版只把“订单诊断 + 责任人追问”讲完整了。
>
> 这一版给出最终完整方案：
>
> ```text
> 订单诊断
> 预警扫描
> 供应商评分
> ```
>
> 三条业务链路都要具备：
>
> ```text
> 问题聚焦
> 后端事实补足
> 数据充分性判断
> 角色码/状态码/风险码中文化
> 自然回答
> ```

---

## 0. 这份文档解决什么问题

你现在的 Agent 已经不是“不能跑”的问题。

它现在的问题是：

```text
能跑，但还不够聪明。
能回答，但经常没有真正回答用户问的焦点。
能变快，但快路径容易像死模板。
能给业务结论，但会暴露 PURCHASER / IN_PROGRESS / HIGH 这种内部码值。
```

典型问题有三类。

### 0.1 订单诊断问题

用户问：

```text
让哪个采购员跟进？为什么选他？
```

当前 Agent 容易回答成：

```text
这单卡在供应商发货 / 仓库到货登记阶段。
```

这没有真正回答：

```text
1. 哪个人？
2. 为什么是这个人？
3. 如果没有具体人，能不能明确说只能判断到角色？
```

### 0.2 预警扫描问题

用户问：

```text
为什么 PO2026040001 风险这么高？
```

当前 Agent 容易回答成：

```text
该单命中高风险，建议优先处理。
```

这没有真正回答：

```text
1. 为什么判高风险？
2. 命中了哪条规则？
3. 为什么它比其他单更优先？
4. 应该由采购侧还是仓库侧处理？
```

### 0.3 供应商评分问题

用户问：

```text
这个供应商为什么只有 65 分？到底差在哪？
```

当前 Agent 容易回答成：

```text
该供应商评分 65，等级一般，建议关注履约表现。
```

这没有真正回答：

```text
1. 65 分是怎么算出来的？
2. 哪些指标扣分最多？
3. 是确认差、到货差、入库差，还是异常多？
4. 下一步应该怎么管控？
```

所以这次要解决的不是“让模型再润色一下”。

这次要做的是：

```text
Java 后端给足事实。
Python Agent 聚焦问题。
Python Agent 判断事实够不够。
Python Agent 再用人话回答。
```

---

## 1. 最终架构总览

最终链路如下：

```text
用户问题
    ↓
POST /agent/workflow/execute
    ↓
preprocessInput
    ↓
intentClassify
    判断 ORDER_DIAGNOSIS / WARNING_SCAN / SUPPLIER_SCORE
    ↓
entityExtract
    抽取 orderNo / days / supplierId / bizNo
    ↓
answerPlan
    识别 questionFocus
    ↓
routeDecision
    判断复用上下文还是重新加载
    ↓
loadXXXContext
    调 Java 新增上下文接口
    ↓
ruleAnalyze
    把 Java 事实上下文整理成结构化结果
    ↓
contextSelect
    按 questionFocus 只挑必要事实
    ↓
businessAnswerGenerate
    判断数据是否足够
    不够就说明边界
    够就按“结论 -> 原因 -> 证据 -> 下一步”回答
```

这条链路的核心不是 LLM。

核心是：

```text
questionFocus
```

也就是：

```text
用户这一轮到底问的是什么？
```

---

## 2. 三条业务链路的 questionFocus

不要再只靠 `intent`。

`intent` 只能告诉你用户属于哪条业务线：

```text
ORDER_DIAGNOSIS
WARNING_SCAN
SUPPLIER_SCORE
```

但它不能告诉你用户这一轮具体问什么。

所以这次新增：

```text
questionFocus
```

### 2.1 订单诊断 questionFocus

```text
FULL_DIAGNOSIS
    完整诊断。

CAUSE
    用户问为什么没完成、卡在哪。

NEXT_ACTION
    用户问下一步怎么办。

OWNER
    用户问谁处理、哪个采购员处理。

OWNER_REASON
    用户问为什么让这个人/这个角色处理。

EVIDENCE
    用户问判断依据是什么。
```

### 2.2 预警扫描 questionFocus

```text
WARNING_SUMMARY
    用户问总体风险概况。

TOP_RISK
    用户问先处理哪些、哪些最严重。

SPECIFIC_WARNING_REASON
    用户问某张单为什么风险高。

WARNING_OWNER
    用户问该由谁处理这些风险。

WARNING_PRIORITY_REASON
    用户问为什么这些单优先级更高。

WARNING_ACTION
    用户问下一步怎么处理风险。
```

### 2.3 供应商评分 questionFocus

```text
SUPPLIER_FULL_ANALYSIS
    用户问完整履约分析。

SCORE_MEANING
    用户问这个分数意味着什么。

SCORE_REASON
    用户问为什么是这个分。

WEAK_METRIC
    用户问差在哪、哪些指标拖后腿。

COOP_ADVICE
    用户问还能不能继续合作。

SUPPLIER_ACTION
    用户问下一步怎么管控供应商。
```

---

## 3. Java 和 Python 的职责边界

这次必须把边界定死。

### 3.1 Java 后端负责什么

Java 后端负责事实。

```text
1. 订单事实
2. 采购负责人
3. 预警规则命中原因
4. 预警优先级
5. 供应商履约指标
6. 供应商评分拆分
7. 供应商短板指标
8. 所有业务码的中文含义
```

Java 后端不负责：

```text
最终对话表达
多轮追问
用户意图理解
```

### 3.2 Python Agent 负责什么

Python 负责对话。

```text
1. 判断用户问的是哪个 questionFocus
2. 判断是否可以复用上一次上下文
3. 从 Java 上下文里挑本轮必要事实
4. 判断事实够不够
5. 用自然语言回答
```

Python 不应该做：

```text
猜具体采购员是谁
猜仓库负责人是谁
猜供应商评分来源
猜预警规则原因
```

这些都应该由 Java 后端提供。

---

## 4. Java 后端最终新增接口

最终 Java 后端新增三个上下文接口。

```text
GET /agent/context/order/{orderNo}
GET /agent/context/warnings?days=7
GET /agent/context/supplier/{supplierId}?days=30
```

为什么不是继续让 Python 调很多已有业务接口？

原因有三个：

```text
1. Python 多次调 Java 会慢
2. Python 拼业务事实容易漏字段
3. 责任人、评分、预警优先级这些应该由 Java 后端统一规则计算
```

---

## 5. Java 后端完整代码

### 5.1 新增 VO：AgentOrderContextRowVO.java

路径：

```text
inventory_back/src/main/java/com/xixi/agent/vo/AgentOrderContextRowVO.java
```

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

### 5.3 新增 VO：AgentWarningContextVO.java

路径：

```text
inventory_back/src/main/java/com/xixi/agent/vo/AgentWarningContextVO.java
```

```java
package com.xixi.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentWarningContextVO {
    private Integer days;
    private WarningSummary summary;
    private List<WarningItem> items = new ArrayList<>();
    private List<WarningItem> topItems = new ArrayList<>();
    private List<OwnerStat> ownerStats = new ArrayList<>();
    private List<RiskTypeStat> riskTypeStats = new ArrayList<>();

    @Data
    public static class WarningSummary {
        private Integer totalCount;
        private Integer highCount;
        private Integer mediumCount;
        private Integer lowCount;
        private String summaryText;
    }

    @Data
    public static class WarningItem {
        private String riskLevel;
        private String riskLevelName;
        private String bizType;
        private String bizTypeName;
        private Long bizId;
        private String bizNo;
        private String status;
        private String statusName;
        private String problem;
        private String reason;
        private Integer overdueDays;
        private Integer priorityScore;
        private String priorityReason;
        private String suggestOwner;
        private String suggestOwnerName;
        private String suggestAction;
        private Long supplierId;
        private String supplierName;
        private Long warehouseId;
        private String warehouseName;
    }

    @Data
    public static class OwnerStat {
        private String ownerRole;
        private String ownerRoleName;
        private Integer count;
    }

    @Data
    public static class RiskTypeStat {
        private String problem;
        private Integer count;
    }
}
```

说明：

```text
WarningItem 不是简单风险项。
它必须同时给出：

1. 风险等级
2. 中文风险等级
3. 命中规则
4. 具体原因
5. 超时天数
6. 优先级分
7. 为什么优先
8. 建议责任方
```

这样 Python 才能回答：

```text
为什么这个单风险高？
为什么它优先处理？
应该由谁处理？
```

---

### 5.4 新增 VO：AgentSupplierContextVO.java

路径：

```text
inventory_back/src/main/java/com/xixi/agent/vo/AgentSupplierContextVO.java
```

```java
package com.xixi.agent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentSupplierContextVO {
    private Boolean exists;
    private Integer days;
    private SupplierInfo supplier;
    private Metrics metrics;
    private Score score;
    private List<ScoreBreakdown> scoreBreakdown = new ArrayList<>();
    private List<WeakMetric> weakMetrics = new ArrayList<>();
    private String analysisSummary;
    private String suggestion;

    @Data
    public static class SupplierInfo {
        private Long supplierId;
        private String supplierName;
    }

    @Data
    public static class Metrics {
        private Integer totalOrderCount;
        private Integer completedOrderCount;
        private Integer cancelledOrderCount;
        private Integer abnormalArrivalCount;
        private Integer totalArrivalCount;
        private String confirmRate;
        private String arrivalCompletionRate;
        private String inboundCompletionRate;
        private String abnormalArrivalRate;
    }

    @Data
    public static class Score {
        private Integer totalScore;
        private String level;
        private String levelExplain;
    }

    @Data
    public static class ScoreBreakdown {
        private String metricCode;
        private String metricName;
        private Integer maxScore;
        private Integer actualScore;
        private String value;
        private String explain;
    }

    @Data
    public static class WeakMetric {
        private String metricCode;
        private String metricName;
        private String value;
        private String reason;
        private String suggestion;
    }
}
```

说明：

```text
供应商评分最容易“看起来会答，实际上没答”。

只返回 score=65 不够。

必须返回：
1. 每个指标拿了多少分
2. 每个指标满分多少
3. 哪些指标拖后腿
4. 为什么拖后腿
5. 下一步怎么改善
```

---

### 5.5 新增 Mapper：AgentOrderContextMapper.java

路径：

```text
inventory_back/src/main/java/com/xixi/agent/mapper/AgentOrderContextMapper.java
```

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

### 5.6 新增 XML：AgentOrderContextMapper.xml

路径：

```text
inventory_back/src/main/resources/com/xixi/mapper/AgentOrderContextMapper.xml
```

```xml
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.xixi.agent.mapper.AgentOrderContextMapper">
    <select id="getOrderContextBaseByOrderNo" resultType="com.xixi.agent.vo.AgentOrderContextRowVO">
        <!--
            Agent 订单上下文查询。
            一次性返回订单、供应商、采购负责人、到货数量、入库数量。
            这样 Python 回答“哪个采购员跟进、为什么选他”时，不需要猜。
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

### 5.7 新增 Service：AgentBusinessContextService.java

路径：

```text
inventory_back/src/main/java/com/xixi/agent/service/AgentBusinessContextService.java
```

```java
package com.xixi.agent.service;

import com.xixi.agent.vo.AgentOrderContextVO;
import com.xixi.agent.vo.AgentSupplierContextVO;
import com.xixi.agent.vo.AgentWarningContextVO;

public interface AgentBusinessContextService {
    AgentOrderContextVO getOrderContext(String orderNo);

    AgentWarningContextVO scanWarningContext(Integer days);

    AgentSupplierContextVO getSupplierContext(Long supplierId, Integer days);
}
```

---

### 5.8 新增 ServiceImpl：AgentBusinessContextServiceImpl.java

路径：

```text
inventory_back/src/main/java/com/xixi/agent/service/impl/AgentBusinessContextServiceImpl.java
```

完整代码较长，但这是整套 Java 事实上下文的核心。

```java
package com.xixi.agent.service.impl;

import com.xixi.agent.mapper.AgentOrderContextMapper;
import com.xixi.agent.mapper.AgentWarningMapper;
import com.xixi.agent.mapper.SupplierPerformanceMapper;
import com.xixi.agent.service.AgentBusinessContextService;
import com.xixi.agent.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentBusinessContextServiceImpl implements AgentBusinessContextService {

    private static final String ROLE_PURCHASER = "PURCHASER";
    private static final String ROLE_WAREHOUSE = "WAREHOUSE";
    private static final String ROLE_SUPPLIER = "SUPPLIER";
    private static final String ROLE_NONE = "NONE";

    private final AgentOrderContextMapper agentOrderContextMapper;
    private final AgentWarningMapper agentWarningMapper;
    private final SupplierPerformanceMapper supplierPerformanceMapper;

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
        fillOrderStageResponsibilityAndAction(context, row);
        fillOrderEvidence(context, row);
        return context;
    }

    @Override
    public AgentWarningContextVO scanWarningContext(Integer days) {
        int realDays = days == null ? 7 : days;
        List<AgentWarningContextVO.WarningItem> items = new ArrayList<>();

        appendWarnings(
                items,
                agentWarningMapper.getWaitConfirmOverdueOrders(realDays),
                "HIGH",
                "PURCHASE_ORDER",
                "采购订单待供应商确认超时",
                "订单长时间停留在待供应商确认状态",
                ROLE_PURCHASER
        );
        appendWarnings(
                items,
                agentWarningMapper.getInProgressWithoutArrivalOrders(realDays),
                "HIGH",
                "PURCHASE_ORDER",
                "采购订单执行中但无到货",
                "订单进入执行中后长时间没有到货记录",
                ROLE_PURCHASER
        );
        appendWarnings(
                items,
                agentWarningMapper.getPartialArrivalStuckOrders(realDays),
                "MEDIUM",
                "PURCHASE_ORDER",
                "采购订单部分到货后停滞",
                "订单处于部分到货状态且长时间没有新到货",
                ROLE_PURCHASER
        );
        appendWarnings(
                items,
                agentWarningMapper.getArrivedWithoutInboundRecords(realDays),
                "HIGH",
                "ARRIVAL",
                "到货后未生成入库单",
                "到货记录已经存在，但仍未生成入库单",
                ROLE_WAREHOUSE
        );
        appendWarnings(
                items,
                agentWarningMapper.getPendingInboundOverdueRecords(realDays),
                "MEDIUM",
                "INBOUND",
                "待确认入库单超时",
                "入库单长时间处于待入库状态",
                ROLE_WAREHOUSE
        );

        items.sort(Comparator.comparing(AgentWarningContextVO.WarningItem::getPriorityScore).reversed());

        AgentWarningContextVO context = new AgentWarningContextVO();
        context.setDays(realDays);
        context.setItems(items);
        context.setTopItems(items.stream().limit(10).collect(Collectors.toList()));
        context.setSummary(buildWarningSummary(items));
        context.setOwnerStats(buildOwnerStats(items));
        context.setRiskTypeStats(buildRiskTypeStats(items));
        return context;
    }

    @Override
    public AgentSupplierContextVO getSupplierContext(Long supplierId, Integer days) {
        int realDays = days == null ? 30 : days;
        AgentSupplierContextVO context = new AgentSupplierContextVO();
        context.setDays(realDays);

        if (supplierId == null) {
            context.setExists(false);
            context.setAnalysisSummary("supplierId 不能为空。");
            context.setSuggestion("请传入供应商ID。");
            return context;
        }

        SupplierPerformanceMetricsVO metrics = supplierPerformanceMapper.getSupplierPerformanceMetrics(supplierId, realDays);
        if (metrics == null) {
            context.setExists(false);
            context.setAnalysisSummary("供应商不存在。");
            context.setSuggestion("请确认 supplierId 是否正确。");
            return context;
        }

        context.setExists(true);
        context.setSupplier(buildSupplierInfo(metrics));
        context.setMetrics(buildSupplierMetrics(metrics));
        fillSupplierScoreAndAdvice(context, metrics);
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

    private void fillOrderStageResponsibilityAndAction(AgentOrderContextVO context, AgentOrderContextRowVO row) {
        BigDecimal totalOrder = safe(row.getTotalOrderNumber());
        BigDecimal totalArrive = safe(row.getTotalArriveNumber());
        BigDecimal totalInbound = safe(row.getTotalInboundNumber());
        String status = row.getStatus();

        if ("WAIT_CONFIRM".equals(status)) {
            fillOrderContext(context, row, "供应商确认阶段", "采购订单仍处于待供应商确认状态。", ROLE_PURCHASER,
                    "当前需要采购侧跟进供应商确认订单和预计交期。", "请采购负责人联系供应商确认订单，并同步预计交期。");
            return;
        }

        if ("IN_PROGRESS".equals(status) && totalArrive.compareTo(BigDecimal.ZERO) == 0) {
            fillOrderContext(context, row, "供应商发货 / 仓库到货登记阶段", "订单已进入执行中，但目前还没有到货记录。", ROLE_PURCHASER,
                    "当前问题卡在供应商发货前段，仓库暂时没有可登记的到货对象，所以第一责任点在采购侧。", "请采购负责人先联系供应商确认发货时间，到货后再通知仓库登记到货。");
            return;
        }

        if ("PARTIAL_ARRIVAL".equals(status) && totalArrive.compareTo(totalOrder) < 0) {
            fillOrderContext(context, row, "剩余到货阶段", "订单已有部分到货，但仍有剩余采购数量未到货。", ROLE_PURCHASER,
                    "当前还缺剩余到货，核心动作仍然是采购侧推动供应商补齐发货。", "请采购负责人催促供应商补齐剩余到货。");
            return;
        }

        if ("PARTIAL_ARRIVAL".equals(status) && totalArrive.compareTo(totalOrder) >= 0 && totalInbound.compareTo(totalOrder) < 0) {
            fillOrderContext(context, row, "入库确认阶段", "订单已全部到货，但仍有部分数量未确认入库。", ROLE_WAREHOUSE,
                    "当前货物已经到达，问题转到仓库入库确认环节，所以第一责任点在仓库侧。", "请仓库侧检查待确认入库单并执行确认入库。");
            return;
        }

        if ("COMPLETED".equals(status)) {
            fillOrderContext(context, row, "流程已完成", "采购订单已完成，无阻塞。", ROLE_NONE,
                    "订单已经完成，不需要继续指定处理人。", "无需处理。");
            return;
        }

        fillOrderContext(context, row, "未知阶段", "当前状态无法根据规则判断阻塞点。", ROLE_PURCHASER,
                "系统无法自动判断责任点，建议先由采购侧人工核对订单状态和明细数据。", "请采购负责人人工检查订单状态、到货记录和入库记录。");
    }

    private void fillOrderContext(AgentOrderContextVO context, AgentOrderContextRowVO row, String currentStage,
                                  String blockReason, String ownerRole, String ownerReason, String actionText) {
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

    private void fillOrderEvidence(AgentOrderContextVO context, AgentOrderContextRowVO row) {
        context.getEvidence().add(evidence("ORDER_STATUS", "订单状态", statusName(row.getStatus()), "订单当前状态决定了流程所处节点。"));
        context.getEvidence().add(evidence("ORDER_QTY", "采购数量", safe(row.getTotalOrderNumber()).toPlainString(), "采购数量用于判断到货和入库是否完成。"));
        context.getEvidence().add(evidence("ARRIVAL_QTY", "到货数量", safe(row.getTotalArriveNumber()).toPlainString(), "如果到货数量为 0，说明仓库暂时没有可登记的到货对象。"));
        context.getEvidence().add(evidence("INBOUND_QTY", "入库数量", safe(row.getTotalInboundNumber()).toPlainString(), "入库数量用于判断是否已经完成仓库入库确认。"));
    }

    private void appendWarnings(List<AgentWarningContextVO.WarningItem> items, List<WarningSnapshotVO> snapshots,
                                String riskLevel, String bizType, String problem, String reason, String owner) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        for (WarningSnapshotVO snapshot : snapshots) {
            AgentWarningContextVO.WarningItem item = new AgentWarningContextVO.WarningItem();
            item.setRiskLevel(riskLevel);
            item.setRiskLevelName(riskLevelName(riskLevel));
            item.setBizType(bizType);
            item.setBizTypeName(bizTypeName(bizType));
            item.setBizId(snapshot.getBizId());
            item.setBizNo(snapshot.getBizNo());
            item.setStatus(snapshot.getStatus());
            item.setStatusName(statusName(snapshot.getStatus()));
            item.setProblem(problem);
            item.setReason(reason + "，已超时 " + snapshot.getOverdueDays() + " 天。");
            item.setOverdueDays(snapshot.getOverdueDays());
            item.setSuggestOwner(owner);
            item.setSuggestOwnerName(roleName(owner));
            item.setSuggestAction(buildWarningAction(snapshot.getBizNo(), owner, problem));
            item.setSupplierId(snapshot.getSupplierId());
            item.setSupplierName(snapshot.getSupplierName());
            item.setWarehouseId(snapshot.getWarehouseId());
            item.setWarehouseName(snapshot.getWarehouseName());
            item.setPriorityScore(priorityScore(riskLevel, snapshot.getOverdueDays()));
            item.setPriorityReason(buildPriorityReason(riskLevel, snapshot.getOverdueDays(), problem));
            items.add(item);
        }
    }

    private AgentWarningContextVO.WarningSummary buildWarningSummary(List<AgentWarningContextVO.WarningItem> items) {
        AgentWarningContextVO.WarningSummary summary = new AgentWarningContextVO.WarningSummary();
        int high = (int) items.stream().filter(item -> "HIGH".equals(item.getRiskLevel())).count();
        int medium = (int) items.stream().filter(item -> "MEDIUM".equals(item.getRiskLevel())).count();
        int low = (int) items.stream().filter(item -> "LOW".equals(item.getRiskLevel())).count();
        summary.setTotalCount(items.size());
        summary.setHighCount(high);
        summary.setMediumCount(medium);
        summary.setLowCount(low);
        summary.setSummaryText("本次扫描共发现 " + items.size() + " 个执行风险，其中高风险 " + high + " 个，中风险 " + medium + " 个。");
        return summary;
    }

    private List<AgentWarningContextVO.OwnerStat> buildOwnerStats(List<AgentWarningContextVO.WarningItem> items) {
        Map<String, Long> grouped = items.stream().collect(Collectors.groupingBy(AgentWarningContextVO.WarningItem::getSuggestOwner, Collectors.counting()));
        List<AgentWarningContextVO.OwnerStat> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : grouped.entrySet()) {
            AgentWarningContextVO.OwnerStat stat = new AgentWarningContextVO.OwnerStat();
            stat.setOwnerRole(entry.getKey());
            stat.setOwnerRoleName(roleName(entry.getKey()));
            stat.setCount(entry.getValue().intValue());
            result.add(stat);
        }
        return result;
    }

    private List<AgentWarningContextVO.RiskTypeStat> buildRiskTypeStats(List<AgentWarningContextVO.WarningItem> items) {
        Map<String, Long> grouped = items.stream().collect(Collectors.groupingBy(AgentWarningContextVO.WarningItem::getProblem, Collectors.counting()));
        List<AgentWarningContextVO.RiskTypeStat> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : grouped.entrySet()) {
            AgentWarningContextVO.RiskTypeStat stat = new AgentWarningContextVO.RiskTypeStat();
            stat.setProblem(entry.getKey());
            stat.setCount(entry.getValue().intValue());
            result.add(stat);
        }
        result.sort(Comparator.comparing(AgentWarningContextVO.RiskTypeStat::getCount).reversed());
        return result;
    }

    private AgentSupplierContextVO.SupplierInfo buildSupplierInfo(SupplierPerformanceMetricsVO metrics) {
        AgentSupplierContextVO.SupplierInfo supplier = new AgentSupplierContextVO.SupplierInfo();
        supplier.setSupplierId(metrics.getSupplierId());
        supplier.setSupplierName(metrics.getSupplierName());
        return supplier;
    }

    private AgentSupplierContextVO.Metrics buildSupplierMetrics(SupplierPerformanceMetricsVO metrics) {
        AgentSupplierContextVO.Metrics result = new AgentSupplierContextVO.Metrics();
        result.setTotalOrderCount(nullToZero(metrics.getTotalOrderCount()));
        result.setCompletedOrderCount(nullToZero(metrics.getCompletedOrderCount()));
        result.setCancelledOrderCount(nullToZero(metrics.getCancelledOrderCount()));
        result.setAbnormalArrivalCount(nullToZero(metrics.getAbnormalArrivalCount()));
        result.setTotalArrivalCount(nullToZero(metrics.getTotalArrivalCount()));
        result.setConfirmRate(formatRate(safeRate(metrics.getConfirmRate())));
        result.setArrivalCompletionRate(formatRate(safeRate(metrics.getArrivalCompletionRate())));
        result.setInboundCompletionRate(formatRate(safeRate(metrics.getInboundCompletionRate())));
        result.setAbnormalArrivalRate(formatRate(safeRate(metrics.getAbnormalArrivalCount(), metrics.getTotalArrivalCount())));
        return result;
    }

    private void fillSupplierScoreAndAdvice(AgentSupplierContextVO context, SupplierPerformanceMetricsVO metrics) {
        int totalOrderCount = nullToZero(metrics.getTotalOrderCount());
        if (totalOrderCount == 0) {
            AgentSupplierContextVO.Score score = new AgentSupplierContextVO.Score();
            score.setTotalScore(0);
            score.setLevel("数据不足");
            score.setLevelExplain("当前统计周期内没有采购订单履约数据，不能形成有效评分。");
            context.setScore(score);
            context.setAnalysisSummary("该供应商存在，但当前统计周期内暂无采购订单履约数据。");
            context.setSuggestion("建议扩大统计周期后重新分析，例如查看最近90天或180天。");
            return;
        }

        double confirmRate = safeRate(metrics.getConfirmRate());
        double arrivalRate = safeRate(metrics.getArrivalCompletionRate());
        double inboundRate = safeRate(metrics.getInboundCompletionRate());
        double abnormalRate = safeRate(metrics.getAbnormalArrivalCount(), metrics.getTotalArrivalCount());
        double cancelRate = safeRate(metrics.getCancelledOrderCount(), metrics.getTotalOrderCount());

        int confirmScore = (int) Math.round(confirmRate * 20);
        int arrivalScore = (int) Math.round(arrivalRate * 30);
        int inboundScore = (int) Math.round(inboundRate * 20);
        int abnormalScore = (int) Math.round((1 - abnormalRate) * 20);
        int cancelScore = (int) Math.round((1 - cancelRate) * 10);
        int totalScore = Math.max(0, Math.min(100, confirmScore + arrivalScore + inboundScore + abnormalScore + cancelScore));

        AgentSupplierContextVO.Score score = new AgentSupplierContextVO.Score();
        score.setTotalScore(totalScore);
        score.setLevel(resolveLevel(totalScore));
        score.setLevelExplain(resolveLevelExplain(totalScore));
        context.setScore(score);

        addScoreBreakdown(context, "CONFIRM_RATE", "确认及时率", 20, confirmScore, formatRate(confirmRate), "供应商是否及时确认采购订单。");
        addScoreBreakdown(context, "ARRIVAL_COMPLETION_RATE", "到货完成率", 30, arrivalScore, formatRate(arrivalRate), "供应商是否按订单完成到货。");
        addScoreBreakdown(context, "INBOUND_COMPLETION_RATE", "入库完成率", 20, inboundScore, formatRate(inboundRate), "到货后是否顺利完成入库。");
        addScoreBreakdown(context, "ABNORMAL_ARRIVAL_RATE", "异常到货率", 20, abnormalScore, formatRate(abnormalRate), "异常到货越低得分越高。");
        addScoreBreakdown(context, "CANCEL_RATE", "取消/关闭订单率", 10, cancelScore, formatRate(cancelRate), "取消或关闭订单越少得分越高。");

        fillWeakMetrics(context);
        context.setAnalysisSummary("该供应商综合评分 " + totalScore + " 分，等级为" + score.getLevel() + "。");
        context.setSuggestion(buildSupplierSuggestion(context));
    }

    private void addScoreBreakdown(AgentSupplierContextVO context, String code, String name, int maxScore, int actualScore, String value, String explain) {
        AgentSupplierContextVO.ScoreBreakdown item = new AgentSupplierContextVO.ScoreBreakdown();
        item.setMetricCode(code);
        item.setMetricName(name);
        item.setMaxScore(maxScore);
        item.setActualScore(actualScore);
        item.setValue(value);
        item.setExplain(explain);
        context.getScoreBreakdown().add(item);
    }

    private void fillWeakMetrics(AgentSupplierContextVO context) {
        for (AgentSupplierContextVO.ScoreBreakdown item : context.getScoreBreakdown()) {
            if (item.getActualScore() < Math.round(item.getMaxScore() * 0.7)) {
                AgentSupplierContextVO.WeakMetric weak = new AgentSupplierContextVO.WeakMetric();
                weak.setMetricCode(item.getMetricCode());
                weak.setMetricName(item.getMetricName());
                weak.setValue(item.getValue());
                weak.setReason(item.getMetricName() + "得分偏低，只拿到 " + item.getActualScore() + "/" + item.getMaxScore() + " 分。");
                weak.setSuggestion("建议重点跟进" + item.getMetricName() + "，先把该指标提升到稳定水平。");
                context.getWeakMetrics().add(weak);
            }
        }
    }

    private String buildSupplierSuggestion(AgentSupplierContextVO context) {
        if (context.getWeakMetrics().isEmpty()) {
            return "该供应商当前主要指标较稳定，可保持合作并持续监控。";
        }
        String names = context.getWeakMetrics().stream().map(AgentSupplierContextVO.WeakMetric::getMetricName).collect(Collectors.joining("、"));
        return "建议重点关注 " + names + "，后续合作中可以增加过程跟踪和到货节点确认。";
    }

    private AgentOrderContextVO.EvidenceItem evidence(String type, String label, String value, String explain) {
        AgentOrderContextVO.EvidenceItem item = new AgentOrderContextVO.EvidenceItem();
        item.setType(type);
        item.setLabel(label);
        item.setValue(value);
        item.setExplain(explain);
        return item;
    }

    private String buildWarningAction(String bizNo, String owner, String problem) {
        if (ROLE_WAREHOUSE.equals(owner)) {
            return "请仓库侧优先处理 " + bizNo + "，重点核对：" + problem + "。";
        }
        return "请采购侧优先处理 " + bizNo + "，重点核对：" + problem + "。";
    }

    private Integer priorityScore(String riskLevel, Integer overdueDays) {
        int riskScore = "HIGH".equals(riskLevel) ? 100 : "MEDIUM".equals(riskLevel) ? 60 : 30;
        int overdueScore = Math.min(overdueDays == null ? 0 : overdueDays, 30);
        return riskScore + overdueScore;
    }

    private String buildPriorityReason(String riskLevel, Integer overdueDays, String problem) {
        return riskLevelName(riskLevel) + "，且已超时 " + (overdueDays == null ? 0 : overdueDays) + " 天，风险类型为“" + problem + "”。";
    }

    private String roleName(String role) {
        if (ROLE_PURCHASER.equals(role)) {
            return "采购侧";
        }
        if (ROLE_WAREHOUSE.equals(role)) {
            return "仓库侧";
        }
        if (ROLE_SUPPLIER.equals(role)) {
            return "供应商";
        }
        if (ROLE_NONE.equals(role)) {
            return "无需处理";
        }
        return "待确认责任方";
    }

    private String riskLevelName(String riskLevel) {
        if ("HIGH".equals(riskLevel)) {
            return "高风险";
        }
        if ("MEDIUM".equals(riskLevel)) {
            return "中风险";
        }
        if ("LOW".equals(riskLevel)) {
            return "低风险";
        }
        return "未知风险";
    }

    private String bizTypeName(String bizType) {
        if ("PURCHASE_ORDER".equals(bizType)) {
            return "采购订单";
        }
        if ("ARRIVAL".equals(bizType)) {
            return "到货单";
        }
        if ("INBOUND".equals(bizType)) {
            return "入库单";
        }
        return bizType;
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
        if ("PENDING".equals(status)) {
            return "待入库";
        }
        if ("CLOSED".equals(status)) {
            return "已关闭";
        }
        if ("CANCELLED".equals(status)) {
            return "已取消";
        }
        return status == null ? "未知状态" : status;
    }

    private String resolveLevel(int score) {
        if (score >= 90) {
            return "优秀";
        }
        if (score >= 75) {
            return "良好";
        }
        if (score >= 60) {
            return "一般";
        }
        return "较差";
    }

    private String resolveLevelExplain(int score) {
        if (score >= 90) {
            return "核心履约指标表现稳定，适合作为优先合作供应商。";
        }
        if (score >= 75) {
            return "整体表现较好，但仍需关注个别履约指标。";
        }
        if (score >= 60) {
            return "履约表现一般，存在需要重点跟进的短板。";
        }
        return "履约风险较高，不建议放任合作，需要加强管控。";
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeRate(Double value) {
        return value == null ? 0D : value;
    }

    private double safeRate(Integer numerator, Integer denominator) {
        if (denominator == null || denominator == 0) {
            return 0D;
        }
        return (double) nullToZero(numerator) / denominator;
    }

    private String formatRate(double rate) {
        return String.format("%.2f%%", rate * 100);
    }
}
```

---

### 5.9 新增 Controller：AgentContextController.java

路径：

```text
inventory_back/src/main/java/com/xixi/agent/controller/AgentContextController.java
```

```java
package com.xixi.agent.controller;

import com.xixi.agent.service.AgentBusinessContextService;
import com.xixi.pojo.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent/context")
@RequiredArgsConstructor
public class AgentContextController {

    private final AgentBusinessContextService agentBusinessContextService;

    @GetMapping("/order/{orderNo}")
    public Result getOrderContext(@PathVariable String orderNo) {
        return Result.success(agentBusinessContextService.getOrderContext(orderNo));
    }

    @GetMapping("/warnings")
    public Result scanWarningContext(@RequestParam(required = false) Integer days) {
        return Result.success(agentBusinessContextService.scanWarningContext(days));
    }

    @GetMapping("/supplier/{supplierId}")
    public Result getSupplierContext(
            @PathVariable Long supplierId,
            @RequestParam(required = false) Integer days
    ) {
        return Result.success(agentBusinessContextService.getSupplierContext(supplierId, days));
    }
}
```

---

## 6. Python 侧完整改造

### 6.1 新增 schema：app/schemas/business_context.py

路径：

```text
python_ai_workflow_service/app/schemas/business_context.py
```

```python
from typing import Any

from pydantic import Field

from app.schemas.common import ApiModel


class AgentOrderContextVO(ApiModel):
    exists: bool = False
    order: dict[str, Any] | None = None
    stage: dict[str, Any] | None = None
    responsibility: dict[str, Any] | None = None
    evidence: list[dict[str, Any]] = Field(default_factory=list)
    next_action: dict[str, Any] | None = Field(default=None, alias="nextAction")


class AgentWarningContextVO(ApiModel):
    days: int = 7
    summary: dict[str, Any] | None = None
    items: list[dict[str, Any]] = Field(default_factory=list)
    top_items: list[dict[str, Any]] = Field(default_factory=list, alias="topItems")
    owner_stats: list[dict[str, Any]] = Field(default_factory=list, alias="ownerStats")
    risk_type_stats: list[dict[str, Any]] = Field(default_factory=list, alias="riskTypeStats")


class AgentSupplierContextVO(ApiModel):
    exists: bool = False
    days: int = 30
    supplier: dict[str, Any] | None = None
    metrics: dict[str, Any] | None = None
    score: dict[str, Any] | None = None
    score_breakdown: list[dict[str, Any]] = Field(default_factory=list, alias="scoreBreakdown")
    weak_metrics: list[dict[str, Any]] = Field(default_factory=list, alias="weakMetrics")
    analysis_summary: str | None = Field(default=None, alias="analysisSummary")
    suggestion: str | None = None
```

说明：

```text
这里故意用 dict[str, Any]。

原因是 Java 上下文是给 Agent 使用的“事实包”，
里面会持续补字段。

如果 Python schema 写得太死，
后续 Java 多返回一个字段还要改 Python。
```

---

### 6.2 替换 schema：app/schemas/answer_plan.py

```python
from typing import Any

from pydantic import Field

from app.schemas.common import ApiModel


class AnswerPlan(ApiModel):
    interaction_type: str = Field(default="BUSINESS", alias="interactionType")
    intent: str
    question_focus: str = Field(default="FULL_ANALYSIS", alias="questionFocus")
    turn_type: str = Field(default="FIRST_TURN", alias="turnType")
    answer_mode: str = Field(default="FULL_ANALYSIS", alias="answerMode")
    target_biz_no: str | None = Field(default=None, alias="targetBizNo")
    target_order_no: str | None = Field(default=None, alias="targetOrderNo")
    target_supplier_id: int | None = Field(default=None, alias="targetSupplierId")
    needs_refresh: bool = Field(default=True, alias="needsRefresh")
    use_llm: bool = Field(default=True, alias="useLlm")
    max_context_items: int = Field(default=10, alias="maxContextItems")


class SelectedContext(ApiModel):
    interaction_type: str = Field(default="BUSINESS", alias="interactionType")
    intent: str
    question_focus: str = Field(default="FULL_ANALYSIS", alias="questionFocus")
    answer_mode: str = Field(default="FULL_ANALYSIS", alias="answerMode")
    summary: str | None = None
    facts: dict[str, Any] = Field(default_factory=dict)
    items: list[dict[str, Any]] = Field(default_factory=list)
    instruction: str | None = None
```

---

### 6.3 state.py 增加上下文 key

在 `WorkflowStateKeys` 中新增：

```python
ORDER_CONTEXT = "orderContext"
WARNING_CONTEXT = "warningContext"
SUPPLIER_CONTEXT = "supplierContext"
```

在 `WorkflowGraphState` 中新增：

```python
orderContext: dict[str, Any]
warningContext: dict[str, Any]
supplierContext: dict[str, Any]
```

说明：

```text
ORDER_CONTEXT / WARNING_CONTEXT / SUPPLIER_CONTEXT 是 Java 返回的原始事实包。

ORDER_DIAGNOSIS / WARNING_ANALYSIS / SUPPLIER_SCORE 是 Python 整理后的结构化业务结果。
```

---

### 6.4 inventory_backend.py 新增三个上下文方法

在 `InventoryBackendClient` 中新增：

```python
    async def get_agent_order_context(self, order_no: str, authorization: str) -> dict[str, Any] | None:
        data = await self._request("GET", f"/agent/context/order/{order_no}", authorization)
        return data if isinstance(data, dict) else None

    async def get_agent_warning_context(self, days: int, authorization: str) -> dict[str, Any] | None:
        data = await self._request("GET", "/agent/context/warnings", authorization, params={"days": days})
        return data if isinstance(data, dict) else None

    async def get_agent_supplier_context(self, supplier_id: int, days: int, authorization: str) -> dict[str, Any] | None:
        data = await self._request(
            "GET",
            f"/agent/context/supplier/{supplier_id}",
            authorization,
            params={"days": days},
        )
        return data if isinstance(data, dict) else None
```

---

### 6.5 answer_plan.py 完整问题聚焦逻辑

路径：

```text
python_ai_workflow_service/app/workflows/nodes/answer_plan.py
```

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

        plan = AnswerPlan(
            interactionType=interaction_type,
            intent=intent,
            questionFocus=self._question_focus(intent, message),
            turnType="FOLLOW_UP" if self._has_reusable_result(state, intent) else "FIRST_TURN",
            answerMode="FOCUSED_ANSWER",
            targetBizNo=self._extract_biz_no(message),
            targetOrderNo=self._extract_order_no(message),
            targetSupplierId=self._extract_supplier_id(message),
            needsRefresh=self._needs_refresh(state, intent, message),
            useLlm=False,
            maxContextItems=10,
        )
        return {WorkflowStateKeys.ANSWER_PLAN: plan.model_dump(by_alias=True)}

    def _question_focus(self, intent: str, message: str) -> str:
        text = message or ""

        if intent == WorkflowIntent.ORDER_DIAGNOSIS.value:
            owner = self._contains_any(text, ["谁", "哪个采购员", "哪位采购员", "负责人", "谁跟进"])
            reason = self._contains_any(text, ["为什么", "原因", "依据", "为什么选"])
            if owner and reason:
                return "OWNER_REASON"
            if owner:
                return "OWNER"
            if self._contains_any(text, ["下一步", "怎么处理", "怎么办", "怎么解决"]):
                return "NEXT_ACTION"
            if reason or self._contains_any(text, ["卡在哪", "没完成"]):
                return "CAUSE"
            if self._contains_any(text, ["证据", "根据什么"]):
                return "EVIDENCE"
            return "FULL_DIAGNOSIS"

        if intent == WorkflowIntent.WARNING_SCAN.value:
            if self._extract_biz_no(text):
                return "SPECIFIC_WARNING_REASON"
            if self._contains_any(text, ["最严重", "优先", "先处理", "处理哪些"]):
                return "TOP_RISK"
            if self._contains_any(text, ["谁处理", "谁负责", "哪个角色"]):
                return "WARNING_OWNER"
            if self._contains_any(text, ["为什么优先", "为什么先"]):
                return "WARNING_PRIORITY_REASON"
            if self._contains_any(text, ["怎么处理", "怎么办", "下一步"]):
                return "WARNING_ACTION"
            return "WARNING_SUMMARY"

        if intent == WorkflowIntent.SUPPLIER_SCORE.value:
            if self._contains_any(text, ["为什么", "怎么算", "怎么来的"]):
                return "SCORE_REASON"
            if self._contains_any(text, ["差在哪", "短板", "拖后腿", "哪个指标"]):
                return "WEAK_METRIC"
            if self._contains_any(text, ["分数", "意味着", "等级"]):
                return "SCORE_MEANING"
            if self._contains_any(text, ["合作", "还能不能", "是否继续"]):
                return "COOP_ADVICE"
            if self._contains_any(text, ["怎么管控", "下一步", "怎么改善"]):
                return "SUPPLIER_ACTION"
            return "SUPPLIER_FULL_ANALYSIS"

        return "CLARIFY"

    def _needs_refresh(self, state: dict, intent: str, message: str) -> bool:
        if not self._has_reusable_result(state, intent):
            return True
        return self._contains_any(message, ["重新", "刷新", "重新扫描", "最新", "再算"])

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

### 6.6 三个 load context 节点

#### load_order_context.py

```python
import json
from typing import Any

from app.clients.inventory_backend import InventoryBackendClient
from app.repositories.session_store import SessionStore
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
            return {WorkflowStateKeys.ERROR_MESSAGE: "未识别采购订单号"}

        context = await self.backend.get_agent_order_context(str(order_no), authorization)
        self.session_store.save_tool_message(thread_id, "loadOrderContext", self._json(entity), self._json(context))

        if not context or not context.get("exists"):
            return {WorkflowStateKeys.ERROR_MESSAGE: "采购订单号不存在"}

        order = context.get("order") or {}
        return {
            WorkflowStateKeys.ORDER_CONTEXT: context,
            WorkflowStateKeys.ORDER_SNAPSHOT: {
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
            },
        }

    def _json(self, value: Any) -> str:
        return json.dumps(value, ensure_ascii=False, default=str)
```

#### load_warning_context.py

```python
import json
from typing import Any

from app.clients.inventory_backend import InventoryBackendClient
from app.repositories.session_store import SessionStore
from app.workflows.state import WorkflowStateKeys


class LoadWarningContextNode:
    def __init__(self, backend: InventoryBackendClient, session_store: SessionStore):
        self.backend = backend
        self.session_store = session_store

    async def __call__(self, state: dict) -> dict:
        entity = dict(state.get(WorkflowStateKeys.ENTITY, {}) or {})
        thread_id = str(state.get(WorkflowStateKeys.THREAD_ID, ""))
        authorization = str(state.get(WorkflowStateKeys.AUTHORIZATION, ""))
        days = int(entity.get("days") or 7)

        context = await self.backend.get_agent_warning_context(days, authorization)
        self.session_store.save_tool_message(thread_id, "loadWarningContext", self._json({"days": days}), self._json(context))

        if not context:
            return {WorkflowStateKeys.ERROR_MESSAGE: "预警上下文为空"}

        return {WorkflowStateKeys.WARNING_CONTEXT: context}

    def _json(self, value: Any) -> str:
        return json.dumps(value, ensure_ascii=False, default=str)
```

#### load_supplier_context.py

```python
import json
from typing import Any

from app.clients.inventory_backend import InventoryBackendClient
from app.repositories.session_store import SessionStore
from app.workflows.state import WorkflowStateKeys


class LoadSupplierContextNode:
    def __init__(self, backend: InventoryBackendClient, session_store: SessionStore):
        self.backend = backend
        self.session_store = session_store

    async def __call__(self, state: dict) -> dict:
        entity = dict(state.get(WorkflowStateKeys.ENTITY, {}) or {})
        thread_id = str(state.get(WorkflowStateKeys.THREAD_ID, ""))
        authorization = str(state.get(WorkflowStateKeys.AUTHORIZATION, ""))
        supplier_id = entity.get("supplierId")
        days = int(entity.get("days") or 30)

        if supplier_id is None:
            return {WorkflowStateKeys.ERROR_MESSAGE: "未识别到供应商ID"}

        context = await self.backend.get_agent_supplier_context(int(supplier_id), days, authorization)
        self.session_store.save_tool_message(thread_id, "loadSupplierContext", self._json(entity), self._json(context))

        if not context or not context.get("exists"):
            return {WorkflowStateKeys.ERROR_MESSAGE: "供应商不存在"}

        return {
            WorkflowStateKeys.SUPPLIER_CONTEXT: context,
            WorkflowStateKeys.SUPPLIER_METRICS: context.get("metrics") or {},
        }

    def _json(self, value: Any) -> str:
        return json.dumps(value, ensure_ascii=False, default=str)
```

---

### 6.7 三个 analyze 节点

#### order_rule_analyze.py

```python
from app.workflows.state import WorkflowStateKeys


class OrderRuleAnalyzeNode:
    async def __call__(self, state: dict) -> dict:
        context = dict(state.get(WorkflowStateKeys.ORDER_CONTEXT, {}) or {})
        if not context:
            return {WorkflowStateKeys.ERROR_MESSAGE: "订单上下文为空"}

        order = context.get("order") or {}
        stage = context.get("stage") or {}
        responsibility = context.get("responsibility") or {}
        next_action = context.get("nextAction") or {}
        evidence_items = context.get("evidence") or []

        evidence = [
            f"{item.get('label')}为 {item.get('value')}，{item.get('explain')}"
            for item in evidence_items
            if item.get("label") and item.get("value")
        ]

        return {
            WorkflowStateKeys.ORDER_DIAGNOSIS: {
                "orderNo": order.get("orderNo"),
                "currentStage": stage.get("currentStage"),
                "blockReason": stage.get("blockReason"),
                "evidence": evidence,
                "suggestOwner": responsibility.get("ownerRole"),
                "suggestAction": next_action.get("actionText"),
                "responsibility": responsibility,
                "nextAction": next_action,
            }
        }
```

#### warning_rule_analyze.py

```python
from app.workflows.state import WorkflowStateKeys


class WarningRuleAnalyzeNode:
    async def __call__(self, state: dict) -> dict:
        context = dict(state.get(WorkflowStateKeys.WARNING_CONTEXT, {}) or {})
        if not context:
            return {WorkflowStateKeys.ERROR_MESSAGE: "预警上下文为空"}

        summary = context.get("summary") or {}
        items = context.get("items") or []
        result = {
            "summary": summary.get("summaryText") or f"本次扫描共发现 {len(items)} 个执行风险。",
            "summaryStats": summary,
            "items": items,
            "topItems": context.get("topItems") or [],
            "ownerStats": context.get("ownerStats") or [],
            "riskTypeStats": context.get("riskTypeStats") or [],
        }
        return {WorkflowStateKeys.WARNING_ANALYSIS: result}
```

#### supplier_score_rule.py

```python
from app.workflows.state import WorkflowStateKeys


class SupplierScoreRuleNode:
    async def __call__(self, state: dict) -> dict:
        context = dict(state.get(WorkflowStateKeys.SUPPLIER_CONTEXT, {}) or {})
        if not context:
            return {WorkflowStateKeys.ERROR_MESSAGE: "供应商上下文为空"}

        supplier = context.get("supplier") or {}
        score = context.get("score") or {}
        metrics = context.get("metrics") or {}

        result = {
            "supplierId": supplier.get("supplierId"),
            "supplierName": supplier.get("supplierName"),
            "score": score.get("totalScore", 0),
            "level": score.get("level", "数据不足"),
            "levelExplain": score.get("levelExplain"),
            "confirmRate": metrics.get("confirmRate", "0.00%"),
            "arrivalCompletionRate": metrics.get("arrivalCompletionRate", "0.00%"),
            "inboundCompletionRate": metrics.get("inboundCompletionRate", "0.00%"),
            "abnormalArrivalRate": metrics.get("abnormalArrivalRate", "0.00%"),
            "scoreBreakdown": context.get("scoreBreakdown") or [],
            "weakMetrics": context.get("weakMetrics") or [],
            "analysis": context.get("analysisSummary"),
            "suggestion": context.get("suggestion"),
        }
        return {WorkflowStateKeys.SUPPLIER_SCORE: result}
```

---

### 6.8 context_select.py 完整三流程选择逻辑

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
            )
        elif intent == WorkflowIntent.ORDER_DIAGNOSIS.value:
            selected = self._order_context(state, plan)
        elif intent == WorkflowIntent.WARNING_SCAN.value:
            selected = self._warning_context(state, plan)
        elif intent == WorkflowIntent.SUPPLIER_SCORE.value:
            selected = self._supplier_context(state, plan)
        else:
            selected = SelectedContext(
                interactionType=interaction_type,
                intent=intent,
                questionFocus=plan.get("questionFocus", "CLARIFY"),
                answerMode="CLARIFY",
                summary="信息不足",
                facts={},
            )

        return {WorkflowStateKeys.SELECTED_CONTEXT: selected.model_dump(by_alias=True)}

    def _order_context(self, state: dict, plan: dict) -> SelectedContext:
        diagnosis = dict(state.get(WorkflowStateKeys.ORDER_DIAGNOSIS, {}) or {})
        question_focus = plan.get("questionFocus", "FULL_DIAGNOSIS")
        return SelectedContext(
            interactionType=InteractionType.BUSINESS.value,
            intent=WorkflowIntent.ORDER_DIAGNOSIS.value,
            questionFocus=question_focus,
            answerMode=plan.get("answerMode", "FOCUSED_ANSWER"),
            summary=diagnosis.get("blockReason"),
            facts={
                "diagnosis": diagnosis,
                "responsibility": diagnosis.get("responsibility") or {},
                "nextAction": diagnosis.get("nextAction") or {},
                "evidence": diagnosis.get("evidence") or [],
            },
        )

    def _warning_context(self, state: dict, plan: dict) -> SelectedContext:
        analysis = dict(state.get(WorkflowStateKeys.WARNING_ANALYSIS, {}) or {})
        question_focus = plan.get("questionFocus", "WARNING_SUMMARY")
        target_biz_no = plan.get("targetBizNo")
        items = analysis.get("items") or []

        if question_focus == "SPECIFIC_WARNING_REASON" and target_biz_no:
            selected_items = [item for item in items if item.get("bizNo") == target_biz_no]
        elif question_focus in {"TOP_RISK", "WARNING_PRIORITY_REASON"}:
            selected_items = analysis.get("topItems") or items[:10]
        else:
            selected_items = items[:10]

        return SelectedContext(
            interactionType=InteractionType.BUSINESS.value,
            intent=WorkflowIntent.WARNING_SCAN.value,
            questionFocus=question_focus,
            answerMode=plan.get("answerMode", "FOCUSED_ANSWER"),
            summary=analysis.get("summary"),
            facts={
                "summaryStats": analysis.get("summaryStats") or {},
                "ownerStats": analysis.get("ownerStats") or [],
                "riskTypeStats": analysis.get("riskTypeStats") or [],
            },
            items=selected_items,
        )

    def _supplier_context(self, state: dict, plan: dict) -> SelectedContext:
        score = dict(state.get(WorkflowStateKeys.SUPPLIER_SCORE, {}) or {})
        question_focus = plan.get("questionFocus", "SUPPLIER_FULL_ANALYSIS")
        return SelectedContext(
            interactionType=InteractionType.BUSINESS.value,
            intent=WorkflowIntent.SUPPLIER_SCORE.value,
            questionFocus=question_focus,
            answerMode=plan.get("answerMode", "FOCUSED_ANSWER"),
            summary=score.get("analysis") or score.get("suggestion"),
            facts={
                "score": score,
                "scoreBreakdown": score.get("scoreBreakdown") or [],
                "weakMetrics": score.get("weakMetrics") or [],
            },
        )
```

---

### 6.9 answer_humanizer.py

```python
from typing import Any


class AnswerHumanizer:
    CODE_LABELS = {
        "PURCHASER": "采购侧",
        "WAREHOUSE": "仓库侧",
        "SUPPLIER": "供应商",
        "NONE": "无需处理",
        "WAIT_CONFIRM": "待供应商确认",
        "IN_PROGRESS": "执行中",
        "PARTIAL_ARRIVAL": "部分到货",
        "COMPLETED": "已完成",
        "CLOSED": "已关闭",
        "CANCELLED": "已取消",
        "PENDING": "待入库",
        "HIGH": "高风险",
        "MEDIUM": "中风险",
        "LOW": "低风险",
        "PURCHASE_ORDER": "采购订单",
        "ARRIVAL": "到货单",
        "INBOUND": "入库单",
    }

    def normalize_text(self, text: Any) -> str:
        result = "" if text is None else str(text)
        for code, label in self.CODE_LABELS.items():
            result = result.replace(code, label)
        return result

    def join_items(self, items: list[dict], key: str = "bizNo", limit: int = 5) -> str:
        values = [str(item.get(key)) for item in items[:limit] if item.get(key)]
        return "、".join(values) if values else "暂无"
```

---

### 6.10 business_answer_generate.py 完整三流程回答逻辑

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
        error_message = str(state.get(WorkflowStateKeys.ERROR_MESSAGE, ""))
        if error_message:
            return {WorkflowStateKeys.LLM_ANSWER: error_message}

        selected_context = dict(state.get(WorkflowStateKeys.SELECTED_CONTEXT, {}) or {})
        interaction_type = selected_context.get("interactionType", InteractionType.BUSINESS.value)

        if interaction_type != InteractionType.BUSINESS.value:
            return {WorkflowStateKeys.LLM_ANSWER: self._non_business_answer(interaction_type)}

        answer = self._focused_answer(selected_context)
        return {WorkflowStateKeys.LLM_ANSWER: self.humanizer.normalize_text(answer)}

    def _non_business_answer(self, interaction_type: str) -> str:
        if interaction_type == InteractionType.SOCIAL.value:
            return "好，后面你把订单号、供应商ID或扫描范围发给我，我继续帮你看。"
        if interaction_type == InteractionType.META.value:
            return "我是采购入库协同业务 Agent，主要帮你看订单卡点、采购执行风险和供应商履约表现。"
        return "我需要一个更具体的业务对象，比如订单号、供应商ID，或者“扫描最近7天采购风险”。"

    def _focused_answer(self, selected_context: dict) -> str:
        intent = selected_context.get("intent")
        if intent == "ORDER_DIAGNOSIS":
            return self._order_answer(selected_context)
        if intent == "WARNING_SCAN":
            return self._warning_answer(selected_context)
        if intent == "SUPPLIER_SCORE":
            return self._supplier_answer(selected_context)
        return selected_context.get("summary") or "当前问题暂时无法生成明确回答。"

    def _order_answer(self, selected_context: dict) -> str:
        focus = selected_context.get("questionFocus")
        facts = selected_context.get("facts") or {}
        diagnosis = facts.get("diagnosis") or {}
        responsibility = facts.get("responsibility") or {}
        next_action = facts.get("nextAction") or {}

        order_no = diagnosis.get("orderNo") or "这张订单"
        owner_name = responsibility.get("ownerUserName")
        owner_role_name = responsibility.get("ownerRoleName") or "责任方"
        owner_reason = responsibility.get("ownerReason") or diagnosis.get("blockReason")
        action_text = next_action.get("actionText") or diagnosis.get("suggestAction")

        if focus == "OWNER":
            if owner_name:
                return f"{order_no} 建议先让 {owner_name} 跟进。{owner_name} 是这张订单当前匹配到的{owner_role_name}。"
            return f"{order_no} 现在还不能定位到具体某个人，只能判断到责任角色是{owner_role_name}。如果要精确到人，需要后端返回这张订单的负责人姓名或账号。"

        if focus == "OWNER_REASON":
            if owner_name:
                return f"建议让 {owner_name} 跟进。\n\n原因是：{owner_reason}\n\n下一步：{action_text}"
            return f"现在还不能定位到具体某个{owner_role_name}，只能判断到责任角色是{owner_role_name}。\n\n为什么是{owner_role_name}：{owner_reason}\n\n下一步建议：{action_text}。如果要精确到具体人，需要后端返回这张订单的负责人姓名或账号。"

        if focus == "NEXT_ACTION":
            who = owner_name or owner_role_name
            return f"这单下一步先由{who}处理。具体动作是：{action_text}"

        if focus == "CAUSE":
            return f"{order_no} 当前卡在“{diagnosis.get('currentStage')}”。直接原因是：{diagnosis.get('blockReason')}"

        if focus == "EVIDENCE":
            evidence = "；".join(diagnosis.get("evidence") or [])
            return f"我这样判断的依据是：{evidence}。"

        return f"{order_no} 当前卡在“{diagnosis.get('currentStage')}”。原因是：{diagnosis.get('blockReason')} 建议动作：{action_text}"

    def _warning_answer(self, selected_context: dict) -> str:
        focus = selected_context.get("questionFocus")
        facts = selected_context.get("facts") or {}
        items = selected_context.get("items") or []
        summary_stats = facts.get("summaryStats") or {}

        if focus == "SPECIFIC_WARNING_REASON":
            if not items:
                return "我没有在本次扫描结果里找到这张单，可能是单号不在当前扫描范围内。"
            item = items[0]
            return f"{item.get('bizNo')} 被判为{item.get('riskLevelName')}，原因是：{item.get('reason')} 它命中的风险类型是“{item.get('problem')}”。建议由{item.get('suggestOwnerName')}处理，动作是：{item.get('suggestAction')}"

        if focus in {"TOP_RISK", "WARNING_PRIORITY_REASON"}:
            biz_nos = self.humanizer.join_items(items)
            reasons = "；".join([item.get("priorityReason", "") for item in items[:3] if item.get("priorityReason")])
            return f"建议优先处理这几单：{biz_nos}。优先原因是：{reasons}"

        if focus == "WARNING_OWNER":
            owner_stats = facts.get("ownerStats") or []
            if not owner_stats:
                return "当前没有可统计的责任方。"
            parts = [f"{item.get('ownerRoleName')} {item.get('count')} 个" for item in owner_stats]
            return f"本次风险主要涉及这些责任方：{'，'.join(parts)}。建议先处理高风险数量最多的责任方。"

        if focus == "WARNING_ACTION":
            biz_nos = self.humanizer.join_items(items)
            return f"下一步建议先处理高优先级风险单据：{biz_nos}。先清高风险，再处理中风险。"

        return f"本次扫描共发现 {summary_stats.get('totalCount', 0)} 个风险，其中高风险 {summary_stats.get('highCount', 0)} 个，中风险 {summary_stats.get('mediumCount', 0)} 个。"

    def _supplier_answer(self, selected_context: dict) -> str:
        focus = selected_context.get("questionFocus")
        facts = selected_context.get("facts") or {}
        score = facts.get("score") or {}
        breakdown = facts.get("scoreBreakdown") or []
        weak_metrics = facts.get("weakMetrics") or []

        supplier_name = score.get("supplierName") or "该供应商"

        if focus == "SCORE_MEANING":
            return f"{supplier_name} 当前得分 {score.get('score')}，等级是“{score.get('level')}”。这个分数说明它不是不可合作，但履约稳定性还有短板，需要结合具体扣分指标看。"

        if focus == "SCORE_REASON":
            parts = [f"{item.get('metricName')} {item.get('actualScore')}/{item.get('maxScore')} 分" for item in breakdown]
            return f"{supplier_name} 的分数主要由这些指标构成：{'，'.join(parts)}。总分低的原因通常来自低分指标，而不是单一结论。"

        if focus == "WEAK_METRIC":
            if not weak_metrics:
                return f"{supplier_name} 当前没有特别突出的短板指标，主要是继续保持跟踪。"
            parts = [f"{item.get('metricName')}：{item.get('reason')}" for item in weak_metrics]
            return f"{supplier_name} 当前主要短板是：{'；'.join(parts)}。"

        if focus == "COOP_ADVICE":
            suggestion = score.get("suggestion") or facts.get("suggestion")
            return suggestion or f"{supplier_name} 可以继续合作，但建议结合短板指标做过程管控。"

        if focus == "SUPPLIER_ACTION":
            if weak_metrics:
                names = "、".join([item.get("metricName") for item in weak_metrics if item.get("metricName")])
                return f"下一步建议重点盯 {names}。先把短板指标拉到稳定水平，再考虑扩大合作。"
            return "下一步建议保持常规履约监控，重点关注确认、到货、入库三个节点。"

        return f"{supplier_name} 当前得分 {score.get('score')}，等级 {score.get('level')}。{score.get('analysis') or ''}"

    def _to_text(self, value) -> str:
        return json.dumps(value, ensure_ascii=False, default=str)
```

---

## 7. session_store.py 需要恢复的状态

把 `load_state_by_thread_id` 的 `restorable_keys` 改成：

```python
restorable_keys = [
    WorkflowStateKeys.INTENT,
    WorkflowStateKeys.ACTIVE_INTENT,
    WorkflowStateKeys.ENTITY,
    WorkflowStateKeys.ORDER_CONTEXT,
    WorkflowStateKeys.ORDER_SNAPSHOT,
    WorkflowStateKeys.ORDER_DIAGNOSIS,
    WorkflowStateKeys.WARNING_CONTEXT,
    WorkflowStateKeys.WARNING_ANALYSIS,
    WorkflowStateKeys.SUPPLIER_CONTEXT,
    WorkflowStateKeys.SUPPLIER_METRICS,
    WorkflowStateKeys.SUPPLIER_SCORE,
    WorkflowStateKeys.ANSWER_PLAN,
    WorkflowStateKeys.SELECTED_CONTEXT,
]
```

原因：

```text
用户追问时，Agent 需要复用完整事实包。

否则用户先问“扫描最近7天风险”，再问“为什么这单最严重”，
Python 如果只保存 summary，不保存 warningContext，就无法回答原因。
```

---

## 8. 测试用例

### 8.1 订单诊断

```json
{
  "message": "帮我分析 PO2026040022 为什么还没完成"
}
```

然后追问：

```json
{
  "message": "让哪个采购员跟进？为什么选他？",
  "threadId": "上一步 threadId"
}
```

合格回答必须满足：

```text
1. 有具体采购员就说具体采购员
2. 没具体采购员就说只能定位到采购侧
3. 说明为什么是采购侧
4. 给下一步动作
```

### 8.2 预警扫描

```json
{
  "message": "扫描最近7天采购执行风险"
}
```

然后追问：

```json
{
  "message": "为什么 PO2026040001 风险这么高？",
  "threadId": "上一步 threadId"
}
```

合格回答必须满足：

```text
1. 说明风险等级
2. 说明命中规则
3. 说明超时天数
4. 说明为什么优先
5. 说明由谁处理
```

### 8.3 供应商评分

```json
{
  "message": "分析供应商1最近180天履约表现"
}
```

然后追问：

```json
{
  "message": "为什么只有这个分？到底差在哪？",
  "threadId": "上一步 threadId"
}
```

合格回答必须满足：

```text
1. 说明总分和等级
2. 说明每个指标的得分构成
3. 指出低分指标
4. 给出改善动作
```

---

## 9. 最终效果

落地后，Agent 不再只是：

```text
规则摘要器
```

而是会变成：

```text
问题聚焦器
事实解释器
边界判断器
业务建议器
```

最重要的是：

```text
它不会再装作知道自己不知道的东西。
```

比如具体负责人不存在时，它会明确说：

```text
现在只能判断到采购侧，不能定位到具体采购员。
```

供应商评分时，它不会只说：

```text
65 分，一般。
```

而是会说：

```text
65 分主要是确认及时率、到货完成率、入库完成率这些指标扣分导致的。
```

预警扫描时，它不会只说：

```text
这单高风险。
```

而是会说：

```text
它命中了“采购订单待供应商确认超时”，已经超时 50 天，所以优先级高。
```

这才是三条业务链路都完整的 Agent。

