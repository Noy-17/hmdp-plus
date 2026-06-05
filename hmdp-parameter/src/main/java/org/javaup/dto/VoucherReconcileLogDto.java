package org.javaup.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class VoucherReconcileLogDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    
    
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
}