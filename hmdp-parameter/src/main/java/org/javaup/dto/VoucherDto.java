package org.javaup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class VoucherDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 商铺id
     */
    @NotNull
    private Long shopId;

    /**
     * 代金券标题
     */
    @NotBlank
    private String title;

    /**
     * 副标题
     */
    @NotBlank
    private String subTitle;

    /**
     * 使用规则
     */
    @NotBlank
    private String rules;

    /**
     * 支付金额
     */
    @NotNull
    private Long payValue;

    /**
     * 抵扣金额
     */
    @NotNull
    private Long actualValue;

    /**
     * 优惠券类型 0,普通券；1,秒杀券
     */
    @NotNull
    private Integer type;

    /**
     * 优惠券状态 1,上架; 2,下架; 3,过期
     */
    @NotNull
    private Integer status;


}
