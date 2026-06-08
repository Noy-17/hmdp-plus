CREATE DATABASE IF NOT EXISTS hmdp_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE hmdp_ai;

CREATE TABLE IF NOT EXISTS tb_user_behavior (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    target_id BIGINT,
    target_type VARCHAR(32),
    event_timestamp BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_user_time (user_id, event_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
