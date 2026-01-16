package org.example.campusitem.dto;

import java.math.BigDecimal;

public class ItemPublishRequest {
    private String userId;
    private String itemType;
    private String title;
    private String description;
    private String locationText;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String imageUrl;
    private Boolean rewardEnabled;
    private BigDecimal rewardPoints;

    // 手动添加 Getter 和 Setter
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocationText() { return locationText; }
    public void setLocationText(String locationText) { this.locationText = locationText; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Boolean getRewardEnabled() { return rewardEnabled; }
    public void setRewardEnabled(Boolean rewardEnabled) { this.rewardEnabled = rewardEnabled; }

    public BigDecimal getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(BigDecimal rewardPoints) { this.rewardPoints = rewardPoints; }
}