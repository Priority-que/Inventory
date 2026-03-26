# 面向中小制造企业的供应商协同采购与仓储履约平台
# V1 按模块按 Controller 开发文档

## 1. 文档目标

本文档用于把后端开发任务拆到 `Controller` 粒度，方便两个人直接按模块和类名开工。

拆分原则如下：

- 严格按模块划分：`auth`、`system`、`base`、`supplier`、`purchase`、`warehouse`、`inventory`、`message`、`log`、`report`
- 每个模块再拆成若干个 `Controller`
- 每个 `Controller` 明确职责、主要接口、依赖关系、难度和建议负责人
- 技术更强的一侧负责强事务、强状态流转、强权限校验的 Controller
- 技术相对弱的一侧负责 CRUD 为主、查询为主、耦合较低的 Controller

本文档基于以下基线：

- [V1统一需求说明书](D:/code/project/inventory/document/V1统一需求说明书.md)
- [V1统一系统设计文档](D:/code/project/inventory/document/V1统一系统设计文档.md)
- [V1__init.sql](D:/code/project/inventory/inventory_back/sql/V1__init.sql)

## 2. Controller 命名和路由约定

### 2.1 包结构约定

建议按模块组织代码：

```text
com.example.inventory
├── auth
│   └── controller
├── system
│   └── controller
├── base
│   └── controller
├── supplier
│   └── controller
├── purchase
│   └── controller
├── warehouse
│   └── controller
├── inventory
│   └── controller
├── message
│   └── controller
├── log
│   └── controller
└── report
    └── controller
```

### 2.2 路由前缀约定

- `auth` 模块：`/api/auth`
- `system` 模块：`/api/system`
- `base` 模块：`/api/base`
- `supplier` 模块：`/api/supplier`
- `purchase` 模块：`/api/purchase`
- `warehouse` 模块：`/api/warehouse`
- `inventory` 模块：`/api/inventory`
- `message` 模块：`/api/message`
- `log` 模块：`/api/log`
- `report` 模块：`/api/report`

### 2.3 人员约定

- `A 线`：技术更强的同学
- `B 线`：技术相对弱的同学

## 3. 总体模块拆分表

| 模块 | Controller | 主要职责 | 难度 | 建议负责人 |
|---|---|---|---|---|
| `auth` | `AuthController` | 登录、退出、当前用户、修改密码 | 高 | A 线 |
| `system` | `UserController` | 用户管理 | 中 | B 线 |
| `system` | `RoleController` | 角色管理、角色授权 | 中 | B 线 |
| `system` | `MenuController` | 菜单管理、菜单树 | 中 | B 线 |
| `system` | `DictTypeController` | 字典类型管理 | 低 | B 线 |
| `system` | `DictDataController` | 字典数据管理 | 低 | B 线 |
| `system` | `ConfigController` | 系统参数管理 | 低 | B 线 |
| `base` | `MaterialController` | 物料管理 | 低 | B 线 |
| `base` | `WarehouseController` | 仓库管理 | 低 | B 线 |
| `supplier` | `SupplierController` | 供应商资料维护、详情 | 中 | B 线 |
| `supplier` | `SupplierAuditController` | 供应商审核、停用 | 中 | B 线 |
| `purchase` | `PurchaseRequestController` | 采购申请增删改查、提交、撤回 | 高 | A 线 |
| `purchase` | `PurchaseApprovalController` | 待审批列表、审批通过、驳回、历史 | 高 | A 线 |
| `purchase` | `PurchaseOrderController` | 采购订单生成、查询、取消、关闭 | 高 | A 线 |
| `purchase` | `SupplierOrderController` | 供应商确认订单、反馈交期、我的订单 | 高 | A 线 |
| `warehouse` | `ArrivalRecordController` | 到货登记、到货列表、到货详情 | 高 | A 线 |
| `warehouse` | `InboundOrderController` | 入库单创建、确认入库、取消、查询 | 高 | A 线 |
| `inventory` | `InventoryController` | 库存台账、库存流水查询 | 中 | A 线 |
| `inventory` | `InventoryAlertController` | 库存预警查询 | 中 | A 线 |
| `message` | `MessageController` | 消息列表、已读、未读数 | 低 | B 线 |
| `log` | `OperLogController` | 操作日志查询 | 低 | B 线 |
| `report` | `PurchaseReportController` | 采购金额、订单状态、供应商订单统计 | 中 | B 线 |
| `report` | `InventoryReportController` | 入库趋势、库存预警统计 | 中 | B 线 |

