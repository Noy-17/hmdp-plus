package org.javaup.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopDoc {

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

    private String openHours;

    private Location location;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Double distance;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        private double lat;
        private double lon;
    }
}
