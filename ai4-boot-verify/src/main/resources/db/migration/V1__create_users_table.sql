CREATE TABLE users (
                       id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
                       username VARCHAR(50) NOT NULL COMMENT '用户名',
                       email VARCHAR(100) NOT NULL COMMENT '用户邮箱',
                       password VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码哈希',
                       PRIMARY KEY (id),
                       UNIQUE KEY uk_users_username (username)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;
