package org.example.campususer.dto;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 赏币流水记录视图对象 VO
 * 用于返回给前端的流水记录
 */
@Data
public class CoinLogVO {

    /**
     * 流水记录ID
     */
    private String logId;

    /**
     * 流水类型
     * Recharge: 充值
     * Withdraw: 提现
     * Freeze: 冻结
     * Reward: 悬赏支出
     * Settle: 结算收入
     */
    private String type;

    /**
     * 变动金额（正数为收入，负数为支出）
     */
    private BigDecimal amount;

    /**
     * 关联物品ID（可选）
     */
    private String relatedItemId;

    /**
     * 创建时间
     */
    private String createTime;
}
