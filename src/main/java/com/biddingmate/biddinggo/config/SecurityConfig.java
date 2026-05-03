package com.biddingmate.biddinggo.config;

import com.biddingmate.biddinggo.auth.handler.AccessDeniedHandlerImpl;
import com.biddingmate.biddinggo.auth.handler.AuthenticationEntryPointImpl;
import com.biddingmate.biddinggo.auth.jwt.JwtAuthenticationFilter;
import com.biddingmate.biddinggo.auth.jwt.JwtProvider;
import com.biddingmate.biddinggo.auth.oauth2.CustomFailureHandler;
import com.biddingmate.biddinggo.auth.oauth2.CustomSuccessHandler;
import com.biddingmate.biddinggo.auth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomFailureHandler customFailureHandler;
    private final CustomSuccessHandler customSuccessHandler;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Value("${FRONTEND_URL:http://localhost:*,http://127.0.0.1:*}")
    private String corsAllowedOriginPatterns;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            HandlerExceptionResolver handlerExceptionResolver,
            JwtProvider jWTProvider,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement((seession) -> seession
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 무한 루프 방지
                .addFilterBefore(new JwtAuthenticationFilter(jWTProvider), UsernamePasswordAuthenticationFilter.class)
                // 필터내부 예외 발생시 GlobalExceptionHandler으로 던짐
                .exceptionHandling(exception -> exception
                        // 401 Unauthorized (인증 되지 않은 사용자가 리소스 접근시)
                        .authenticationEntryPoint(new AuthenticationEntryPointImpl())

                        // 403 Forbidden (인증된 사용자가 권한 없는 리소스 접근시)
                        .accessDeniedHandler(new AccessDeniedHandlerImpl())
                )
                // oauth2 로그인 관련
                .oauth2Login((oauth2)-> oauth2
                        .userInfoEndpoint((userInfoEndpointConfig -> userInfoEndpointConfig
                                .userService(customOAuth2UserService)))
                        .failureHandler(customFailureHandler)
                        .successHandler(customSuccessHandler))


                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/actuator/**", "/login/**", "/oauth2/**", "/success.html", "/register-info.html", "/index.html",
                                "/api/v1/auth/check", "/api/v1/auth/refresh",
                                "/api/v1/admin/auth/signup", "/api/v1/admin/auth/login",
                                "/api/v1/payments/virtual-accounts/deposit",
                                "/swagger-ui/**", "/v3/api-docs/**",
                                "/api/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/auctions",
                                "/api/v1/auctions/**",
                                "/api/v1/auctions/search").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.stream(corsAllowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers(org.springframework.boot.autoconfigure.security.servlet.PathRequest.toStaticResources().atCommonLocations())
                .requestMatchers("/favicon.ico", "/resources/**", "/error");
    }

    // 패스워드인코딩 빈 추가
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }
}
