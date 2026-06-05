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
@TableName("tb_voucher_order")
public class VoucherOrder implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id")
    private Long id;
    
    private Long userId;
    
    private Long voucherId;
    
    private Integer payType;
    
    private Integer status;
    
    private Integer reconciliationStatus;
    
    private LocalDateTime createTime;
    
    private LocalDateTime payTime;
    
    private LocalDateTime useTime;
    
    private LocalDateTime refundTime;
    
    private LocalDateTime updateTime;
    
}
