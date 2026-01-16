package org.example.campususer.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.campususer.util.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器
 * 验证请求中的 JWT Token 是否有效
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    /**
     * 请求处理前的拦截
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器
     * @return true-放行，false-拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求路径
        String path = request.getRequestURI();
        log.debug("拦截器处理请求: {}", path);

        // 获取 Authorization 请求头
        String authHeader = request.getHeader("Authorization");

        // 检查 token 是否存在
        if (authHeader == null || authHeader.trim().isEmpty()) {
            log.warn("请求缺少 Authorization 头: {}", path);
            sendUnauthorizedResponse(response, "未提供认证信息，请登录");
            return false;
        }

        // 检查格式是否正确（Bearer token）
        if (!authHeader.startsWith("Bearer ")) {
            log.warn("Authorization 头格式错误: {}", authHeader);
            sendUnauthorizedResponse(response, "认证信息格式错误");
            return false;
        }

        // 提取 token
        String token = authHeader.substring(7);

        // 验证 token 是否有效
        if (!jwtUtil.validateToken(token)) {
            log.warn("Token 验证失败: {}", token);
            sendUnauthorizedResponse(response, "登录已过期或无效，请重新登录");
            return false;
        }

        // 解析 userId 并存储到请求属性中（供 Controller 使用）
        try {
            String userId = jwtUtil.getUserIdFromToken(token);
            request.setAttribute("userId", userId);
            log.debug("Token 验证通过，userId: {}", userId);
        } catch (Exception e) {
            log.error("解析 userId 失败", e);
            sendUnauthorizedResponse(response, "认证信息解析失败");
            return false;
        }

        // 放行
        return true;
    }

    /**
     * 发送 401 未授权响应
     * @param response 响应对象
     * @param message 错误消息
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json;charset=UTF-8");
        
        // 手动构建 JSON 响应，避免依赖 ObjectMapper
        String jsonResponse = String.format(
            "{\"code\":401,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
            message,
            System.currentTimeMillis()
        );
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
