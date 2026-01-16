package org.example.campusitem.dto;

import java.math.BigDecimal;
import java.util.List;

public class ItemListItemDTO {
    private String itemID;
    private String type;
    private String title;
    private String status;
    private String location;
    private Double latitude;
    private Double longitude;
    private BigDecimal rewardPoints;
    private List<String> images;
    private String publishTime;

    // 手写 Getter/Setter
    public String getItemID() { return itemID; }
    public void setItemID(String itemID) { this.itemID = itemID; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public BigDecimal getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(BigDecimal rewardPoints) { this.rewardPoints = rewardPoints; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public String getPublishTime() { return publishTime; }
    public void setPublishTime(String publishTime) { this.publishTime = publishTime; }
}