## 4. 按模块按 Controller 详细拆分

## 4.1 `auth` 模块

### 4.1.1 `AuthController`

#### 主要职责

- 用户登录
- 用户退出
- 获取当前登录用户信息
- 获取当前用户菜单
- 修改密码

#### 建议接口

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/auth/menus`
- `PUT /api/auth/password`

#### 依赖表

- `sys_user`
- `sys_role`
- `sys_user_role`
- `sys_menu`
- `sys_role_menu`

#### 核心难点

- JWT 签发与解析
- 登录失败和无权限处理
- 当前用户角色和菜单聚合

#### 负责人

- A 线

## 4.2 `system` 模块

### 4.2.1 `UserController`

#### 主要职责

- 用户新增
- 用户编辑
- 用户禁用、启用
- 重置密码
- 用户列表和详情查询
- 用户绑定角色

#### 建议接口

- `POST /api/system/users`
- `PUT /api/system/users/{id}`
- `PUT /api/system/users/{id}/status`
- `PUT /api/system/users/{id}/reset-password`
- `GET /api/system/users`
- `GET /api/system/users/{id}`
- `PUT /api/system/users/{id}/roles`

#### 依赖表

- `sys_user`
- `sys_user_role`

#### 难度

- 中

#### 负责人

- B 线

### 4.2.2 `RoleController`

#### 主要职责

- 角色新增
- 角色编辑
- 角色启用、停用
- 角色列表查询
- 角色菜单授权

#### 建议接口

- `POST /api/system/roles`
- `PUT /api/system/roles/{id}`
- `PUT /api/system/roles/{id}/status`
- `GET /api/system/roles`
- `GET /api/system/roles/{id}`
- `PUT /api/system/roles/{id}/menus`

#### 依赖表

- `sys_role`
- `sys_role_menu`

#### 难度

- 中

#### 负责人

- B 线

### 4.2.3 `MenuController`

#### 主要职责

- 菜单新增
- 菜单编辑
- 菜单删除
- 菜单树查询
- 按角色查询菜单

#### 建议接口

- `POST /api/system/menus`
- `PUT /api/system/menus/{id}`
- `DELETE /api/system/menus/{id}`
- `GET /api/system/menus/tree`
- `GET /api/system/menus/role/{roleId}`

#### 依赖表

- `sys_menu`
- `sys_role_menu`

#### 难度

- 中

#### 负责人

- B 线

### 4.2.4 `DictTypeController`

#### 主要职责

- 字典类型增删改查
- 字典类型启停用

#### 建议接口

- `POST /api/system/dict-types`
- `PUT /api/system/dict-types/{id}`
- `GET /api/system/dict-types`
- `GET /api/system/dict-types/{id}`
- `PUT /api/system/dict-types/{id}/status`

#### 依赖表

- `sys_dict_type`

#### 难度

- 低

#### 负责人

- B 线

### 4.2.5 `DictDataController`

#### 主要职责

- 字典数据增删改查
- 根据字典类型查询字典项

#### 建议接口

- `POST /api/system/dict-data`
- `PUT /api/system/dict-data/{id}`
- `GET /api/system/dict-data`
- `GET /api/system/dict-data/{id}`
- `GET /api/system/dict-data/type/{dictType}`
- `PUT /api/system/dict-data/{id}/status`

#### 依赖表

- `sys_dict_data`
- `sys_dict_type`

#### 难度

- 低

#### 负责人

- B 线

### 4.2.6 `ConfigController`

#### 主要职责

- 系统参数增删改查
- 参数启停用

#### 建议接口

- `POST /api/system/configs`
- `PUT /api/system/configs/{id}`
- `GET /api/system/configs`
- `GET /api/system/configs/{id}`
- `PUT /api/system/configs/{id}/status`

#### 依赖表

- `sys_config`

#### 难度

- 低

#### 负责人

- B 线

## 4.3 `base` 模块

### 4.3.1 `MaterialController`

#### 主要职责

- 物料新增
- 物料编辑
- 物料启停用
- 物料分页查询
- 物料详情查询

#### 建议接口

- `POST /api/base/materials`
- `PUT /api/base/materials/{id}`
- `PUT /api/base/materials/{id}/status`
- `GET /api/base/materials`
- `GET /api/base/materials/{id}`

#### 依赖表

- `base_material`

#### 难度

- 低

#### 负责人

- B 线

### 4.3.2 `WarehouseController`

#### 主要职责

- 仓库新增
- 仓库编辑
- 仓库启停用
- 仓库分页查询
- 仓库详情查询

#### 建议接口

- `POST /api/base/warehouses`
- `PUT /api/base/warehouses/{id}`
- `PUT /api/base/warehouses/{id}/status`
- `GET /api/base/warehouses`
- `GET /api/base/warehouses/{id}`

#### 依赖表

- `base_warehouse`

#### 难度

- 低

#### 负责人

- B 线

## 4.4 `supplier` 模块

### 4.4.1 `SupplierController`

#### 主要职责

- 供应商资料维护
- 供应商详情查看
- 供应商列表查询
- 提交审核

#### 建议接口

- `POST /api/supplier/profile`
- `PUT /api/supplier/profile`
- `GET /api/supplier/profile`
- `GET /api/supplier/list`
- `POST /api/supplier/profile/submit`

#### 依赖表

- `supplier`

#### 核心点

- 供应商只能维护自己的资料
- 提交审核后状态从 `DRAFT` 变为 `PENDING`

#### 难度

- 中

#### 负责人

- B 线

### 4.4.2 `SupplierAuditController`

#### 主要职责

- 管理员审核供应商
- 管理员驳回供应商
- 管理员停用供应商

#### 建议接口

- `GET /api/supplier/audit/pending`
- `PUT /api/supplier/audit/{id}/approve`
- `PUT /api/supplier/audit/{id}/reject`
- `PUT /api/supplier/audit/{id}/disable`

#### 依赖表

- `supplier`

#### 核心点

- 审核状态流转
- 审核意见记录

#### 难度

- 中

#### 负责人

- B 线

## 4.5 `purchase` 模块

### 4.5.1 `PurchaseRequestController`

#### 主要职责

- 新建采购申请
- 编辑采购申请
- 删除草稿采购申请
- 提交采购申请
- 撤回采购申请
- 采购申请列表
- 采购申请详情

#### 建议接口

- `POST /api/purchase/requests`
- `PUT /api/purchase/requests/{id}`
- `DELETE /api/purchase/requests/{id}`
- `POST /api/purchase/requests/{id}/submit`
- `POST /api/purchase/requests/{id}/withdraw`
- `GET /api/purchase/requests`
- `GET /api/purchase/requests/{id}`

#### 依赖表

- `purchase_request`
- `purchase_request_item`
- `base_material`

#### 核心点

- 草稿和驳回单允许修改
- 明细至少一条
- 提交后不可再改明细

#### 难度

- 高

#### 负责人

- A 线

### 4.5.2 `PurchaseApprovalController`

#### 主要职责

- 待审批列表
- 审批通过
- 审批驳回
- 审批历史

#### 建议接口

- `GET /api/purchase/approvals/todo`
- `PUT /api/purchase/approvals/{requestId}/approve`
- `PUT /api/purchase/approvals/{requestId}/reject`
- `GET /api/purchase/approvals/history/{requestId}`

#### 依赖表

- `purchase_request`
- `purchase_request_approval`

#### 核心点

- 只有采购主管能审批
- 同一张申请只能审批一次
- 审批意见必填

#### 难度

- 高

#### 负责人

- A 线

### 4.5.3 `PurchaseOrderController`

#### 主要职责

- 生成采购订单
- 采购订单列表
- 采购订单详情
- 取消采购订单
- 关闭采购订单

#### 建议接口

- `POST /api/purchase/orders`
- `GET /api/purchase/orders`
- `GET /api/purchase/orders/{id}`
- `PUT /api/purchase/orders/{id}/cancel`
- `PUT /api/purchase/orders/{id}/close`

#### 依赖表

- `purchase_order`
- `purchase_order_item`
- `purchase_request`
- `purchase_request_item`
- `supplier`

#### 核心点

- 仅已审批通过申请可生成订单
- 一张申请只能生成一张订单
- 仅 `ACTIVE` 供应商可选

#### 难度

- 高

#### 负责人

- A 线

### 4.5.4 `SupplierOrderController`

#### 主要职责

- 供应商查看我的订单
- 供应商查看订单详情
- 供应商确认订单
- 供应商反馈预计交期

#### 建议接口

- `GET /api/purchase/supplier-orders`
- `GET /api/purchase/supplier-orders/{id}`
- `PUT /api/purchase/supplier-orders/{id}/confirm`
- `PUT /api/purchase/supplier-orders/{id}/delivery-date`

#### 依赖表

- `purchase_order`
- `purchase_order_item`
- `supplier`

#### 核心点

- 供应商只能看自己的订单
- 确认后订单状态转为 `IN_PROGRESS`

#### 难度

- 高

#### 负责人

- A 线

## 4.6 `warehouse` 模块

### 4.6.1 `ArrivalRecordController`

#### 主要职责

- 到货登记
- 到货列表
- 到货详情

#### 建议接口

- `POST /api/warehouse/arrivals`
- `GET /api/warehouse/arrivals`
- `GET /api/warehouse/arrivals/{id}`

#### 依赖表

- `arrival_record`
- `arrival_record_item`
- `purchase_order`
- `purchase_order_item`
- `base_warehouse`

#### 核心点

- 到货数量不能超过订单剩余数量
- 合格数量和不合格数量之和必须等于到货数量
- 部分到货后订单状态转为 `PARTIAL_ARRIVAL`

#### 难度

- 高

#### 负责人

- A 线

### 4.6.2 `InboundOrderController`

#### 主要职责

- 创建入库单
- 确认入库
- 取消待入库单
- 入库单列表
- 入库单详情

#### 建议接口

- `POST /api/warehouse/inbounds`
- `PUT /api/warehouse/inbounds/{id}/confirm`
- `PUT /api/warehouse/inbounds/{id}/cancel`
- `GET /api/warehouse/inbounds`
- `GET /api/warehouse/inbounds/{id}`

#### 依赖表

- `inbound_order`
- `inbound_order_item`
- `arrival_record`
- `arrival_record_item`
- `inventory`
- `inventory_flow`

#### 核心点

- 确认入库必须走事务
- 入库时同时更新库存台账和库存流水
- 全部完成入库后订单状态更新为 `COMPLETED`

#### 难度

- 高

#### 负责人

- A 线

## 4.7 `inventory` 模块

### 4.7.1 `InventoryController`

#### 主要职责

- 库存台账分页查询
- 库存台账详情查询
- 库存流水分页查询

#### 建议接口

- `GET /api/inventory/stocks`
- `GET /api/inventory/stocks/{id}`
- `GET /api/inventory/flows`

#### 依赖表

- `inventory`
- `inventory_flow`
- `base_material`
- `base_warehouse`

#### 难度

- 中

#### 负责人

- A 线

### 4.7.2 `InventoryAlertController`

#### 主要职责

- 低库存预警查询
- 超储预警查询
- 预警汇总统计查询

#### 建议接口

- `GET /api/inventory/alerts/low-stock`
- `GET /api/inventory/alerts/over-stock`
- `GET /api/inventory/alerts/summary`

#### 依赖表

- `inventory`
- `base_material`

#### 核心点

- 预警通过库存数量和物料阈值动态计算

#### 难度

- 中

#### 负责人

- A 线

## 4.8 `message` 模块

### 4.8.1 `MessageController`

#### 主要职责

- 消息列表查询
- 消息详情查询
- 标记已读
- 批量已读
- 未读数量统计

#### 建议接口

- `GET /api/message/messages`
- `GET /api/message/messages/{id}`
- `PUT /api/message/messages/{id}/read`
- `PUT /api/message/messages/read-all`
- `GET /api/message/messages/unread-count`

#### 依赖表

- `sys_message`

#### 难度

- 低

#### 负责人

- B 线

## 4.9 `log` 模块

### 4.9.1 `OperLogController`

#### 主要职责

- 操作日志列表查询
- 操作日志详情查询

#### 建议接口

- `GET /api/log/oper-logs`
- `GET /api/log/oper-logs/{id}`

#### 依赖表

- `sys_oper_log`

#### 难度

- 低

#### 负责人

- B 线

## 4.10 `report` 模块

### 4.10.1 `PurchaseReportController`

#### 主要职责

- 采购金额统计
- 订单状态统计
- 供应商订单统计

#### 建议接口

- `GET /api/report/purchase/amount-summary`
- `GET /api/report/purchase/status-summary`
- `GET /api/report/purchase/supplier-summary`

#### 依赖表

- `purchase_order`
- `purchase_order_item`
- `supplier`

#### 难度

- 中

#### 负责人

- B 线

### 4.10.2 `InventoryReportController`

#### 主要职责

- 入库趋势统计
- 库存预警统计

#### 建议接口

- `GET /api/report/inventory/inbound-trend`
- `GET /api/report/inventory/alert-summary`

#### 依赖表

- `inbound_order`
- `inbound_order_item`
- `inventory`
- `base_material`

#### 难度

- 中

#### 负责人

- B 线

## 5. 推荐并行开发顺序

### 第一阶段

#### A 线

- `AuthController`
- `PurchaseRequestController`
- `PurchaseApprovalController`

#### B 线

- `DictTypeController`
- `DictDataController`
- `ConfigController`
- `MaterialController`
- `WarehouseController`

### 第二阶段

#### A 线

- `PurchaseOrderController`
- `SupplierOrderController`
- `ArrivalRecordController`
- `InboundOrderController`

#### B 线

- `SupplierController`
- `SupplierAuditController`
- `UserController`
- `RoleController`
- `MenuController`

### 第三阶段

#### A 线

- `InventoryController`
- `InventoryAlertController`

#### B 线

- `MessageController`
- `OperLogController`
- `PurchaseReportController`
- `InventoryReportController`

## 6. 最适合你们当前水平的分工结论

### A 线最适合负责

- `AuthController`
- `PurchaseRequestController`
- `PurchaseApprovalController`
- `PurchaseOrderController`
- `SupplierOrderController`
- `ArrivalRecordController`
- `InboundOrderController`
- `InventoryController`
- `InventoryAlertController`

这些 Controller 的共同特点是：

- 权限和身份判断多
- 状态流转复杂
- 事务要求高
- 多表联动多
- 容易因为重复操作导致 bug

### B 线最适合负责

- `UserController`
- `RoleController`
- `MenuController`
- `DictTypeController`
- `DictDataController`
- `ConfigController`
- `MaterialController`
- `WarehouseController`
- `SupplierController`
- `SupplierAuditController`
- `MessageController`
- `OperLogController`
- `PurchaseReportController`
- `InventoryReportController`

这些 Controller 的共同特点是：

- 以 CRUD 和查询为主
- 状态流转相对简单
- 单表或少量联表
- 对主链路事务影响较小

## 7. 你们开工时最重要的约定

1. 先由 A 线把 `AuthController` 和权限底座跑通，B 线再开始写后台管理类 Controller。
2. `purchase`、`warehouse`、`inventory` 这三个模块尽量不要两个人同时改。
3. 所有状态变更接口都用明确动作接口，不要把状态流转塞进普通更新接口。
4. 列表查询和报表聚合统一放到 XML，避免 Service 里写复杂 SQL。
5. 先跑通主链路，再补报表和消息中心。
