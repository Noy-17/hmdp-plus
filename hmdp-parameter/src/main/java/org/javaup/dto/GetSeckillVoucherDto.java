package org.javaup.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class GetSeckillVoucherDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 优惠券id
     */
    @NotNull
    private Long voucherId;
}
