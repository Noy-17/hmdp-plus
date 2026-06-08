package org.javaup.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.*;

@Configuration
public class GatewaySentinelConfig {

    @PostConstruct
    public void initBlockHandler() {
        GatewayCallbackManager.setBlockHandler((exchange, t) -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.put("errorMsg", "当前请求量过大，请稍后重试");
            body.put("data", null);

            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body);
        });
    }

    @PostConstruct
    public void initGatewayFlowRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        rules.add(new GatewayFlowRule("voucher-service")
                .setCount(300)
                .setIntervalSec(1)
                .setBurst(100));

        rules.add(new GatewayFlowRule("shop-service")
                .setCount(500)
                .setIntervalSec(1));

        rules.add(new GatewayFlowRule("user-service")
                .setCount(200)
                .setIntervalSec(1));

        rules.add(new GatewayFlowRule("blog-service")
                .setCount(300)
                .setIntervalSec(1));

        rules.add(new GatewayFlowRule("follow-service")
                .setCount(200)
                .setIntervalSec(1));

        rules.add(new GatewayFlowRule("ai-agent-service")
                .setCount(100)
                .setIntervalSec(1));

        GatewayRuleManager.loadRules(rules);
    }

    @PostConstruct
    public void initSystemRules() {
        List<SystemRule> rules = new ArrayList<>();
        SystemRule rule = new SystemRule();
        rule.setHighestSystemLoad(4.0);
        rule.setHighestCpuUsage(0.8);
        rule.setAvgRt(200);
        rule.setMaxThread(200);
        rule.setQps(1000);
        rules.add(rule);
        SystemRuleManager.loadRules(rules);
    }
}
