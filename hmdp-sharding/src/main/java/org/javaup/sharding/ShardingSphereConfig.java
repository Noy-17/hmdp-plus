package org.javaup.sharding;

import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

@Configuration
public class ShardingSphereConfig {

    @Bean
    public DataSource dataSource() throws IOException {
        try {
            return YamlShardingSphereDataSourceFactory.createDataSource(
                    new ClassPathResource("shardingsphere.yaml").getFile());
        } catch (SQLException e) {
            throw new IOException("Failed to create ShardingSphere DataSource", e);
        }
    }
}
