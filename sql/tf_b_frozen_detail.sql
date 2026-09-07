-- =====================================================================
-- 智运宝 - 账户冻结明细记录表（tf_b_frozen_detail）
-- =====================================================================
-- 设计说明：
--   1. 记录账户中每一笔冻结款项的完整生命周期（冻结 -> 解冻/打款），
--      支撑账户页「冻结金额明细」列表、冻结总额统计与历史查询。
--   2. 与 tf_b_account 的关系：一张账户可有多条冻结明细；
--      tf_b_account.frozen_amount 应恒等于本表 status=1 的记录金额合计
--      （两条链路由业务代码在同一事务内维护）。
--   3. 状态流转（按业务类型）：
--      - 运费托管(biz_type=1)：运单发布冻结 -> status=1 冻结中；
--        运单完成/取消触发解冻 -> status=2 已解冻（退回可用余额），
--        并记录 finish_time=解冻时间；
--      - 提现冻结(biz_type=2)：提现申请 -> status=1 处理中（不可动支）；
--        打款成功 -> status=3 已打款（资金转出，不退回可用余额）。
--   4. 命名与审计字段风格与 tf_b_account / tf_b_order 保持一致
--      （id/user_id/delete_flag/create_time/create_name/update_time/update_name）。
-- =====================================================================

-- 库（按需执行，库名/字符集按实际环境调整）
-- CREATE DATABASE IF NOT EXISTS `monkey_ams`
--   DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
-- USE `monkey_ams`;

-- 建表前清理（生产环境请谨慎，建议由变更脚本管理）
DROP TABLE IF EXISTS `tf_b_frozen_detail`;

CREATE TABLE `tf_b_frozen_detail` (
      `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
      `user_id`      VARCHAR(64)  NOT NULL                COMMENT '账户所属用户ID（tf_b_account.user_id）',
      `user_name`    VARCHAR(64)  DEFAULT NULL            COMMENT '用户名称（冗余，便于直接展示）',
      `biz_type`     TINYINT      DEFAULT 1      COMMENT '冻结业务类型：1运费托管 2提现冻结',
      `biz_type_name` VARCHAR(32) DEFAULT NULL            COMMENT '业务类型名称（冗余）：运费托管/提现冻结',
      `ref_id`       VARCHAR(64)  DEFAULT NULL                COMMENT '关联业务ID（运单ID/提现申请ID）',
      `order_no`     VARCHAR(64)  DEFAULT NULL                COMMENT '关联单号（YD运单号/TX提现单号）',
      `amount`       DECIMAL(16,2) DEFAULT NULL               COMMENT '冻结金额（元），须大于0',
      `frozen_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '冻结时间',
      `status`       TINYINT      DEFAULT 1      COMMENT '状态：1冻结中/处理中 2已解冻(退回可用余额) 3已打款(提现完成)',
      `status_desc`  VARCHAR(32)  DEFAULT NULL            COMMENT '状态描述（冗余）：冻结中/已解冻/已打款',
      `finish_time`  DATETIME     DEFAULT NULL            COMMENT '结束时间：运费托管解冻时间 或 提现打款时间',
      `remark`       VARCHAR(255) DEFAULT NULL            COMMENT '备注',
      `delete_flag`  TINYINT      DEFAULT 0      COMMENT '删除标记：0正常 1已删除',
      `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `create_name`  VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
      `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      `update_name`  VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
      PRIMARY KEY (`id`),
      KEY `idx_user_status` (`user_id`, `status`, `delete_flag`),
      KEY `idx_ref`         (`biz_type`, `ref_id`),
      KEY `idx_order_no`    (`order_no`),
      KEY `idx_frozen_time` (`frozen_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '账户冻结明细记录表';

-- =====================================================================
-- 示例数据（可按需执行）
-- 与账户页联调示例对应：运费托管冻结 2 笔 + 提现冻结 1 笔，合计 4100.50
-- =====================================================================
INSERT INTO `tf_b_frozen_detail`
(`user_id`, `user_name`, `biz_type`, `biz_type_name`, `ref_id`, `order_no`,
 `amount`, `frozen_time`, `status`, `status_desc`, `finish_time`, `remark`, `create_name`)
VALUES
    ('U001', '张三', 1, '运费托管', '1001', 'YD20260902001', 1800.00, '2026-09-02 09:40:00', 1, '冻结中', NULL, '运单运输中，运费托管', 'gkk'),
    ('U001', '张三', 1, '运费托管', '1002', 'YD20260902002', 2000.00, '2026-09-02 08:26:00', 1, '冻结中', NULL, '运单运输中，运费托管', 'gkk'),
    ('U001', '张三', 2, '提现冻结', 'W2001', 'TX20260902001', 300.50, '2026-09-02 15:10:00', 1, '冻结中', NULL, '提现处理中', 'gkk');

-- =====================================================================
-- 常用查询示例
-- 1) 某用户当前冻结总额（应等于 tf_b_account.frozen_amount）
--    SELECT IFNULL(SUM(amount), 0) AS frozen_total
--    FROM tf_b_frozen_detail
--    WHERE user_id = 'U001' AND status = 1 AND delete_flag = 0;
--
-- 2) 某用户冻结明细列表（账户页展示，按冻结时间倒序）
--    SELECT id, biz_type_name AS biz_type, order_no AS ref_no, amount,
--           DATE_FORMAT(frozen_time, '%Y-%m-%d %H:%i') AS frozen_time,
--           status_desc AS status
--    FROM tf_b_frozen_detail
--    WHERE user_id = 'U001' AND delete_flag = 0
--    ORDER BY frozen_time DESC;
--
-- 3) 解冻（运费托管完成）示例：单条明细置为已解冻 + 同步账户余额
--    UPDATE tf_b_frozen_detail
--    SET status = 2, status_desc = '已解冻', finish_time = NOW(), update_time = NOW()
--    WHERE ref_id = '1001' AND biz_type = 1 AND status = 1 AND delete_flag = 0;
--
--    -- 注意：须与 account 模块解冻（frozen_amount 减、available_amount 加）在同一个事务内执行，
--    -- 或由 AccountUnfrozenTransportMoneyConsumer 通过 MQ 异步完成，保证两表一致。
-- =====================================================================
