-- =====================================================================
-- 智运宝 - 货主结算单（清算单）表（tf_b_settlement_shipper）
-- =====================================================================
-- 设计说明：
--   1. 业务触发点：运单「回单确认(6)」后货主点击「结算申请」，
--      订单流转为 结算申请(7)，系统按运单为货主生成一张「应付清算单」，
--      固化本次运输运费、服务费、优惠等应付口径；
--      「结算(8)」阶段平台完成扣划后回写 status=3、paid_amount、settle_time。
--   2. 一单一结算：一个运单（tf_b_order.order_id）只允许生成一张货主结算单，
--      由唯一索引 uk_order_id 兜底，防止前端重复点击 / MQ 重复投递造成重复记账。
--      注：除主键 id 外字段统一允许为空（配合 DEFAULT 约定），而 MySQL 唯一索引
--      不约束多行 NULL，故 order_id / settlement_no 非空须由业务层写入时保证。
--   3. 金额口径（单位：元）：
--        payable_amount(应付总额) = transport_money + service_fee - discount_amount + other_amount
--        frozen_amount (已托管)   = tf_b_frozen_detail 中该运单 biz_type=1(运费托管)status=1 的金额合计
--        refund_amount (退还货主) = frozen_amount - paid_amount
--      注：service_fee 为「货主承担」的平台服务费，默认 0；
--          若业务启用，须在发布冻结时连同运费一并托管（frozen_amount 需覆盖 payable_amount）。
--   4. 资金联动（与 tf_b_account / tf_b_frozen_detail 的一致性）：
--      - 结算申请：仅生成本单，不动资金，托管运费保持冻结；
--      - 结算完成：货主账户 frozen_amount 减、承运方账户 available_amount 加（金额=承运方结算单 settle_amount），
--        同时将 tf_b_frozen_detail 对应记录置为已结束，并回写本单 status=3 / settle_time；
--      - 任何时刻须保证「tf_b_account.frozen_amount = tf_b_frozen_detail status=1 合计」成立，
--        建议扣划与解冻在同一个事务内，或统一走结算流水记账。
--   5. 对账(9)/发票(10) 由后续环节处理：本表 invoice_status 仅标记是否已开票，
--      如需对账批次明细，可另建对账单表按 settle_batch_no 关联。
--   6. 命名与审计字段风格与 tf_b_account / tf_b_order / tf_b_frozen_detail 保持一致。
-- =====================================================================

-- 库（按需执行，库名/字符集按实际环境调整）
-- CREATE DATABASE IF NOT EXISTS `monkey_ams`
--   DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
-- USE `monkey_ams`;

-- 建表前清理（生产环境请谨慎，建议由变更脚本管理）
DROP TABLE IF EXISTS `tf_b_settlement_shipper`;

