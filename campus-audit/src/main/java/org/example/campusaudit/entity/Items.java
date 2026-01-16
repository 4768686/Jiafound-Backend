package org.example.campusaudit.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 物品启事表
 * @TableName items
 */
@TableName(value ="items")
@Data
public class Items {
    /**
     * 主键UUID
     */
    @TableId
    private String itemId;

    /**
     * 发布者ID
     */
    private String userId;

    /**
     * 类型: Lost, Found
     */
    private String itemType;

    /**
     * 物品描述
     */
    private String description;

    /**
     * 地点文字描述
     */
    private String locationText;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 状态: Pending_Audit, Published, Claiming, Finished...
     */
    private String status;

    /**
     * 悬赏金额
     */
    private BigDecimal rewardPoints;

    /**
     * 发布时间
     */
    private Date publishTime;

    /**
     * 
     */
    private Date updateTime;
}