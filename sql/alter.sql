
-- 已有库升级脚本（Step 0 / Step 1）
-- 1. 放开 chat_history.message 长度，避免 AI 历史超过 64KB 时落库失败
-- 2. 新增 turnId、success 及对应索引
-- 注意：chat_event 建表语句在 create_table.sql 中，已有库需要单独执行该表

ALTER TABLE chat_history
    MODIFY COLUMN message LONGTEXT NOT NULL COMMENT '消息';

ALTER TABLE chat_history
    ADD COLUMN turnId BIGINT NULL
        COMMENT '轮次ID：同一轮用户消息和AI消息共享' AFTER messageType,
    ADD COLUMN success TINYINT NULL
        COMMENT '本轮是否成功完成：1成功 0失败；user行为null' AFTER turnId,
    ADD INDEX idx_turnId (turnId),
    ADD INDEX idx_appId_turnId (appId, turnId);
