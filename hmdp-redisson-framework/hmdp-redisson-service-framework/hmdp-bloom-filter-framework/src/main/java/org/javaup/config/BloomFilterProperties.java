package org.javaup.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties(prefix = BloomFilterProperties.PREFIX)
public class BloomFilterProperties {

    public static final String PREFIX = "bloom-filter";
    
    private Map<String, Filter> filters;

    @Data
    public static class Filter {
        private String name;
        private Long expectedInsertions = 20000L;
        private Double falseProbability = 0.01D;
    }
}
