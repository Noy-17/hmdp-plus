package org.javaup.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class UpdateSeckillVoucherDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 优惠券id
     */
    @NotNull
    private Long voucherId;

    /**
     * 代金券标题
     */
    private String title;

    /**
     * 副标题
     */
    private String subTitle;

    /**
     * 使用规则（普通券可用，秒杀受众不再依赖该字段）
     */
    private String rules;

    /**
     * 支付金额
     */
    private Long payValue;

    /**
     * 抵扣金额
     */
    private Long actualValue;

    /**
     * 优惠券类型 0,普通券；1,秒杀券
     */
    private Integer type;

    /**
     * 优惠券状态 1,上架; 2,下架; 3,过期
     */
    private Integer status;
    
    /**
     * 生效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;
    
    /**
     * 失效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 允许参与的会员等级，逗号分隔，如："1,2,3"
     */
    private String allowedLevels;

    /**
     * 最低会员等级
     */
    private Integer minLevel;

    /**
     * 允许参与的城市，逗号分隔，如："北京,上海"
     */
    private String allowedCities;
}
