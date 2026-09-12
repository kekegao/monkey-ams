-- =====================================================================
-- 智运宝 - 承运方结算单（清算单）表（tf_b_settlement_carrier）
-- =====================================================================
-- 设计说明：
--   1. 业务触发点：货主发起「结算申请」后，系统在同一时点按运单为承运方生成
--      一张「应收清算单」，固化本次运输应收运费、平台佣金、其他扣款与实收口径；
--      「结算(8)」阶段平台打款后回写 status=3、pay_time。
--   2. 一单一结算：一个运单（tf_b_order.order_id）只允许生成一张承运方结算单，
--      由唯一索引 uk_order_id 兜底，防止重复申请 / MQ 重复投递造成重复打款。
--      注：除主键 id 外字段统一允许为空（配合 DEFAULT 约定），而 MySQL 唯一索引
--      不约束多行 NULL，故 order_id / settlement_no 非空须由业务层写入时保证。
--   3. 金额口径（单位：元）：
--        settle_amount(实收金额) = transport_money + other_amount
--                                  - service_fee - tax_amount - deductible_amount
--      其中：service_fee 为「承运方承担」的平台服务费（佣金，从运费中内扣），
--            deductible_amount 为违约金/货损等扣款，other_amount 为装卸/等待费等加项。
--   4. 发货保证金说明：承运方在「发货(4)」时冻结的发货保证金，
--      已于「回单确认(6)」时解冻退回账户，仅在本单展示核算（ship_deposit_amount），
--      不参与 settle_amount 计算。
--   5. 资金联动（与 tf_b_account / tf_b_frozen_detail 的一致性）：
--      - 结算申请：仅生成本单，不动资金；
--      - 结算完成：货主托管运费划扣（货主 frozen_amount 减），
--        同时按本单 settle_amount 增加承运方账户 available_amount，并回写 status=3 / pay_time；
--      - 每次账户变动须同步写 tf_b_frozen_detail 或结算流水，保证账实一致。
--   6. 安全提示：receiver_* 为收款账户快照，属敏感信息，
--      建议落库前脱敏或加密（如仅保留前4后4），展示层再按权限还原。
--   7. 命名与审计字段风格与 tf_b_account / tf_b_order / tf_b_frozen_detail 保持一致。
-- =====================================================================

-- 库（按需执行，库名/字符集按实际环境调整）
-- CREATE DATABASE IF NOT EXISTS `monkey_ams`
--   DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
-- USE `monkey_ams`;

-- 建表前清理（生产环境请谨慎，建议由变更脚本管理）
DROP TABLE IF EXISTS `tf_b_settlement_carrier`;

