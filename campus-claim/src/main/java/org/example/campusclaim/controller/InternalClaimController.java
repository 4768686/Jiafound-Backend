package org.example.campusclaim.controller;

import org.example.campusclaim.service.ClaimsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 内部微服务接口控制器
 * 供其他子系统（如审核、用户）调用
 */
@RestController
public class InternalClaimController {

    @Autowired
    private ClaimsService claimsService;

    // 1. [被调] 审核管理子系统 -> 强制裁决
    @PostMapping("/internal/v1/claim/arbitration/enforce")
    public Map<String, Object> enforceArbitration(@RequestBody Map<String, String> body) {
        String claimId = body.get("claimID");
        String decision = body.get("decision");
        String ticketId = body.get("ticketID");

        System.out.println(">>> [Internal] 收到强制裁决请求: " + decision);
        claimsService.enforceArbitration(claimId, decision, ticketId);

        return Map.of("code", 200, "message", "认领单已按裁决结果强制关闭");
    }

    // 2. [被调] 纠纷审核子系统 -> 获取认领信息 (对应你提供的新接口)
    // 请求路径：/internal/v1/claim/info/{claimId}
    @GetMapping("/internal/v1/claim/info/{claimId}")
    public Map<String, Object> getInternalClaimInfo(@PathVariable String claimId) {
        System.out.println(">>> [Internal] 收到认领信息查询: " + claimId);
        
        Map<String, Object> data = claimsService.getClaimInfoForInternal(claimId);
        
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("message", "查询成功");
        res.put("data", data);
        return res;
    }

    // 3. [被调] 纠纷审核子系统 -> 获取物品快照 (对应你提供的新接口)
    // 请求路径：/internal/v1/item/snapshot/{itemId}
    @GetMapping("/internal/v1/item/snapshot/{itemId}")
    public Map<String, Object> getItemSnapshot(@PathVariable String itemId) {
        System.out.println(">>> [Internal] 收到物品快照查询: " + itemId);
        
        Map<String, Object> data = claimsService.getItemSnapshotForInternal(itemId);
        
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("message", "查询成功");
        res.put("data", data);
        return res;
    }

    @PostMapping("/internal/v1/claim/dispute/notify")
    public Map<String, Object> notifyDisputeCreated(@RequestBody Map<String, String> body) {
        String itemId = body.get("itemID");
        String ticketId = body.get("ticketID"); // 队友可能会传 ticketID，虽然我们这里暂时没存，但可以打印日志

        System.out.println(">>> [Internal] 收到纠纷创建通知 (工单号: " + ticketId + ")，更新物品 " + itemId + " 状态为 Disputed");
        
        // 调用 Service 更新状态
        claimsService.markAsDisputed(itemId);
        
        return Map.of("code", 200, "message", "状态已同步");
    }
}