package org.example.campusaudit.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class RulingRequestDTO {
    @NotBlank(message = "ticket_id 不能为空")
    private String ticketId; //

    @NotBlank(message = "ruling_result 不能为空")
    private String rulingResult; // 取值如: InitiatorWin, RespondentWin

    @NotNull(message = "扣除信用分不能为空")
    private Integer deductCredit; //
}
