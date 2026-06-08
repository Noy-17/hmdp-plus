package org.javaup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankedCandidatesResponse {
    private List<RankedCandidate> ranked;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RankedCandidate {
        private Long userId;
        private Double score;
        private String reason;
    }
}
