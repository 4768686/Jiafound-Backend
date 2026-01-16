package org.example.campusaudit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeductCoinPunishRequest {
    private String userId;
    private Integer amount;
    private String reason;
    private String relatedItemId;
}
