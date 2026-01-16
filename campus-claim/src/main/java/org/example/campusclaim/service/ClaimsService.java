package org.example.campusclaim.service;

import org.example.campusclaim.entity.Claims;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface ClaimsService extends IService<Claims> {

    List<Map<String, Object>> getHallList();

    // 获取认领/归还申请状态
    Map<String, Object> getClaimStatus(String itemId, String userId);

    // 获取物品详情
    Map<String, Object> getItemDetail(String itemId);

    List<Map<String, Object>> getMyAppliedHistory(String userId);

    List<Map<String, Object>> getMyPublishedHistory(String userId);
    
    // 创建认领/归还申请
    Map<String, Object> createClaim(Claims claim);
    
    // 同意申请
    Map<String, Object> approveClaim(String itemId, String publisherId);
    
    // 拒绝申请
    Map<String, Object> rejectClaim(String itemId, String publisherId, String reason);
    
    // 确认交接 (核心双向确认逻辑)
    Map<String, Object> confirmHandover(String itemId, String userId);

    // 撤销：Applying -> Published；Disputed -> Accepted（仅发起纠纷的一方可撤销）
    Map<String, Object> cancelClaim(String itemId, String userId);

    // 强制仲裁
    void enforceArbitration(String claimId, String decision, String ticketId);

    void markAsDisputed(String itemId);

    // 内部调用：获取认领关联信息
    Map<String, Object> getClaimInfoForInternal(String claimId);

    // 内部调用：获取物品快照
    Map<String, Object> getItemSnapshotForInternal(String itemId);
}