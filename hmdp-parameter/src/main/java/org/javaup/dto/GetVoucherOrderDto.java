package org.javaup.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
public class GetVoucherOrderDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单id
     */
    @NotNull
    private Long orderId;

}
