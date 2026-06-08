package org.javaup.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaup.dto.RankCandidatesRequest;
import org.javaup.dto.RankedCandidatesResponse;
import org.javaup.dto.Result;
import org.javaup.dto.RankedCandidatesResponse.RankedCandidate;
import org.javaup.service.UserProfileService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai-internal")
@RequiredArgsConstructor
public class AiInternalController {

    private final UserProfileService userProfileService;

    @PostMapping("/rank/candidates")
    public Result<RankedCandidatesResponse> rankCandidates(@RequestBody RankCandidatesRequest req) {
        if (req.getCandidateUserIds() == null || req.getCandidateUserIds().isEmpty()) {
            return Result.ok(new RankedCandidatesResponse(List.of()));
        }

        List<RankedCandidate> ranked = new ArrayList<>();
        for (Long userId : req.getCandidateUserIds()) {
            double score = computeScore(userId);
            ranked.add(new RankedCandidate(userId, score, scoreReason(score)));
        }

        ranked.sort(Comparator.comparingDouble(RankedCandidate::getScore).reversed());

        RankedCandidatesResponse resp = new RankedCandidatesResponse();
        resp.setRanked(ranked);
        return Result.ok(resp);
    }

    private double computeScore(Long userId) {
        Map<Object, Object> profile = userProfileService.getProfile(userId);
        if (profile.isEmpty()) {
            return 0.0;
        }
        double score = 0.0;
        Object freq = profile.get("purchaseFrequency");
        if (freq != null) {
            score += Double.parseDouble(freq.toString()) * 2.0;
        }
        Object shopViews = profile.get("shopViews");
        if (shopViews != null) {
            score += Double.parseDouble(shopViews.toString()) * 0.5;
        }
        Object blogLikes = profile.get("blogLikes");
        if (blogLikes != null) {
            score += Double.parseDouble(blogLikes.toString()) * 0.3;
        }
        return score;
    }

    private String scoreReason(double score) {
        if (score >= 6) return "高活跃用户";
        if (score >= 3) return "中等活跃用户";
        if (score > 0) return "一般活跃用户";
        return "新用户或无行为记录";
    }
}
