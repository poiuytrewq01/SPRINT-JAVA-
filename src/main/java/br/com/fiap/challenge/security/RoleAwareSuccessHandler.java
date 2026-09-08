package br.com.fiap.challenge.security;

import br.com.fiap.challenge.enums.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;

/**
 * Decide para onde o usuario vai depois de autenticar.
 *
 * Cada perfil tem uma area propria, entao um destino unico apos o login
 * obrigaria uma tela intermediaria de redirecionamento. O destino sai de
 * Role.homePath: incluir um novo perfil nao exige alterar esta classe.
 */
@Component
public class RoleAwareSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String FALLBACK_URL = "/";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        getRedirectStrategy().sendRedirect(request, response, resolveTargetUrl(authentication));
    }

    private String resolveTargetUrl(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::toRole)
                .filter(role -> role != null)
                .findFirst()
                .map(Role::getHomePath)
                .orElse(FALLBACK_URL);
    }

    /** Converte a autoridade ("ROLE_TUTOR") de volta no enum, ou null se nao houver correspondencia. */
    private Role toRole(String authority) {
        return Arrays.stream(Role.values())
                .filter(role -> role.getAuthority().equals(authority))
                .findFirst()
                .orElse(null);
    }
}
