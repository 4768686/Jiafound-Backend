package org.example.campususer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提现请求 DTO
 */
@Data
public class WithdrawRequest {

    /**
     * 提现赏币数量
     * 最低提现：100 赏币
     * 汇率：100 赏币 = 1 元
     */
    @NotNull(message = "提现金额不能为空")
    @Min(value = 100, message = "最低提现100赏币")
    private Integer coinAmount;

    /**
     * 提现账户信息（可选）
     * 例如：支付宝账号、银行卡号等
     */
    private String accountInfo;

    /**
     * 提现说明（可选）
     */
    private String remark;

    /**
     * 计算提现金额（单位：元）
     */
    public Double getWithdrawAmount() {
        return coinAmount != null ? coinAmount / 100.0 : 0.0;
    }
}
