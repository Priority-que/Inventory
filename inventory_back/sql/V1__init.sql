CREATE DATABASE IF NOT EXISTS inventory
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE inventory;

DROP TABLE IF EXISTS sys_oper_log;
DROP TABLE IF EXISTS sys_message;
DROP TABLE IF EXISTS inventory_flow;
DROP TABLE IF EXISTS inventory;
DROP TABLE IF EXISTS inbound_order_item;
DROP TABLE IF EXISTS inbound_order;
DROP TABLE IF EXISTS arrival_record_item;
DROP TABLE IF EXISTS arrival_record;
DROP TABLE IF EXISTS purchase_order_item;
DROP TABLE IF EXISTS purchase_order;
DROP TABLE IF EXISTS purchase_request_approval;
DROP TABLE IF EXISTS purchase_request_item;
DROP TABLE IF EXISTS purchase_request;
DROP TABLE IF EXISTS supplier;
DROP TABLE IF EXISTS base_warehouse;
DROP TABLE IF EXISTS base_material;
DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS sys_dict_data;
DROP TABLE IF EXISTS sys_dict_type;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    password VARCHAR(255) NOT NULL COMMENT '密码密文',
    real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    dept_name VARCHAR(64) DEFAULT NULL COMMENT '部门名称',
    main_role_code VARCHAR(32) DEFAULT NULL COMMENT '主角色编码',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '用户状态 ENABLED/DISABLED',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_status (status),
    KEY idx_sys_user_main_role_code (main_role_code),
    KEY idx_sys_user_dept_name (dept_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_code VARCHAR(32) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    role_sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '角色状态 ENABLED/DISABLED',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_sys_role_role_code (role_code),
    KEY idx_sys_role_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_sys_user_role_user_role (user_id, role_id),
    KEY idx_sys_user_role_user_id (user_id),
    KEY idx_sys_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE sys_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID',
    menu_name VARCHAR(64) NOT NULL COMMENT '菜单名称',
    menu_type VARCHAR(20) NOT NULL COMMENT '菜单类型 DIRECTORY/MENU/BUTTON',
    route_path VARCHAR(128) DEFAULT NULL COMMENT '前端路由',
    component VARCHAR(128) DEFAULT NULL COMMENT '前端组件',
    permission_code VARCHAR(128) DEFAULT NULL COMMENT '权限标识',
    icon VARCHAR(64) DEFAULT NULL COMMENT '图标',
    visible TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可见',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '菜单状态 ENABLED/DISABLED',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    KEY idx_sys_menu_parent_id (parent_id),
    KEY idx_sys_menu_type (menu_type),
    KEY idx_sys_menu_status (status),
    KEY idx_sys_menu_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统菜单表';

CREATE TABLE sys_role_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_sys_role_menu_role_menu (role_id, menu_id),
    KEY idx_sys_role_menu_role_id (role_id),
    KEY idx_sys_role_menu_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

CREATE TABLE sys_dict_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    dict_type VARCHAR(64) NOT NULL COMMENT '字典类型编码',
    dict_name VARCHAR(64) NOT NULL COMMENT '字典名称',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_sys_dict_type_dict_type (dict_type),
    KEY idx_sys_dict_type_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型表';

CREATE TABLE sys_dict_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    dict_type_id BIGINT NOT NULL COMMENT '字典类型ID',
    dict_label VARCHAR(64) NOT NULL COMMENT '字典标签',
    dict_value VARCHAR(64) NOT NULL COMMENT '字典值',
    dict_sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_sys_dict_data_type_value (dict_type_id, dict_value),
    KEY idx_sys_dict_data_type_id (dict_type_id),
    KEY idx_sys_dict_data_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典数据表';

CREATE TABLE sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    config_key VARCHAR(64) NOT NULL COMMENT '参数键',
    config_name VARCHAR(64) NOT NULL COMMENT '参数名称',
    config_value VARCHAR(500) NOT NULL COMMENT '参数值',
    config_type VARCHAR(32) NOT NULL DEFAULT 'SYSTEM' COMMENT '参数类型',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_sys_config_config_key (config_key),
    KEY idx_sys_config_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统参数表';

CREATE TABLE base_material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    material_code VARCHAR(64) NOT NULL COMMENT '物料编码',
    material_name VARCHAR(128) NOT NULL COMMENT '物料名称',
    specification VARCHAR(128) DEFAULT NULL COMMENT '规格型号',
    unit VARCHAR(32) NOT NULL COMMENT '计量单位',
    category_name VARCHAR(64) DEFAULT NULL COMMENT '分类名称',
    safety_stock DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '安全库存',
    upper_limit_stock DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '库存上限',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_base_material_material_code (material_code),
    KEY idx_base_material_material_name (material_name),
    KEY idx_base_material_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料基础信息表';

CREATE TABLE base_warehouse (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    warehouse_code VARCHAR(64) NOT NULL COMMENT '仓库编码',
    warehouse_name VARCHAR(128) NOT NULL COMMENT '仓库名称',
    warehouse_address VARCHAR(255) DEFAULT NULL COMMENT '仓库地址',
    manager_name VARCHAR(64) DEFAULT NULL COMMENT '负责人',
    manager_phone VARCHAR(20) DEFAULT NULL COMMENT '负责人电话',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_base_warehouse_warehouse_code (warehouse_code),
    KEY idx_base_warehouse_warehouse_name (warehouse_name),
    KEY idx_base_warehouse_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓库基础信息表';

CREATE TABLE supplier (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '供应商用户ID',
    supplier_code VARCHAR(64) NOT NULL COMMENT '供应商编码',
    supplier_name VARCHAR(128) NOT NULL COMMENT '供应商名称',
    contact_person VARCHAR(64) NOT NULL COMMENT '联系人',
    contact_phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    address VARCHAR(255) DEFAULT NULL COMMENT '地址',
    license_no VARCHAR(64) DEFAULT NULL COMMENT '营业执照号',
    license_file_url VARCHAR(255) DEFAULT NULL COMMENT '营业执照文件地址',
    qualification_file_url VARCHAR(255) DEFAULT NULL COMMENT '资质文件地址',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/PENDING/REJECTED/ACTIVE/DISABLED',
    audit_remark VARCHAR(255) DEFAULT NULL COMMENT '审核意见',
    audit_time DATETIME DEFAULT NULL COMMENT '审核时间',
    auditor_id BIGINT DEFAULT NULL COMMENT '审核人ID',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_supplier_user_id (user_id),
    UNIQUE KEY uk_supplier_supplier_code (supplier_code),
    KEY idx_supplier_name (supplier_name),
    KEY idx_supplier_status (status),
    KEY idx_supplier_auditor_id (auditor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商信息表';

CREATE TABLE purchase_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    request_no VARCHAR(64) NOT NULL COMMENT '采购申请单号',
    request_title VARCHAR(128) NOT NULL COMMENT '申请标题',
    requester_id BIGINT NOT NULL COMMENT '申请人ID',
    request_dept VARCHAR(64) NOT NULL COMMENT '申请部门',
    expected_arrival_date DATE NOT NULL COMMENT '期望到货日期',
    submit_time DATETIME DEFAULT NULL COMMENT '提交时间',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/PENDING_APPROVAL/APPROVED/REJECTED/WITHDRAWN/ORDER_CREATED',
    reviewer_id BIGINT DEFAULT NULL COMMENT '最后审批人ID',
    review_time DATETIME DEFAULT NULL COMMENT '最后审批时间',
    review_remark VARCHAR(255) DEFAULT NULL COMMENT '最后审批意见',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_purchase_request_request_no (request_no),
    KEY idx_purchase_request_requester_id (requester_id),
    KEY idx_purchase_request_status (status),
    KEY idx_purchase_request_reviewer_id (reviewer_id),
    KEY idx_purchase_request_request_dept (request_dept)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购申请主表';

CREATE TABLE purchase_request_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    request_id BIGINT NOT NULL COMMENT '采购申请ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    material_code VARCHAR(64) NOT NULL COMMENT '物料编码快照',
    material_name VARCHAR(128) NOT NULL COMMENT '物料名称快照',
    specification VARCHAR(128) DEFAULT NULL COMMENT '规格型号快照',
    unit VARCHAR(32) NOT NULL COMMENT '单位快照',
    request_qty DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '申请数量',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    KEY idx_purchase_request_item_request_id (request_id),
    KEY idx_purchase_request_item_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购申请明细表';

CREATE TABLE purchase_request_approval (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    request_id BIGINT NOT NULL COMMENT '采购申请ID',
    action_type VARCHAR(32) NOT NULL COMMENT '动作 SUBMIT/APPROVE/REJECT/WITHDRAW/RESUBMIT',
    from_status VARCHAR(32) DEFAULT NULL COMMENT '变更前状态',
    to_status VARCHAR(32) NOT NULL COMMENT '变更后状态',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) NOT NULL COMMENT '操作人姓名快照',
    action_remark VARCHAR(255) DEFAULT NULL COMMENT '操作说明',
    action_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_purchase_request_approval_request_id (request_id),
    KEY idx_purchase_request_approval_operator_id (operator_id),
    KEY idx_purchase_request_approval_action_type (action_type),
    KEY idx_purchase_request_approval_action_time (action_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购申请审批历史表';

CREATE TABLE purchase_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '采购订单号',
    request_id BIGINT NOT NULL COMMENT '来源申请ID',
    supplier_id BIGINT NOT NULL COMMENT '供应商ID',
    purchaser_id BIGINT NOT NULL COMMENT '采购员ID',
    planned_delivery_date DATE NOT NULL COMMENT '计划交期',
    supplier_expected_delivery_date DATE DEFAULT NULL COMMENT '供应商反馈交期',
    supplier_confirm_time DATETIME DEFAULT NULL COMMENT '供应商确认时间',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    status VARCHAR(32) NOT NULL DEFAULT 'WAIT_CONFIRM' COMMENT '状态 WAIT_CONFIRM/IN_PROGRESS/PARTIAL_ARRIVAL/COMPLETED/CLOSED/CANCELLED',
    supplier_feedback_remark VARCHAR(255) DEFAULT NULL COMMENT '供应商反馈说明',
    close_time DATETIME DEFAULT NULL COMMENT '关闭时间',
    close_reason VARCHAR(255) DEFAULT NULL COMMENT '关闭原因',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_purchase_order_order_no (order_no),
    UNIQUE KEY uk_purchase_order_request_id (request_id),
    KEY idx_purchase_order_supplier_id (supplier_id),
    KEY idx_purchase_order_purchaser_id (purchaser_id),
    KEY idx_purchase_order_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单主表';

CREATE TABLE purchase_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '采购订单ID',
    request_item_id BIGINT NOT NULL COMMENT '来源采购申请明细ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    material_code VARCHAR(64) NOT NULL COMMENT '物料编码快照',
    material_name VARCHAR(128) NOT NULL COMMENT '物料名称快照',
    specification VARCHAR(128) DEFAULT NULL COMMENT '规格型号快照',
    unit VARCHAR(32) NOT NULL COMMENT '单位快照',
    order_qty DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '订单数量',
    unit_price DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    line_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '行金额',
    arrived_qty DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '累计到货数量',
    inbound_qty DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '累计入库数量',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    KEY idx_purchase_order_item_order_id (order_id),
    KEY idx_purchase_order_item_request_item_id (request_item_id),
    KEY idx_purchase_order_item_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单明细表';

CREATE TABLE arrival_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    arrival_no VARCHAR(64) NOT NULL COMMENT '到货单号',
    order_id BIGINT NOT NULL COMMENT '采购订单ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    arrival_date DATE NOT NULL COMMENT '到货日期',
    total_arrival_qty DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '总到货数量',
    total_qualified_qty DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '总合格数量',
    total_unqualified_qty DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '总不合格数量',
    status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态 NORMAL/ABNORMAL',
    abnormal_remark VARCHAR(255) DEFAULT NULL COMMENT '异常说明',
    operator_id BIGINT NOT NULL COMMENT '登记人ID',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_arrival_record_arrival_no (arrival_no),
    KEY idx_arrival_record_order_id (order_id),
    KEY idx_arrival_record_warehouse_id (warehouse_id),
    KEY idx_arrival_record_status (status),
    KEY idx_arrival_record_operator_id (operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='到货登记主表';

CREATE TABLE arrival_record_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    arrival_id BIGINT NOT NULL COMMENT '到货主表ID',
    order_item_id BIGINT NOT NULL COMMENT '采购订单明细ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    material_code VARCHAR(64) NOT NULL COMMENT '物料编码快照',
    material_name VARCHAR(128) NOT NULL COMMENT '物料名称快照',
    specification VARCHAR(128) DEFAULT NULL COMMENT '规格型号快照',
    unit VARCHAR(32) NOT NULL COMMENT '单位快照',
    arrival_qty DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '到货数量',
    qualified_qty DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '合格数量',
    unqualified_qty DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '不合格数量',
    abnormal_remark VARCHAR(255) DEFAULT NULL COMMENT '异常说明',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    KEY idx_arrival_record_item_arrival_id (arrival_id),
    KEY idx_arrival_record_item_order_item_id (order_item_id),
    KEY idx_arrival_record_item_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='到货登记明细表';

CREATE TABLE inbound_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    inbound_no VARCHAR(64) NOT NULL COMMENT '入库单号',
    arrival_id BIGINT NOT NULL COMMENT '来源到货ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    total_inbound_qty DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '总入库数量',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/COMPLETED/CANCELLED/ABNORMAL',
    operator_id BIGINT NOT NULL COMMENT '入库人ID',
    inbound_time DATETIME DEFAULT NULL COMMENT '确认入库时间',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_inbound_order_inbound_no (inbound_no),
    KEY idx_inbound_order_arrival_id (arrival_id),
    KEY idx_inbound_order_warehouse_id (warehouse_id),
    KEY idx_inbound_order_status (status),
    KEY idx_inbound_order_operator_id (operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入库单主表';

CREATE TABLE inbound_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    inbound_id BIGINT NOT NULL COMMENT '入库主表ID',
    arrival_item_id BIGINT NOT NULL COMMENT '来源到货明细ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    material_code VARCHAR(64) NOT NULL COMMENT '物料编码快照',
    material_name VARCHAR(128) NOT NULL COMMENT '物料名称快照',
    specification VARCHAR(128) DEFAULT NULL COMMENT '规格型号快照',
    unit VARCHAR(32) NOT NULL COMMENT '单位快照',
    inbound_qty DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '入库数量',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    KEY idx_inbound_order_item_inbound_id (inbound_id),
    KEY idx_inbound_order_item_arrival_item_id (arrival_item_id),
    KEY idx_inbound_order_item_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入库单明细表';

CREATE TABLE inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    current_qty DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '当前库存',
    last_inbound_time DATETIME DEFAULT NULL COMMENT '最后入库时间',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '修改人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_inventory_material_warehouse (material_id, warehouse_id),
    KEY idx_inventory_material_id (material_id),
    KEY idx_inventory_warehouse_id (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存台账表';

CREATE TABLE inventory_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    flow_no VARCHAR(64) NOT NULL COMMENT '库存流水号',
    inventory_id BIGINT NOT NULL COMMENT '库存台账ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型 INBOUND/INIT',
    biz_id BIGINT NOT NULL COMMENT '业务主键ID',
    before_qty DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '变更前数量',
    change_qty DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '变更数量',
    after_qty DECIMAL(18,3) NOT NULL DEFAULT 0.000 COMMENT '变更后数量',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名快照',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    operate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    UNIQUE KEY uk_inventory_flow_flow_no (flow_no),
    KEY idx_inventory_flow_inventory_id (inventory_id),
    KEY idx_inventory_flow_material_id (material_id),
    KEY idx_inventory_flow_warehouse_id (warehouse_id),
    KEY idx_inventory_flow_biz_type_biz_id (biz_type, biz_id),
    KEY idx_inventory_flow_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存流水表';

CREATE TABLE sys_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    receiver_id BIGINT NOT NULL COMMENT '接收人ID',
    sender_id BIGINT DEFAULT NULL COMMENT '发送人ID',
    title VARCHAR(128) NOT NULL COMMENT '消息标题',
    content VARCHAR(500) NOT NULL COMMENT '消息内容',
    message_type VARCHAR(32) NOT NULL COMMENT '消息类型',
    biz_type VARCHAR(32) DEFAULT NULL COMMENT '业务类型',
    biz_id BIGINT DEFAULT NULL COMMENT '业务主键ID',
    read_status VARCHAR(20) NOT NULL DEFAULT 'UNREAD' COMMENT '读取状态 UNREAD/READ',
    read_time DATETIME DEFAULT NULL COMMENT '读取时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    KEY idx_sys_message_receiver_id (receiver_id),
    KEY idx_sys_message_message_type (message_type),
    KEY idx_sys_message_read_status (read_status),
    KEY idx_sys_message_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统消息表';

CREATE TABLE sys_oper_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    log_type VARCHAR(20) NOT NULL COMMENT '日志类型 LOGIN/LOGOUT/BUSINESS',
    module_name VARCHAR(64) NOT NULL COMMENT '模块名称',
    biz_type VARCHAR(32) DEFAULT NULL COMMENT '业务类型',
    biz_id BIGINT DEFAULT NULL COMMENT '业务主键ID',
    operation_type VARCHAR(32) NOT NULL COMMENT '操作类型',
    operation_desc VARCHAR(255) DEFAULT NULL COMMENT '操作描述',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名快照',
    request_uri VARCHAR(255) DEFAULT NULL COMMENT '请求路径',
    request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方式',
    ip_address VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
    success_flag TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否成功',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    operate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_sys_oper_log_log_type (log_type),
    KEY idx_sys_oper_log_module_name (module_name),
    KEY idx_sys_oper_log_biz_type_biz_id (biz_type, biz_id),
    KEY idx_sys_oper_log_operator_id (operator_id),
    KEY idx_sys_oper_log_operation_type (operation_type),
    KEY idx_sys_oper_log_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';
