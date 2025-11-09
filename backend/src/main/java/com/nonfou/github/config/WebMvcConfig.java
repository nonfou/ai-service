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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册管理后台安全拦截器
        registry.addInterceptor(adminSecurityInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/login"); // 登录接口不拦截
    }
}
