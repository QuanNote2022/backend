package com.mineral.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类
 * 配置安全过滤器链和认证管理器
 */
@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityFilterConfig {

    /**
     * JWT 认证过滤器
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 配置安全过滤器链
     * 
     * @param http HttpSecurity 对象
     * @return SecurityFilterChain 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（使用 JWT 不需要 CSRF 保护）
            .csrf().disable()
            // 配置会话管理为无状态（不使用 Session）
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // 配置请求授权规则
            .authorizeHttpRequests()
                // 允许匿名访问认证相关接口
                .antMatchers("/auth/**").permitAll()
                // 允许匿名访问矿物信息查询接口
                .antMatchers("/mineral/info/**").permitAll()
                // 允许匿名访问矿物分类接口
                .antMatchers("/mineral/categories").permitAll()
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            .and()
            // 添加 JWT 认证过滤器到用户名密码过滤器之前
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 配置认证管理器
     * 
     * @param config 认证配置
     * @return AuthenticationManager 认证管理器
     * @throws Exception 配置异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
