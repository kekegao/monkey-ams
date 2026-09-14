-- =====================================================================
-- 智运宝 - 账户收入支出流水表（tf_b_income_expense）
-- =====================================================================
-- 设计说明：
--   1. 定位：账户资金流水的「唯一财务口径」表，记录每一笔引起账户余额/可用/冻结变化的资金动作，
--      按「收入 / 支出 / 冻结 / 解冻」四向记账，支撑：
--        - 用户端「我的收支」「收支明细」列表与区间汇总；
--        - 平台公司对公账户的收支总览与平台服务费（毛利）统计；
--        - 与 tf_b_account 的余额对账（期初 + 收入 - 支出 = 期末）。
--   2. 当前业务场景与代码落点（account-bsm-biz）：
--        biz_type=1 充值                 -> AccountRechargeProtocolImpl#recharge（direction=1 收入，待接入流水）
--        biz_type=2 提现                 -> AccountWithdrawProtocolImpl（direction=3 冻结 -> direction=2 支出，待接入流水）
--        biz_type=3 运费托管             -> AccountProtocolImpl#frozenTransportMoneyAccount（direction=3 冻结）
--                                        -> AccountProtocolImpl#unfrozenTransportMoneyAccount（direction=4 解冻）
--        biz_type=4 发货保证金           -> frozenShipMoneyAccount / unfrozenShipMoneyAccount（direction=3 / 4）
--        biz_type=5 货主清算扣划         -> AccountProtocolImpl#settleShipperMoneyToCompanyAccount
--                                          货主侧 direction=2 支出（托管冻结被扣划）
--                                          平台侧 direction=1 收入（资金进入平台公司对公账户）
--        biz_type=6 承运方清算划账       -> AccountProtocolImpl#settleCarrierMoneyFromCompanyAccount
--                                          平台侧 direction=2 支出（平台公司对公账户出账）
--                                          承运方侧 direction=1 收入（承运方账户入账）
--        biz_type=7 人工调账             -> 运营后台差错处理（须成对记收支，并回填 related_flow_no）
--   3. 资金链路（与现有清算模型一致，对账只走平台账户，不再从货主直接扣给承运方）：
--        货主充值 -> 运费托管冻结 ->（货主清算）平台公司对公账户 ->（承运方对账）承运方账户 -> 提现
--      即：钱先由货主清算进入平台对公账户，再由平台划付承运方，平台赚取承运方服务费。
--   4. 记账口径：
--      - direction 1收入 / 2支出 影响 balance；3冻结 / 4解冻 只影响 available 与 frozen，不影响 balance；
--      - amount 恒为正数（方向由 direction 表达），金额单位：元；
--      - 每笔流水记录余额快照（balance/available/frozen 的 before、after），
--        用于对账 SQL：balance_after - balance_before = (direction=1 ? +amount : direction=2 ? -amount : 0)；
--      - 同一账户同一业务单号同方向只允许一笔流水（uk_biz_dedup 唯一索引兜底），
--        与账户侧「按清算单号幂等」的设计呼应，杜绝重复记账。
--   5. 幂等与一致性：
--      - uk_flow_no：流水号唯一（业务方预生成，写流水与改账户在同一事务内完成）；
--      - uk_biz_dedup(user_id, biz_type, biz_no, direction)：重复消费 / 重试时插入失败即视为已记账，
--        直接返回既有流水，资金动作不再重复执行；
--      - 跨账户划转（货主->平台、平台->承运方）须在一个事务内写「两条方向相反」的流水，
--        通过 biz_no + counterparty_user_id 双向配对，任一侧失败则整体回滚，绝不产生单边账。
--   6. 命名与审计字段风格与 tf_b_account / tf_b_frozen_detail / tf_b_settlement_* 保持一致。
-- =====================================================================

-- 库（按需执行，库名/字符集按实际环境调整）
-- CREATE DATABASE IF NOT EXISTS `monkey_ams`
--   DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
-- USE `monkey_ams`;

-- 建表前清理（生产环境请谨慎，建议由变更脚本管理）
DROP TABLE IF EXISTS `tf_b_income_expense`;

