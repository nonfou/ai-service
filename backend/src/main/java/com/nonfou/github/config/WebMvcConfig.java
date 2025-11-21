package com.nonfou.github.config;

import com.nonfou.github.interceptor.AdminSecurityInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AdminSecurityInterceptor adminSecurityInterceptor;

    @Autowired
    private CsrfInterceptor csrfInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册CSRF拦截器 (优先级最高)
        // 对所有修改数据的请求(POST, PUT, DELETE, PATCH)进行CSRF Token验证
        registry.addInterceptor(csrfInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",        // 登录接口不需要CSRF Token
                        "/api/auth/send-code",    // 发送验证码接口不需要CSRF Token
                        "/api/auth/csrf-token",   // CSRF Token获取接口不需要验证
                        "/api/auth/status",       // 状态检查接口不需要验证(GET请求)
                        "/api/auth/test",         // 测试接口不需要验证
                        "/api/admin/login"        // 管理后台登录接口不需要验证
                );

        // 注册管理后台安全拦截器
        registry.addInterceptor(adminSecurityInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/login"); // 登录接口不拦截
    }
}
