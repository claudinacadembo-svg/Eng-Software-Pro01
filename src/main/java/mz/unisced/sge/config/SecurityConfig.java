package mz.unisced.sge.config;

import mz.unisced.sge.security.AuditoriaAccessDeniedHandler;
import mz.unisced.sge.security.Permissoes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Configuracao de seguranca. Combina duas camadas de RBAC:
 *
 * <ul>
 *   <li>autorizacao ao nivel da URL, definida aqui;</li>
 *   <li>autorizacao ao nivel do metodo, com {@code @PreAuthorize} nos servicos
 *       e controladores (activada por {@code @EnableMethodSecurity}).</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final AuditoriaAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(AuditoriaAccessDeniedHandler accessDeniedHandler) {
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(pedidos -> pedidos
                        .requestMatchers("/login", "/css/**", "/js/**", "/favicon.ico").permitAll()
                        .requestMatchers("/h2-console/**").hasRole(Permissoes.PAPEL_ADMINISTRADOR)
                        .requestMatchers("/utilizadores/**").hasAuthority(Permissoes.UTILIZADOR_LER)
                        .requestMatchers("/papeis/**").hasAuthority(Permissoes.PAPEL_LER)
                        .requestMatchers("/auditoria/**").hasAuthority(Permissoes.AUDITORIA_VER)
                        .requestMatchers("/relatorios/**").hasAuthority(Permissoes.RELATORIO_VER)
                        .requestMatchers("/expedientes/**").hasAuthority(Permissoes.EXPEDIENTE_LER)
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?erro")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?terminado")
                        .permitAll())
                .exceptionHandling(excepcoes -> excepcoes.accessDeniedHandler(accessDeniedHandler))
                // A consola H2 e servida em frame e nao aceita token CSRF; fica
                // acessivel apenas ao papel ADMINISTRADOR (regra acima).
                .csrf(csrf -> csrf.ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")))
                .headers(cabecalhos -> cabecalhos.frameOptions(frames -> frames.sameOrigin()));

        return http.build();
    }
}
