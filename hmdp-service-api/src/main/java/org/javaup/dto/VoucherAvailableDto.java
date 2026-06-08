package org.javaup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoucherAvailableDto {
    private Long id;
    private Long shopId;
    private String title;
    private String subTitle;
    private Long payValue;
    private Long actualValue;
    private Integer type;
    private Integer stock;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
}
