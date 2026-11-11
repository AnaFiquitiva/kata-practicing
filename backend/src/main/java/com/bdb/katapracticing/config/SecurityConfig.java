package com.bdb.katapracticing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seguridad de la API REST. El front-end (Nginx) es un proyecto aparte y solo consume /api/**.
 *  - Público: registrar practicante, token CSRF, login y health check.
 *  - Rol ANALISTA: listar, consultar, cambiar estado y descargar hojas de vida.
 * La sesión viaja en una cookie HttpOnly (JSESSIONID) y las escrituras exigen token CSRF.
 */
@Configuration
public class SecurityConfig {

    private final ObjectMapper json;

    public SecurityConfig(ObjectMapper json) {
        this.json = json;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/csrf", "/actuator/health", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/practicantes", "/api/auth/login").permitAll()
                .anyRequest().hasRole("ANALISTA"))
            // Login por API: el front envía usuario/contraseña con fetch y recibe JSON (no redirecciones).
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .successHandler((req, res, auth) ->
                    escribir(res, HttpStatus.OK, Map.of("usuario", auth.getName())))
                .failureHandler((req, res, ex) ->
                    escribir(res, HttpStatus.UNAUTHORIZED, error(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos")))
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
            // Sin sesión → 401 (el front redirige a login.html).
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    UserDetailsService usuarios(@Value("${app.analista.usuario}") String usuario,
                                @Value("${app.analista.password}") String password,
                                PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.withUsername(usuario).password(encoder.encode(password)).roles("ANALISTA").build());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static Map<String, Object> error(HttpStatus estado, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("estado", estado.value());
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("fecha", LocalDateTime.now().toString());
        return cuerpo;
    }

    private void escribir(HttpServletResponse res, HttpStatus estado, Object cuerpo) throws IOException {
        res.setStatus(estado.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        json.writeValue(res.getOutputStream(), cuerpo);
    }
}
