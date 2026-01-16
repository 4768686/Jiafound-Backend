package org.example.campusitem.entity;

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

    // 手动添加 Getter 和 Setter
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocationText() { return locationText; }
    public void setLocationText(String locationText) { this.locationText = locationText; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(BigDecimal rewardPoints) { this.rewardPoints = rewardPoints; }
    public Date getPublishTime() { return publishTime; }
    public void setPublishTime(Date publishTime) { this.publishTime = publishTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}