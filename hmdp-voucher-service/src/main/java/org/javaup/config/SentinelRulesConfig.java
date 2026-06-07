package org.javaup.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SentinelRulesConfig {

    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        FlowRule tokenRule = new FlowRule();
        tokenRule.setResource("GET:/voucher-order/seckill/token/{id}");
        tokenRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        tokenRule.setCount(500);
        rules.add(tokenRule);

        FlowRule seckillRule = new FlowRule();
        seckillRule.setResource("POST:/voucher-order/seckill/{id}");
        seckillRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        seckillRule.setCount(200);
        rules.add(seckillRule);

        FlowRuleManager.loadRules(rules);
    }
}
