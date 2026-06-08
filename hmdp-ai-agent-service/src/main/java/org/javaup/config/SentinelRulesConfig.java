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

        FlowRule searchRule = new FlowRule();
        searchRule.setResource("POST:/ai/search");
        searchRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        searchRule.setCount(50);

        FlowRule voucherRule = new FlowRule();
        voucherRule.setResource("POST:/ai/recommend/voucher");
        voucherRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        voucherRule.setCount(30);

        FlowRule shopRule = new FlowRule();
        shopRule.setResource("POST:/ai/recommend/shop");
        shopRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        shopRule.setCount(30);

        rules.add(searchRule);
        rules.add(voucherRule);
        rules.add(shopRule);
        FlowRuleManager.loadRules(rules);
    }
}
