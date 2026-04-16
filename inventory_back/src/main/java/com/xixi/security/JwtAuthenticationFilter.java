package com.xixi.security;

import com.xixi.config.JwtProperties;
import com.xixi.service.TokenBlacklistService;
import com.xixi.service.impl.SecurityUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SecurityUserDetailsService securityUserDetailsService;
    private final JwtProperties jwtProperties;
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
                                    throws ServletException, IOException {
        String header = request.getHeader(jwtProperties.getHeader());
        if(header == null || !header.startsWith(jwtProperties.getPrefix()+" ")){
            filterChain.doFilter(request,response);
            return ;
        }
        String token = header.substring((jwtProperties.getPrefix()+" ").length());
        if(!jwtTokenService.validateToken(token)){
            filterChain.doFilter(request,response);
            return ;
        }
        String jti = jwtTokenService.getJti(token);
        if(tokenBlacklistService.isBlacklist(jti)){
            filterChain.doFilter(request,response);
            return ;
        }
        String username = jwtTokenService.getUsername(token);
        UserDetails userDetails = securityUserDetailsService.loadUserByUsername(username);
        if(!userDetails.isEnabled()){
            filterChain.doFilter(request,response);
            return ;
        }
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request,response);
    }
}
