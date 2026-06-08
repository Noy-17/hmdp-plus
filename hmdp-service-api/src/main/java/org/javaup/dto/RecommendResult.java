package org.javaup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecommendResult {
    private Long id;
    private String name;
    private String reason;
    private Double score;
    private Long shopId;
}
