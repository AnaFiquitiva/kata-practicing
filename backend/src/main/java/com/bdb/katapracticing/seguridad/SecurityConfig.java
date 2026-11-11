package com.bdb.katapracticing.seguridad;

import com.bdb.katapracticing.compartido.Rutas;
import com.bdb.katapracticing.compartido.ErrorResponse;
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
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Seguridad de la API REST. El front-end (Nginx) es un proyecto aparte y solo consume /api/**.
 *  - Público: raíz de la API, registrar practicante, token CSRF, login, documentación y health check.
 *  - Rol ANALISTA: listar, consultar, cambiar estado y descargar hojas de vida.
 * La sesión viaja en una cookie HttpOnly (JSESSIONID) y las escrituras exigen token CSRF.
 */
@Configuration
public class SecurityConfig {

    private static final String ROL_ANALISTA = "ANALISTA";
    private static final String MENSAJE_CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos";

    private final ObjectMapper json;

    public SecurityConfig(ObjectMapper json) {
        this.json = json;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(Rutas.CSRF, Rutas.HEALTH, "/error").permitAll()
                .requestMatchers(HttpMethod.GET, Rutas.API, Rutas.DOCS + "/**").permitAll()
                .requestMatchers(HttpMethod.POST, Rutas.PRACTICANTES, Rutas.LOGIN).permitAll()
                .anyRequest().hasRole(ROL_ANALISTA))
            // Login por API: el front envía usuario/contraseña con fetch y recibe JSON (no redirecciones).
            .formLogin(form -> form
                .loginProcessingUrl(Rutas.LOGIN)
                .successHandler((peticion, respuesta, autenticacion) ->
                    escribirJson(respuesta, HttpStatus.OK, Map.of("usuario", autenticacion.getName())))
                .failureHandler((peticion, respuesta, excepcion) ->
                    escribirJson(respuesta, HttpStatus.UNAUTHORIZED,
                        ErrorResponse.de(HttpStatus.UNAUTHORIZED, MENSAJE_CREDENCIALES_INVALIDAS)))
                .permitAll())
            .logout(logout -> logout
                .logoutUrl(Rutas.LOGOUT)
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
            // Sin sesión → 401 (el front redirige a login.html).
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    UserDetailsService usuarios(@Value("${app.analista.usuario}") String usuario,
                                @Value("${app.analista.password}") String clave,
                                PasswordEncoder codificador) {
        return new InMemoryUserDetailsManager(
            User.withUsername(usuario).password(codificador.encode(clave)).roles(ROL_ANALISTA).build());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void escribirJson(HttpServletResponse respuesta, HttpStatus estado, Object cuerpo) throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(respuesta.getOutputStream(), cuerpo);
    }
}
