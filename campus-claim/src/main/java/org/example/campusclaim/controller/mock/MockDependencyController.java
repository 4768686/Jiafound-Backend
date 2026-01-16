package org.example.campusclaim.controller.mock;

import org.example.campusclaim.entity.CoinLog;
import org.example.campusclaim.mapper.CoinLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * 模拟外部子系统（用户、物品）的接口
 * 作用：让 ClaimServiceImpl 发出的 HTTP 请求有地方接收
 */
@RestController
public class MockDependencyController {

    @Autowired
    private CoinLogMapper coinLogMapper;

    // 1. [模拟] 物品认领与结算 -> 用户管理与激励 (赏币结算)
    // 请求路径：/internal/v1/user/bounty/settle
    @PostMapping("/internal/v1/user/bounty/settle")
    public Map<String, Object> mockSettle(@RequestBody Map<String, Object> payload) {
        System.out.println(">>> [Mock用户系统] 收到结算请求: " + payload);
        
        // 模拟写入日志逻辑 (之前已经实现过)
        String claimerId = (String) payload.get("claimerID");
        String finderId = (String) payload.get("finderID");
        String itemId = (String) payload.get("itemID");
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());

        // 写入模拟日志 (为了让你看数据库有反应)
        createMockLog(finderId, amount.negate(), "SETTLE", itemId);
        createMockLog(claimerId, amount, "REWARD", itemId);

        return Map.of(
            "code", 200, 
            "message", "结算成功",
            "data", Map.of("transactionID", "LOG_" + System.currentTimeMillis())
        );
    }

    // 2. [模拟] 物品认领与结算 -> 智能检索与发布 (同步状态)
    // 请求路径：/internal/v1/item/status/sync
    @PutMapping("/internal/v1/item/status/sync")
    public Map<String, Object> mockSync(@RequestBody Map<String, Object> payload) {
        System.out.println(">>> [Mock物品系统] 收到状态同步请求: " + payload);
        // 实际逻辑中，物品子系统会去改 items 表
        // 但因为我们在同一个库，ClaimsService 已经自己改了，这里只负责返回“成功”信号
        return Map.of("code", 200, "message", "物品状态已同步");
    }

    private void createMockLog(String userId, BigDecimal amount, String type, String itemId) {
        CoinLog log = new CoinLog();
        log.setLogId(UUID.randomUUID().toString().replace("-", ""));
        log.setUserId(userId);
        log.setAmount(amount);
        log.setType(type);
        log.setRelatedItemId(itemId);
        log.setCreateTime(new Date());
        coinLogMapper.insert(log);
    }
}