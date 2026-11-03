# PetHealth — Challenge FIAP | Java Advanced

Plataforma web em Spring Boot que aproxima tutores de pets e clínicas
veterinárias. Reúne o histórico clínico do animal (vacinas, prontuários,
lembretes), formaliza o vínculo entre o pet e o veterinário e transforma o
cuidado preventivo em hábito com um check-in diário gamificado.

**Entrega final (4ª sprint).** Consolida as sprints anteriores — API REST,
Thymeleaf, Flyway e Spring Security — e acrescenta **mensageria assíncrona com
RabbitMQ**: as ações de um perfil geram notificações para o outro.

---

## Links da entrega

| | |
|---|---|
| **Aplicação no ar** | [https://sprint-java.onrender.com](https://sprint-java.onrender.com/) |
| **Vídeo da apresentação (Sprint 4)** | `PREENCHER: link do YouTube` |
| **Repositório** | https://github.com/poiuytrewq01/SPRINT-JAVA- |

> A aplicação está no plano gratuito do Render. O contêiner hiberna após alguns
> minutos sem acesso, então **o primeiro carregamento pode levar cerca de 3
> minutos**. Os acessos seguintes são imediatos.

## Equipe

| Nome | RM |
|---|---|
| Artur Pioli Silva | RM565597 |
| Matheus Arazin Oliveira | RM556649 |
| Pedro Gabriel Claes | RM566058 |
| Kevin Martins | RM563454 |

A divisão de responsabilidades de cada sprint está em
[`docs/cronograma.md`](docs/cronograma.md).

## Sumário

- [O problema e a solução](#o-problema-e-a-solução)
- [Como executar](#como-executar)
- [Usuários de demonstração](#usuários-de-demonstração)
- [Fluxos do sistema](#fluxos-do-sistema)
- [Mensageria com RabbitMQ](#mensageria-com-rabbitmq)
- [Arquitetura e decisões](#arquitetura-e-decisões)
- [Banco de dados e migrations](#banco-de-dados-e-migrations)
- [Segurança](#segurança)
- [Integração com as outras disciplinas](#integração-com-as-outras-disciplinas)
- [API REST](#api-rest)
- [Testes](#testes)
- [Deploy](#deploy)
- [Evolução por sprint](#evolução-por-sprint)

---

## O problema e a solução

O desafio partiu de dois problemas observados na relação entre tutores e
clínicas:

1. **Vínculo fraco entre clínica e tutor.** O veterinário de um pet era um
   dado que o tutor preenchia sozinho, sem que o profissional soubesse. Não
   havia compromisso de nenhum dos lados.
2. **Cuidado reativo.** O tutor só procura a clínica quando o animal adoece.
   Vacinas vencem e retornos são esquecidos.

O PetHealth responde a cada um com um mecanismo próprio:

| Problema | Resposta no sistema |
|---|---|
| Vínculo fraco | O vínculo vira uma **solicitação com aprovação**: o tutor pede, o veterinário aceita ou recusa, e só então o prontuário é liberado. Cada decisão gera uma **notificação** para o outro lado. |
| Cuidado reativo | **Check-in diário** com sequência e níveis, **score de saúde** de 0 a 100 e **retorno automático** em seis meses a cada atendimento, avisado ao tutor. |

Três perfis usam a plataforma: **tutor**, **veterinário** e **administrador**,
cada um com sua área e suas permissões.

---

## Como executar

### Pré-requisitos

| Ferramenta | Versão |
|---|---|
| JDK | 21 |
| Maven | 3.9+ (ou o wrapper `./mvnw` incluído) |
| Docker | qualquer versão recente, para subir PostgreSQL e RabbitMQ |

### Passo a passo

**1. Clonar o repositório**

```bash
git clone https://github.com/poiuytrewq01/SPRINT-JAVA-.git
cd SPRINT-JAVA-
```

**2. Subir banco e broker**

```bash
docker compose up -d
```

Sobem dois contêineres:

| Serviço | Endereço | Credenciais |
|---|---|---|
| PostgreSQL 16 | `localhost:5432` | database, usuário e senha `pethealth` |
| RabbitMQ 3.13 | `localhost:5672` (AMQP) | `guest` / `guest` |
| Painel do RabbitMQ | <http://localhost:15672> | `guest` / `guest` |

São exatamente os valores que a aplicação usa por padrão; nenhuma variável de
ambiente precisa ser configurada para rodar localmente.

**3. Executar a aplicação**

```bash
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
```

Na primeira execução o Flyway cria o schema e insere os dados de
demonstração. O log mostra as cinco migrations:

```
Migrating schema "public" to version "1 - create core schema"
Migrating schema "public" to version "2 - create security schema"
Migrating schema "public" to version "3 - create vet link requests"
Migrating schema "public" to version "4 - seed demo data"
Migrating schema "public" to version "5 - create notifications"
```

Em um banco que já tinha as quatro primeiras (da Sprint 3), apenas a V5 é
aplicada.

**4. Acessar**

| Recurso | Endereço |
|---|---|
| Aplicação | <http://localhost:8080> |
| Documentação da API | <http://localhost:8080/swagger-ui.html> (perfil ADMIN) |

Para recomeçar do zero, apagando os dados:

```bash
docker compose down -v && docker compose up -d
```

---

## Usuários de demonstração

Todos usam a senha **`senha123`**. As contas são criadas pela migration `V4`,
com as senhas já armazenadas como hash BCrypt.

| Perfil | E-mail | O que enxerga |
|---|---|---|
| Tutor | `maria@email.com` | Rex e Mia — tem uma solicitação de vínculo pendente |
| Tutor | `joao@email.com` | Thor — com uma vacina vencida |
| Veterinário | `ana@clinicapet.com` | Rex como paciente e a solicitação da Mia para aprovar |
| Veterinário | `carlos@clinicapet.com` | Thor como paciente |
| Administrador | `admin@pethealth.com` | Painel consolidado, situação da fila e contas de acesso |

Os dados usam datas relativas a hoje, então o cenário continua coerente
independentemente de quando o banco for criado. O Rex chega com uma sequência
de 4 dias, sendo o último check-in ontem — registrar um check-in leva a
sequência a 5 na hora.

**Roteiro rápido para ver tudo funcionando** (duas abas, uma anônima):

1. Entre como **Ana** e aprove a solicitação da Mia em *Solicitações*.
2. Na outra aba, entre como **Maria**: o menu mostra *Notificações (1)*.
3. Volte para a Ana, abra o paciente Mia e registre um atendimento.
4. A Maria recebe a segunda notificação, com a data do retorno.
5. Entre como **admin**: o card *Mensageria* mostra o broker conectado e as
   notificações entregues.

---

## Fluxos do sistema

Além do CRUD, o sistema tem quatro fluxos com estado, transição e regra de
negócio.

### Fluxo 1 — Check-in diário e gamificação

**Quem:** tutor · **Onde:** `/tutor/pets/{id}`

O tutor registra uma atividade do pet. O `ActivityCheckInService` valida que
ainda não houve check-in hoje, grava o registro e recalcula a sequência:

- último check-in foi ontem → a sequência cresce;
- houve um dia de intervalo → a sequência volta a 1;
- o recorde histórico é atualizado quando superado;
- o nível (Iniciante → Bronze → Prata → … → Lendário) é derivado da sequência.

A regra "um check-in por pet por dia" está no service **e** como constraint
única no banco: a validação em código dá a mensagem clara, e a constraint
protege contra requisições concorrentes.

### Fluxo 2 — Solicitação e aprovação de vínculo

**Quem:** tutor e veterinário · **Onde:** `/tutor/vinculos` e `/vet/solicitacoes`

```
        tutor solicita                 veterinário decide
  ────────────────────────►  PENDENTE  ────────────────────►  APROVADA
     (vet é notificado)         │                             (vínculo criado,
                                │                              tutor é notificado)
                                ├──────────────────────────►  RECUSADA
                                │                             (tutor é notificado)
                                └── tutor desiste ─────────►  CANCELADA
```

Só a aprovação altera `pets.veterinarian_id`, e as duas escritas acontecem na
mesma transação. Os estados finais são imutáveis — a regra de transição vive
no próprio enum `LinkRequestStatus`, não espalhada em condicionais.

### Fluxo 3 — Atendimento clínico e retorno automático

**Quem:** veterinário e tutor · **Onde:** `/vet/pacientes/{id}`

O veterinário registra o atendimento. Na mesma transação, o
`ClinicalRecordService` agenda um lembrete de retorno para seis meses depois.
O tutor é notificado da data e pode marcar o lembrete como concluído.

Acesso ao prontuário exige vínculo aprovado (fluxo 2): um veterinário não
enxerga o paciente de outro.

### Fluxo 4 — Notificações assíncronas *(Sprint 4)*

**Quem:** tutor e veterinário · **Onde:** `/notificacoes`

Os fluxos 2 e 3 publicam eventos que atravessam o RabbitMQ e viram
notificações para a outra parte. O menu mostra o contador de não lidas; abrir
uma notificação a marca como lida e leva ao ponto da aplicação a que ela se
refere.

| Evento | Quem provoca | Quem recebe | Leva para |
|---|---|---|---|
| `LINK_REQUESTED` | tutor pede vínculo | veterinário | `/vet/solicitacoes` |
| `LINK_APPROVED` / `LINK_REJECTED` | veterinário decide | tutor | `/tutor/vinculos` |
| `RETURN_SCHEDULED` | veterinário registra atendimento | tutor | `/tutor/pets/{id}` |

---

## Mensageria com RabbitMQ

### Por que assíncrono

A notificação é consequência da ação, não parte dela. Se o veterinário aprova
um vínculo, a aprovação precisa ser gravada **agora**; o aviso ao tutor pode
chegar um instante depois. Desacoplar as duas coisas traz três ganhos:

- **Resposta rápida:** a tela do veterinário não espera o processamento da
  notificação.
- **Isolamento de falhas:** broker fora do ar ou consumidor com erro não
  desfazem a aprovação.
- **Reprocessamento:** uma mensagem que falhou fica na fila e é tentada de
  novo, em vez de se perder numa exceção.

### Por que RabbitMQ, e não Kafka

O caso é um **comando ponto a ponto**: cada mensagem tem um destinatário e,
depois de processada, não precisa ser relida. É o modelo do RabbitMQ (*smart
broker*, entrega por push, mensagem removida após o ack, DLQ nativa). Kafka
brilha em *event streaming* — log persistente, múltiplos consumidores
relendo o histórico, volume massivo — o que aqui seria infraestrutura sem
necessidade correspondente.

### Caminho de uma notificação

```
 VetLinkRequestService.approve()          ── transação JPA ──┐
   └─ NotificationPublisher.linkDecided()                    │
        └─ ApplicationEventPublisher.publishEvent(evento)    │
                                                             ▼ COMMIT
 NotificationRelay  @TransactionalEventListener(AFTER_COMMIT) @Async
   └─ RabbitTemplate.convertAndSend(exchange, routingKey, evento)   ── JSON ──►

 [pethealth.notifications]  direct exchange
        │ routing key "notification.created"
        ▼
 (pethealth.notifications.queue)  durável, com dead-letter configurado
        │
        ▼
 NotificationConsumer  @RabbitListener
   └─ NotificationService.store(evento)   idempotente por eventId
        └─ INSERT em notifications                       ──► tela /notificacoes

        falhou 3 vezes (retry 1s, 2s)
        ▼
 [pethealth.notifications.dlx] ──► (pethealth.notifications.dlq)
```

### Decisões que valem registro

**Publicar só depois do commit.** O service não fala com o RabbitMQ: ele
publica um evento do Spring, e o `NotificationRelay` o envia em
`AFTER_COMMIT`. Se a transação sofrer rollback, nenhuma mensagem sai — o
tutor nunca recebe "vínculo aprovado" de uma aprovação que não foi gravada.

**Envio em outra thread (`@Async`).** Com o broker lento ou fora do ar, quem
espera é a notificação, não o usuário. O `connection-timeout` de 5 s impede
que uma conexão pendurada segure recursos por um minuto.

**Consumidor idempotente.** O RabbitMQ entrega *pelo menos uma vez*: se a
conexão cair entre o processamento e o ack, a mensagem volta. Cada evento
carrega um `eventId` (UUID); o consumidor ignora ids já processados, e a
constraint `uk_notifications_event` garante o mesmo no banco.

**Retry e DLQ.** Três tentativas com espera exponencial. Esgotadas, a
mensagem é rejeitada sem reenfileirar e o RabbitMQ a move para a DLQ. Sem
isso, uma mensagem que sempre falha seria reentregue para sempre e travaria a
fila (*poison message*). Destinatário inexistente lança
`AmqpRejectAndDontRequeueException`, porque tentar de novo não resolveria.

**JSON com pacotes confiáveis.** As mensagens são JSON (legíveis no painel e
independentes de Java), e o conversor só aceita desserializar classes do
pacote `messaging` — uma mensagem forjada não consegue pedir a criação de
uma classe arbitrária.

**Topologia como código.** Exchanges, filas e bindings são beans em
`RabbitMQConfig`, declarados automaticamente na primeira conexão. Nada é
criado à mão no painel.

**Limite conhecido.** Se o broker estiver fora do ar no instante do envio, a
notificação se perde (fica no log); a operação de negócio permanece gravada.
A evolução seria o padrão *Transactional Outbox*: gravar o evento numa tabela
na mesma transação e publicar a partir dela.

### Vendo a fila funcionar

- **Painel do admin** (`/admin`): broker conectado, mensagens aguardando,
  consumidores, mensagens na DLQ e total entregue — consultados ao vivo via
  `AmqpAdmin`.
- **Painel do RabbitMQ** (<http://localhost:15672>): em *Queues*, as duas
  filas, com a taxa de mensagens publicadas e confirmadas; em *Exchanges*,
  os bindings.
- **Log da aplicação**: `Notificacao ... publicada` no produtor e
  `Notificacao ... recebida da fila` no consumidor.

---

## Arquitetura e decisões

```
br.com.fiap.challenge
├── config/         SecurityConfig, CacheConfig, OpenApiConfig
├── controller/     @RestController — API REST (JSON)
├── web/            @Controller — telas Thymeleaf (HTML)
├── messaging/      RabbitMQ: topologia, produtor, consumidor, status   (Sprint 4)
├── service/        regras de negócio e transações
├── repository/     Spring Data JPA
├── entity/         mapeamento objeto-relacional
├── dto/
│   ├── form/       formulários das telas
│   ├── request/    entrada da API
│   └── response/   saída da API
├── enums/          Role, LinkRequestStatus, NotificationType, PetLevel, …
├── exception/      ApiExceptionHandler (JSON) e WebExceptionHandler (HTML)
└── security/       AuthenticatedUser, UserDetailsService, success handler
```

**`web/` separado de `controller/`.** Os dois clientes têm necessidades
diferentes — um recebe HTML e redirects, o outro JSON e códigos de status. A
separação por pacote permite que cada um tenha o seu tratamento de erros, via
`@ControllerAdvice` com `basePackages`.

**`messaging/` separado de `service/`.** Os services falam a língua do
negócio (`linkDecided`, `returnScheduled`); só o pacote `messaging` conhece
exchange, routing key e `RabbitTemplate`. Trocar o broker afetaria um pacote,
não o domínio.

**Autenticação separada do domínio.** `User` guarda credenciais e perfil;
`Tutor` e `Veterinarian` continuam sendo os registros de negócio.

**`PetAccessGuard` como componente próprio.** Proteger a rota por perfil
garante apenas que quem entrou em `/tutor/**` é um tutor — não que aquele pet
seja dele. A verificação de propriedade do registro vive em um lugar só.

**`RedirectFeedback` para as ações das telas.** Toda ação POST segue o mesmo
roteiro: executar, mostrar sucesso ou erro de regra de negócio, redirecionar
(POST-Redirect-GET). O roteiro está numa classe utilitária e cada controller
declara só a ação e o destino.

**Contador de notificações em `@ControllerAdvice`.** O contador aparece no
menu de todas as telas; o `NotificationModelAdvice` o coloca no `Model` uma
vez, em vez de cada controller lembrar de fazê-lo.

**`open-in-view` desligado.** Evita consultas disparadas durante a
renderização. Em troca, cada consulta declara com `@EntityGraph` o que a tela
vai usar.

---

## Banco de dados e migrations

### Modelo

```
users ──┬─(1:1)─► tutors ──(1:N)─► pets ──┬──(1:N)─► vaccines
   │    │                            │     ├──(1:N)─► clinical_records
   │    └─(1:1)─► veterinarians ◄────┤     ├──(1:N)─► reminders
   │                    ▲            │     ├──(1:N)─► activity_check_ins
   │                    │            │     └──(1:1)─► pet_streaks
   │                    └────────── vet_link_requests
   │
   └──(1:N)─► notifications                                  (Sprint 4)
```

Diagramas completos em [`docs/der_clyvovet (1).html`](docs/) e
[`docs/DIAGRAMA DE CLASSES.png`](docs/).

### Migrations

| Versão | Conteúdo |
|---|---|
| `V1__create_core_schema.sql` | Tabelas do domínio, chaves estrangeiras e índices |
| `V2__create_security_schema.sql` | Tabela `users` com constraint de coerência perfil/vínculo |
| `V3__create_vet_link_requests.sql` | Solicitações de vínculo, com índice único parcial |
| `V4__seed_demo_data.sql` | Carga inicial de demonstração |
| `V5__create_notifications.sql` | Notificações gravadas pelo consumidor do RabbitMQ |

O Hibernate roda com `ddl-auto: validate` — confere se as entidades batem com
o schema, mas nunca o altera.

> **Nunca** edite uma migration já aplicada. O Flyway guarda o checksum de
> cada arquivo em `flyway_schema_history` e recusa a inicialização se um
> script executado mudar. Por isso a tabela de notificações entrou como V5, e
> não como alteração das anteriores.

### Integridade garantida no banco

| Constraint | O que impede |
|---|---|
| `ck_users_role_link` | Perfil incoerente com o vínculo (um `TUTOR` apontando para veterinário) |
| `uk_check_in_pet_date` | Dois check-ins do mesmo pet no mesmo dia |
| `uk_vet_link_pending` | Segunda solicitação pendente para o mesmo par pet/veterinário (índice único **parcial**) |
| `uk_notifications_event` | A mesma mensagem da fila gravada duas vezes (idempotência) |
| `ck_notifications_link_interno` | Notificação apontando para fora da aplicação (open redirect) |
| `uk_users_email`, `uk_tutors_cpf`, `uk_veterinarians_crmv` | Duplicidade de identificadores |

---

## Segurança

### Regras de rota

| Rota | Quem acessa |
|---|---|
| `/`, `/login`, `/css/**` | Público |
| `/tutor/**` | `ROLE_TUTOR` |
| `/vet/**` | `ROLE_VETERINARIAN` |
| `/admin/**`, `/swagger-ui/**` | `ROLE_ADMIN` |
| `/notificacoes/**` | Autenticado — cada um vê apenas as próprias |
| `/api/**` | Autenticado (`DELETE` exige `ROLE_ADMIN`) |
| qualquer outra | Autenticado |

As regras são avaliadas na ordem declarada e `anyRequest()` fica por último. A
política é negar por padrão.

### Duas camadas de autorização

Perfil e propriedade do dado são coisas diferentes. `/tutor/pets/42` só é
liberada para um tutor — mas o pet 42 pode ser de outra pessoa. Por isso toda
rota que recebe um id confere o dono: o `PetAccessGuard` para pets, o
`VetLinkRequestService` para solicitações e o `NotificationService` para
notificações. O identificador do usuário vem sempre de
`@AuthenticationPrincipal`, nunca da URL ou do formulário.

### Outras proteções

- **BCrypt** (custo 10) para senhas.
- **CSRF** ativo na cadeia web; o Thymeleaf insere o token em todo formulário
  POST — inclusive logout e "abrir notificação", que alteram estado.
- **Duas cadeias de filtros:** sessão + CSRF + formulário para o site;
  autenticação básica sem estado para a API.
- **Open redirect:** o destino de uma notificação é conferido no controller e
  no banco — só caminhos internos.
- **Desserialização:** o conversor JSON do RabbitMQ só aceita classes do
  pacote `messaging`.

---

## Integração com as outras disciplinas

> `PREENCHER:` confirmar com a equipe quais disciplinas do semestre entram
> aqui e anexar as evidências produzidas em cada uma (documento, print,
> link). As duas abaixo já têm evidência dentro deste repositório.

### Banco de Dados

O banco não é um detalhe de infraestrutura: parte das regras de negócio é
garantida nele.

| Evidência | Onde |
|---|---|
| Modelo entidade-relacionamento | [`docs/der_clyvovet (1).html`](docs/) |
| Diagrama de classes | [`docs/DIAGRAMA DE CLASSES.png`](docs/) |
| Schema versionado (DDL) | [`src/main/resources/db/migration`](src/main/resources/db/migration) — V1 a V5 |
| Regras no banco | constraints `CHECK`, `UNIQUE` e índice único parcial (tabela acima) |
| Consultas analíticas | [`docs/banco-de-dados/consultas-analiticas.sql`](docs/banco-de-dados/consultas-analiticas.sql) |

As consultas analíticas usam `JOIN`, agregação com `FILTER`, função de janela
(`RANK() OVER`) e aritmética de datas para responder perguntas de negócio:
quem está com vacina vencida, quanto tempo cada veterinário leva para
responder às solicitações, o ranking de engajamento e o volume de
notificações por usuário.

### DevOps & Cloud

| Evidência | Onde |
|---|---|
| Build multi-stage em contêiner | [`Dockerfile`](Dockerfile) |
| Ambiente local reproduzível (PostgreSQL + RabbitMQ) | [`docker-compose.yml`](docker-compose.yml) |
| Configuração por variável de ambiente (12-factor) | [`application.yaml`](src/main/resources/application.yaml) |
| Deploy em nuvem | Render (aplicação + PostgreSQL) e CloudAMQP (RabbitMQ) |

---

## API REST

A API das sprints anteriores continua ativa em `/api/**`, autenticada.

```bash
curl -u ana@clinicapet.com:senha123 http://localhost:8080/api/pets
curl -u ana@clinicapet.com:senha123 http://localhost:8080/api/pets/1/health
```

O Swagger UI (`/swagger-ui.html`, perfil ADMIN) já vem configurado com o
esquema de autenticação básica — use o botão **Authorize** antes do "Try it
out". A collection do Postman está em
[`docs/challenge_pet_postman.json`](docs/challenge_pet_postman.json).

---

## Testes

```bash
./mvnw test
```

| Teste | O que verifica |
|---|---|
| `ChallengeApplicationTests` | O contexto inteiro sobe (beans, segurança, mensageria) |
| `NotificationServiceTest` | Gravação, idempotência por `eventId`, destinatário inexistente → DLQ, dono da notificação |
| `NotificationRelayTest` | Exchange e routing key corretas; broker fora do ar não propaga erro |

Os testes rodam no perfil `test`, com H2 em memória, Flyway desligado e os
listeners do RabbitMQ sem iniciar — não exigem PostgreSQL nem RabbitMQ na
máquina.

---

## Deploy

### Variáveis de ambiente

| Variável | Exemplo |
|---|---|
| `DB_URL` | `jdbc:postgresql://host:5432/pethealth` |
| `DB_USERNAME` | `pethealth` |
| `DB_PASSWORD` | *(senha do serviço)* |
| `RABBITMQ_URL` | `amqps://usuario:senha@host.rmq.cloudamqp.com/vhost` |
| `PORT` | definida pela plataforma |

### RabbitMQ em produção (CloudAMQP)

O Render não oferece RabbitMQ gerenciado. O broker de produção fica no
**CloudAMQP**, plano gratuito *Little Lemur* (até 20 conexões e 100 filas —
a aplicação usa uma conexão e duas filas).

1. Crie uma instância *Little Lemur* em <https://www.cloudamqp.com>.
2. Copie a **AMQP URL** (começa com `amqps://`).
3. No serviço do Render, em *Environment*, crie `RABBITMQ_URL` com essa URL.
4. Faça o redeploy. Exchange, filas e bindings são criados na primeira
   conexão; o card *Mensageria* do admin deve aparecer como **Conectado**.

O prefixo `amqps` ativa o TLS automaticamente no Spring Boot.

### PostgreSQL

No Render, use o **Internal Hostname** do banco quando a aplicação estiver na
mesma rede, e a **External URL** apenas para acesso local com DBeaver ou
DataGrip. O Flyway aplica a V5 automaticamente no primeiro deploy desta
versão.

---

## Evolução por sprint

| Sprint | Entrega |
|---|---|
| 1–2 | API REST com JPA, validação, paginação, HATEOAS, cache e Swagger |
| 3 | Telas Thymeleaf, Flyway, Spring Security com três perfis, três fluxos de negócio, deploy no Render |
| 4 | Notificações assíncronas com RabbitMQ (retry, DLQ, idempotência), tela de notificações, status da fila no painel, consultas analíticas, refatoração das ações das telas, revisão de textos e acentuação da interface |

## Stack

Java 21 · Spring Boot 3.4.5 · Spring MVC · Spring Data JPA · Spring Security ·
Spring AMQP · RabbitMQ 3.13 · Thymeleaf · Flyway · PostgreSQL 16 · Bean
Validation · Lombok · springdoc-openapi · Docker
