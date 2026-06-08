package org.javaup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankCandidatesRequest {
    private Long voucherId;
    private List<Long> candidateUserIds;
}
