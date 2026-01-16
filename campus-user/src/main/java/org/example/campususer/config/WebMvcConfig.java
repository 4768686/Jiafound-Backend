package org.example.campususer.config;

import org.example.campususer.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * Web MVC 配置
 * 注册拦截器和配置路径规则
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    /**
     * 注册拦截器
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                // 拦截所有请求
                .addPathPatterns("/**")
                
                // 放行不需要认证的路径
                .excludePathPatterns(
                        // 用户登录接口
                        "/api/v1/user/login",
                        
                        // 发送验证码接口
                        "/api/v1/auth/send-code",
                        
                        // 内部接口（跨服务调用）
                        "/internal/**",
                        
                        // Swagger 文档相关路径
                        "/swagger-resources/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        
                        // 静态资源
                        "/static/**",
                        "/public/**",
                        // 注意：已迁移到Azure云存储，不再需要本地/uploads/**路径
                        
                        // 健康检查
                        "/actuator/**",
                        
                        // 错误页面
                        "/error"
                );
    }
}
