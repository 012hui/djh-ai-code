
-- 创建库
create database if not exists djh_ai_code;

-- 切换库
use djh_ai_code;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;
-- 应用表
create table app
(
    id           bigint auto_increment comment 'id' primary key,
    appName      varchar(256)                       null comment '应用名称',
    cover        varchar(512)                       null comment '应用封面',
    initPrompt   text                               null comment '应用初始化的 prompt',
    codeGenType  varchar(64)                        null comment '代码生成类型（枚举）',
    deployKey    varchar(64)                        null comment '部署标识',
    deployedTime datetime                           null comment '部署时间',
    priority     int      default 0                 not null comment '优先级',
    userId       bigint                             not null comment '创建用户id',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    UNIQUE KEY uk_deployKey (deployKey), -- 确保部署标识唯一
    INDEX idx_appName (appName),         -- 提升基于应用名称的查询性能
    INDEX idx_userId (userId)            -- 提升基于用户 ID 的查询性能
) comment '应用' collate = utf8mb4_unicode_ci;

-- 对话历史表
create table chat_history
(
    id          bigint auto_increment comment 'id' primary key,
    -- AI 历史可能包含完整代码，使用 longtext 避免 TEXT 的 64KB 截断
    message     longtext                           not null comment '消息',
    messageType varchar(32)                        not null comment 'user/ai',
    turnId      bigint                             null comment '轮次ID：同一轮用户消息和AI消息共享',
    success     tinyint                            null comment '本轮是否成功完成：1成功 0失败；user行为null',
    appId       bigint                             not null comment '应用id',
    userId      bigint                             not null comment '创建用户id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    INDEX idx_appId (appId),                       -- 提升基于应用的查询性能
    INDEX idx_createTime (createTime),             -- 提升基于时间的查询性能
    INDEX idx_appId_createTime (appId, createTime), -- 游标查询核心索引
    INDEX idx_turnId (turnId),
    INDEX idx_appId_turnId (appId, turnId)
) comment '对话历史' collate = utf8mb4_unicode_ci;

-- 工具调用事件表
-- 一行对应一次已执行完成的工具调用；summary 只存紧凑摘要，不存完整参数/结果
create table chat_event
(
    id         bigint auto_increment comment 'id' primary key,
    appId      bigint        not null comment '应用id',
    turnId     bigint        not null comment '轮次ID，对应 chat_history.turnId',
    seq        int           not null comment '轮内顺序，从1递增',
    toolName   varchar(64)   not null comment '工具名',
    summary    varchar(1024) not null comment '工具执行摘要',
    success    tinyint       not null comment '工具是否成功：1成功 0失败',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    isDelete   tinyint  default 0 not null comment '是否删除',
    UNIQUE KEY uk_turn_seq (turnId, seq),
    INDEX idx_appId_createTime (appId, createTime),
    INDEX idx_turnId (turnId)
) comment '工具调用事件' collate = utf8mb4_unicode_ci;
