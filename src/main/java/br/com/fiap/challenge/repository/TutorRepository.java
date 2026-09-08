package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.entity.Tutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, Long> {

    Optional<Tutor> findByEmail(String email);

    Optional<Tutor> findByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    /*
     * O CAST(:param AS String) nao e enfeite.
     *
     * Quando o filtro vem nulo, o PostgreSQL recebe um parametro sem tipo
     * declarado dentro de CONCAT/LOWER, nao consegue inferi-lo e assume bytea,
     * falhando com "function lower(bytea) does not exist". O CAST informa o
     * tipo e a consulta passa.
     *
     * No H2 da sprint anterior isso nunca aparecia: ele e permissivo com
     * parametros sem tipo. E um caso classico de query que funciona em um banco
     * e quebra em outro -- motivo pelo qual o ambiente de desenvolvimento deve
     * usar o mesmo banco da producao.
     */
    @Query("SELECT t FROM Tutor t WHERE " +
            "(CAST(:name  AS String) IS NULL OR LOWER(t.name)  LIKE LOWER(CONCAT('%', CAST(:name  AS String), '%'))) AND " +
            "(CAST(:email AS String) IS NULL OR LOWER(t.email) LIKE LOWER(CONCAT('%', CAST(:email AS String), '%')))")
    Page<Tutor> findByFilters(@Param("name") String name,
                              @Param("email") String email,
                              Pageable pageable);
}
