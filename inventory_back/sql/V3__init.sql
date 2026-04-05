CREATE DATABASE IF NOT EXISTS inventory
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE inventory;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `inventory_log`;
DROP TABLE IF EXISTS `inventory`;
DROP TABLE IF EXISTS `inbound_item`;
DROP TABLE IF EXISTS `inbound`;
DROP TABLE IF EXISTS `arrival_item`;
DROP TABLE IF EXISTS `arrival`;
DROP TABLE IF EXISTS `purchase_order_item`;
DROP TABLE IF EXISTS `purchase_order`;
DROP TABLE IF EXISTS `purchase_request_review`;
DROP TABLE IF EXISTS `purchase_request_item`;
DROP TABLE IF EXISTS `purchase_request`;
DROP TABLE IF EXISTS `supplier_file`;
DROP TABLE IF EXISTS `supplier`;
DROP TABLE IF EXISTS `warehouse`;
DROP TABLE IF EXISTS `material`;
DROP TABLE IF EXISTS `user_role`;
DROP TABLE IF EXISTS `role`;
DROP TABLE IF EXISTS `user`;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
    `password` VARCHAR(255) NOT NULL COMMENT '密码密文',
    `name` VARCHAR(64) NOT NULL COMMENT '用户姓名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `dept` VARCHAR(64) DEFAULT NULL COMMENT '部门名称',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY `uk_user_username` (`username`),
    KEY `idx_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `role` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `code` VARCHAR(64) NOT NULL COMMENT '角色编码',
    `name` VARCHAR(64) NOT NULL COMMENT '角色名称',
    `sort_number` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY `uk_role_code` (`code`),
    KEY `idx_role_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE `user_role` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `is_primary` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主角色',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_user_role_user_role` (`user_id`, `role_id`),
    KEY `idx_user_role_user_primary` (`user_id`, `is_primary`),
    KEY `idx_user_role_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE `material` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `code` VARCHAR(64) NOT NULL COMMENT '物料编码',
    `name` VARCHAR(128) NOT NULL COMMENT '物料名称',
    `specification` VARCHAR(128) DEFAULT NULL COMMENT '规格型号',
    `unit` VARCHAR(32) NOT NULL COMMENT '单位',
    `category_name` VARCHAR(64) DEFAULT NULL COMMENT '分类名称',
    `safety_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '安全库存',
    `upper_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '库存上限',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY `uk_material_code` (`code`),
    KEY `idx_material_name` (`name`),
    KEY `idx_material_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料表';

CREATE TABLE `warehouse` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `code` VARCHAR(64) NOT NULL COMMENT '仓库编码',
    `name` VARCHAR(128) NOT NULL COMMENT '仓库名称',
    `address` VARCHAR(255) DEFAULT NULL COMMENT '仓库地址',
    `manager_name` VARCHAR(64) DEFAULT NULL COMMENT '负责人姓名',
    `manager_phone` VARCHAR(20) DEFAULT NULL COMMENT '负责人电话',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY `uk_warehouse_code` (`code`),
    KEY `idx_warehouse_name` (`name`),
    KEY `idx_warehouse_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓库表';

CREATE TABLE `supplier` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '供应商账号ID',
    `code` VARCHAR(64) NOT NULL COMMENT '供应商编码',
    `name` VARCHAR(128) NOT NULL COMMENT '供应商名称',
    `contact_name` VARCHAR(64) NOT NULL COMMENT '联系人',
    `contact_phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
    `license_no` VARCHAR(64) DEFAULT NULL COMMENT '营业执照号',
    `file_round` INT NOT NULL DEFAULT 1 COMMENT '当前附件轮次',
    `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/PENDING/REJECTED/ACTIVE/DISABLED',
    `submit_time` DATETIME DEFAULT NULL COMMENT '提交审核时间',
    `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `review_user_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
    `review_note` VARCHAR(255) DEFAULT NULL COMMENT '审核说明',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY `uk_supplier_user_id` (`user_id`),
    UNIQUE KEY `uk_supplier_code` (`code`),
    KEY `idx_supplier_name` (`name`),
    KEY `idx_supplier_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商表';

