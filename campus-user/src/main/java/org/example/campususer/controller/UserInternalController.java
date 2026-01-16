package org.example.campususer.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.example.campususer.dto.ApiResponse;
import org.example.campususer.dto.UserBatchQueryDTO;
import org.example.campususer.service.UsersService;
import org.example.campususer.utils.Result;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户子系统内部接口控制器
 * 专门处理面向其他子系统的内部调用（使用 /internal 前缀）
 * 供物品管理、认领审核等模块调用
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class UserInternalController {

    private final UsersService usersService;

    // ============================================================
    // 面向其他子系统的内部接口 (Internal API)
    // ============================================================

    /**
     * 冻结赏币（发布悬赏物品时调用）
     * POST /internal/v1/user/coin/freeze
     * 
     * @apiNote 供物品管理子系统调用
     */
    @PostMapping("/internal/v1/user/coin/freeze")
    public ApiResponse<Void> freezeCoin(@RequestBody FreezeCoinRequest request) {
        log.info("冻结赏币请求: userId={}, amount={}, itemId={}", 
                request.getUserId(), request.getAmount(), request.getItemId());
        usersService.freezeCoin(request.getUserId(), request.getAmount(), request.getItemId());
        return ApiResponse.success("赏币冻结成功");
    }

    /**
     * 解冻赏币（取消发布物品时调用）
     * POST /internal/v1/user/coin/unfreeze
     * 
     * @apiNote 供物品管理子系统调用
     */
    @PostMapping("/internal/v1/user/coin/unfreeze")
    public ApiResponse<Void> unfreezeCoin(@RequestBody UnfreezeCoinRequest request) {
        log.info("解冻赏币请求: userId={}, amount={}, itemId={}", 
                request.getUserId(), request.getAmount(), request.getItemId());
        usersService.unfreezeCoin(request.getUserId(), request.getAmount(), request.getItemId());
        return ApiResponse.success("赏币解冻成功");
    }

    /**
     * 结算赏金（物品认领成功后调用）
     * POST /internal/v1/user/bounty/settle
     * 
     * @apiNote 供认领审核子系统调用
     */
    @PostMapping("/internal/v1/user/bounty/settle")
    public ApiResponse<Void> settleBounty(@RequestBody SettleBountyRequest request) {
        log.info("结算赏金请求: claimerId={}, finderId={}, amount={}, itemId={}", 
                request.getClaimerId(), request.getFinderId(), request.getAmount(), request.getItemId());
        usersService.settleBounty(
                request.getClaimerId(), 
                request.getFinderId(), 
                request.getAmount(), 
                request.getItemId()
        );
        return ApiResponse.success("赏金结算成功");
    }

    /**
     * 账号处罚（虚假信息、恶意行为等）
     * PATCH /internal/v1/user/account/punish
     * 
     * @apiNote 供审核管理子系统调用
     */
    @PatchMapping("/internal/v1/user/account/punish")
    public ApiResponse<Void> punishUser(@RequestBody PunishUserRequest request) {
        log.info("惩罚用户请求: userId={}, reason={}, freezeAccount={}", 
                request.getUserId(), request.getReason(), request.getFreezeAccount());
        usersService.punishUser(
                request.getUserId(), 
                request.getReason(), 
                request.getFreezeAccount()
        );
        return ApiResponse.success("用户处罚已执行");
    }

    /**
     * 扣除用户赏币作为惩罚（可以扣到负数）
     * POST /internal/v1/user/coin/deduct-punish
     * 
     * @apiNote 供审核管理子系统调用，用于对违规用户进行赏币惩罚
     */
    @PostMapping("/internal/v1/user/coin/deduct-punish")
    public ApiResponse<Void> deductCoinAsPunishment(@RequestBody DeductCoinPunishRequest request) {
        log.info("惩罚扣币请求: userId={}, amount={}, reason={}, relatedItemId={}", 
                request.getUserId(), request.getAmount(), request.getReason(), request.getRelatedItemId());
        usersService.deductCoinAsPunishment(
                request.getUserId(), 
                request.getAmount(), 
                request.getReason(),
                request.getRelatedItemId()
        );
        return ApiResponse.success("惩罚扣币已执行");
    }

    /**
     * 内部接口：根据 ID 批量查询用户名称
     * POST /internal/v1/user/names/batch-query
     * 
     * @apiNote 供其他子系统调用
     */
    @PostMapping("/internal/v1/user/names/batch-query")
    public Result<Map<String, String>> batchQueryNames(@RequestBody UserBatchQueryDTO dto) {
        if (dto.getUserIds() == null || dto.getUserIds().isEmpty()) {
            return Result.success(new HashMap<>());
        }

        Map<String, String> nameMap = usersService.getNameMapByUserIds(dto.getUserIds());
        return Result.success(nameMap);
    }

    // ============================================================
    // 内部接口请求 DTO（子系统间调用）
    // ============================================================

    /**
     * 冻结赏币请求 DTO
     */
    @lombok.Data
    public static class FreezeCoinRequest {
        private String userId;
        private BigDecimal amount;
        private String itemId;
    }

    /**
     * 解冻赏币请求 DTO
     */
    @lombok.Data
    public static class UnfreezeCoinRequest {
        private String userId;
        private BigDecimal amount;
        private String itemId;
    }

    /**
     * 结算赏金请求 DTO
     */
    @lombok.Data
    public static class SettleBountyRequest {
        private String claimerId;
        private String finderId;
        private BigDecimal amount;
        private String itemId;
    }

    /**
     * 惩罚用户请求 DTO
     */
    @lombok.Data
    public static class PunishUserRequest {
        private String userId;
        private String reason;
        private Boolean freezeAccount;
    }

    /**
     * 惩罚扣币请求 DTO
     */
    @lombok.Data
    public static class DeductCoinPunishRequest {
        private String userId;          // 用户ID
        private BigDecimal amount;      // 扣除金额（正数）
        private String reason;          // 惩罚原因
        private String relatedItemId;   // 关联物品ID（可选）
    }
}
