package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.entity.User;
import br.com.fiap.challenge.enums.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Carrega o usuario para autenticacao.
     *
     * O EntityGraph traz tutor e veterinarian na mesma consulta. Sem ele, o
     * acesso a esses campos aconteceria depois de a transacao ter fechado --
     * as associacoes sao LAZY -- e resultaria em LazyInitializationException
     * na primeira requisicao apos o login.
     */
    @EntityGraph(attributePaths = {"tutor", "veterinarian"})
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRoleOrderByNameAsc(Role role);

    /**
     * Listagem do painel administrativo. A ordenacao vai no nome do metodo, e
     * nao em um Sort, porque assim o @EntityGraph se aplica -- o findAll(Sort)
     * herdado do JpaRepository nao aceita a anotacao e deixaria tutor e
     * veterinarian sem carregar.
     */
    @EntityGraph(attributePaths = {"tutor", "veterinarian"})
    List<User> findAllByOrderByRoleAscNameAsc();
}
