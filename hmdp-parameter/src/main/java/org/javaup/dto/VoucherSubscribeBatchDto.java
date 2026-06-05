package org.javaup.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class VoucherSubscribeBatchDto implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 优惠券id集合
     * */
    @NotNull
    private List<Long> voucherIdList;
}
