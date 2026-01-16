package org.example.campususer.controller;

import org.example.campususer.dto.ApiResponse;
import org.example.campususer.dto.BindRequest;
import org.example.campususer.dto.CoinLogVO;
import org.example.campususer.dto.LoginRequest;
import org.example.campususer.dto.LoginResponse;
import org.example.campususer.dto.PageResponse;
import org.example.campususer.dto.RechargeRequest;
import org.example.campususer.dto.SendCodeRequest;
import org.example.campususer.dto.UpdateProfileRequest;
import org.example.campususer.dto.UserProfileVO;
import org.example.campususer.dto.WithdrawRequest;
import org.example.campususer.service.CoinLogsService;
import org.example.campususer.service.UsersService;
import org.example.campususer.util.JwtUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户与激励子系统前端接口控制器
 * 面向前端小程序的 Client API
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UsersService usersService;
    private final CoinLogsService coinLogsService;
    private final JwtUtil jwtUtil;

    // ============================================================
    // 面向前端小程序 (Client API)
    // ============================================================

    /**
     * 微信登录
     * POST /api/v1/user/login
     */
    @PostMapping("/api/v1/user/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Validated LoginRequest request) {
        log.info("用户登录请求: code={}", request.getCode());
        LoginResponse response = usersService.login(request);
        return ApiResponse.success("登录成功", response);
    }

    /**
     * 发送验证码
     * POST /api/v1/auth/send-code
     */
    @PostMapping("/api/v1/auth/send-code")
    public ApiResponse<Void> sendVerifyCode(@RequestBody @Validated SendCodeRequest request) {
        log.info("发送验证码请求: email={}, type={}", request.getEmail(), request.getType());
        usersService.sendVerifyCode(request.getEmail());
        return ApiResponse.success("验证码已发送至邮箱");
    }

    /**
     * 实名认证（绑定学号、姓名、邮箱）
     * POST /api/v1/user/bind
     */
    @PostMapping("/api/v1/user/bind")
    public ApiResponse<Void> bindIdentity(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Validated BindRequest request) {
        String userId = jwtUtil.getUserIdFromHeader(authHeader);
        log.info("用户实名认证请求: userId={}, studentId={}", userId, request.getStudentID());
        usersService.bindIdentity(userId, request);
        return ApiResponse.success("实名认证成功");
    }

    /**
     * 获取个人信息
     * GET /api/v1/user/profile
     */
    @GetMapping("/api/v1/user/profile")
    public ApiResponse<UserProfileVO> getUserProfile(@RequestHeader("Authorization") String authHeader) {
        String userId = jwtUtil.getUserIdFromHeader(authHeader);
        log.info("获取用户信息请求: userId={}", userId);
        UserProfileVO profile = usersService.getUserProfile(userId);
        return ApiResponse.success(profile);
    }

    /**
     * 修改个人信息
     * PUT /api/v1/user/profile
     */
    @PutMapping("/api/v1/user/profile")
    public ApiResponse<Void> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Validated UpdateProfileRequest request) {
        String userId = jwtUtil.getUserIdFromHeader(authHeader);
        log.info("修改用户信息请求: userId={}", userId);
        usersService.updateProfile(userId, request);
        return ApiResponse.success("个人信息修改成功");
    }

    /**
     * 上传头像
     * POST /api/v1/user/avatar/upload
     * 
     * @param file 头像文件
     * @param authHeader JWT Token
     * @return 图片访问URL
     */
    @PostMapping("/api/v1/user/avatar/upload")
    public ApiResponse<String> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader) {
        // 1. 校验用户登录状态
        String userId = jwtUtil.getUserIdFromHeader(authHeader);
        log.info("上传头像请求: userId={}, filename={}, size={}", 
                userId, file.getOriginalFilename(), file.getSize());
        
        // 2. 调用 Service 上传图片
        String imageUrl = usersService.uploadAvatar(file);
        
        log.info("头像上传成功: userId={}, url={}", userId, imageUrl);
        return ApiResponse.success("头像上传成功", imageUrl);
    }

    /**
     * 充值（Mock 模式）
     * POST /api/v1/coin/recharge
     */
    @PostMapping("/api/v1/coin/recharge")
    public ApiResponse<Void> recharge(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Validated RechargeRequest request) {
        String userId = jwtUtil.getUserIdFromHeader(authHeader);
        log.info("充值请求: userId={}, amount={}", userId, request.getAmount());
        coinLogsService.recharge(userId, request.getAmount());
        return ApiResponse.success("充值成功");
    }

    /**
     * 提现（Mock 模式）
     * POST /api/v1/coin/withdraw
     */
    @PostMapping("/api/v1/coin/withdraw")
    public ApiResponse<Void> withdraw(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Validated WithdrawRequest request) {
        String userId = jwtUtil.getUserIdFromHeader(authHeader);
        log.info("提现请求: userId={}, coinAmount={}", userId, request.getCoinAmount());
        coinLogsService.withdraw(userId, request.getCoinAmount());
        return ApiResponse.success("提现申请已提交");
    }

    /**
     * 获取赏币流水
     * GET /api/v1/coin/logs?page=1&size=10
     */
    @GetMapping("/api/v1/coin/logs")
    public ApiResponse<PageResponse<CoinLogVO>> getCoinLogs(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userId = jwtUtil.getUserIdFromHeader(authHeader);
        log.info("获取流水请求: userId={}, page={}, size={}", userId, page, size);
        PageResponse<CoinLogVO> logs = coinLogsService.getCoinLogs(userId, page, size);
        return ApiResponse.success(logs);
    }
}
