package br.com.fiap.challenge.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Perfis de acesso da plataforma.
 *
 * O Spring Security espera autoridades prefixadas com "ROLE_". O prefixo fica
 * concentrado aqui para que nenhuma outra classe precise concatenar strings:
 * as regras de rota usam hasRole("TUTOR") e o prefixo e aplicado pelo framework.
 */
@Getter
@RequiredArgsConstructor
public enum Role {

    /** Dono do pet: gerencia os proprios pets, faz check-ins e pede vinculo com veterinario. */
    TUTOR("Tutor", "/tutor"),

    /** Profissional: atende os pets vinculados, registra prontuarios e responde solicitacoes. */
    VETERINARIAN("Veterinario", "/vet"),

    /** Operacao da plataforma: visao consolidada, sem acesso de escrita ao dominio clinico. */
    ADMIN("Administrador", "/admin");

    private final String label;

    /** Rota inicial do perfil, usada para redirecionar apos o login. */
    private final String homePath;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
