package org.example.campusaudit.dto;

import lombok.Data;
import lombok.NonNull;

@Data
public class AuditDecisionDTO {
    @NonNull
    private String itemID;
    @NonNull
    private String decision; // e.g., "APPROVE", "REJECT"
}
