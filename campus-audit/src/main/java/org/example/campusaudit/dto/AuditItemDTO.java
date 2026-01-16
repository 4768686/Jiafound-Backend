package org.example.campusaudit.dto;

import lombok.Data;

@Data
public class AuditItemDTO {
    private String itemID;
    private String publisherNickname;
    private String description;
    private String submitTime;
}
