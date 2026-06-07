package org.javaup.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class SentinelRulesConfig {

    @PostConstruct
    public void initDegradeRules() {
        DegradeRule rule = new DegradeRule();
        rule.setResource("/user/login");
        rule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        rule.setCount(0.5);
        rule.setTimeWindow(10);
        rule.setMinRequestAmount(5);
        rule.setStatIntervalMs(10000);

        DegradeRuleManager.loadRules(Collections.singletonList(rule));
    }
}
