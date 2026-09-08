package br.com.fiap.challenge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados da documentacao OpenAPI (Swagger UI).
 *
 * O esquema de seguranca declarado aqui e o mesmo da cadeia de filtros da API:
 * autenticacao basica. Sem essa declaracao, o Swagger UI enviaria as
 * requisicoes sem credencial e todo "Try it out" responderia 401.
 */
@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI challengePetOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Challenge Pet - API")
                        .version("v3")
                        .description("""
                                     Plataforma de saude e engajamento para pets.

                                     Todos os endpoints exigem autenticacao. Use as credenciais \
                                     de um usuario cadastrado (por exemplo ana@clinicapet.com).
                                     """)
                        .contact(new Contact().name("Pedro Gabriel Claes")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH))
                .components(new Components().addSecuritySchemes(BASIC_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")));
    }
}
