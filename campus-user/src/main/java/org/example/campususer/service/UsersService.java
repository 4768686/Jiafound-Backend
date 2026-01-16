package org.example.campususer.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.example.campususer.dto.BindRequest;
import org.example.campususer.dto.LoginRequest;
import org.example.campususer.dto.LoginResponse;
import org.example.campususer.dto.UpdateProfileRequest;
import org.example.campususer.dto.UserProfileVO;
import org.example.campususer.entity.Users;

import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 31830
* @description 针对表【users(用户基础信息表)】的数据库操作Service
* @createDate 2026-01-06 21:20:36
*/
public interface UsersService extends IService<Users> {
    //根据UserId批量获取对应昵称
    Map<String, String> getNameMapByUserIds(List<String> userIds);

    /**
     * 微信登录
     * @param req 登录请求（包含微信code和用户信息）
     * @return 登录响应（包含token和用户信息）
     */
    LoginResponse login(LoginRequest req);

    /**
     * 发送验证码到邮箱
     * @param email 邮箱地址
     */
    void sendVerifyCode(String email);

    /**
     * 绑定用户身份（实名认证）
     * @param userId 用户ID
     * @param req 绑定请求（包含学号、姓名、邮箱、验证码）
     */
    void bindIdentity(String userId, BindRequest req);

    /**
     * 获取用户详细信息
     * @param userId 用户ID
     * @return 用户信息
     */
    UserProfileVO getUserProfile(String userId);

    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param req 更新请求
     */
    void updateProfile(String userId, UpdateProfileRequest req);

    /**
     * 上传用户头像
     * @param file 头像文件
     * @return 头像访问URL
     */
    String uploadAvatar(org.springframework.web.multipart.MultipartFile file);

    // ============================================================
    // 子系统间接口（供物品管理、审核等模块调用）
    // ============================================================

    /**
     * 冻结用户赏币（发布悬赏时调用）
     * @param userId 用户ID
     * @param amount 冻结金额
     * @param itemId 关联物品ID
     */
    void freezeCoin(String userId, BigDecimal amount, String itemId);

    /**
     * 解冻用户赏币（取消发布悬赏时调用）
     * @param userId 用户ID
     * @param amount 解冻金额
     * @param itemId 关联物品ID
     */
    void unfreezeCoin(String userId, BigDecimal amount, String itemId);

    /**
     * 结算赏金（物品认领成功后调用）
     * @param claimerId 失主ID（支付方）
     * @param finderId 拾主ID（收款方）
     * @param amount 赏金金额
     * @param itemId 关联物品ID
     */
    void settleBounty(String claimerId, String finderId, BigDecimal amount, String itemId);

    /**
     * 惩罚用户（虚假信息、恶意行为等）
     * @param userId 用户ID
     * @param reason 惩罚原因
     * @param freezeAccount 是否冻结账号
     */
    void punishUser(String userId, String reason, boolean freezeAccount);

    /**
     * 扣除用户赏币作为惩罚（可以扣到负数）
     * @param userId 用户ID
     * @param amount 扣除金额（正数）
     * @param reason 惩罚原因
     * @param relatedItemId 关联物品ID（可选）
     */
    void deductCoinAsPunishment(String userId, BigDecimal amount, String reason, String relatedItemId);
}