CREATE TABLE `tf_b_settlement_shipper` (
      `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
      `settlement_no`     VARCHAR(32)   DEFAULT NULL            COMMENT '货主结算单号（HJS+23位流水：yyyyMMddHHmmssSSS+6位随机）',
      `order_id`          VARCHAR(64)   DEFAULT NULL            COMMENT '运单号（tf_b_order.order_id）',
      `shipper_user_id`   VARCHAR(64)   DEFAULT NULL            COMMENT '货主用户ID（tf_b_order.shipper_user_id）',
      `shipper_user_name` VARCHAR(64)   DEFAULT NULL            COMMENT '货主用户名称（冗余，便于直接展示）',
      `shipper_name`      VARCHAR(128)  DEFAULT NULL            COMMENT '货主名称/企业名称（冗余）',
      `shipper_mobile`    VARCHAR(20)   DEFAULT NULL            COMMENT '货主手机号（冗余）',
      `carrier_user_id`   VARCHAR(64)   DEFAULT NULL            COMMENT '承运方用户ID（冗余，便于双方核对同一运单）',
      `carrier_mobile`    VARCHAR(20)   DEFAULT NULL            COMMENT '承运方手机号（冗余）',
      `carrier_name`      VARCHAR(128)  DEFAULT NULL            COMMENT '承运方名称（冗余）',
      `transport_money`   DECIMAL(16,2) DEFAULT 0.00            COMMENT '订单运费金额（tf_b_order.transport_money）',
      `service_fee_rate`  DECIMAL(8,4)  DEFAULT 0.0000          COMMENT '货主承担的服务费率（如 0.0500 表示 5%），默认 0',
      `service_fee`       DECIMAL(16,2) DEFAULT 0.00            COMMENT '货主承担的平台服务费',
      `discount_amount`   DECIMAL(16,2) DEFAULT 0.00            COMMENT '优惠/减免金额',
      `other_amount`      DECIMAL(16,2) DEFAULT 0.00            COMMENT '其他费用（正数加收、负数减免）',
      `payable_amount`    DECIMAL(16,2) DEFAULT 0.00            COMMENT '应付总额=运费+服务费-优惠+其他',
      `frozen_no`         VARCHAR(64)   DEFAULT NULL            COMMENT '托管冻结流水号（tf_b_frozen_detail.frozen_no，便于追溯扣划来源）',
      `frozen_amount`     DECIMAL(16,2) DEFAULT 0.00            COMMENT '已托管冻结金额（对应运费托管明细合计）',
      `paid_amount`       DECIMAL(16,2) DEFAULT 0.00            COMMENT '实付金额（结算(8)完成后由托管运费中扣划）',
      `refund_amount`     DECIMAL(16,2) DEFAULT 0.00            COMMENT '退还货主金额=托管金额-实付金额',
      `settle_batch_no`   VARCHAR(32)   DEFAULT NULL            COMMENT '结算批次号（平台批量结算时写入，可为空）',
      `status`            TINYINT       DEFAULT 1               COMMENT '结算状态：1待结算 2结算中 3已结算 4已作废',
      `status_desc`       VARCHAR(32)   DEFAULT NULL            COMMENT '状态描述（冗余）：待结算/结算中/已结算/已作废',
      `invoice_status`    TINYINT       DEFAULT 0               COMMENT '开票状态：0未开票 1已开票（对应订单状态机 10 发票）',
      `apply_time`        DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '结算申请时间',
      `settle_time`       DATETIME      DEFAULT NULL            COMMENT '结算完成（扣划）时间',
      `remark`            VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
      `delete_flag`       TINYINT       DEFAULT 0               COMMENT '删除标记：0正常 1已删除',
      `create_time`       DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `create_name`       VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
      `update_time`       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      `update_name`       VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_settlement_no` (`settlement_no`),
      UNIQUE KEY `uk_order_id`      (`order_id`),
      KEY `idx_user_status` (`shipper_user_id`, `status`, `delete_flag`),
      KEY `idx_status`      (`status`, `delete_flag`),
      KEY `idx_apply_time`  (`apply_time`),
      KEY `idx_batch`       (`settle_batch_no`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '货主结算单（应付清算单）表';

-- =====================================================================
-- 示例数据（可按需执行）
-- 与 tf_b_frozen_detail 示例运单保持一致：YD20260902001 / YD20260902002（货主 U001 张三）
-- =====================================================================
INSERT INTO `tf_b_settlement_shipper`
(`settlement_no`, `order_id`, `shipper_user_id`, `shipper_user_name`, `shipper_name`, `shipper_mobile`,
 `carrier_user_id`, `carrier_name`, `transport_money`, `service_fee_rate`, `service_fee`,
 `discount_amount`, `other_amount`, `payable_amount`,
 `frozen_no`, `frozen_amount`, `paid_amount`, `refund_amount`,
 `settle_batch_no`, `status`, `status_desc`, `invoice_status`, `apply_time`, `settle_time`, `remark`, `create_name`)
VALUES
    ('HJS202609120001', 'YD20260902001', 'U001', '张三', '张三物流部', '13800000001',
     'U002', '李四运输队', 1800.00, 0.0000, 0.00,
     0.00, 0.00, 1800.00,
     'FZ20260902001', 1800.00, 1800.00, 0.00,
     'PC20260912', 3, '已结算', 0, '2026-09-12 10:00:00', '2026-09-12 10:05:00', '回单确认后货主发起结算申请，已按托管运费扣划', 'gkk'),
    ('HJS202609120002', 'YD20260902002', 'U001', '张三', '张三物流部', '13800000001',
     'U002', '李四运输队', 2000.00, 0.0000, 0.00,
     0.00, 0.00, 2000.00,
     'FZ20260902002', 2000.00, 0.00, 0.00,
     NULL, 1, '待结算', 0, '2026-09-12 11:20:00', NULL, '结算申请已提交，待平台结算', 'gkk');

-- =====================================================================
-- 常用查询示例
-- 1) 平台待结算队列（结算(8) 批处理扫描，按申请时间正序处理）
--    SELECT id, settlement_no, order_id, shipper_user_id, frozen_no,
--           payable_amount, frozen_amount
--    FROM tf_b_settlement_shipper
--    WHERE status = 1 AND delete_flag = 0
--    ORDER BY apply_time ASC;
--
-- 2) 货主「我的结算单」列表（按申请时间倒序）
--    SELECT settlement_no, order_id, transport_money, payable_amount, paid_amount,
--           status_desc, DATE_FORMAT(apply_time, '%Y-%m-%d %H:%i') AS apply_time
--    FROM tf_b_settlement_shipper
--    WHERE shipper_user_id = 'U001' AND delete_flag = 0
--    ORDER BY apply_time DESC;
--
-- 3) 按运单核对货主/承运方两侧结算单
--    SELECT s.settlement_no AS shipper_no, s.payable_amount, s.status_desc AS shipper_status,
--           c.settlement_no AS carrier_no, c.settle_amount, c.status_desc AS carrier_status
--    FROM tf_b_settlement_shipper s
--    LEFT JOIN tf_b_settlement_carrier c
--           ON c.order_id = s.order_id AND c.delete_flag = 0
--    WHERE s.order_id = 'YD20260902001' AND s.delete_flag = 0;
--
-- 4) 结算完成回写示例（须与账户扣划、冻结明细结束在同一事务内）
--    UPDATE tf_b_settlement_shipper
--    SET status = 3, status_desc = '已结算', paid_amount = 1800.00, refund_amount = 0.00,
--        settle_time = NOW(), update_time = NOW()
--    WHERE settlement_no = 'HJS202609120001' AND status IN (1, 2) AND delete_flag = 0;
-- =====================================================================
