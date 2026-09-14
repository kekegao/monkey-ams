-- =====================================================================
-- 智运宝 - 承运方清算工单表（tf_b_settlement_carrier_work_order）
-- =====================================================================
-- 设计说明：
--   1. 业务触发点：货主发起「结算申请」、承运方清算单（tf_b_settlement_carrier）落库后，
--      系统以「工单」形式记录本条清算消息（延迟 30S）投递 MQ 并驱动承运方打款清算的全过程：
--      待投递 -> 已投递 -> 处理中 -> 处理成功 / 处理失败（可重试）/ 已作废；
--      与 SettlementServiceImpl#applySettlement 中「承运方清算工单生成后，在这里做承运方清算功能，
--      走延迟队列，延迟30S」的落地位置一一对应。
--   2. 与 tf_b_settlement_carrier 的关系：一张承运方清算单对应一张清算工单（1:1）。
--      工单冗余清算金额与双方快照，消费端可免回查、独立执行清算；
--      唯一索引 uk_settlement_no 兜底，防止重复建单。
--   3. 幂等性设计（三层）：
--      (1) 建单幂等：uk_idempotent_key（幂等键 = CARRIER_CLEARING:{orderId}）唯一约束，
--          同一运单的重复 / 并发申请只会落一条工单，重复插入抛 DuplicateKey 后返回既有工单；
--      (2) 投递幂等：uk_message_id（MQ 消息唯一ID）唯一约束，同一条清算消息不会被重复投递入库；
--      (3) 消费幂等：消费端以「状态机 CAS」推进
--              UPDATE ... SET status = 3 WHERE work_order_no = ? AND status = 2 AND delete_flag = 0
--          受影响行数为 0 即判定重复消费，直接 ACK 跳过，保证打款清算只执行一次。
--   4. 重试策略：消费失败置 status=5、retry_count 自增、next_retry_time 记录下次重试时间，
--      由定时任务 / 延迟队列按 next_retry_time 重投；retry_count >= max_retry_count 时不再自动重试并告警，
--      需人工介入后置为已作废或补偿。
--   5. 职责边界：清算工单只负责「消息投递与执行调度」的可观测与重试，
--      实际资金动作仍由清算单 + 账户域在同一事务内完成（见 tf_b_settlement_carrier 设计说明），
--      工单不参与金额计算，仅记录执行结果。
--   6. 命名与审计字段风格与 tf_b_settlement_shipper / tf_b_settlement_carrier 保持一致。
-- =====================================================================

-- 库（按需执行，库名/字符集按实际环境调整）
-- CREATE DATABASE IF NOT EXISTS `monkey_ams`
--   DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
-- USE `monkey_ams`;

-- 建表前清理（生产环境请谨慎，建议由变更脚本管理）
DROP TABLE IF EXISTS `tf_b_settlement_carrier_work_order`;