CREATE TABLE `tf_b_settlement_carrier` (
      `id`                     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
      `settlement_no`          VARCHAR(32)   DEFAULT NULL            COMMENT '承运方结算单号（CJS+23位流水：yyyyMMddHHmmssSSS+6位随机）',
      `order_id`               VARCHAR(64)   DEFAULT NULL            COMMENT '运单号（tf_b_order.order_id）',
      `shipper_user_id`        VARCHAR(64)   DEFAULT NULL            COMMENT '货主用户ID（冗余，便于双方核对同一运单）',
      `shipper_name`           VARCHAR(128)  DEFAULT NULL            COMMENT '货主名称/企业名称（冗余）',
      `shipper_mobile`         VARCHAR(20)   DEFAULT NULL            COMMENT '货主手机号（冗余）',
      `shipper_mobile`         VARCHAR(20)   DEFAULT NULL            COMMENT '货主手机号（冗余）',
      `carrier_user_id`        VARCHAR(64)   DEFAULT NULL            COMMENT '承运方用户ID（tf_b_order.carrier_user_id）',
      `carrier_user_name`      VARCHAR(64)   DEFAULT NULL            COMMENT '承运方用户名称（冗余，便于直接展示）',
      `carrier_name`           VARCHAR(128)  DEFAULT NULL            COMMENT '承运方名称/车队名称（冗余）',
      `carrier_mobile`         VARCHAR(20)   DEFAULT NULL            COMMENT '承运方手机号（冗余）',
      `transport_money`        DECIMAL(16,2) DEFAULT 0.00            COMMENT '应收运费金额（tf_b_order.transport_money）',
      `service_fee_rate`       DECIMAL(8,4)  DEFAULT 0.0000          COMMENT '承运方承担的服务费率（如 0.0500 表示 5%）',
      `service_fee`            DECIMAL(16,2) DEFAULT 0.00            COMMENT '承运方承担的平台服务费（佣金，从运费内扣）',
      `tax_amount`             DECIMAL(16,2) DEFAULT 0.00            COMMENT '税费（如代扣税费，默认 0）',
      `other_amount`           DECIMAL(16,2) DEFAULT 0.00            COMMENT '其他费用加项（装卸费/等待费等，正数加收）',
      `deductible_amount`      DECIMAL(16,2) DEFAULT 0.00            COMMENT '其他扣款（违约金/货损赔付等，正数扣减）',
      `settle_amount`          DECIMAL(16,2) DEFAULT 0.00            COMMENT '实收金额=运费+其他费用-服务费-税费-扣款',
      `ship_deposit_amount`    DECIMAL(16,2) DEFAULT 0.00            COMMENT '发货保证金（回单确认时已解冻退回，仅展示核算，不参与实收计算）',
      `receiver_account_name`  VARCHAR(128)  DEFAULT NULL            COMMENT '收款户名（快照，建议加密/脱敏存储）',
      `receiver_bank_name`     VARCHAR(128)  DEFAULT NULL            COMMENT '收款银行（快照）',
      `receiver_bank_card_no`  VARCHAR(64)   DEFAULT NULL            COMMENT '收款账号（快照，建议仅存前4后4）',
      `settle_batch_no`        VARCHAR(32)   DEFAULT NULL            COMMENT '结算批次号（平台批量结算时写入，可为空）',
      `status`                 TINYINT       DEFAULT 1               COMMENT '结算状态：1待结算 2结算中 3已结算 4已作废',
      `status_desc`            VARCHAR(32)   DEFAULT NULL            COMMENT '状态描述（冗余）：待结算/结算中/已结算/已作废',
      `invoice_status`         TINYINT       DEFAULT 0               COMMENT '开票状态：0未开票 1已开票（对应订单状态机 10 发票）',
      `apply_time`             DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '结算申请时间（与货主结算单同时生成）',
      `settle_time`            DATETIME      DEFAULT NULL            COMMENT '结算完成时间',
      `pay_time`               DATETIME      DEFAULT NULL            COMMENT '实际打款到承运方账户时间',
      `remark`                 VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
      `delete_flag`            TINYINT       DEFAULT 0               COMMENT '删除标记：0正常 1已删除',
      `create_time`            DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `create_name`            VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
      `update_time`            DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      `update_name`            VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_settlement_no` (`settlement_no`),
      UNIQUE KEY `uk_order_id`      (`order_id`),
      KEY `idx_user_status` (`carrier_user_id`, `status`, `delete_flag`),
      KEY `idx_status`      (`status`, `delete_flag`),
      KEY `idx_apply_time`  (`apply_time`),
      KEY `idx_batch`       (`settle_batch_no`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '承运方结算单（应收清算单）表';

-- =====================================================================
-- 示例数据（可按需执行）
-- 与 tf_b_settlement_shipper / tf_b_frozen_detail 示例运单保持一致：
-- YD20260902001（已结算）、YD20260902002（待结算），承运方 U002 李四运输队
-- =====================================================================
INSERT INTO `tf_b_settlement_carrier`
(`settlement_no`, `order_id`, `shipper_user_id`, `shipper_name`,
 `carrier_user_id`, `carrier_user_name`, `carrier_name`, `carrier_mobile`,
 `transport_money`, `service_fee_rate`, `service_fee`, `tax_amount`, `other_amount`, `deductible_amount`,
 `settle_amount`, `ship_deposit_amount`,
 `receiver_account_name`, `receiver_bank_name`, `receiver_bank_card_no`,
 `settle_batch_no`, `status`, `status_desc`, `invoice_status`, `apply_time`, `settle_time`, `pay_time`, `remark`, `create_name`)
VALUES
    ('CJS202609120001', 'YD20260902001', 'U001', '张三物流部',
     'U002', '李四', '李四运输队', '13900000002',
     1800.00, 0.0500, 90.00, 0.00, 0.00, 0.00,
     1710.00, 500.00,
     '李四', '中国工商银行', '6222****6688',
     'PC20260912', 3, '已结算', 0, '2026-09-12 10:00:00', '2026-09-12 10:05:00', '2026-09-12 10:05:00', '运费扣除 5% 平台服务费后已打款', 'gkk'),
    ('CJS202609120002', 'YD20260902002', 'U001', '张三物流部',
     'U002', '李四', '李四运输队', '13900000002',
     2000.00, 0.0500, 100.00, 0.00, 0.00, 0.00,
     1900.00, 500.00,
     '李四', '中国工商银行', '6222****6688',
     NULL, 1, '待结算', 0, '2026-09-12 11:20:00', NULL, NULL, '结算申请已提交，待平台打款', 'gkk');

-- =====================================================================
-- 常用查询示例
-- 1) 平台待打款队列（结算(8) 批处理扫描，按申请时间正序处理）
--    SELECT id, settlement_no, order_id, carrier_user_id, settle_amount,
--           receiver_account_name, receiver_bank_name, receiver_bank_card_no
--    FROM tf_b_settlement_carrier
--    WHERE status = 1 AND delete_flag = 0
--    ORDER BY apply_time ASC;
--
-- 2) 承运方「我的结算单」列表（按申请时间倒序）
--    SELECT settlement_no, order_id, transport_money, service_fee, settle_amount,
--           status_desc, DATE_FORMAT(apply_time, '%Y-%m-%d %H:%i') AS apply_time
--    FROM tf_b_settlement_carrier
--    WHERE carrier_user_id = 'U002' AND delete_flag = 0
--    ORDER BY apply_time DESC;
--
-- 3) 结算完成回写示例（须与货主托管运费扣划、承运方账户入账在同一事务内）
--    UPDATE tf_b_settlement_carrier
--    SET status = 3, status_desc = '已结算', settle_amount = 1710.00,
--        settle_time = NOW(), pay_time = NOW(), update_time = NOW()
--    WHERE settlement_no = 'CJS202609120001' AND status IN (1, 2) AND delete_flag = 0;
--
-- 4) 运单双侧结算金额一致性校验（货主实付应等于承运方应收+平台佣金口径）
--    SELECT s.order_id, s.paid_amount AS shipper_paid,
--           c.settle_amount AS carrier_received, c.service_fee AS platform_fee
--    FROM tf_b_settlement_shipper s
--    JOIN tf_b_settlement_carrier c ON c.order_id = s.order_id AND c.delete_flag = 0
--    WHERE s.delete_flag = 0
--      AND s.paid_amount <> (c.settle_amount + c.service_fee + c.tax_amount - c.other_amount + c.deductible_amount);
-- =====================================================================
