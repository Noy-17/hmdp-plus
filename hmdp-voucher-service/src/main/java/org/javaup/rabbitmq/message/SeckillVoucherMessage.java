package org.javaup.rabbitmq.message;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeckillVoucherMessage {

    private Long userId;

    private Long voucherId;

    private Long orderId;

    private Long traceId;

    private Integer beforeQty;

    private Integer changeQty;

    private Integer afterQty;

    private Boolean autoIssue;
}
