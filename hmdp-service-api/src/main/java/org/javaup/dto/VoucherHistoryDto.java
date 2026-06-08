package org.javaup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoucherHistoryDto {
    private Long userId;
    private Long voucherId;
    private Long orderId;
    private Long shopId;
    private String voucherTitle;
    private LocalDateTime purchaseTime;
}
