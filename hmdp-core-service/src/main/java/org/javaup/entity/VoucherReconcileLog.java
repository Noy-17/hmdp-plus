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
@TableName("tb_voucher_reconcile_log")
public class VoucherReconcileLog implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id")
    private Long id;
    
    private Long orderId;
    
    private Long userId;
    
    private Long voucherId;
    
    private String messageId;
    
    private String detail;
    
    private Integer beforeQty;
    
    private Integer changeQty;
    
    private Integer afterQty;
    
    private Long traceId;
    
    private Integer logType;
    
    private Integer businessType;
    
    private Integer reconciliationStatus;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}