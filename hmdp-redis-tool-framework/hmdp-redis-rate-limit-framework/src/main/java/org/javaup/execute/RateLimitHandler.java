package org.javaup.execute;
import org.javaup.ratelimit.extension.RateLimitScene;

public interface RateLimitHandler {
   
    void execute(Long voucherId, Long userId, RateLimitScene scene);
}
