#  Cronograma de Desenvolvimento — Challenge Clyvo-Vet

**Disciplina:** Java Advanced — FIAP  
**Sprints:** 1 a 4  
**Período:** Abril — Novembro 2026

---

##  Divisão de Responsabilidades

| Nome | RM | Responsabilidade |
|------|----|-----------------|
| Artur Pioli Silva | RM-565597 | Entidades JPA, Repositórios, Enums |
| Matheus Arazin Oliveira | RM-556649 | Services e Regras de Negócio |
| Pedro Gabriel Claes | RM-566058 | Controllers REST e HATEOAS |
| Kevin Martins | RM-563454 | Documentação, Testes e Postman |

---

##  Cronograma Detalhado

| Semana | Período | Atividade | Responsável | Status |
|--------|---------|-----------|-------------|--------|
| 1 | 07/04 – 11/04 | Definição do problema e proposta de solução (Clyvo-Vet) | Todos |  Concluído |
| 1 | 07/04 – 11/04 | Modelagem das entidades (Tutor, Pet, Veterinarian, Vaccine, ClinicalRecord, Reminder, ActivityCheckIn, PetStreak) | Artur Pioli Silva |  Concluído |
| 2 | 14/04 – 18/04 | Configuração do projeto Spring Boot + dependências (pom.xml) | Pedro Gabriel Claes |  Concluído |
| 2 | 14/04 – 18/04 | Implementação das entidades JPA com relacionamentos e validações | Artur Pioli Silva |  Concluído |
| 3 | 22/04 – 25/04 | Implementação dos Repositories com JPQL customizado | Artur Pioli Silva |  Concluído |
| 3 | 22/04 – 25/04 | Criação dos DTOs (Request e Response) com Bean Validation | Matheus Arazin Oliveira |  Concluído |
| 4 | 28/04 – 02/05 | Implementação dos Services (regras de negócio, além do CRUD) | Matheus Arazin Oliveira |  Concluído |
| 4 | 28/04 – 02/05 | Sistema de gamificação (check-in diário, streak, PetLevel) | Matheus Arazin Oliveira |  Concluído |
| 5 | 05/05 – 09/05 | Implementação dos Controllers REST com paginação, ordenação e filtros | Pedro Gabriel Claes |  Concluído |
| 5 | 05/05 – 09/05 | Tratamento global de exceções (GlobalExceptionHandler) | Pedro Gabriel Claes |  Concluído |
| 6 | 12/05 – 16/05 | Implementação do HATEOAS (Nível 3 REST) em todos os controllers | Pedro Gabriel Claes |  Concluído |
| 6 | 12/05 – 16/05 | Configuração do Cache (@Cacheable + @CacheEvict) | Matheus Arazin Oliveira |  Concluído |
| 6 | 12/05 – 16/05 | Configuração do Swagger/OpenAPI 3 | Pedro Gabriel Claes |  Concluído |
| 7 | 19/05 – 23/05 | Testes dos endpoints via Postman — exportação da collection | Kevin Martins |  Concluído |
| 7 | 19/05 – 23/05 | Elaboração do README e Cronograma | Kevin Martins |  Concluído |
| 7 | 19/05 – 23/05 | Push final para repositório público no GitHub | Todos |  Concluído |

---

##  Sprint 3 — Thymeleaf, Flyway e Spring Security

> `PREENCHER:` responsável de cada item. Os commits desta sprint saíram de uma
> máquina do laboratório (autor "Laboratório FIAP"), então o cronograma é a
> evidência de quem fez o quê.

| Atividade | Responsável | Status |
|-----------|-------------|--------|
| PostgreSQL, Thymeleaf e Security no build; configuração por ambiente | | Concluído |
| Versionamento do banco com Flyway (V1–V4) | | Concluído |
| Autenticação BCrypt e proteção de rotas por perfil | | Concluído |
| Fluxo de solicitação e aprovação de vínculo | | Concluído |
| Telas Thymeleaf para tutor, veterinário e administrador | | Concluído |
| Containerização e deploy no Render | | Concluído |

---

##  Sprint 4 — Entrega final

> Proposta de divisão — ajustar ao que cada um efetivamente fez. O
> responsável revisa, testa localmente, commita com a própria conta do GitHub
> e apresenta a parte no vídeo.

| Atividade | Responsável | Status |
|-----------|-------------|--------|
| Topologia RabbitMQ (exchange, fila, DLQ) e produtor após o commit | Pedro Gabriel Claes | |
| Consumidor idempotente, migration V5 e tela de notificações | Matheus Arazin Oliveira | |
| Refatoração das ações das telas (`RedirectFeedback`) e revisão de UI/acentuação | Artur Pioli Silva | |
| Testes de mensageria, consultas analíticas SQL e README final | Kevin Martins | |
| Deploy do RabbitMQ no CloudAMQP e variável no Render | Pedro Gabriel Claes | |
| Gravação e edição do vídeo final | Todos | |

---

## 🔗 Links

- **Repositório GitHub:** https://github.com/poiuytrewq01/SPRINT-JAVA-
- **Swagger UI (local):** http://localhost:8080/swagger-ui.html
