package org.example.campusclaim.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.example.campusclaim.entity.Claims;
import org.example.campusclaim.service.ClaimsService;
import org.example.campusclaim.service.DisputeService;
import org.example.campusclaim.mapper.ClaimsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.example.campusclaim.mapper.UserMapper;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

@Service
public class ClaimsServiceImpl extends ServiceImpl<ClaimsMapper, Claims> implements ClaimsService {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DisputeService disputeService;

    @Value("${service.user.url}")
    private String userServiceUrl;

    @Value("${service.item.url}")
    private String itemServiceUrl;

    @Value("${service.audit.url}")
    private String auditServiceUrl;

    private String getUserContact(String userId) {
        if (userId == null) return "未知用户";
        // 从 users 表查询 email
        String email = userMapper.selectEmailByUserId(userId);
        return email != null ? email : "暂无邮箱";
    }

    @Override
    public List<Map<String, Object>> getHallList() {
        List<Map<String, Object>> list = baseMapper.selectHallList();
        return list;
    }

    @Override
    public List<Map<String, Object>> getMyAppliedHistory(String userId) {
        return baseMapper.selectMyAppliedList(userId);
    }

    @Override
    public List<Map<String, Object>> getMyPublishedHistory(String userId) {
        return baseMapper.selectMyPublishedList(userId);
    }

    @Override
    public Map<String, Object> getClaimStatus(String itemId, String userId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 先尝试获取我的申请
        QueryWrapper<Claims> query = new QueryWrapper<>();
        query.eq("item_id", itemId).eq("applicant_id", userId).orderByDesc("create_time").last("LIMIT 1");
        Claims myClaim = this.getOne(query);

        // 2. 获取物品详情，确认发布者是谁
        Map<String, Object> itemInfo = baseMapper.selectItemDetail(itemId);
        if (itemInfo == null) {
            result.put("status", "Unknown");
            result.put("hasApplied", false);
            result.put("msg", "物品不存在");
            return result;
        }
        String publisherId = (String) itemInfo.get("publisherID");

        // 3. 如果我不是申请人，我是发布者
        if (myClaim == null) {
            Claims activeClaim = getActiveClaim(itemId); // 获取当前活跃的申请
            if (activeClaim != null && userId.equals(publisherId)) {
                // 我是发布者，且有人正在申请，我应该看到这条申请
                myClaim = activeClaim;
            }
        }

        if (myClaim == null) {
            result.put("status", "Published");
            result.put("hasApplied", false);
        } else {
            result.put("status", myClaim.getStatus());
            result.put("claimId", myClaim.getClaimId());
            // 只有申请人自己才算 hasApplied (控制前端按钮显示)
            result.put("hasApplied", userId.equals(myClaim.getApplicantId()));
            result.put("applyMessage", myClaim.getApplyMessage());
            result.put("rejectReply", myClaim.getRejectReply());
            result.put("finderConfirmed", Boolean.TRUE.equals(myClaim.getFinderConfirm()));
            result.put("ownerConfirmed", Boolean.TRUE.equals(myClaim.getOwnerConfirm()));

            String status = myClaim.getStatus();
            if ("Accepted".equals(status) || "Success".equals(status)) {
                if (userId.equals(myClaim.getApplicantId())) {
                    // 我是申请人 -> 看发布者邮箱
                    result.put("contactInfo", getUserContact(publisherId));
                } else if (userId.equals(publisherId)) {
                    // 我是发布者 -> 看申请人邮箱
                    result.put("contactInfo", getUserContact(myClaim.getApplicantId()));
                } else {
                    result.put("contactInfo", "无权限查看");
                }
            }
        }
        if (myClaim != null && "Disputed".equals(myClaim.getStatus())) {
            // 查询当前活跃的纠纷工单，获取发起人
            String initiatorId = disputeService.getActiveTicketInitiator(myClaim.getClaimId());
            result.put("disputeInitiator", initiatorId);
        }
        return result;
    }