CREATE TABLE `tf_b_income_expense` (
      `id`                       BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
      `flow_no`                  VARCHAR(32)   DEFAULT NULL            COMMENT '收支流水号（SZ+yyyyMMddHHmmssSSS+6位随机）',
      `user_id`                  VARCHAR(64)   DEFAULT NULL            COMMENT '账户所属用户ID（tf_b_account.user_id）',
      `user_name`                VARCHAR(64)   DEFAULT NULL            COMMENT '用户名称（冗余，便于直接展示）',
      `account_type`             TINYINT       DEFAULT 1               COMMENT '账户类型：1用户账户 2平台公司对公账户（account.platform.company-user-id 配置的账户）',
      `direction`                TINYINT       DEFAULT 1               COMMENT '记账方向：1收入 2支出 3冻结 4解冻',
      `direction_desc`           VARCHAR(32)   DEFAULT NULL            COMMENT '方向描述（冗余）：收入/支出/冻结/解冻',
      `biz_type`                 TINYINT       DEFAULT 1               COMMENT '业务类型：1充值 2提现 3运费托管 4发货保证金 5货主清算扣划 6承运方清算划账 7人工调账',
      `biz_type_name`            VARCHAR(32)   DEFAULT NULL            COMMENT '业务类型名称（冗余）：充值/提现/运费托管/发货保证金/货主清算扣划/承运方清算划账/人工调账',
      `income_expense_type`      TINYINT       DEFAULT 0               COMMENT '收支科目：0不适用(冻结/解冻) 1运费收入 2运费支出 3平台服务费 4充值 5提现 6保证金 7人工调账 8其他',
      `income_expense_type_name` VARCHAR(32)   DEFAULT NULL            COMMENT '收支科目名称（冗余）：运费收入/运费支出/平台服务费/充值/提现/保证金/人工调账/其他',
      `amount`                   DECIMAL(16,2) DEFAULT 0.00            COMMENT '发生金额（元，恒为正数；方向由 direction 表达）',
      `balance_before`           DECIMAL(16,2) DEFAULT 0.00            COMMENT '变动前账户余额（tf_b_account.balance）',
      `balance_after`            DECIMAL(16,2) DEFAULT 0.00            COMMENT '变动后账户余额',
      `available_before`         DECIMAL(16,2) DEFAULT 0.00            COMMENT '变动前可用余额（tf_b_account.available_amount）',
      `available_after`          DECIMAL(16,2) DEFAULT 0.00            COMMENT '变动后可用余额',
      `frozen_before`            DECIMAL(16,2) DEFAULT 0.00            COMMENT '变动前冻结金额（tf_b_account.frozen_amount）',
      `frozen_after`             DECIMAL(16,2) DEFAULT 0.00            COMMENT '变动后冻结金额',
      `counterparty_user_id`     VARCHAR(64)   DEFAULT NULL            COMMENT '对手方用户ID（跨账户划转时必填：货主清算填平台公司、承运方划账填平台公司）',
      `counterparty_name`        VARCHAR(128)  DEFAULT NULL            COMMENT '对手方名称（冗余）',
      `counterparty_account_type` TINYINT      DEFAULT NULL            COMMENT '对手方账户类型：1用户账户 2平台公司对公账户',
      `order_id`                 VARCHAR(64)   DEFAULT NULL            COMMENT '关联运单号（tf_b_order.order_id，非运单类业务可为空）',
      `biz_no`                   VARCHAR(64)   DEFAULT NULL            COMMENT '关联业务单号（清算单号/提现单号/冻结流水号/充值单号），幂等去重用',
      `flow_time`                DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '资金发生时间',
      `status`                   TINYINT       DEFAULT 1               COMMENT '状态：1成功(已入账) 2处理中 3失败 4已冲正',
      `status_desc`              VARCHAR(32)   DEFAULT NULL            COMMENT '状态描述（冗余）：成功/处理中/失败/已冲正',
      `related_flow_no`          VARCHAR(32)   DEFAULT NULL            COMMENT '关联原流水号（冲正/反向记账时回填，指向被冲正的流水）',
      `remark`                   VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
      `delete_flag`              TINYINT       DEFAULT 0               COMMENT '删除标记：0正常 1已删除',
      `create_time`              DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `create_name`              VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
      `update_time`              DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      `update_name`              VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_flow_no`    (`flow_no`),
      UNIQUE KEY `uk_biz_dedup`  (`user_id`, `biz_type`, `biz_no`, `direction`),
      KEY `idx_user_time`        (`user_id`, `flow_time`, `delete_flag`),
      KEY `idx_user_direction`   (`user_id`, `direction`, `flow_time`),
      KEY `idx_biz`              (`biz_type`, `biz_no`),
      KEY `idx_order_id`         (`order_id`),
      KEY `idx_counterparty`     (`counterparty_user_id`, `flow_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '账户收入支出流水表';

-- =====================================================================
-- 示例数据（可按需执行）
-- 与 tf_b_account / tf_b_frozen_detail / tf_b_settlement_carrier 示例保持一致：
--   货主 U001（张三，运费 1800，托管冻结 1800）-> 平台公司对公账户 P001 -> 承运方 U002（李四，实收 1710）
--   平台公司账户 user_id 取自配置 account.platform.company-user-id（示例为 P001）
-- 口径校验：平台收入 1800 - 平台支出 1710 = 90（承运方服务费 5%），即该单平台服务费收入
-- =====================================================================
INSERT INTO `tf_b_income_expense`
(`flow_no`, `user_id`, `user_name`, `account_type`, `direction`, `direction_desc`,
 `biz_type`, `biz_type_name`, `income_expense_type`, `income_expense_type_name`, `amount`,
 `balance_before`, `balance_after`, `available_before`, `available_after`, `frozen_before`, `frozen_after`,
 `counterparty_user_id`, `counterparty_name`, `counterparty_account_type`,
 `order_id`, `biz_no`, `flow_time`, `status`, `status_desc`, `related_flow_no`, `remark`, `create_name`)
VALUES
    -- 1) 货主充值 5000：外部资金进入货主账户（收入）
    ('SZ202609120900001', 'U001', '张三', 1, 1, '收入',
     1, '充值', 4, '充值', 5000.00,
     0.00, 5000.00, 0.00, 5000.00, 0.00, 0.00,
     NULL, NULL, NULL,
     NULL, 'CZ20260912090001', '2026-09-12 09:00:00', 1, '成功', NULL, '线上支付充值', 'gkk'),

    -- 2) 发布货源冻结运费 1800：可用余额 -> 冻结金额（冻结，不影响余额）
    ('SZ202609120940001', 'U001', '张三', 1, 3, '冻结',
     3, '运费托管', 0, NULL, 1800.00,
     5000.00, 5000.00, 5000.00, 3200.00, 0.00, 1800.00,
     NULL, NULL, NULL,
     'YD20260902001', 'DJ20260912094001', '2026-09-12 09:40:00', 1, '成功', NULL, '运单 YD20260902001 运费托管冻结', 'gkk'),

    -- 3) 货主清算扣划：货主托管冻结款划至平台公司对公账户（货主支出）
    ('SZ202609121000001', 'U001', '张三', 1, 2, '支出',
     5, '货主清算扣划', 2, '运费支出', 1800.00,
     5000.00, 3200.00, 3200.00, 3200.00, 1800.00, 0.00,
     'P001', '智运宝平台公司账户', 2,
     'YD20260902001', 'QSS202609120001', '2026-09-12 10:00:00', 1, '成功', NULL, '运单完成，托管运费扣划至平台公司对公账户', 'gkk'),

    -- 4) 货主清算扣划（对手方视角）：平台公司对公账户收款（平台收入）
    ('SZ202609121000002', 'P001', '智运宝平台公司账户', 2, 1, '收入',
     5, '货主清算扣划', 1, '运费收入', 1800.00,
     0.00, 1800.00, 0.00, 1800.00, 0.00, 0.00,
     'U001', '张三', 1,
     'YD20260902001', 'QSS202609120001', '2026-09-12 10:00:00', 1, '成功', NULL, '货主 U001 托管运费到账', 'gkk'),

    -- 5) 承运方对账划账：平台公司对公账户出账（平台支出，仅划付承运方实收额）
    ('SZ202609121005001', 'P001', '智运宝平台公司账户', 2, 2, '支出',
     6, '承运方清算划账', 2, '运费支出', 1710.00,
     1800.00, 90.00, 1800.00, 90.00, 0.00, 0.00,
     'U002', '李四', 1,
     'YD20260902001', 'CJS202609120001', '2026-09-12 10:05:00', 1, '成功', NULL, '承运方对账，划付承运方实收金额', 'gkk'),

    -- 6) 承运方对账划账（对手方视角）：承运方账户收款（承运方收入）
    ('SZ202609121005002', 'U002', '李四', 1, 1, '收入',
     6, '承运方清算划账', 1, '运费收入', 1710.00,
     0.00, 1710.00, 0.00, 1710.00, 0.00, 0.00,
     'P001', '智运宝平台公司账户', 2,
     'YD20260902001', 'CJS202609120001', '2026-09-12 10:05:00', 1, '成功', NULL, '运费扣除 5% 平台服务费后入账', 'gkk'),

    -- 7) 承运方提现申请冻结 500：可用余额 -> 冻结金额（冻结，不影响余额）
    ('SZ202609121510001', 'U002', '李四', 1, 3, '冻结',
     2, '提现', 0, NULL, 500.00,
     1710.00, 1710.00, 1710.00, 1210.00, 0.00, 500.00,
     NULL, NULL, NULL,
     NULL, 'TX20260902001', '2026-09-12 15:10:00', 1, '成功', NULL, '提现申请，资金冻结', 'gkk'),

    -- 8) 提现打款成功：余额与冻结同步扣减（支出）
    ('SZ202609121515001', 'U002', '李四', 1, 2, '支出',
     2, '提现', 5, '提现', 500.00,
     1710.00, 1210.00, 1210.00, 1210.00, 500.00, 0.00,
     NULL, NULL, NULL,
     NULL, 'TX20260902001', '2026-09-12 15:15:00', 1, '成功', 'SZ202609121510001', '提现打款至尾号 6688 银行卡', 'gkk');

