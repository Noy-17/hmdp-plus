package org.javaup.delay.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DelayedVoucherReminderMessage {
    
    private Long voucherId;
    
    private LocalDateTime beginTime;
}