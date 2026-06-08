package org.javaup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShopSearchResult {
    private Long id;
    private String name;
    private String address;
    private String area;
    private Long typeId;
    private String images;
    private Long avgPrice;
    private Integer sold;
    private Integer comments;
    private Integer score;
    private Double distance;
}