-- =====================================================================
-- 常用查询示例
-- 1) 用户收支明细列表（按发生时间倒序，分页）
--    SELECT flow_no, direction_desc, biz_type_name, income_expense_type_name, amount,
--           balance_after, available_after, frozen_after,
--           DATE_FORMAT(flow_time, '%Y-%m-%d %H:%i') AS flow_time, status_desc
--    FROM tf_b_income_expense
--    WHERE user_id = 'U002' AND status = 1 AND delete_flag = 0
--    ORDER BY flow_time DESC, id DESC;
--
-- 2) 用户区间收支汇总（收入 / 支出 / 净额）
--    SELECT IFNULL(SUM(CASE WHEN direction = 1 THEN amount ELSE 0 END), 0) AS income_total,
--           IFNULL(SUM(CASE WHEN direction = 2 THEN amount ELSE 0 END), 0) AS expense_total,
--           IFNULL(SUM(CASE WHEN direction = 1 THEN amount ELSE -amount END), 0) AS net_amount
--    FROM tf_b_income_expense
--    WHERE user_id = 'U002' AND direction IN (1, 2) AND status = 1 AND delete_flag = 0
--      AND flow_time >= '2026-09-01 00:00:00' AND flow_time < '2026-10-01 00:00:00';
--
-- 3) 账户余额对账：期初 + 收入 - 支出 应等于当前账户余额（资金安全每日巡检）
--    SELECT a.user_id, a.balance AS account_balance,
--           IFNULL(SUM(CASE t.direction WHEN 1 THEN t.amount WHEN 2 THEN -t.amount ELSE 0 END), 0) AS flow_balance
--    FROM tf_b_account a
--    LEFT JOIN tf_b_income_expense t
--           ON t.user_id = a.user_id AND t.status = 1 AND t.delete_flag = 0
--    WHERE a.delete_flag = 0
--    GROUP BY a.user_id, a.balance
--    HAVING account_balance <> flow_balance;
--
-- 4) 平台公司对公账户收支总览与平台服务费（收入 - 支出 = 平台毛利）
--    SELECT IFNULL(SUM(CASE WHEN direction = 1 THEN amount ELSE 0 END), 0) AS platform_income,
--           IFNULL(SUM(CASE WHEN direction = 2 THEN amount ELSE 0 END), 0) AS platform_expense,
--           IFNULL(SUM(CASE WHEN direction = 1 THEN amount ELSE -amount END), 0) AS platform_profit
--    FROM tf_b_income_expense
--    WHERE account_type = 2 AND direction IN (1, 2) AND status = 1 AND delete_flag = 0;
--
-- 5) 单运单资金链路（收支双向配对，排查「钱从哪来、到哪去」）
--    SELECT flow_no, user_id, user_name, account_type, direction_desc, biz_type_name,
--           amount, counterparty_user_id, counterparty_name, flow_time
--    FROM tf_b_income_expense
--    WHERE order_id = 'YD20260902001' AND delete_flag = 0
--    ORDER BY flow_time ASC, id ASC;
--
-- 6) 记账幂等：同一账户同一业务单号同方向重复记账时，uk_biz_dedup 唯一索引报错即视为已记账，
--    直接按业务单号回查既有流水返回，资金动作不再重复执行。
--    SELECT flow_no, user_id, direction_desc, amount, status_desc
--    FROM tf_b_income_expense
--    WHERE user_id = 'U002' AND biz_type = 6 AND biz_no = 'CJS202609120001'
--      AND direction = 1 AND delete_flag = 0;
-- =====================================================================
