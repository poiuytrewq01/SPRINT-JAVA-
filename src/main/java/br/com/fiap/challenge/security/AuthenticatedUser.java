package br.com.fiap.challenge.security;

import br.com.fiap.challenge.entity.User;
import br.com.fiap.challenge.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptador entre a entidade User e o contrato UserDetails do Spring Security.
 *
 * A entidade nao implementa UserDetails diretamente: sao responsabilidades
 * diferentes. User modela a conta no dominio; esta classe traduz essa conta
 * para o vocabulario do framework (authorities, credenciais, flags de conta).
 * Assim a entidade nao carrega metodos como isAccountNonExpired(), que nao
 * significam nada para o negocio.
 *
 * Alem do contrato, expoe os identificadores de dominio (tutorId e
 * veterinarianId) ja resolvidos. E o que permite aos controllers filtrarem
 * dados pelo dono da sessao sem uma consulta extra a cada requisicao.
 */
@Getter
public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean active;
    private final Long tutorId;
    private final Long veterinarianId;

    public AuthenticatedUser(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.passwordHash = user.getPassword();
        this.role = user.getRole();
        this.active = user.isEnabled();
        this.tutorId = user.getTutor() != null ? user.getTutor().getId() : null;
        this.veterinarianId = user.getVeterinarian() != null ? user.getVeterinarian().getId() : null;
    }

    /** Primeiro nome, usado no cabecalho das telas. */
    public String getFirstName() {
        return name.split(" ")[0];
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getAuthority()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    /** O Spring Security chama de "username" o identificador do login: aqui, o e-mail. */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
