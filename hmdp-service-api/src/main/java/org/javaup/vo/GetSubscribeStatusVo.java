package org.javaup.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public class GetSubscribeStatusVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long voucherId;

    private Integer subscribeStatus;
}
