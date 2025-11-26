package com.tripdog.config;

import com.tripdog.hook.filter.ReqFilter;
import com.tripdog.hook.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080,http://localhost:5173,http://localhost:4200,http://127.0.0.1:3000,http://127.0.0.1:8080,http://127.0.0.1:5173,http://127.0.0.1:4200,https://trip-doge-frontend.zeabur.app}")
    private String allowedOrigins;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")  // 拦截所有的请求
                .excludePathPatterns(
                        "/user/register",          // 注册
                        "/user/login",             // 登录
                        "/user/sendEmail",         // 发送验证码
                        "/user/email/send",        // 发送验证码（新路径）
                        "/roles/list",             // 角色列表（可能需要在未登录时访问）
                        "/api-docs/**",            // Swagger API 文档
                        "/swagger-ui/**",          // Swagger UI 资源
                        "/swagger-ui.html",        // Swagger UI 首页
                        "/actuator",               // 监控配置
                        "/actuator/**",
                        "/error"                   // 错误页面
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public FilterRegistrationBean<ReqFilter> reqFilter() {
        FilterRegistrationBean<ReqFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new ReqFilter());
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.setOrder(1);
        return filterRegistrationBean;
    }
}
