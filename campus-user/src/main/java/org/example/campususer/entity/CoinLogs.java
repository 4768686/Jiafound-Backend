package org.example.campususer.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 资金流水表
 * @TableName coin_logs
 */
@TableName(value ="coin_logs")
@Data
public class CoinLogs {
    /**
     * 主键UUID
     */
    @TableId
    private String logId;

    /**
     * 所属用户
     */
    private String userId;

    /**
     * 变动金额(+/-)
     */
    private BigDecimal amount;

    /**
     * 类型: Recharge, Withdraw, Freeze, Reward, Settle
     */
    private String type;

    /**
     * 关联物品ID(可选)
     */
    private String relatedItemId;

    /**
     * 
     */
    private Date createTime;
}