CREATE TABLE `supplier_file` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
    `file_round` INT NOT NULL DEFAULT 1 COMMENT '附件轮次',
    `file_type` VARCHAR(32) NOT NULL COMMENT '文件类型 BUSINESS_LICENSE/QUALIFICATION/OTHER',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名称',
    `file_url` VARCHAR(500) NOT NULL COMMENT '文件地址',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小',
    `mime_type` VARCHAR(100) DEFAULT NULL COMMENT '文件类型',
    `active_flag` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否当前有效',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    KEY `idx_supplier_file_supplier_round` (`supplier_id`, `file_round`),
    KEY `idx_supplier_file_supplier_type` (`supplier_id`, `file_type`, `active_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商附件表';

CREATE TABLE `purchase_request` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `request_no` VARCHAR(64) NOT NULL COMMENT '采购申请单号',
    `title` VARCHAR(128) NOT NULL COMMENT '申请标题',
    `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
    `dept` VARCHAR(64) NOT NULL COMMENT '申请部门',
    `expected_date` DATE NOT NULL COMMENT '期望到货日期',
    `submit_time` DATETIME DEFAULT NULL COMMENT '提交时间',
    `review_user_id` BIGINT DEFAULT NULL COMMENT '最后审批人ID',
    `review_time` DATETIME DEFAULT NULL COMMENT '最后审批时间',
    `review_note` VARCHAR(255) DEFAULT NULL COMMENT '最后审批说明',
    `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/PENDING_APPROVAL/APPROVED/REJECTED/WITHDRAWN/ORDER_CREATED',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY `uk_purchase_request_no` (`request_no`),
    KEY `idx_purchase_request_applicant` (`applicant_id`),
    KEY `idx_purchase_request_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购申请主表';

CREATE TABLE `purchase_request_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `request_id` BIGINT NOT NULL COMMENT '采购申请ID',
    `material_id` BIGINT NOT NULL COMMENT '物料ID',
    `material_code` VARCHAR(64) NOT NULL COMMENT '物料编码快照',
    `material_name` VARCHAR(128) NOT NULL COMMENT '物料名称快照',
    `specification` VARCHAR(128) DEFAULT NULL COMMENT '规格型号快照',
    `unit` VARCHAR(32) NOT NULL COMMENT '单位快照',
    `request_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '申请数量',
    `sort_number` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    KEY `idx_purchase_request_item_request` (`request_id`),
    KEY `idx_purchase_request_item_material` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购申请明细表';

CREATE TABLE `purchase_request_review` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `request_id` BIGINT NOT NULL COMMENT '采购申请ID',
    `action_type` VARCHAR(32) NOT NULL COMMENT '动作 SUBMIT/APPROVE/REJECT/WITHDRAW/RESUBMIT',
    `from_status` VARCHAR(32) DEFAULT NULL COMMENT '变更前状态',
    `to_status` VARCHAR(32) NOT NULL COMMENT '变更后状态',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(64) NOT NULL COMMENT '操作人姓名快照',
    `operate_note` VARCHAR(255) DEFAULT NULL COMMENT '操作说明',
    `operate_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY `idx_purchase_request_review_request` (`request_id`),
    KEY `idx_purchase_request_review_operator` (`operator_id`),
    KEY `idx_purchase_request_review_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购申请审批历史表';

CREATE TABLE `purchase_order` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '采购订单号',
    `request_id` BIGINT NOT NULL COMMENT '来源采购申请ID',
    `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
    `purchaser_id` BIGINT NOT NULL COMMENT '采购员ID',
    `plan_date` DATE NOT NULL COMMENT '计划交期',
    `supplier_date` DATE DEFAULT NULL COMMENT '供应商反馈交期',
    `confirm_time` DATETIME DEFAULT NULL COMMENT '供应商确认时间',
    `total_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    `status` VARCHAR(32) NOT NULL DEFAULT 'WAIT_CONFIRM' COMMENT '状态 WAIT_CONFIRM/IN_PROGRESS/PARTIAL_ARRIVAL/COMPLETED/CLOSED/CANCELLED',
    `supplier_note` VARCHAR(255) DEFAULT NULL COMMENT '供应商反馈说明',
    `close_time` DATETIME DEFAULT NULL COMMENT '关闭时间',
    `close_reason` VARCHAR(255) DEFAULT NULL COMMENT '关闭原因',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY `uk_purchase_order_no` (`order_no`),
    UNIQUE KEY `uk_purchase_order_request` (`request_id`),
    KEY `idx_purchase_order_supplier` (`supplier_id`),
    KEY `idx_purchase_order_purchaser` (`purchaser_id`),
    KEY `idx_purchase_order_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单主表';

CREATE TABLE `purchase_order_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '采购订单ID',
    `request_item_id` BIGINT NOT NULL COMMENT '来源申请明细ID',
    `material_id` BIGINT NOT NULL COMMENT '物料ID',
    `material_code` VARCHAR(64) NOT NULL COMMENT '物料编码快照',
    `material_name` VARCHAR(128) NOT NULL COMMENT '物料名称快照',
    `specification` VARCHAR(128) DEFAULT NULL COMMENT '规格型号快照',
    `unit` VARCHAR(32) NOT NULL COMMENT '单位快照',
    `order_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '订单数量',
    `unit_price` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    `line_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '行金额',
    `arrived_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '累计到货数量',
    `inbound_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '累计入库数量',
    `sort_number` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    KEY `idx_purchase_order_item_order` (`order_id`),
    KEY `idx_purchase_order_item_request_item` (`request_item_id`),
    KEY `idx_purchase_order_item_material` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单明细表';

CREATE TABLE `arrival` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `arrival_no` VARCHAR(64) NOT NULL COMMENT '到货单号',
    `order_id` BIGINT NOT NULL COMMENT '采购订单ID',
    `warehouse_id` BIGINT NOT NULL COMMENT '仓库ID',
    `arrival_date` DATE NOT NULL COMMENT '到货日期',
    `arrival_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '总到货数量',
    `qualified_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '总合格数量',
    `unqualified_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '总不合格数量',
    `status` VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '状态 NORMAL/ABNORMAL',
    `abnormal_note` VARCHAR(255) DEFAULT NULL COMMENT '异常说明',
    `operator_id` BIGINT NOT NULL COMMENT '登记人ID',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY `uk_arrival_no` (`arrival_no`),
    KEY `idx_arrival_order` (`order_id`),
    KEY `idx_arrival_warehouse` (`warehouse_id`),
    KEY `idx_arrival_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='到货主表';

CREATE TABLE `arrival_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `arrival_id` BIGINT NOT NULL COMMENT '到货主表ID',
    `order_item_id` BIGINT NOT NULL COMMENT '采购订单明细ID',
    `material_id` BIGINT NOT NULL COMMENT '物料ID',
    `material_code` VARCHAR(64) NOT NULL COMMENT '物料编码快照',
    `material_name` VARCHAR(128) NOT NULL COMMENT '物料名称快照',
    `specification` VARCHAR(128) DEFAULT NULL COMMENT '规格型号快照',
    `unit` VARCHAR(32) NOT NULL COMMENT '单位快照',
    `arrival_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '到货数量',
    `qualified_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '合格数量',
    `unqualified_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '不合格数量',
    `abnormal_note` VARCHAR(255) DEFAULT NULL COMMENT '异常说明',
    `sort_number` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    KEY `idx_arrival_item_arrival` (`arrival_id`),
    KEY `idx_arrival_item_order_item` (`order_item_id`),
    KEY `idx_arrival_item_material` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='到货明细表';

CREATE TABLE `inbound` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `inbound_no` VARCHAR(64) NOT NULL COMMENT '入库单号',
    `arrival_id` BIGINT NOT NULL COMMENT '来源到货单ID',
    `warehouse_id` BIGINT NOT NULL COMMENT '仓库ID',
    `inbound_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '总入库数量',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/COMPLETED/CANCELLED/ABNORMAL',
    `operator_id` BIGINT NOT NULL COMMENT '入库人ID',
    `inbound_time` DATETIME DEFAULT NULL COMMENT '确认入库时间',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY `uk_inbound_no` (`inbound_no`),
    UNIQUE KEY `uk_inbound_arrival` (`arrival_id`),
    KEY `idx_inbound_warehouse` (`warehouse_id`),
    KEY `idx_inbound_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入库主表';

CREATE TABLE `inbound_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `inbound_id` BIGINT NOT NULL COMMENT '入库单ID',
    `arrival_item_id` BIGINT NOT NULL COMMENT '来源到货明细ID',
    `material_id` BIGINT NOT NULL COMMENT '物料ID',
    `material_code` VARCHAR(64) NOT NULL COMMENT '物料编码快照',
    `material_name` VARCHAR(128) NOT NULL COMMENT '物料名称快照',
    `specification` VARCHAR(128) DEFAULT NULL COMMENT '规格型号快照',
    `unit` VARCHAR(32) NOT NULL COMMENT '单位快照',
    `inbound_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '入库数量',
    `sort_number` INT NOT NULL DEFAULT 0 COMMENT '排序值',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    KEY `idx_inbound_item_inbound` (`inbound_id`),
    KEY `idx_inbound_item_arrival_item` (`arrival_item_id`),
    KEY `idx_inbound_item_material` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入库明细表';

CREATE TABLE `inventory` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `material_id` BIGINT NOT NULL COMMENT '物料ID',
    `warehouse_id` BIGINT NOT NULL COMMENT '仓库ID',
    `current_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '当前库存数量',
    `last_inbound_time` DATETIME DEFAULT NULL COMMENT '最后入库时间',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY `uk_inventory_material_warehouse` (`material_id`, `warehouse_id`),
    KEY `idx_inventory_material` (`material_id`),
    KEY `idx_inventory_warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存台账表';

CREATE TABLE `inventory_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `log_no` VARCHAR(64) NOT NULL COMMENT '库存流水号',
    `inventory_id` BIGINT NOT NULL COMMENT '库存台账ID',
    `material_id` BIGINT NOT NULL COMMENT '物料ID',
    `warehouse_id` BIGINT NOT NULL COMMENT '仓库ID',
    `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型 INBOUND/INIT',
    `biz_id` BIGINT NOT NULL COMMENT '业务主键ID',
    `before_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '变更前数量',
    `change_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '变更数量',
    `after_number` DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '变更后数量',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名快照',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `operate_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    UNIQUE KEY `uk_inventory_log_no` (`log_no`),
    KEY `idx_inventory_log_inventory` (`inventory_id`),
    KEY `idx_inventory_log_material` (`material_id`),
    KEY `idx_inventory_log_warehouse` (`warehouse_id`),
    KEY `idx_inventory_log_biz` (`biz_type`, `biz_id`),
    KEY `idx_inventory_log_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存流水表';
