package org.example.campusaudit.sercurity;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.campusaudit.utils.JwtUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod hm)) return true;

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = jwtUtils.parseToken(token);

            // 提取并存入当前子系统的 ThreadLocal
            String userId = claims.get("sub", String.class);
            String role = claims.get("roleType", String.class);
            UserContext.set(userId, role);

            // 权限校验
            if (hm.hasMethodAnnotation(AdminOnly.class) && !UserContext.isAdmin()) {
                response.setStatus(403);
                return false;
            }
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        UserContext.clear();
    }
}
