package br.com.fiap.challenge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Sobe o contexto inteiro da aplicacao. Falha se algum bean nao puder ser
 * construido -- dependencia circular, componente sem implementacao, erro de
 * configuracao do Spring Security.
 *
 * Roda no perfil "test", que usa H2 em memoria: nao depende de um PostgreSQL
 * disponivel na maquina de quem executa os testes.
 */
@SpringBootTest
@ActiveProfiles("test")
class ChallengeApplicationTests {

    @Test
    void contextLoads() {
    }
}
