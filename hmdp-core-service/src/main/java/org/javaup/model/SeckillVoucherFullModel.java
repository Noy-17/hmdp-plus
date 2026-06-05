package org.javaup.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class SeckillVoucherFullModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    
    private Long id;
    
    private Long voucherId;

    private Integer initStock;
    
    private Integer stock;
    
    private String allowedLevels;
    
    private Integer minLevel;
    
    private LocalDateTime createTime;
    
    private LocalDateTime beginTime;
    
    private LocalDateTime endTime;
    
    private Integer status;
    
    private Long shopId;

}
