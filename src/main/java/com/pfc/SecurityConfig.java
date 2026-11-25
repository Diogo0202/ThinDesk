package com.pfc;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/chamados", "/api/chamados/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/dist/**", "/plugins/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/api/chamados/*/resolver").hasRole("TECHNICIAN")
                .requestMatchers("/clientes", "/api/clientes/**", "/ajustes-horarios", "/api/ajustes-horarios/**").hasRole("TECHNICIAN")
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {})
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
            );
        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(
            @Value("${spring.security.user.name:admin}") String adminName,
            @Value("${spring.security.user.password}") String adminPassword,
            @Value("${THINDESK_USER_USERNAME:}") String userName,
            @Value("${THINDESK_USER_PASSWORD:}") String userPassword) {
        List<UserDetails> users = new ArrayList<>();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        users.add(User.withUsername(adminName).password("{bcrypt}" + encoder.encode(adminPassword))
                .roles("TECHNICIAN").build());
        if (!userName.isBlank() && !userPassword.isBlank() && !userName.equals(adminName)) {
            users.add(User.withUsername(userName).password("{bcrypt}" + encoder.encode(userPassword))
                    .roles("USER").build());
        }
        return new InMemoryUserDetailsManager(users);
    }
}
