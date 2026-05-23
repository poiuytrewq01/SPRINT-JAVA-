package br.com.fiap.challenge.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Habilita o suporte a cache na aplicação via @EnableCaching.
 *
 * O tipo de cache configurado em application.properties é "simple"
 * (ConcurrentMapCacheManager), adequado para desenvolvimento e ambientes
 * com uma única instância da aplicação.
 *
 * Para produção com múltiplas instâncias (cluster), substituir por Redis:
 * spring.cache.type=redis — sem necessidade de alterar o código da aplicação,
 * apenas a configuração (Open/Closed Principle).
 *
 * Caches utilizados:
 * - "petHealthSummary": resultado do cálculo de saúde por pet (key = petId)
 * - "veterinarians": listagem de veterinários (pouco mutável)
 */
@Configuration
@EnableCaching
public class CacheConfig {}
