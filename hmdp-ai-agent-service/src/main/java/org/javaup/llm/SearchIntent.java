package org.javaup.llm;

import lombok.Data;

@Data
public class SearchIntent {
    private String keyword;
    private Integer typeId;
    private String area;
    private Double avgPriceMin;
    private Double avgPriceMax;
    private String sortBy;
    private String sortOrder;
}
