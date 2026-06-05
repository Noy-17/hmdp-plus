package org.javaup.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_rollback_failure_log")
public class RollbackFailureLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 优惠券id */
    private Long voucherId;

    /** 用户id */
    private Long userId;

    /** 订单id */
    private Long orderId;

    /** 追踪唯一标识 */
    private Long traceId;

    /** 失败原因或详情 */
    private String detail;

    /** Lua返回码（BaseCode），用于判定失败类型 */
    private Integer resultCode;

    /** 已尝试的重试次数 */
    private Integer retryAttempts;

    /** 来源组件，例如：redis_voucher_data / producer */
    private String source;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}