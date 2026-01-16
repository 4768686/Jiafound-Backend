package org.example.campusclaim.controller;

import org.example.campusclaim.entity.Claims;
import org.example.campusclaim.service.ClaimsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/item")
@CrossOrigin(origins = "*")
public class ClaimController {

    @Autowired
    private ClaimsService claimsService;

    // 统一封装返回格式
    private Map<String, Object> result(Map<String, Object> data) {
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("msg", "操作成功");
        res.put("data", data);
        return res;
    }

    /** 同意申请参数 */
    public static class ApproveReq {
        private String itemID;
        private String publisherID;
        public String getItemID() { return itemID; }
        public void setItemID(String itemID) { this.itemID = itemID; }
        public String getPublisherID() { return publisherID; }
        public void setPublisherID(String publisherID) { this.publisherID = publisherID; }
    }

    /** 拒绝申请参数 */
    public static class RejectReq {
        private String itemID;
        private String publisherID;
        private String rejectReply;
        public String getItemID() { return itemID; }
        public void setItemID(String itemID) { this.itemID = itemID; }
        public String getPublisherID() { return publisherID; }
        public void setPublisherID(String publisherID) { this.publisherID = publisherID; }
        public String getRejectReply() { return rejectReply; }
        public void setRejectReply(String rejectReply) { this.rejectReply = rejectReply; }
    }

    /** 确认交接参数 */
    public static class ConfirmReq {
        private String itemID;
        private String userID;
        public String getItemID() { return itemID; }
        public void setItemID(String itemID) { this.itemID = itemID; }
        public String getUserID() { return userID; }
        public void setUserID(String userID) { this.userID = userID; }
    }

    public static class DisputeReq {
        private String itemID;
        private String reason;
        private String userID; // 前端必须传这个
        private List<String> evidence;
        public String getItemID() { return itemID; }
        public void setItemID(String itemID) { this.itemID = itemID; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getUserID() { return userID; }
        public void setUserID(String userID) { this.userID = userID; }
        public List<String> getEvidence() { return evidence; }
        public void setEvidence(List<String> evidence) { this.evidence = evidence; }
    }

    /** 撤销参数 */
    public static class CancelReq {
        private String itemID;
        private String userID;
        public String getItemID() { return itemID; }
        public void setItemID(String itemID) { this.itemID = itemID; }
        public String getUserID() { return userID; }
        public void setUserID(String userID) { this.userID = userID; }
    }

    /** 强制裁决参数 */
    public static class ArbitrationReq {
        private String claimID;
        private String decision;
        private String ticketID;
        public String getClaimID() { return claimID; }
        public void setClaimID(String claimID) { this.claimID = claimID; }
        public String getDecision() { return decision; }
        public void setDecision(String decision) { this.decision = decision; }
        public String getTicketID() { return ticketID; }
        public void setTicketID(String ticketID) { this.ticketID = ticketID; }
    }

    // ==========================================
    //  接口实现
    // ==========================================

    @GetMapping("/detail")
    public Map<String, Object> getDetail(@RequestParam("itemID") String itemId) {
        System.out.println(">>> [后端收到] 查询详情: " + itemId);
        return result(claimsService.getItemDetail(itemId));
    }
    
    // 获取大厅列表 (之前新增的)
    @GetMapping("/hall")
    public Map<String, Object> getHallList() {
        System.out.println(">>> [后端收到] 查询大厅列表");
        return result(Map.of("list", claimsService.getHallList())); 
    }

    // 1. 发起认领 /item/claim (使用实体类，无需修改)
    @PostMapping("/claim")
    public Map<String, Object> claim(@RequestBody Claims claim) {
        System.out.println(">>> [后端收到] 认领请求: itemID=" + claim.getItemId() + ", applicantID=" + claim.getApplicantId());
        return result(claimsService.createClaim(claim));
    }

    // 2. 发起归还 /item/return (复用认领逻辑)
    @PostMapping("/return")
    public Map<String, Object> returnItem(@RequestBody Claims claim) {
        System.out.println(">>> [后端收到] 归还请求");
        return result(claimsService.createClaim(claim));
    }

    // 3. 同意申请 /item/approve
    @PutMapping("/approve")
    public Map<String, Object> approve(@RequestBody ApproveReq req) {
        System.out.println(">>> [后端收到] 同意申请: " + req.getItemID());
        return result(claimsService.approveClaim(req.getItemID(), req.getPublisherID()));
    }

    // 4. 拒绝申请 /item/reject
    @PutMapping("/reject")
    public Map<String, Object> reject(@RequestBody RejectReq req) {
        System.out.println(">>> [后端收到] 拒绝申请: " + req.getItemID());
        return result(claimsService.rejectClaim(req.getItemID(), req.getPublisherID(), req.getRejectReply()));
    }

    // 5. 确认交接 /item/confirm
    @PutMapping("/confirm")
    public Map<String, Object> confirm(@RequestBody ConfirmReq req) { 
        System.out.println(">>> [后端收到] 确认交接: " + req.getItemID());
        Map<String, Object> data = claimsService.confirmHandover(req.getItemID(), req.getUserID());
        
        Map<String, Object> res = result(data);
        if ("Success".equals(data.get("status")) || "FINISHED".equals(data.get("status"))) {
            res.put("msg", "交接完成");
        } else {
            res.put("msg", "您已确认，等待对方确认");
        }
        return res;
    }

    // 6. 获取当前状态
    @GetMapping("/status")
    public Map<String, Object> getStatus(@RequestParam("itemID") String itemId, @RequestParam("userID") String userId) {
        return result(claimsService.getClaimStatus(itemId, userId));
    }

    @GetMapping("/history/applied")
    public Map<String, Object> getMyApplied(@RequestParam("userID") String userId) {
        return result(Map.of("list", claimsService.getMyAppliedHistory(userId)));
    }

    @GetMapping("/history/published")
    public Map<String, Object> getMyPublished(@RequestParam("userID") String userId) {
        return result(Map.of("list", claimsService.getMyPublishedHistory(userId)));
    }

    // 7. 撤销申请/撤销纠纷
    @PutMapping("/cancel")
    public Map<String, Object> cancel(@RequestBody CancelReq req) {
        System.out.println(">>> [后端收到] 撤销请求: itemID=" + req.getItemID() + ", userID=" + req.getUserID());
        return result(claimsService.cancelClaim(req.getItemID(), req.getUserID()));
    }
}