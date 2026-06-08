package org.javaup.feign;

import org.javaup.dto.RankCandidatesRequest;
import org.javaup.dto.RankedCandidatesResponse;
import org.javaup.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "hmdp-ai-agent-service", path = "/ai-internal")
public interface AiVoucherRankingClient {

    @PostMapping("/rank/candidates")
    Result<RankedCandidatesResponse> rankCandidates(@RequestBody RankCandidatesRequest req);
}
