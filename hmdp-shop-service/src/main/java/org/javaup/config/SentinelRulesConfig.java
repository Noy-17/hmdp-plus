package org.javaup.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class SentinelRulesConfig {

    @PostConstruct
    public void initHotParamRules() {
        ParamFlowRule rule = new ParamFlowRule();
        rule.setResource("GET:/shop/{id}");
        rule.setParamIdx(0);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(50);
        rule.setDurationInSec(1);

        ParamFlowItem hotShop = new ParamFlowItem();
        hotShop.setObject("1");
        hotShop.setClassType(Long.class.getName());
        hotShop.setCount(200);

        rule.setParamFlowItemList(Collections.singletonList(hotShop));
        ParamFlowRuleManager.loadRules(Collections.singletonList(rule));
    }
}
