package com.mineral.config;

import com.mineral.common.BusinessException;
import com.mineral.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT 认证过滤器
 * 拦截请求，验证 JWT Token，设置用户认证信息
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * JWT 工具类
     */
    private final JwtUtil jwtUtil;

    /**
     * 过滤请求，验证 JWT Token
     * 
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        // 从请求中获取 Token
        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token)) {
            try {
                // 验证 Token 是否有效
                if (jwtUtil.validateToken(token)) {
                    // 从 Token 中获取用户信息
                    String userId = jwtUtil.getUserIdFromToken(token);
                    String username = jwtUtil.getUsernameFromToken(token);

                    // 创建认证对象
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 设置 Spring Security 上下文
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    // 将用户 ID 设置到请求属性中，供后续使用
                    request.setAttribute("userId", userId);
                }
            } catch (Exception e) {
                // 记录 JWT 认证错误
                log.error("JWT authentication error: {}", e.getMessage());
                // 抛出业务异常
                throw new BusinessException(ErrorCode.TOKEN_INVALID, "Token 无效或已过期");
            }
        }

        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从 HTTP 请求头中获取 JWT Token
     * 
     * @param request HTTP 请求
     * @return Token 字符串；不存在返回 null
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 获取 Authorization 头
        String bearerToken = request.getHeader("Authorization");
        // 检查是否以 "Bearer " 开头
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // 去掉 "Bearer " 前缀，返回 Token
            return bearerToken.substring(7);
        }
        return null;
    }
}
