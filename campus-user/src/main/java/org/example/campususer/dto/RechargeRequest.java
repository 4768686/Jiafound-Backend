package org.example.campususer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 充值请求 DTO
 */
@Data
public class RechargeRequest {

    /**
     * 充值金额（单位：元）
     * 限制为：1, 2, 5, 10
     */
    @NotNull(message = "充值金额不能为空")
    @Positive(message = "充值金额必须为正数")
    private Integer amount;

    /**
     * 支付方式（可选）
     * 例如：wechat, alipay
     */
    private String paymentMethod;

    /**
     * 验证充值金额是否合法
     */
    public boolean isValidAmount() {
        return amount != null && (amount == 1 || amount == 2 || amount == 5 || amount == 10);
    }
}
