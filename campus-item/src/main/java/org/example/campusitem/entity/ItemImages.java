package org.example.campusitem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 物品图片表
 * @TableName item_images
 */
@TableName(value ="item_images")
@Data
public class ItemImages {
    /**
     * 自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long imageId;

    /**
     * 关联物品ID
     */
    private String itemId;

    /**
     * 图片存储路径
     */
    private String imageUrl;

    /**
     * 显示排序
     */
    private Integer sortOrder;

    /**
     * 
     */
    private Date createTime;

    // 手动添加 Getter 和 Setter
    public Long getImageId() { return imageId; }
    public void setImageId(Long imageId) { this.imageId = imageId; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}