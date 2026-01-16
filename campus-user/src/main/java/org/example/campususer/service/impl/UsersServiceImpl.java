package org.example.campususer.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.example.campususer.config.JwtConfig;
import org.example.campususer.config.WeChatConfig;
import org.example.campususer.dto.BindRequest;
import org.example.campususer.dto.LoginRequest;
import org.example.campususer.dto.LoginResponse;
import org.example.campususer.dto.UpdateProfileRequest;
import org.example.campususer.dto.UserProfileVO;
import org.example.campususer.entity.Users;
import org.example.campususer.mapper.UsersMapper;
import org.example.campususer.service.CoinLogsService;
import org.example.campususer.service.MailService;
import org.example.campususer.service.UsersService;
import org.example.campususer.utils.AzureBlobUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
* @author 31830
* @description 针对表【users(用户基础信息表)】的数据库操作Service实现
* @createDate 2026-01-06 21:20:36
*/
@Slf4j
@Service
@RequiredArgsConstructor
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users>
    implements UsersService{
    
    private final WeChatConfig weChatConfig;
    private final JwtConfig jwtConfig;
    private final MailService mailService;
    private final StringRedisTemplate redisTemplate;
    private final CoinLogsService coinLogsService;
    private final AzureBlobUtils azureBlobUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest req) {
        String code = req.getCode();
        
        // 1. 调用微信接口获取 openid
        String url = weChatConfig.getCode2SessionUrl(code);
        String response;
        try {
            response = HttpUtil.get(url);
            log.info("微信登录响应: {}", response);
        } catch (Exception e) {
            log.error("调用微信接口失败", e);
            throw new RuntimeException("微信登录失败，请稍后重试");
        }
        
        JSONObject jsonObject = JSONUtil.parseObj(response);
        
        // 检查是否有错误
        if (jsonObject.containsKey("errcode")) {
            Integer errcode = jsonObject.getInt("errcode");
            String errmsg = jsonObject.getStr("errmsg");
            log.error("微信接口返回错误: errcode={}, errmsg={}", errcode, errmsg);
            throw new RuntimeException("微信登录失败: " + errmsg);
        }
        
        String openid = jsonObject.getStr("openid");
        if (openid == null || openid.isEmpty()) {
            throw new RuntimeException("获取openid失败");
        }
        
        // 2. 查询用户是否存在
        LambdaQueryWrapper<Users> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Users::getOpenId, openid);
        Users user = this.getOne(queryWrapper);
        
        boolean isFirstLogin = false;
        boolean needCertification = false;
        
        if (user == null) {
            // 新用户，自动注册
            user = new Users();
            user.setUserId(UUID.randomUUID().toString().replace("-", ""));
            user.setOpenId(openid);
            
            // 从微信用户信息中提取昵称和头像
            if (req.getUserInfo() != null) {
                Object nickname = req.getUserInfo().get("nickName");
                Object avatar = req.getUserInfo().get("avatarUrl");
                
                if (nickname != null) {
                    user.setNickname(nickname.toString());
                }
                if (avatar != null) {
                    user.setAvatarUrl(avatar.toString());
                }
            }
            
            user.setIsCertified(0);
            user.setRoleType("User");
            user.setAccountStatus("Normal");
            user.setCoinBalance(BigDecimal.ZERO);
            user.setFrozenBalance(BigDecimal.ZERO);
            user.setCreateTime(new Date());
            user.setUpdateTime(new Date());
            
            this.save(user);
            isFirstLogin = true;
            needCertification = true;
            
            log.info("新用户注册成功 userId={}, openid={}", user.getUserId(), openid);
        } else {
            // 老用户，更新最后登录时间
            user.setUpdateTime(new Date());
            this.updateById(user);
            
            // 检查是否需要实名认证
            needCertification = user.getIsCertified() == null || user.getIsCertified() == 0;
            
            log.info("用户登录成功: userId={}, openid={}", user.getUserId(), openid);
        }
        
        // 3. 生成 JWT Token
        String token = generateToken(user.getUserId(), user.getRoleType());
        
        // 4. 构造响应
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(token);
        loginResponse.setIsFirstLogin(isFirstLogin);
        loginResponse.setNeedCertification(needCertification);
        
        // 构造用户信息
        UserProfileVO userProfileVO = convertToProfileVO(user);
        loginResponse.setUserInfo(userProfileVO);
        
        return loginResponse;
    }

    @Override
    public void sendVerifyCode(String email) {
        // 1. 生成 6 位随机验证码
        String code = RandomUtil.randomNumbers(6);
        
        // 2. 先将验证码存入 Redis（5分钟有效期）
        String redisKey = "verify:email:" + email;
        redisTemplate.opsForValue().set(redisKey, code, 5, TimeUnit.MINUTES);
        log.info("验证码已生成 email={}, code={}", email, code);
        
        // 3. 异步发送邮件（不阻塞请求）
        mailService.sendVerifyCodeAsync(email, code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindIdentity(String userId, BindRequest req) {
        // 1. 校验 Redis 中的验证码
        String redisKey = "verify:email:" + req.getEmail();
        String cachedCode = redisTemplate.opsForValue().get(redisKey);
        
        if (cachedCode == null) {
            throw new RuntimeException("验证码已过期，请重新获取");
        }
        
        if (!cachedCode.equals(req.getVerifyCode())) {
            throw new RuntimeException("验证码错误");
        }
        
        // 2. 查询用户
        Users user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 3. 检查学号是否已被绑定
        LambdaQueryWrapper<Users> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Users::getStudentId, req.getStudentID());
        queryWrapper.ne(Users::getUserId, userId);
        long count = this.count(queryWrapper);
        
        if (count > 0) {
            throw new RuntimeException("该学号已被其他用户绑定");
        }
        
        // 4. 更新用户信息
        user.setStudentId(req.getStudentID());
        user.setRealName(req.getRealName());
        user.setEmail(req.getEmail());
        user.setIsCertified(1);
        user.setUpdateTime(new Date());
        
        boolean updateSuccess = this.updateById(user);
        log.info("用户身份绑定 updateSuccess={}, userId={}, studentId={}, realName={}, email={}", 
            updateSuccess, userId, req.getStudentID(), req.getRealName(), req.getEmail());
        
        if (!updateSuccess) {
            throw new RuntimeException("更新用户信息失败");
        }
        
        // 5. 删除已使用的验证码
        redisTemplate.delete(redisKey);
        
        log.info("用户身份绑定成功: userId={}, studentId={}", userId, req.getStudentID());
    }

    /**
     * 生成 JWT Token
     */
    private String generateToken(String userId, String roleType) {
        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getExpiration());
        
        return Jwts.builder()
                .subject(userId)
                .claim("roleType", roleType)  // 添加角色信息到JWT
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * 将Users 实体转换为 UserProfileVO
     */
    private UserProfileVO convertToProfileVO(Users user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(user.getUserId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        

        // 脱敏处理
        if (user.getStudentId() != null && !user.getStudentId().isEmpty()) {
            vo.setStudentId(maskStudentId(user.getStudentId()));
            log.debug("学号脱敏后: {}", vo.getStudentId());
        } else {
            log.warn("用户 {} 的学号为空或null", user.getUserId());
        }
        
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            vo.setEmail(maskEmail(user.getEmail()));
        }
        
        vo.setIsCertified(user.getIsCertified() != null && user.getIsCertified() == 1);
        vo.setRoleType(user.getRoleType());
        vo.setAccountStatus(user.getAccountStatus());
        vo.setCoinBalance(user.getCoinBalance());
        vo.setFrozenBalance(user.getFrozenBalance());
        
        if (user.getCreateTime() != null) {
            vo.setCreateTime(user.getCreateTime().toString());
        }
        if (user.getUpdateTime() != null) {
            vo.setUpdateTime(user.getUpdateTime().toString());
        }
        
        return vo;
    }

    /**
     * 学号脱敏：显示前3位和后3位
     */
    private String maskStudentId(String studentId) {
        if (studentId.length() <= 5) {
            return studentId;
        }
        return studentId.substring(0, 3) + "*****" + studentId.substring(studentId.length() - 2);
    }

    /**
     * 邮箱脱敏：显示前3位和@后面的部分
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 3) {
            return email;
        }
        return email.substring(0, 3) + "***" + email.substring(atIndex);
    }

    @Override
    public UserProfileVO getUserProfile(String userId) {
        Users user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        log.info("获取用户信息: userId={}, studentId={}, isCertified={}", 
            user.getUserId(), user.getStudentId(), user.getIsCertified());
        return convertToProfileVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(String userId, UpdateProfileRequest req) {
        // 1. 查询用户
        Users user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        boolean needUpdate = false;

        // 2. 更新昵称
        if (req.getNickname() != null && !req.getNickname().isEmpty()) {
            user.setNickname(req.getNickname());
            needUpdate = true;
        }

        // 3. 更新头像
        if (req.getAvatarUrl() != null && !req.getAvatarUrl().isEmpty()) {
            user.setAvatarUrl(req.getAvatarUrl());
            needUpdate = true;
        }

        // 4. 更新邮箱（需要验证码）
        if (req.getEmail() != null && !req.getEmail().isEmpty()) {
            if (req.getVerifyCode() == null || req.getVerifyCode().isEmpty()) {
                throw new RuntimeException("修改邮箱需要验证码");
            }

            // 校验验证码
            String redisKey = "verify:email:" + req.getEmail();
            String cachedCode = redisTemplate.opsForValue().get(redisKey);

            if (cachedCode == null) {
                throw new RuntimeException("验证码已过期，请重新获取");
            }

            if (!cachedCode.equals(req.getVerifyCode())) {
                throw new RuntimeException("验证码错误");
            }

            user.setEmail(req.getEmail());
            needUpdate = true;

            // 删除已使用的验证码
            redisTemplate.delete(redisKey);
        }

        // 5. 执行更新
        if (needUpdate) {
            user.setUpdateTime(new Date());
            this.updateById(user);
            log.info("用户信息更新成功: userId={}", userId);
        }
    }

    // ============================================================
    // 子系统间接口实现
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freezeCoin(String userId, BigDecimal amount, String itemId) {
        // 1. 查询用户
        Users user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 检查可用余额
        if (user.getCoinBalance().compareTo(amount) < 0) {
            throw new RuntimeException("可用余额不足，当前余额：" + user.getCoinBalance() + " 赏币");
        }

        // 3. 扣减 coin_balance，增加frozen_balance
        BigDecimal newBalance = user.getCoinBalance().subtract(amount);
        BigDecimal newFrozenBalance = user.getFrozenBalance().add(amount);
        
        user.setCoinBalance(newBalance);
        user.setFrozenBalance(newFrozenBalance);
        user.setUpdateTime(new Date());
        this.updateById(user);

        // 4. 记录流水
        org.example.campususer.entity.CoinLogs coinLog = new org.example.campususer.entity.CoinLogs();
        coinLog.setLogId(UUID.randomUUID().toString().replace("-", ""));
        coinLog.setUserId(userId);
        coinLog.setAmount(amount.negate()); // 负数表示扣减
        coinLog.setType("FREEZE");
        coinLog.setRelatedItemId(itemId);
        coinLog.setCreateTime(new Date());
        coinLogsService.save(coinLog);

        log.info("冻结赏币成功: userId={}, amount={}, itemId={}, newBalance={}, newFrozenBalance={}", 
                userId, amount, itemId, newBalance, newFrozenBalance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeCoin(String userId, BigDecimal amount, String itemId) {
        // 1. 查询用户
        Users user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 检查冻结余额是否充足
        if (user.getFrozenBalance().compareTo(amount) < 0) {
            throw new RuntimeException("冻结余额不足，当前冻结余额： " + user.getFrozenBalance() + " 赏币");
        }

        // 3. 扣减 frozen_balance，增加coin_balance（恢复可用余额）
        BigDecimal newFrozenBalance = user.getFrozenBalance().subtract(amount);
        BigDecimal newBalance = user.getCoinBalance().add(amount);
        
        user.setFrozenBalance(newFrozenBalance);
        user.setCoinBalance(newBalance);
        user.setUpdateTime(new Date());
        this.updateById(user);

        // 4. 记录流水
        org.example.campususer.entity.CoinLogs coinLog = new org.example.campususer.entity.CoinLogs();
        coinLog.setLogId(UUID.randomUUID().toString().replace("-", ""));
        coinLog.setUserId(userId);
        coinLog.setAmount(amount); // 正数表示解冻返还
        coinLog.setType("UNFREEZE");
        coinLog.setRelatedItemId(itemId);
        coinLog.setCreateTime(new Date());
        coinLogsService.save(coinLog);

        log.info("解冻赏币成功: userId={}, amount={}, itemId={}, newBalance={}, newFrozenBalance={}", 
                userId, amount, itemId, newBalance, newFrozenBalance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleBounty(String claimerId, String finderId, BigDecimal amount, String itemId) {
        // 1. 查询失主和拾主
        Users claimer = this.getById(claimerId);
        Users finder = this.getById(finderId);
        
        if (claimer == null) {
            throw new RuntimeException("失主用户不存在");
        }
        if (finder == null) {
            throw new RuntimeException("拾主用户不存在");
        }

        // 2. 检查失主冻结余额是否充足
        if (claimer.getFrozenBalance().compareTo(amount) < 0) {
            throw new RuntimeException("失主冻结余额不足，当前冻结余额：" + claimer.getFrozenBalance() + " 赏币");
        }

        // 3. 失主：扣除frozen_balance
        BigDecimal claimerNewFrozenBalance = claimer.getFrozenBalance().subtract(amount);
        claimer.setFrozenBalance(claimerNewFrozenBalance);
        claimer.setUpdateTime(new Date());
        this.updateById(claimer);

        // 4. 拾主：增加coin_balance
        BigDecimal finderNewBalance = finder.getCoinBalance().add(amount);
        finder.setCoinBalance(finderNewBalance);
        finder.setUpdateTime(new Date());
        this.updateById(finder);

        // 5. 记录失主的流水（支出）
        org.example.campususer.entity.CoinLogs claimerLog = new org.example.campususer.entity.CoinLogs();
        claimerLog.setLogId(UUID.randomUUID().toString().replace("-", ""));
        claimerLog.setUserId(claimerId);
        claimerLog.setAmount(amount.negate()); // 负数表示支出
        claimerLog.setType("SETTLE");
        claimerLog.setRelatedItemId(itemId);
        claimerLog.setCreateTime(new Date());
        coinLogsService.save(claimerLog);

        // 6. 记录拾主的流水（收入)
        org.example.campususer.entity.CoinLogs finderLog = new org.example.campususer.entity.CoinLogs();
        finderLog.setLogId(UUID.randomUUID().toString().replace("-", ""));
        finderLog.setUserId(finderId);
        finderLog.setAmount(amount); // 正数表示收入
        finderLog.setType("REWARD");
        finderLog.setRelatedItemId(itemId);
        finderLog.setCreateTime(new Date());
        coinLogsService.save(finderLog);

        log.info("结算赏金成功: claimerId={}, finderId={}, amount={}, itemId={}", 
                claimerId, finderId, amount, itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void punishUser(String userId, String reason, boolean freezeAccount) {
        // 1. 查询用户
        Users user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 冻结账号状态
        if (freezeAccount) {
            user.setAccountStatus("Frozen");
            log.info("账号已冻结 userId={}, reason={}", userId, reason);
        }

        // 3. 更新用户状态（可选：后续可扩展信用分系统/punishment_logs 表）
        user.setUpdateTime(new Date());
        this.updateById(user);

        log.info("惩罚用户成功: userId={}, reason={}, freezeAccount={}", userId, reason, freezeAccount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductCoinAsPunishment(String userId, BigDecimal amount, String reason, String relatedItemId) {
        // 1. 验证参数
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("扣除金额必须大于0");
        }

        // 2. 使用原子SQL直接更新余额（避免查询+更新两步，提升性能）
        int affected = baseMapper.deductCoinBalance(userId, amount);
        if (affected == 0) {
            throw new RuntimeException("用户不存在");
        }

        // 3. 记录流水（优化UUID生成）
        Date now = new Date();
        String logId = UUID.randomUUID().toString().replaceAll("-", "");
        org.example.campususer.entity.CoinLogs coinLog = new org.example.campususer.entity.CoinLogs();
        coinLog.setLogId(logId);
        coinLog.setUserId(userId);
        coinLog.setAmount(amount.negate()); // 负数表示扣减
        coinLog.setType("PUNISH");
        coinLog.setRelatedItemId(relatedItemId);
        coinLog.setCreateTime(now);
        coinLogsService.save(coinLog);

        // 4. 精简日志输出（仅debug模式记录详细信息）
        if (log.isDebugEnabled()) {
            log.debug("惩罚扣币: userId={}, amount={}, reason={}", userId, amount, reason);
        }
    }

    /**
     * 上传用户头像到 Azure 云存储
     * @param file 头像文件
     * @return 头像访问URL
     */
    @Override
    public String uploadAvatar(MultipartFile file) {
        // 1. 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        
        // 2. 获取原始文件名和后缀
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new RuntimeException("文件名格式不正确");
        }
        
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        
        // 3. 校验文件后缀（仅允许 jpg, png, jpeg, gif, webp）
        if (!".jpg".equals(suffix) && !".png".equals(suffix) 
            && !".jpeg".equals(suffix) && !".gif".equals(suffix) && !".webp".equals(suffix)) {
            throw new RuntimeException("仅支持 jpg、png、jpeg、gif、webp 格式的图片");
        }
        
        // 4. 校验文件大小（最大5MB）
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new RuntimeException("文件大小不能超过5MB");
        }
        
        // 5. 上传到 Azure Blob Storage
        try {
            String avatarUrl = azureBlobUtils.uploadFile(file);
            log.info("头像上传成功，Azure URL: {}", avatarUrl);
            return avatarUrl;
        } catch (IOException e) {
            log.error("头像上传到 Azure 失败", e);
            throw new RuntimeException("头像上传失败: " + e.getMessage());
        }
    }
    
    @Override
    public Map<String, String> getNameMapByUserIds(List<String> userIds) {
        // 1. 使用 MyBatis-Plus 批量查询
        // 优化点：只查询 id 和 nickname 两个字段，减少数据库 IO 和内存占用
        List<Users> users = this.list(new LambdaQueryWrapper<Users>()
                .select(Users::getUserId, Users::getNickname)
                .in(Users::getUserId, userIds));

        // 2. 将结果 List 转换为 Map<Id, Name>
        return users.stream().collect(Collectors.toMap(
                Users::getUserId,
                Users::getNickname,
                (existing, replacement) -> existing // 防止 ID 重复导致报错
        ));
    }

}




