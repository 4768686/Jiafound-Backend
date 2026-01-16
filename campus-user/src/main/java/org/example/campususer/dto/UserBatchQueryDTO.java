package org.example.campususer.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserBatchQueryDTO {
    private List<String> userIds;
}
