-- ============================================================
-- User Center Database Schema
-- ============================================================

-- Demo 阶段与 game-server 共用 fruit_island，和 application-common.yml 保持一致。
CREATE DATABASE IF NOT EXISTS fruit_island
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE fruit_island;

-- ============================================================
-- 1. User
-- ============================================================
CREATE TABLE user
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    nickname    VARCHAR(32) COMMENT '用户昵称',
    avatar      VARCHAR(255) COMMENT '头像地址',
    status      TINYINT  DEFAULT 1 COMMENT '账号状态 1正常 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT = '用户账号表';

-- ============================================================
-- 2. User Login
-- ============================================================
CREATE TABLE user_login
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '登录记录ID',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    platform    VARCHAR(32)  NOT NULL COMMENT '登录平台',
    platform_uid VARCHAR(128) NOT NULL COMMENT '平台唯一ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_platform_uid (platform, platform_uid),
    INDEX idx_user_id (user_id)
) COMMENT = '用户登录方式表';

-- ============================================================
-- 3. User Token
-- ============================================================
CREATE TABLE user_token
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    token       VARCHAR(512) NOT NULL COMMENT 'JWT Token',
    expire_time DATETIME COMMENT '过期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_token (token)
) COMMENT = '用户登录Token表';

-- ============================================================
-- 4. User Device
-- ============================================================
CREATE TABLE user_device
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    device_type     VARCHAR(32) COMMENT '设备类型',
    device_id       VARCHAR(128) COMMENT '设备唯一ID',
    app_version     VARCHAR(32) COMMENT '客户端版本',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) COMMENT = '用户设备表';

-- ============================================================
-- 5. User Profile
-- ============================================================
CREATE TABLE user_profile
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    gender      TINYINT DEFAULT 0 COMMENT '性别',
    birthday    DATE COMMENT '生日',
    signature   VARCHAR(128) COMMENT '签名',
    country     VARCHAR(32),
    language    VARCHAR(32),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id)
) COMMENT = '用户扩展信息';
