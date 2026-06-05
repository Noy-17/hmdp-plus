package org.javaup.ratelimit.extension;

import org.javaup.enums.BaseCode;

public class NoOpRateLimitPenaltyPolicy implements RateLimitPenaltyPolicy {
    @Override
    public void apply(RateLimitContext ctx, BaseCode reason) {
        // no-op
    }
}