    @Override
    public Map<String, Object> getItemDetail(String itemId) {
        // 1. 获取基本信息
        Map<String, Object> detail = baseMapper.selectItemDetail(itemId);
        if (detail == null) throw new RuntimeException("物品不存在");
        
        // 2. 获取真实图片列表
        List<String> images = baseMapper.selectItemImages(itemId);
        detail.put("images", images != null ? images : List.of());
        
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createClaim(Claims claim) {
        if (claim.getClaimId() == null) {
            claim.setClaimId(UUID.randomUUID().toString().replace("-", ""));
        }
        // 1. 初始状态设为 Applying
        claim.setStatus("Applying");
        claim.setCreateTime(new Date());
        claim.setFinderConfirm(false);
        claim.setOwnerConfirm(false);

        this.save(claim);
        return Map.of("status", "Applying");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approveClaim(String itemId, String publisherId) {
        Claims claim = getActiveClaim(itemId);
        if (claim == null) return error("未找到有效的申请记录");

        claim.setStatus("Accepted");
        claim.setUpdateTime(new Date());
        this.updateById(claim);

        syncItemStatus(itemId, "Claiming");
        //baseMapper.updateItemStatus(itemId, "Claiming");

        // 获取申请人的邮箱，返回给发布者
        String applicantEmail = getUserContact(claim.getApplicantId());

        return Map.of("status", "Accepted", "contactInfo", applicantEmail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> rejectClaim(String itemId, String publisherId, String reason) {
        Claims claim = getActiveClaim(itemId);
        if (claim == null) return error("未找到有效的申请记录");

        // 4. 拒绝后状态设为 Rejected
        claim.setStatus("Rejected");
        claim.setRejectReply(reason);
        claim.setUpdateTime(new Date());
        this.updateById(claim);

        return Map.of("status", "Published"); // 告诉前端回到发布态
    }

   @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirmHandover(String itemId, String userId) {
        Claims claim = getActiveClaim(itemId);
        if (claim == null) return error("申请记录不存在或已结束");

        // 1. 获取物品详情，判断是 "Lost"(寻物) 还是 "Found"(招领)
        Map<String, Object> itemInfo = baseMapper.selectItemDetail(itemId);
        if (itemInfo == null) return error("物品不存在");
        
        String itemType = (String) itemInfo.get("itemType"); // Lost 或 Found
        boolean isLost = "Lost".equalsIgnoreCase(itemType) || "寻物".equals(itemType);

        boolean isApplicant = userId.equals(claim.getApplicantId());

        // 2. 核心修正逻辑：根据物品类型映射身份
        if (isLost) {
            if (isApplicant) {
                claim.setFinderConfirm(true); // 申请人(拾主)确认
            } else {
                claim.setOwnerConfirm(true);  // 发布者(失主)确认
            }
        } else {
            if (isApplicant) {
                claim.setOwnerConfirm(true);  // 申请人(失主)确认
            } else {
                claim.setFinderConfirm(true); // 发布者(拾主)确认
            }
        }

        // 3. 检查双方是否都已确认
        boolean finderDone = Boolean.TRUE.equals(claim.getFinderConfirm());
        boolean ownerDone = Boolean.TRUE.equals(claim.getOwnerConfirm());

        Map<String, Object> data = new HashMap<>();
        
        if (finderDone && ownerDone) {
            // 双方都确认 -> 完成
            claim.setStatus("Success");
            claim.setUpdateTime(new Date());
            data.put("status", "Success");
            // 结算赏金
            settleBounty(claim, itemId);

            // 同步物品状态
            syncItemStatus(itemId, "Finished");
            //baseMapper.updateItemStatus(itemId, "Finished");

        } else {
            // 只有一方确认 -> 维持 Accepted
            claim.setStatus("Accepted");
            data.put("status", "Accepted");
            // 返回给前端更新按钮状态
            data.put("finderConfirmed", finderDone);
            data.put("ownerConfirmed", ownerDone);
        }

        this.updateById(claim);
        return data;
    }

@Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsDisputed(String itemId) {
        // 1. 找到当前的认领单
        Claims claim = getActiveClaim(itemId);
        
        // 容错：如果找不到活跃单，尝试查最新一条
        if (claim == null) {
            QueryWrapper<Claims> q = new QueryWrapper<>();
            q.eq("item_id", itemId).orderByDesc("create_time").last("LIMIT 1");
            claim = this.getOne(q);
        }

        if (claim == null) {
             System.err.println(">>> 未找到关联认领单，无法更新状态");
             return;
        }
        
        System.out.println(">>> [Service] 收到回调，更新认领单状态为 Disputed: " + claim.getClaimId());

        // 2. 更新认领单状态 (Claims表)
        claim.setStatus("Disputed");
        claim.setRejectReply(null); 
        claim.setUpdateTime(new Date());
        this.updateById(claim);
        
        // 3. 【关键】通知物品子系统更新物品状态 (Items表)
        // 之前状态没变就是因为缺了这一行！
        syncItemStatus(itemId, "Disputed"); 
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enforceArbitration(String claimId, String decision, String ticketId) {
        Claims claim = this.getById(claimId);
        if (claim == null) throw new RuntimeException("认领单不存在: " + claimId);

        if ("CANCEL_AND_REFUND".equals(decision)) {
            // 场景：强制取消并退款
            claim.setStatus("Rejected"); 
            claim.setRejectReply("管理员裁决(工单" + ticketId + ")：强制关闭并退回赏金");
            
            // 还需要把物品状态改回 Published
            syncItemStatus(claim.getItemId(), "Published");
            //baseMapper.updateItemStatus(claim.getItemId(), "Published");

        } else if ("FORCE_FINISH".equals(decision)) {
            // 场景：强制完成
            claim.setStatus("Success");
            claim.setFinderConfirm(true);
            claim.setOwnerConfirm(true);
            
            // 强制结算
            settleBounty(claim, claim.getItemId());
            // 强制完成物品
            syncItemStatus(claim.getItemId(), "Finished");
            //baseMapper.updateItemStatus(claim.getItemId(), "Finished");
        }
        
        claim.setUpdateTime(new Date());
        this.updateById(claim);
    }

    @Override
    public Map<String, Object> getClaimInfoForInternal(String claimId) {
        // 1. 查认领单
        Claims claim = this.getById(claimId);
        if (claim == null) throw new RuntimeException("认领单不存在");

        // 2. 查物品详情（为了获取发布者ID）
        Map<String, Object> itemInfo = baseMapper.selectItemDetail(claim.getItemId());
        String publisherId = (String) itemInfo.get("publisherID");
        if (publisherId == null) publisherId = (String) itemInfo.get("user_id"); // 兼容字段名

        // 3. 组装返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("itemId", claim.getItemId());
        data.put("publisherId", publisherId);
        return data;
    }

    @Override
    public Map<String, Object> getItemSnapshotForInternal(String itemId) {
        // 1. 复用现有的 Mapper 查询
        Map<String, Object> itemInfo = baseMapper.selectItemDetail(itemId);
        if (itemInfo == null) throw new RuntimeException("物品不存在");

        // 2. 提取需要的字段 (description, rewardPoints)
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("itemId", itemId);
        // Mapper 里 description 别名是 title，也可能是 description，看具体 SQL
        // 你的 SQL: i.description as title, i.description as description
        snapshot.put("description", itemInfo.get("description") != null ? itemInfo.get("description") : itemInfo.get("title"));
        snapshot.put("rewardPoints", itemInfo.get("rewardPoints"));

        return snapshot;
    }

    private Claims getActiveClaim(String itemId) {
        QueryWrapper<Claims> query = new QueryWrapper<>();
        query.eq("item_id", itemId)
             // 查 Applying 或 Accepted 的记录
             .in("status", "Applying", "Accepted")
             .orderByDesc("create_time")
             .last("LIMIT 1");
        return this.getOne(query);
    }

    // 封装一下调用逻辑
    private void syncItemStatus(String itemId, String newStatus) {
        // 构造通知参数
        Map<String, Object> req = new HashMap<>();
        req.put("itemID", itemId);
        req.put("newStatus", newStatus); // 注意：队友接口可能叫 status 或 newStatus，要确认
        req.put("operatorID", "SYSTEM");

        try {
            System.out.println(">>> [同步] 通知物品子系统变更状态: " + newStatus);
            
            // 1. 远程调用
            // 注意：请确保和队友确认过 URL 是 /sync 还是 /update
            String targetUrl = itemServiceUrl + "/internal/v1/item/status/sync";
            
            restTemplate.put(targetUrl, req);
            
            System.out.println(">>> [成功] 远程状态同步完成");

        } catch (Exception e) {
            System.err.println(">>> [失败] 远程同步失败 (" + e.getMessage() + ")，切换回本地模式");
            //baseMapper.updateItemStatus(itemId, newStatus);
        }
    }

    private void settleBounty(Claims claim, String itemId) {
        try {
            // 1. 查询物品详情获取真实金额和发布者
            Map<String, Object> itemInfo = baseMapper.selectItemDetail(itemId);
            if (itemInfo == null) return;
            
            // 获取悬赏金额
            Object rewardObj = itemInfo.get("rewardPoints");
            double amount = rewardObj != null ? Double.parseDouble(rewardObj.toString()) : 0.0;
            
            if (amount <= 0) return; // 无赏金不调用

            String publisherId = (String) itemInfo.get("publisherID"); 
            // 兼容旧数据
            if (publisherId == null) publisherId = (String) itemInfo.get("user_id");

            // 2. 构造符合接口规范的参数
            Map<String, Object> settleReq = new HashMap<>();
            settleReq.put("claimerID", claim.getApplicantId()); // 认领人（收款）
            settleReq.put("finderID", publisherId);             // 发布人（付款）
            
            settleReq.put("itemID", itemId);
            settleReq.put("amount", amount);

            String targetUrl = userServiceUrl + "/internal/v1/user/bounty/settle";
            System.out.println(">>> [Client] 调用结算接口: " + settleReq);
            
            // 发送 POST 请求
            Map response = restTemplate.postForObject(targetUrl, settleReq, Map.class);
            
            // 简单检查返回
            if (response != null && Integer.valueOf(200).equals(response.get("code"))) {
                System.out.println(">>> 结算成功，交易号: " + ((Map)response.get("data")).get("transactionID"));
            }

        } catch (Exception e) {
            System.err.println(">>> 结算调用失败: " + e.getMessage());
            // 根据业务需求，这里可能需要抛出异常触发回滚
            // throw new RuntimeException("结算失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancelClaim(String itemId, String userId) {
        // 只允许撤销 Applying 或 Disputed 的单子（按时间取最近一条）
        QueryWrapper<Claims> q = new QueryWrapper<>();
        q.eq("item_id", itemId)
         .in("status", "Applying", "Disputed")
         .orderByDesc("create_time")
         .last("LIMIT 1");
        Claims claim = this.getOne(q);

        if (claim == null) throw new RuntimeException("当前没有可撤销的申请");

        String status = claim.getStatus();

        if ("Applying".equals(status)) {
            if (!claim.getApplicantId().equals(userId)) {
                throw new RuntimeException("无权撤销他人的申请");
            }
            
            claim.setStatus("Cancelled");
            claim.setUpdateTime(new Date());
            this.updateById(claim);

            // 物品回滚为 Published
            syncItemStatus(itemId, "Published");
            //baseMapper.updateItemStatus(itemId, "Published");
            
            return Map.of("status", "Published", "msg", "申请已撤销");
        } 
        
        else if ("Disputed".equals(status)) {
            // 调用 DisputeService 撤销工单，并验证发起人是否匹配
            boolean revoked = disputeService.revokeTicket(claim.getClaimId(), userId);
            if (!revoked) throw new RuntimeException("无权撤销或工单不存在");

            // 状态回滚为 Accepted (交接中)
            claim.setStatus("Accepted");
            claim.setUpdateTime(new Date());
            
            this.updateById(claim);
            
            return Map.of("status", "Accepted", "msg", "维权已撤销，请继续交接");
        }

        throw new RuntimeException("当前状态不可撤销");
    }

    private Map<String, Object> error(String msg) {
        throw new RuntimeException(msg);
    }
}