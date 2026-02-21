package com.bookstore.config;

import com.bookstore.model.UserType;
import com.bookstore.security.CustomUserDetailsService;
import com.bookstore.security.JwtAccessDeniedHandler;
import com.bookstore.security.JwtAuthenticationEntryPoint;
import com.bookstore.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String BOOKS_API_PATTERN = "/api/books/**";
    private static final String USERS_API_PATTERN = "/api/users/**";
    private static final String ORDERS_API_PATTERN = "/api/orders/**";
    
    // Role constants from UserType enum
    private static final String ROLE_SUPER_USER = UserType.SUPER_USER.name();
    private static final String ROLE_ADMIN = UserType.ADMIN.name();

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .cacheControl(cache -> cache.disable())
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // Payment endpoints
                .requestMatchers(HttpMethod.GET, "/api/payments/config").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/payments/success").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/payments/create-checkout-session/**").authenticated()
                
                // Books endpoints - Role-based access control
                // Public GET endpoints - no authentication required for browsing
                .requestMatchers(HttpMethod.GET, "/api/books/isbn/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/books/search/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/books").permitAll()
                // SUPER_USER and ADMIN can get book by ID
                .requestMatchers(HttpMethod.GET, "/api/books/*").hasAnyRole(ROLE_SUPER_USER, ROLE_ADMIN)
                // Only ADMIN can create, update, delete books
                .requestMatchers(HttpMethod.POST, BOOKS_API_PATTERN).hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.PUT, BOOKS_API_PATTERN).hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.DELETE, BOOKS_API_PATTERN).hasRole(ROLE_ADMIN)
                
                // Users endpoints - Role-based access control
                // /me endpoint for regular users to access own profile
                .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/users/me").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/users/me/change-password").authenticated()
                // Only SUPER_USER and ADMIN can list all users
                .requestMatchers(HttpMethod.GET, "/api/users").hasAnyRole(ROLE_SUPER_USER, ROLE_ADMIN)
                .requestMatchers(HttpMethod.GET, USERS_API_PATTERN).hasAnyRole(ROLE_SUPER_USER, ROLE_ADMIN)           
                .requestMatchers(HttpMethod.POST, USERS_API_PATTERN).hasAnyRole(ROLE_SUPER_USER, ROLE_ADMIN)              
                .requestMatchers(HttpMethod.PUT, USERS_API_PATTERN).hasAnyRole(ROLE_SUPER_USER, ROLE_ADMIN)
                .requestMatchers(HttpMethod.DELETE, USERS_API_PATTERN).hasRole(ROLE_ADMIN)
                
                // Orders endpoints - Role-based access control
                // /me endpoint for regular users to access own orders
                .requestMatchers(HttpMethod.GET, "/api/orders/me").authenticated()
                // Only SUPER_USER and ADMIN can list all orders
                .requestMatchers(HttpMethod.GET, "/api/orders").hasAnyRole(ROLE_SUPER_USER, ROLE_ADMIN)
                // Only SUPER_USER and ADMIN can view orders by user ID
                .requestMatchers(HttpMethod.GET, "/api/orders/user/**").hasAnyRole(ROLE_SUPER_USER, ROLE_ADMIN)
                // All authenticated users can create, view specific order, and cancel (controller validates ownership)
                .requestMatchers(HttpMethod.POST, "/api/orders").authenticated()
                .requestMatchers(HttpMethod.GET, ORDERS_API_PATTERN).authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/orders/cancel/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/orders/deliver/**").authenticated()
                // Only SUPER_USER and ADMIN can confirm orders
                .requestMatchers(HttpMethod.PUT, "/api/orders/confirm/**").hasAnyRole(ROLE_SUPER_USER, ROLE_ADMIN)
                // Only ADMIN can delete orders
                .requestMatchers(HttpMethod.DELETE, ORDERS_API_PATTERN).hasRole(ROLE_ADMIN)
                
                // Order items - SUPER_USER and ADMIN only
                .requestMatchers("/api/order-items/**").hasAnyRole(ROLE_SUPER_USER, ROLE_ADMIN)
                
                // All other requests
                .anyRequest().permitAll()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
