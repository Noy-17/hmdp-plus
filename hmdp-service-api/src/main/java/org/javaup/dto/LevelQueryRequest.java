package org.javaup.dto;

import lombok.Data;

import java.util.Set;

@Data
public class LevelQueryRequest {
    private Set<Integer> levels;
    private Integer minLevel;
    private Integer limit;
}
