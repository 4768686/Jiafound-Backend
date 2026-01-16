package org.example.campusaudit.dto;

import lombok.Data;

@Data
public class DisputeTicketDTO {
    private String ticketId;
    private String claimId;
    private String initiatorName;
    private String reason;
    private String status;
    private String createTime;
    private Boolean isOvertime;
}