CREATE TABLE `tf_b_settlement_carrier_work_order` (
      `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
      `work_order_no`       VARCHAR(32)   DEFAULT NULL            COMMENT '清算工单号（CJSG+流水：yyyyMMddHHmmss+序号）',
      `settlement_no`       VARCHAR(32)   DEFAULT NULL            COMMENT '承运方清算单号（tf_b_settlement_carrier.settlement_no）',
      `order_id`            VARCHAR(64)   DEFAULT NULL            COMMENT '运单号（tf_b_order.order_id）',
      `idempotent_key`      VARCHAR(128)  DEFAULT NULL            COMMENT '业务幂等键（CARRIER_CLEARING:{orderId}），建单幂等唯一约束',
      `shipper_user_id`     VARCHAR(64)   DEFAULT NULL            COMMENT '货主用户ID（冗余，便于双方核对同一运单）',
      `shipper_name`        VARCHAR(128)  DEFAULT NULL            COMMENT '货主名称/企业名称（冗余）',
      `shipper_mobile`      VARCHAR(20)   DEFAULT NULL            COMMENT '货主手机号（冗余）',
      `carrier_user_id`     VARCHAR(64)   DEFAULT NULL            COMMENT '承运方用户ID（冗余，清算归属方）',
      `carrier_user_name`   VARCHAR(64)   DEFAULT NULL            COMMENT '承运方用户名称（冗余，便于直接展示）',
      `carrier_name`        VARCHAR(128)  DEFAULT NULL            COMMENT '承运方名称/车队名称（冗余）',
      `carrier_mobile`      VARCHAR(20)   DEFAULT NULL            COMMENT '承运方手机号（冗余）',
      `transport_money`     DECIMAL(16,2) DEFAULT 0.00            COMMENT '应收运费金额（tf_b_order.transport_money）',
      `service_fee`         DECIMAL(16,2) DEFAULT 0.00            COMMENT '承运方承担的平台服务费（佣金，从运费内扣）',
      `settle_amount`       DECIMAL(16,2) DEFAULT 0.00            COMMENT '实收金额=运费+其他费用-服务费-税费-扣款（清算金额快照）',
      `receiver_account_name` VARCHAR(128) DEFAULT NULL           COMMENT '收款户名（快照，建议加密/脱敏存储）',
      `receiver_bank_name`  VARCHAR(128)  DEFAULT NULL            COMMENT '收款银行（快照）',
      `receiver_bank_card_no` VARCHAR(64) DEFAULT NULL            COMMENT '收款账号（快照，建议仅存前4后4）',
      `exchange_name`       VARCHAR(128)  DEFAULT NULL            COMMENT 'MQ 交换机（monkey.business.exchange）',
      `routing_key`         VARCHAR(128)  DEFAULT NULL            COMMENT 'MQ 路由键（business.settlement.carrier.clearing）',
      `queue_name`          VARCHAR(128)  DEFAULT NULL            COMMENT 'MQ 队列名（settlement_carrier_clearing_queue）',
      `message_id`          VARCHAR(64)   DEFAULT NULL            COMMENT 'MQ 消息唯一ID（工单创建时预生成，投递/消费统一幂等标识）',
      `message_type`        VARCHAR(64)   DEFAULT NULL            COMMENT '消息类型（SETTLEMENT_CARRIER_CLEARING）',
      `business_id`         VARCHAR(64)   DEFAULT NULL            COMMENT 'MQ 业务ID（RabbitMessage.businessId，取运单号）',
      `payload`             TEXT          DEFAULT NULL            COMMENT '消息体快照（JSON，便于重放与排障）',
      `delay_seconds`       INT           DEFAULT 30              COMMENT '延迟投递秒数（走延迟队列，默认 30S）',
      `status`              TINYINT       DEFAULT 1               COMMENT '工单状态：1待投递 2已投递 3处理中 4处理成功 5处理失败 6已作废',
      `status_desc`         VARCHAR(32)   DEFAULT NULL            COMMENT '状态描述（冗余）：待投递/已投递/处理中/处理成功/处理失败/已作废',
      `retry_count`         INT           DEFAULT 0               COMMENT '已重试次数',
      `max_retry_count`     INT           DEFAULT 5               COMMENT '最大重试次数（超出后不再自动重试并告警）',
      `send_time`           DATETIME      DEFAULT NULL            COMMENT '消息投递时间（首次发送到 MQ 的时间）',
      `consume_time`        DATETIME      DEFAULT NULL            COMMENT '消息消费时间（消费端开始处理时间）',
      `finish_time`         DATETIME      DEFAULT NULL            COMMENT '处理完成时间（成功或最终失败）',
      `next_retry_time`     DATETIME      DEFAULT NULL            COMMENT '下次重试时间（重试调度扫描依据）',
      `error_msg`           VARCHAR(512)  DEFAULT NULL            COMMENT '最近一次失败原因',
      `remark`              VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
      `delete_flag`         TINYINT       DEFAULT 0               COMMENT '删除标记：0正常 1已删除',
      `create_time`         DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `create_name`         VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
      `update_time`         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      `update_name`         VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_work_order_no`  (`work_order_no`),
      UNIQUE KEY `uk_settlement_no`  (`settlement_no`),
      UNIQUE KEY `uk_idempotent_key` (`idempotent_key`),
      UNIQUE KEY `uk_message_id`     (`message_id`),
      KEY `idx_order_id`     (`order_id`),
      KEY `idx_user_status`  (`carrier_user_id`, `status`, `delete_flag`),
      KEY `idx_status_retry` (`status`, `next_retry_time`, `delete_flag`),
      KEY `idx_send_time`    (`send_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '承运方清算MQ工单记录表';

-- =====================================================================
-- 示例数据（可按需执行）
-- 与 tf_b_settlement_carrier 示例保持一致：
--   CJS202609120001（YD20260902001，已结算）-> 工单处理成功
--   CJS202609120002（YD20260902002，待结算）-> 工单已投递，等待消费
-- 注：uk_message_id 唯一索引不约束多行 NULL，message_id 允许在建单时先置空、投递时回填。
-- =====================================================================
INSERT INTO `tf_b_settlement_carrier_work_order`
(`work_order_no`, `settlement_no`, `order_id`, `idempotent_key`,
 `shipper_user_id`, `shipper_name`, `shipper_mobile`,
 `carrier_user_id`, `carrier_user_name`, `carrier_name`, `carrier_mobile`,
 `transport_money`, `service_fee`, `settle_amount`,
 `receiver_account_name`, `receiver_bank_name`, `receiver_bank_card_no`,
 `exchange_name`, `routing_key`, `queue_name`, `message_id`, `message_type`, `business_id`, `payload`, `delay_seconds`,
 `status`, `status_desc`, `retry_count`, `max_retry_count`,
 `send_time`, `consume_time`, `finish_time`, `next_retry_time`, `error_msg`, `remark`, `create_name`)
VALUES
    ('CJSG202609120001', 'CJS202609120001', 'YD20260902001', 'CARRIER_CLEARING:YD20260902001',
     'U001', '张三物流部', '13800000001',
     'U002', '李四', '李四运输队', '13900000002',
     1800.00, 90.00, 1710.00,
     '李四', '中国工商银行', '6222****6688',
     'monkey.business.exchange', 'business.settlement.carrier.clearing', 'settlement_carrier_clearing_queue',
     '9f2c1e6a-4b7d-4f1e-8a33-2c5d7b91c001', 'SETTLEMENT_CARRIER_CLEARING', 'YD20260902001',
     '{"orderId":"YD20260902001","settlementNo":"CJS202609120001","settleAmount":1710.00}', 30,
     4, '处理成功', 0, 5,
     '2026-09-12 10:00:00', '2026-09-12 10:00:30', '2026-09-12 10:05:00', NULL, NULL, '延迟30S投递，承运方实收运费已入账', 'gkk'),
    ('CJSG202609120002', 'CJS202609120002', 'YD20260902002', 'CARRIER_CLEARING:YD20260902002',
     'U001', '张三物流部', '13800000001',
     'U002', '李四', '李四运输队', '13900000002',
     2000.00, 100.00, 1900.00,
     '李四', '中国工商银行', '6222****6688',
     'monkey.business.exchange', 'business.settlement.carrier.clearing', 'settlement_carrier_clearing_queue',
     '9f2c1e6a-4b7d-4f1e-8a33-2c5d7b91c002', 'SETTLEMENT_CARRIER_CLEARING', 'YD20260902002',
     '{"orderId":"YD20260902002","settlementNo":"CJS202609120002","settleAmount":1900.00}', 30,
     2, '已投递', 0, 5,
     '2026-09-12 11:20:00', NULL, NULL, NULL, NULL, '延迟30S投递，等待消费端执行打款清算', 'gkk');

-- =====================================================================
-- 常用查询示例
-- 1) 待投递 / 待重试工单扫描（定时任务按 next_retry_time 正序补偿投递）
--    SELECT id, work_order_no, settlement_no, order_id, retry_count,
--           routing_key, message_id, next_retry_time
--    FROM tf_b_settlement_carrier_work_order
--    WHERE delete_flag = 0
--      AND ((status = 1) OR (status = 5 AND retry_count < max_retry_count
--           AND (next_retry_time IS NULL OR next_retry_time <= NOW())))
--    ORDER BY next_retry_time ASC;
--
-- 2) 建单幂等命中（重复申请时返回既有工单，不重复建单）
--    SELECT work_order_no, settlement_no, status, status_desc
--    FROM tf_b_settlement_carrier_work_order
--    WHERE idempotent_key = 'CARRIER_CLEARING:YD20260902001' AND delete_flag = 0;
--
-- 3) 消费幂等 CAS：仅当工单处于「已投递(2)」时推进为「处理中(3)」，
--    受影响行数为 1 才继续执行打款清算，为 0 表示已被消费 / 状态不符，直接 ACK 跳过。
--    UPDATE tf_b_settlement_carrier_work_order
--    SET status = 3, status_desc = '处理中', consume_time = NOW(), update_time = NOW()
--    WHERE work_order_no = 'CJSG202609120002' AND status = 2 AND delete_flag = 0;
--
-- 4) 处理成功回写（须与承运方账户入账在同一事务内或由消费端保证最终一致）
--    UPDATE tf_b_settlement_carrier_work_order
--    SET status = 4, status_desc = '处理成功', finish_time = NOW(), error_msg = NULL, update_time = NOW()
--    WHERE work_order_no = 'CJSG202609120002' AND status = 3 AND delete_flag = 0;
--
-- 5) 处理失败回写并安排重试（retry_count 达上限时由告警转入人工处理）
--    UPDATE tf_b_settlement_carrier_work_order
--    SET status = 5, status_desc = '处理失败', retry_count = retry_count + 1,
--        error_msg = '承运方账户入账失败', next_retry_time = DATE_ADD(NOW(), INTERVAL 30 SECOND), update_time = NOW()
--    WHERE work_order_no = 'CJSG202609120002' AND status = 3 AND delete_flag = 0;
-- =====================================================================
