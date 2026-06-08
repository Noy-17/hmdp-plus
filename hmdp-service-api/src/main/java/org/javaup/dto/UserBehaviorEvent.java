package org.javaup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBehaviorEvent implements Serializable {
    private Long userId;
    private String eventType;
    private Long targetId;
    private String targetType;
    private Long timestamp;
}
