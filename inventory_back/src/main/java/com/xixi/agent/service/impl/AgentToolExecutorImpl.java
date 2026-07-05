package com.xixi.agent.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.agent.model.AgentToolResult;
import com.xixi.agent.service.AgentRagService;
import com.xixi.agent.service.AgentToolExecutor;
import com.xixi.agent.vo.RagSearchResultVO;
import com.xixi.entity.Supplier;
import com.xixi.mapper.PurchaseOrderItemMapper;
import com.xixi.mapper.PurchaseOrderMapper;
import com.xixi.mapper.SupplierFileMapper;
import com.xixi.mapper.SupplierMapper;
import com.xixi.pojo.query.arrival.ArrivalQuery;
import com.xixi.pojo.query.inbound.InboundQuery;
import com.xixi.pojo.query.inventory.InventoryLogPageQuery;
import com.xixi.pojo.query.inventory.InventoryPageQuery;
import com.xixi.pojo.query.purchase.PurchaseOrderQuery;
import com.xixi.pojo.query.purchase.PurchaseRequestQuery;
import com.xixi.pojo.query.supplier.SupplierPageQuery;
import com.xixi.pojo.vo.arrival.ArrivalVO;
import com.xixi.pojo.vo.inbound.InboundVO;
import com.xixi.pojo.vo.inventory.InventoryLogPageVO;
import com.xixi.pojo.vo.inventory.InventoryPageVO;
import com.xixi.pojo.vo.purchase.PurchaseOrderItemVO;
import com.xixi.pojo.vo.purchase.PurchaseOrderVO;
import com.xixi.pojo.vo.purchase.PurchaseRequestItemVO;
import com.xixi.pojo.vo.purchase.PurchaseRequestPageVO;
import com.xixi.pojo.vo.purchase.PurchaseRequestReviewVO;
import com.xixi.pojo.vo.purchase.PurchaseRequestVO;
import com.xixi.pojo.vo.supplier.SupplierVO;
import com.xixi.service.ArrivalService;
import com.xixi.service.InboundService;
import com.xixi.service.InventoryLogService;
import com.xixi.service.InventoryService;
import com.xixi.service.PurchaseRequestReviewService;
import com.xixi.service.SupplierService;
import com.xixi.service.PurchaseOrderService;
import com.xixi.service.PurchaseRequestItemService;
import com.xixi.service.PurchaseRequestService;
import com.xixi.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentToolExecutorImpl implements AgentToolExecutor {
    private static final String LIST_ROLE_TODOS = "list_role_todos";
    private static final String GET_PURCHASE_ORDER_CONTEXT = "get_purchase_order_context";
    private static final String GET_PURCHASE_REQUEST_CONTEXT = "get_purchase_request_context";
    private static final String LIST_INVENTORY_ALERTS = "list_inventory_alerts";
    private static final String GET_SUPPLIER_PROFILE_CONTEXT = "get_supplier_profile_context";
    private static final String SEARCH_BUSINESS_KNOWLEDGE = "search_business_knowledge";

    private final PurchaseRequestService purchaseRequestService;
    private final PurchaseOrderService purchaseOrderService;
    private final ArrivalService arrivalService;
    private final InboundService inboundService;
    private final InventoryService inventoryService;
    private final InventoryLogService inventoryLogService;
    private final SupplierService supplierService;
    private final AgentRagService agentRagService;
    private final PurchaseRequestReviewService purchaseRequestReviewService;
    private final PurchaseRequestItemService purchaseRequestItemService;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final SupplierMapper supplierMapper;
    private final SupplierFileMapper supplierFileMapper;

    @Override
    public List<String> getToolNames() {
        return List.of(
                LIST_ROLE_TODOS,
                GET_PURCHASE_ORDER_CONTEXT,
                GET_PURCHASE_REQUEST_CONTEXT,
                LIST_INVENTORY_ALERTS,
                GET_SUPPLIER_PROFILE_CONTEXT,
                SEARCH_BUSINESS_KNOWLEDGE
        );
    }

    @Override
    public boolean supports(String toolName) {
        return getToolNames().contains(toolName);
    }

    @Override
    public AgentToolResult execute(String toolName, Map<String, Object> arguments) {
        try {
            return switch (toolName) {
                case LIST_ROLE_TODOS -> listRoleTodos(arguments);
                case GET_PURCHASE_ORDER_CONTEXT -> getPurchaseOrderContext(arguments);
                case GET_PURCHASE_REQUEST_CONTEXT -> getPurchaseRequestContext(arguments);
                case LIST_INVENTORY_ALERTS -> listInventoryAlerts(arguments);
                case GET_SUPPLIER_PROFILE_CONTEXT -> getSupplierProfileContext(arguments);
                case SEARCH_BUSINESS_KNOWLEDGE -> searchBusinessKnowledge(arguments);
                default -> AgentToolResult.failure(toolName, "不支持的工具：" + toolName);
            };
        } catch (Exception ex) {
            return AgentToolResult.failure(toolName, "工具执行失败：" + ex.getMessage());
        }
    }

    private AgentToolResult listRoleTodos(Map<String, Object> arguments) {
        List<String> roleCodes = safeRoles();
        int limit = intArg(arguments, "limit", 10, 1, 20);
        AgentToolResult result = AgentToolResult.success(LIST_ROLE_TODOS, "已按当前角色查询业务待办");
        List<Object> todos = new ArrayList<>();

        if (hasRole(roleCodes, "ADMIN")) {
            todos.addAll(adminTodos(limit));
        }
        if (hasRole(roleCodes, "PURCHASE_MANAGER")) {
            todos.addAll(purchaseManagerTodos(limit));
        }
        if (hasRole(roleCodes, "PURCHASER")) {
            todos.addAll(purchaserTodos(limit));
        }
        if (hasRole(roleCodes, "WAREHOUSE")) {
            todos.addAll(warehouseTodos(limit));
        }
        if (hasRole(roleCodes, "SUPPLIER")) {
            todos.addAll(supplierTodos(limit));
        }

        if (todos.isEmpty()) {
            result.setSummary("当前没有查询到明确待办，或当前角色暂无待办工具。");
        } else {
            result.setSummary("查询到 " + todos.size() + " 类待办。");
        }
        result.setItems(todos);
        result.getFacts().put("roleCodes", roleCodes);
        result.getFacts().put("todoCount", todos.size());
        result.getEvidence().add("待办数据来自采购申请、采购订单、到货、入库、库存、供应商等现有业务表。");
        return result;
    }

    private List<Object> purchaseManagerTodos(int limit) {
        List<Object> todos = new ArrayList<>();
        PurchaseRequestQuery pendingQuery = new PurchaseRequestQuery();
        pendingQuery.setPageNum(1);
        pendingQuery.setPageSize(limit);
        pendingQuery.setStatus("PENDING_APPROVAL");
        IPage<PurchaseRequestPageVO> pending = purchaseRequestService.getPurchaseRequestPage(pendingQuery);
        todos.add(todo(
                "PURCHASE_APPROVAL",
                "待审批采购申请",
                pending.getTotal(),
                "/manager/approval",
                "优先处理提交时间较早、期望日期较近的申请。",
                pending.getRecords()
        ));

        LocalDateTime end = LocalDateTime.now().minusHours(24);
        PurchaseRequestQuery overdueQuery = new PurchaseRequestQuery();
        overdueQuery.setPageNum(1);
        overdueQuery.setPageSize(limit);
        overdueQuery.setStatus("PENDING_APPROVAL");
        overdueQuery.setSubmitTimeBegin(LocalDateTime.now().minusDays(30));
        overdueQuery.setSubmitTimeEnd(end);
        IPage<PurchaseRequestPageVO> overdue = purchaseRequestService.getPurchaseRequestPage(overdueQuery);
        todos.add(todo(
                "PURCHASE_APPROVAL_OVERDUE",
                "超过24小时未审批申请",
                overdue.getTotal(),
                "/manager/approval",
                "这些申请已等待较久，建议先核对明细和审批意见。",
                overdue.getRecords()
        ));
        return todos;
    }

    private List<Object> purchaserTodos(int limit) {
        List<Object> todos = new ArrayList<>();
        PurchaseRequestQuery approvedQuery = new PurchaseRequestQuery();
        approvedQuery.setPageNum(1);
        approvedQuery.setPageSize(limit);
        approvedQuery.setStatus("APPROVED");
        IPage<PurchaseRequestPageVO> approved = purchaseRequestService.getMyApprovedPurchaseRequestPage(approvedQuery);
        todos.add(todo("APPROVED_REQUEST_TO_ORDER", "已审批但未创建订单", approved.getTotal(),
                "/purchaser/request", "建议尽快选择供应商并生成采购订单。", approved.getRecords()));

        todos.add(orderTodo("ORDER_WAIT_CONFIRM", "订单待供应商确认", "WAIT_CONFIRM",
                "/purchaser/order", "建议联系供应商确认交期。", limit));
        todos.add(orderTodo("ORDER_IN_PROGRESS", "订单执行中待到货", "IN_PROGRESS",
                "/purchaser/arrival", "建议关注计划交期和供应商反馈。", limit));
        todos.add(orderTodo("ORDER_PARTIAL_ARRIVAL", "部分到货仍有剩余", "PARTIAL_ARRIVAL",
                "/purchaser/arrival", "建议跟进剩余未到货数量。", limit));
        todos.add(orderTodo("ORDER_WAIT_INBOUND", "已到货待入库确认", "WAIT_INBOUND",
                "/purchaser/inbound", "建议联系仓库确认入库。", limit));
        return todos;
    }

    private List<Object> warehouseTodos(int limit) {
        List<Object> todos = new ArrayList<>();
        ArrivalQuery arrivalQuery = new ArrivalQuery();
        arrivalQuery.setPageNum(1);
        arrivalQuery.setPageSize(limit);
        arrivalQuery.setPendingInboundOnly(true);
        IPage<ArrivalVO> pendingInboundArrival = arrivalService.getArrivalPage(arrivalQuery);
        todos.add(todo("ARRIVAL_TO_INBOUND", "到货单未生成入库单", pendingInboundArrival.getTotal(),
                "/warehouse/arrival", "建议根据合格数量生成入库单。", pendingInboundArrival.getRecords()));

        InboundQuery inboundQuery = new InboundQuery();
        inboundQuery.setPageNum(1);
        inboundQuery.setPageSize(limit);
        inboundQuery.setStatus("PENDING");
        IPage<InboundVO> pendingInbound = inboundService.getInboundPage(inboundQuery);
        todos.add(todo("INBOUND_PENDING_CONFIRM", "入库单待确认", pendingInbound.getTotal(),
                "/warehouse/inbound", "建议核对入库明细后确认入库。", pendingInbound.getRecords()));

        InventoryPageQuery lowStockQuery = new InventoryPageQuery();
        lowStockQuery.setPageNum(1);
        lowStockQuery.setPageSize(limit);
        lowStockQuery.setStockStatus("LOW");
        IPage<InventoryPageVO> lowStock = inventoryService.getInventoryPage(lowStockQuery);
        todos.add(todo("LOW_STOCK", "库存低于安全库存", lowStock.getTotal(),
                "/warehouse/inventory", "建议确认需求并发起补货或采购申请。", lowStock.getRecords()));

        InventoryLogPageQuery adjustQuery = new InventoryLogPageQuery();
        adjustQuery.setPageNum(1);
        adjustQuery.setPageSize(limit);
        adjustQuery.setBizType("ADJUST");
        adjustQuery.setBeginTime(LocalDateTime.now().minusDays(7));
        adjustQuery.setEndTime(LocalDateTime.now());
        IPage<InventoryLogPageVO> adjust = inventoryLogService.getInventoryLogPage(adjustQuery);
        todos.add(todo("RECENT_INVENTORY_ADJUST", "最近7天库存调整记录", adjust.getTotal(),
                "/warehouse/inventory", "调整频繁的物料建议复核原因。", adjust.getRecords()));
        return todos;
    }

    private List<Object> supplierTodos(int limit) {
        List<Object> todos = new ArrayList<>();
        Long userId = SecurityUtils.getCurrentUserId();
        Supplier supplier = userId == null ? null : supplierMapper.getSupplierByUserId(userId);
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("type", "SUPPLIER_PROFILE");
        profile.put("title", "供应商资料状态");
        profile.put("route", "/supplier/profile");
        if (supplier == null) {
            profile.put("count", 1);
            profile.put("suggestion", "当前账号未绑定供应商档案，请联系管理员维护。");
        } else {
            Integer activeFileCount = supplierFileMapper.countActiveFile(supplier.getId());
            profile.put("supplierId", supplier.getId());
            profile.put("supplierName", supplier.getName());
            profile.put("status", supplier.getStatus());
            profile.put("activeFileCount", activeFileCount == null ? 0 : activeFileCount);
            profile.put("count", needsSupplierProfileAction(supplier, activeFileCount) ? 1 : 0);
            profile.put("suggestion", supplierProfileSuggestion(supplier, activeFileCount));
        }
        todos.add(profile);

        todos.add(orderTodo("SUPPLIER_ORDER_WAIT_CONFIRM", "有订单待确认", "WAIT_CONFIRM",
                "/supplier/order", "建议确认承诺交期并反馈供应商备注。", limit));

        PurchaseOrderQuery overdueQuery = new PurchaseOrderQuery();
        overdueQuery.setPageNum(1);
        overdueQuery.setPageSize(limit);
        overdueQuery.setStatus("IN_PROGRESS");
        overdueQuery.setPlanDateBegin(LocalDate.now().minusYears(5));
        overdueQuery.setPlanDateEnd(LocalDate.now().minusDays(7));
        IPage<PurchaseOrderVO> overdue = purchaseOrderService.getPurchaseOrderPage(overdueQuery);
        todos.add(todo("SUPPLIER_ORDER_LONG_NO_ARRIVAL", "已确认但长期未到货订单", overdue.getTotal(),
                "/supplier/order", "建议尽快登记到货或说明延期原因。", overdue.getRecords()));
        return todos;
    }

    private List<Object> adminTodos(int limit) {
        List<Object> todos = new ArrayList<>();
        SupplierPageQuery pendingSupplierQuery = new SupplierPageQuery();
        pendingSupplierQuery.setPageNum(1);
        pendingSupplierQuery.setPageSize(limit);
        pendingSupplierQuery.setStatus("PENDING");
        IPage<SupplierVO> pendingSupplier = supplierService.getSupplierPage(pendingSupplierQuery);
        todos.add(todo("SUPPLIER_REVIEW", "供应商资质待审核", pendingSupplier.getTotal(),
                "/admin/supplier", "建议核对附件和资质信息后审核。", pendingSupplier.getRecords()));

        InventoryPageQuery lowStockQuery = new InventoryPageQuery();
        lowStockQuery.setPageNum(1);
        lowStockQuery.setPageSize(limit);
        lowStockQuery.setStockStatus("LOW");
        IPage<InventoryPageVO> lowStock = inventoryService.getInventoryPage(lowStockQuery);
        todos.add(todo("ADMIN_LOW_STOCK", "低库存物料", lowStock.getTotal(),
                "/admin/inventory", "建议确认是否需要补货或调整安全库存。", lowStock.getRecords()));
        return todos;
    }

    private Map<String, Object> orderTodo(String type,
                                          String title,
                                          String status,
                                          String route,
                                          String suggestion,
                                          int limit) {
        PurchaseOrderQuery query = new PurchaseOrderQuery();
        query.setPageNum(1);
        query.setPageSize(limit);
        query.setStatus(status);
        IPage<PurchaseOrderVO> page = purchaseOrderService.getPurchaseOrderPage(query);
        return todo(type, title, page.getTotal(), route, suggestion, page.getRecords());
    }

    private AgentToolResult getPurchaseOrderContext(Map<String, Object> arguments) {
        PurchaseOrderVO order = null;
        Long orderId = longArg(arguments, "orderId");
        if (orderId != null) {
            order = purchaseOrderService.getPurchaseOrderById(orderId);
        }
        String orderNo = stringArg(arguments, "orderNo");
        if (order == null && orderNo != null) {
            PurchaseOrderQuery query = new PurchaseOrderQuery();
            query.setPageNum(1);
            query.setPageSize(1);
            query.setOrderNo(orderNo);
            IPage<PurchaseOrderVO> page = purchaseOrderService.getPurchaseOrderPage(query);
            if (!page.getRecords().isEmpty()) {
                order = purchaseOrderService.getPurchaseOrderById(page.getRecords().get(0).getId());
            }
        }
        if (order == null) {
            return AgentToolResult.failure(GET_PURCHASE_ORDER_CONTEXT, "未查询到有权限访问的采购订单");
        }

        List<PurchaseOrderItemVO> items = purchaseOrderItemMapper.getPurchaseOrderItemByOrderId(order.getId());
        AgentToolResult result = AgentToolResult.success(GET_PURCHASE_ORDER_CONTEXT,
                "采购订单 " + order.getOrderNo() + " 当前状态为 " + order.getStatus());
        result.getFacts().put("order", order);
        result.getFacts().put("items", items);
        result.getFacts().put("stageText", orderStageText(order.getStatus()));
        result.getFacts().put("nextAction", orderNextAction(order.getStatus()));
        result.getFacts().put("route", orderRoute(order.getStatus()));
        result.getItems().addAll(items);
        result.getEvidence().add("订单主表来自 purchase_order，明细来自 purchase_order_item。");
        return result;
    }

    private AgentToolResult getPurchaseRequestContext(Map<String, Object> arguments) {
        PurchaseRequestVO request = null;
        Long requestId = longArg(arguments, "requestId");
        if (requestId != null) {
            request = purchaseRequestService.getPurchaseRequestById(requestId);
        }
        String requestNo = stringArg(arguments, "requestNo");
        if (request == null && requestNo != null) {
            PurchaseRequestQuery query = new PurchaseRequestQuery();
            query.setPageNum(1);
            query.setPageSize(1);
            query.setRequestNo(requestNo);
            IPage<PurchaseRequestPageVO> page = purchaseRequestService.getPurchaseRequestPage(query);
            if (!page.getRecords().isEmpty()) {
                request = purchaseRequestService.getPurchaseRequestById(page.getRecords().get(0).getId());
            }
        }
        if (request == null) {
            return AgentToolResult.failure(GET_PURCHASE_REQUEST_CONTEXT, "未查询到有权限访问的采购申请");
        }

        List<PurchaseRequestItemVO> items = purchaseRequestItemService.getPurchaseRequestItemByRequestId(request.getId());
        List<PurchaseRequestReviewVO> reviews = purchaseRequestReviewService.getPurchaseRequestReviewByRequestId(request.getId());
        Integer orderCount = purchaseOrderMapper.countByRequestId(request.getId());
        AgentToolResult result = AgentToolResult.success(GET_PURCHASE_REQUEST_CONTEXT,
                "采购申请 " + request.getRequestNo() + " 当前状态为 " + request.getStatus());
        result.getFacts().put("request", request);
        result.getFacts().put("items", items);
        result.getFacts().put("reviews", reviews);
        result.getFacts().put("orderCreated", orderCount != null && orderCount > 0);
        result.getFacts().put("stageText", requestStageText(request.getStatus()));
        result.getFacts().put("nextAction", requestNextAction(request.getStatus()));
        result.getFacts().put("route", requestRoute(request.getStatus()));
        result.getItems().addAll(items);
        result.getEvidence().add("申请主表来自 purchase_request，明细来自 purchase_request_item，审批记录来自 purchase_request_review。");
        return result;
    }

    private AgentToolResult listInventoryAlerts(Map<String, Object> arguments) {
        String stockStatus = stringArg(arguments, "stockStatus");
        if (stockStatus == null) {
            stockStatus = "LOW";
        }
        int limit = intArg(arguments, "limit", 10, 1, 20);
        InventoryPageQuery query = new InventoryPageQuery();
        query.setPageNum(1);
        query.setPageSize(limit);
        query.setStockStatus(stockStatus);
        IPage<InventoryPageVO> page = inventoryService.getInventoryPage(query);

        AgentToolResult result = AgentToolResult.success(LIST_INVENTORY_ALERTS,
                "查询到 " + page.getTotal() + " 条库存预警记录");
        result.getFacts().put("stockStatus", stockStatus);
        result.getFacts().put("count", page.getTotal());
        result.getFacts().put("route", "/warehouse/inventory");
        result.getItems().addAll(page.getRecords());
        result.getEvidence().add("库存预警来自 inventory 与 material.safety_number/upper_number 的真实比较。");
        return result;
    }

    private AgentToolResult getSupplierProfileContext(Map<String, Object> arguments) {
        Long supplierId = longArg(arguments, "supplierId");
        Supplier supplier = null;
        if (supplierId != null && hasRole(safeRoles(), "ADMIN")) {
            supplier = supplierMapper.getSupplierById(supplierId);
        }
        if (supplier == null) {
            Long userId = SecurityUtils.getCurrentUserId();
            supplier = userId == null ? null : supplierMapper.getSupplierByUserId(userId);
        }
        if (supplier == null) {
            return AgentToolResult.failure(GET_SUPPLIER_PROFILE_CONTEXT, "未查询到当前账号绑定的供应商档案");
        }

        Integer activeFileCount = supplierFileMapper.countActiveFile(supplier.getId());
        AgentToolResult result = AgentToolResult.success(GET_SUPPLIER_PROFILE_CONTEXT,
                "供应商 " + supplier.getName() + " 当前状态为 " + supplier.getStatus());
        result.getFacts().put("supplierId", supplier.getId());
        result.getFacts().put("supplierName", supplier.getName());
        result.getFacts().put("status", supplier.getStatus());
        result.getFacts().put("activeFileCount", activeFileCount == null ? 0 : activeFileCount);
        result.getFacts().put("reviewNote", supplier.getReviewNote());
        result.getFacts().put("route", "/supplier/profile");
        result.getFacts().put("nextAction", supplierProfileSuggestion(supplier, activeFileCount));
        result.getEvidence().add("供应商资料来自 supplier，附件数量来自 supplier_file。");
        return result;
    }

    private AgentToolResult searchBusinessKnowledge(Map<String, Object> arguments) {
        String query = stringArg(arguments, "query");
        if (query == null || query.isBlank()) {
            return AgentToolResult.failure(SEARCH_BUSINESS_KNOWLEDGE, "知识检索缺少 query 参数");
        }
        String bizIntent = stringArg(arguments, "bizIntent");
        int topK = intArg(arguments, "topK", 4, 1, 10);
        List<RagSearchResultVO> records;
        try {
            records = agentRagService.search(query, bizIntent, topK);
        } catch (Exception ex) {
            AgentToolResult unavailable = AgentToolResult.success(SEARCH_BUSINESS_KNOWLEDGE,
                    "知识库当前不可用：" + ex.getMessage());
            unavailable.getFacts().put("query", query);
            unavailable.getFacts().put("bizIntent", bizIntent);
            unavailable.getFacts().put("topK", topK);
            unavailable.getFacts().put("ragAvailable", false);
            unavailable.getEvidence().add("RAG 工具已调用，但向量存储或模型配置当前不可用。");
            return unavailable;
        }
        AgentToolResult result = AgentToolResult.success(SEARCH_BUSINESS_KNOWLEDGE,
                records.isEmpty() ? "知识库未检索到相关规则" : "知识库检索到 " + records.size() + " 条规则片段");
        result.getItems().addAll(records);
        result.getFacts().put("query", query);
        result.getFacts().put("bizIntent", bizIntent);
        result.getFacts().put("topK", topK);
        result.getFacts().put("ragAvailable", true);
        result.getEvidence().add("知识片段来自 AgentRagService.search。");
        return result;
    }

    private Map<String, Object> todo(String type,
                                     String title,
                                     long count,
                                     String route,
                                     String suggestion,
                                     Object records) {
        Map<String, Object> todo = new LinkedHashMap<>();
        todo.put("type", type);
        todo.put("title", title);
        todo.put("count", count);
        todo.put("route", route);
        todo.put("suggestion", suggestion);
        todo.put("records", records);
        return todo;
    }

    private List<String> safeRoles() {
        List<String> roleCodes = SecurityUtils.getCurrentUserRoleCodes();
        return roleCodes == null ? List.of() : roleCodes;
    }

    private boolean hasRole(List<String> roles, String role) {
        return roles != null && roles.contains(role);
    }

    private boolean needsSupplierProfileAction(Supplier supplier, Integer activeFileCount) {
        if (supplier == null) {
            return true;
        }
        return "DRAFT".equals(supplier.getStatus())
                || "REJECTED".equals(supplier.getStatus())
                || activeFileCount == null
                || activeFileCount <= 0;
    }

    private String supplierProfileSuggestion(Supplier supplier, Integer activeFileCount) {
        if (supplier == null) {
            return "当前账号未绑定供应商档案。";
        }
        if ("REJECTED".equals(supplier.getStatus())) {
            return "资质被驳回，请根据驳回原因修改资料并重新提交。";
        }
        if (activeFileCount == null || activeFileCount <= 0) {
            return "请先上传供应商资质附件。";
        }
        if ("DRAFT".equals(supplier.getStatus())) {
            return "资料仍是草稿，建议提交资质审核。";
        }
        if ("PENDING".equals(supplier.getStatus())) {
            return "资料已提交，等待管理员审核。";
        }
        return "供应商资料当前无需处理。";
    }

    private String orderStageText(String status) {
        return switch (status == null ? "" : status) {
            case "WAIT_CONFIRM" -> "待供应商确认";
            case "IN_PROGRESS" -> "供应商已确认，履约中";
            case "PARTIAL_ARRIVAL" -> "部分到货，仍有未到货数量";
            case "WAIT_INBOUND" -> "已全部到货，等待入库";
            case "COMPLETED" -> "已完成";
            case "CLOSED" -> "已关闭";
            case "CANCELLED" -> "已取消";
            default -> "未知状态";
        };
    }

    private String orderNextAction(String status) {
        return switch (status == null ? "" : status) {
            case "WAIT_CONFIRM" -> "联系供应商确认订单和承诺交期。";
            case "IN_PROGRESS" -> "跟进计划交期，必要时登记到货。";
            case "PARTIAL_ARRIVAL" -> "跟进剩余未到货数量。";
            case "WAIT_INBOUND" -> "联系仓库生成或确认入库单。";
            case "COMPLETED" -> "无需继续处理。";
            case "CLOSED", "CANCELLED" -> "核对关闭或取消原因。";
            default -> "需要人工核对订单状态。";
        };
    }

    private String orderRoute(String status) {
        return switch (status == null ? "" : status) {
            case "WAIT_INBOUND" -> "/purchaser/inbound";
            case "IN_PROGRESS", "PARTIAL_ARRIVAL" -> "/purchaser/arrival";
            default -> "/purchaser/order";
        };
    }

    private String requestStageText(String status) {
        return switch (status == null ? "" : status) {
            case "DRAFT" -> "草稿";
            case "PENDING_APPROVAL" -> "待审批";
            case "APPROVED" -> "已审批，待生成订单";
            case "REJECTED" -> "已驳回";
            case "WITHDRAWN" -> "已撤回";
            case "ORDER_CREATED" -> "已生成采购订单";
            default -> "未知状态";
        };
    }

    private String requestNextAction(String status) {
        return switch (status == null ? "" : status) {
            case "DRAFT" -> "补全明细后提交审批。";
            case "PENDING_APPROVAL" -> "等待采购经理审批。";
            case "APPROVED" -> "由采购员创建采购订单。";
            case "REJECTED" -> "根据审批意见修改后重新提交。";
            case "WITHDRAWN" -> "需要时重新编辑并提交。";
            case "ORDER_CREATED" -> "继续跟进采购订单履约。";
            default -> "需要人工核对申请状态。";
        };
    }

    private String requestRoute(String status) {
        if ("PENDING_APPROVAL".equals(status)) {
            return "/manager/approval";
        }
        return "/purchaser/request";
    }

    private String stringArg(Map<String, Object> arguments, String key) {
        if (arguments == null) {
            return null;
        }
        Object value = arguments.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Long longArg(Map<String, Object> arguments, String key) {
        String value = stringArg(arguments, key);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int intArg(Map<String, Object> arguments, String key, int defaultValue, int min, int max) {
        String value = stringArg(arguments, key);
        int number = defaultValue;
        if (value != null) {
            try {
                number = Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                number = defaultValue;
            }
        }
        return Math.max(min, Math.min(max, number));
    }
}
