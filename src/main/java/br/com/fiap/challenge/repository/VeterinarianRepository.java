package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.entity.Veterinarian;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VeterinarianRepository extends JpaRepository<Veterinarian, Long> {

    Optional<Veterinarian> findByCrmv(String crmv);

    boolean existsByCrmv(String crmv);

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
    @Query("SELECT v FROM Veterinarian v WHERE " +
            "(CAST(:name      AS String) IS NULL OR LOWER(v.name)      LIKE LOWER(CONCAT('%', CAST(:name      AS String), '%'))) AND " +
            "(CAST(:specialty AS String) IS NULL OR LOWER(v.specialty) LIKE LOWER(CONCAT('%', CAST(:specialty AS String), '%')))")
    Page<Veterinarian> findByFilters(@Param("name") String name,
                                     @Param("specialty") String specialty,
                                     Pageable pageable);
}
