package org.example.campusaudit.dto;

import lombok.Data;

import java.util.List;

@Data
public class EvidenceRequestDTO {
    List<String> imageUrls;
    String comments;
}
