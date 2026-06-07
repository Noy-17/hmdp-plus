-- ============================================================
-- Seata AT Mode undo_log 表
-- 必须在每个物理库 (hmdp_0 / hmdp_1) 都存在
-- 此表为 Seata 系统表，不参与业务分片
-- ============================================================

USE hmdp_0;
CREATE TABLE IF NOT EXISTS `undo_log` (
    `id`            BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT 'increment id',
    `branch_id`     BIGINT(20)   NOT NULL COMMENT 'branch transaction id',
    `xid`           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    `context`       VARCHAR(128) NOT NULL COMMENT 'undo_log context, e.g. serialization',
    `rollback_info` LONGBLOB     NOT NULL COMMENT 'rollback info (before-image)',
    `log_status`    INT(11)      NOT NULL COMMENT '0:normal status, 1:defense status',
    `log_created`   DATETIME(3)  NOT NULL COMMENT 'create datetime',
    `log_modified`  DATETIME(3)  NOT NULL COMMENT 'modify datetime',
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT mode undo log table';

USE hmdp_1;
CREATE TABLE IF NOT EXISTS `undo_log` (
    `id`            BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT 'increment id',
    `branch_id`     BIGINT(20)   NOT NULL COMMENT 'branch transaction id',
    `xid`           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    `context`       VARCHAR(128) NOT NULL COMMENT 'undo_log context, e.g. serialization',
    `rollback_info` LONGBLOB     NOT NULL COMMENT 'rollback info (before-image)',
    `log_status`    INT(11)      NOT NULL COMMENT '0:normal status, 1:defense status',
    `log_created`   DATETIME(3)  NOT NULL COMMENT 'create datetime',
    `log_modified`  DATETIME(3)  NOT NULL COMMENT 'modify datetime',
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT mode undo log table';
