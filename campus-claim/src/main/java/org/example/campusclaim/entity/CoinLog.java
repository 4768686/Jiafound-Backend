package org.example.campusclaim.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("coin_logs")
public class CoinLog {
    @TableId
    private String logId;
    private String userId;
    private BigDecimal amount;
    private String type; // RECHARGE, FREEZE, SETTLE, REWARD
    private String relatedItemId;
    private Date createTime;
}