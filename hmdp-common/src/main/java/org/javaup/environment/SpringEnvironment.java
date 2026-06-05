package org.javaup.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 启动时自动读取项目根目录的 {@code .env} 文件，注入 Spring Environment。
 * 使 {@code application.yml} 中的 {@code ${VAR_NAME:default}} 能读取到 .env 中定义的变量。
 */
public class SpringEnvironment implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(SpringEnvironment.class);

    private static final String ENV_FILE_NAME = ".env";
    private static final int SPRING_ORDER = 100;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        application.setAllowBeanDefinitionOverriding(true);
        Map<String, Object> envProps = loadEnvFile();
        if (!envProps.isEmpty()) {
            environment.getPropertySources()
                    .addFirst(new MapPropertySource(".env", Collections.unmodifiableMap(envProps)));
        }
    }

    private Map<String, Object> loadEnvFile() {
        Path envPath = resolveEnvPath();
        if (envPath == null) {
            log.debug("未找到 .env 文件，跳过加载");
            return Collections.emptyMap();
        }
        log.info("加载 .env 文件: {}", envPath.toAbsolutePath());
        Map<String, Object> props = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(envPath)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eqIdx = trimmed.indexOf('=');
                if (eqIdx <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eqIdx).trim();
                String value = trimmed.substring(eqIdx + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!key.isEmpty()) {
                    props.put(key, value);
                }
            }
        } catch (IOException e) {
            log.warn("读取 .env 文件失败: {}", e.getMessage());
        }
        return props;
    }

    private Path resolveEnvPath() {
        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            Path p = Paths.get(userDir, ENV_FILE_NAME);
            if (Files.exists(p)) return p;
        }
        Path cwd = Paths.get(ENV_FILE_NAME);
        if (Files.exists(cwd)) return cwd;
        return null;
    }
}
