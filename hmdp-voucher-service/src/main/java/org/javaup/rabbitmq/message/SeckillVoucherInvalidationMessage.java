package org.javaup.rabbitmq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeckillVoucherInvalidationMessage {

    private Long voucherId;

    private String reason;
}
