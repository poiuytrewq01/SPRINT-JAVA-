package br.com.fiap.challenge.config;

import br.com.fiap.challenge.security.RoleAwareSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static br.com.fiap.challenge.enums.Role.*;

/**
 * Politica de seguranca da aplicacao.
 *
 * Sao duas cadeias de filtros porque os dois clientes tem necessidades
 * opostas. O navegador precisa de sessao, formulario de login e protecao
 * CSRF; a API REST e consumida por ferramentas (Postman, Swagger) que nao
 * mantem sessao nem carregam token CSRF, e por isso usa autenticacao basica
 * e nao guarda estado. Tentar atender aos dois com uma cadeia unica levaria
 * a desligar o CSRF do site inteiro so para a API funcionar.
 *
 * As duas cadeias compartilham o mesmo UserDetailsService e o mesmo
 * PasswordEncoder: um usuario so, dois modos de entrada.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RoleAwareSuccessHandler successHandler;

    /**
     * BCrypt aplica um salt aleatorio por senha, entao duas contas com a mesma
     * senha geram hashes diferentes, e o fator de custo 10 torna a verificacao
     * deliberadamente lenta contra forca bruta.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cadeia da API REST. @Order(1) faz o Spring avalia-la primeiro: sem isso,
     * a cadeia do site (que aceita qualquer rota) capturaria /api/** antes.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole(ADMIN.name())
                        .anyRequest().authenticated())
                .httpBasic(basic -> {})
                .build();
    }

    /**
     * Cadeia da aplicacao web.
     *
     * As regras sao avaliadas na ordem declarada e a primeira que casa decide,
     * por isso anyRequest() fica sempre por ultimo. O padrao adotado e negar
     * por default: tudo que nao foi liberado explicitamente exige login.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        // Recursos publicos: tela de login, estaticos e pagina de erro.
                        .requestMatchers("/", "/login", "/acesso-negado", "/error").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                        // Cada area pertence a um unico perfil.
                        .requestMatchers("/tutor/**").hasRole(TUTOR.name())
                        .requestMatchers("/vet/**").hasRole(VETERINARIAN.name())
                        .requestMatchers("/admin/**").hasRole(ADMIN.name())

                        // A documentacao expoe todos os endpoints: restrita a operacao.
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                            .hasRole(ADMIN.name())

                        .anyRequest().authenticated())

                .formLogin(form -> form
                        .loginPage("/login")
                        // Rota processada pelo proprio filtro do Spring Security:
                        // nao existe metodo de controller para ela.
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureUrl("/login?erro")
                        .permitAll())

                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                        .logoutSuccessUrl("/login?sair")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())

                // Autenticado, porem sem o perfil exigido: mostra uma pagina
                // explicativa em vez do 403 cru do container.
                .exceptionHandling(ex -> ex.accessDeniedPage("/acesso-negado"))

                .build();
    }
}
