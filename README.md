# PetHealth — Challenge FIAP | Java Advanced

Aplicação web em Spring Boot para gestão de saúde e engajamento de pets. Reúne o
histórico clínico do animal (vacinas, prontuários, lembretes) e um sistema de
check-in diário que transforma o cuidado preventivo em hábito.

Entrega da 3ª sprint: camada de visualização com Thymeleaf, versionamento de
banco com Flyway e controle de acesso com Spring Security.

---

## Sumário

- [Como executar](#como-executar)
- [Usuários de demonstração](#usuários-de-demonstração)
- [O que foi implementado](#o-que-foi-implementado)
- [Fluxos completos](#fluxos-completos)
- [Arquitetura](#arquitetura)
- [Banco de dados e migrations](#banco-de-dados-e-migrations)
- [Segurança](#segurança)
- [API REST](#api-rest)
- [Testes](#testes)
- [Deploy](#deploy)

---

## Como executar

### Pré-requisitos

| Ferramenta | Versão |
|---|---|
| JDK | 21 |
| Maven | 3.9+ (ou o wrapper `./mvnw` incluído) |
| Docker | qualquer versão recente, para subir o PostgreSQL |

### Passo a passo

**1. Clonar o repositório**

```bash
git clone https://github.com/poiuytrewq01/SPRINT-JAVA-.git
cd SPRINT-JAVA-
```

**2. Subir o banco de dados**

```bash
docker compose up -d
```

Isso cria um PostgreSQL 16 em `localhost:5432` com database, usuário e senha
`pethealth` — exatamente os valores que a aplicação usa por padrão.

> Se preferir usar um PostgreSQL já instalado, crie o database e informe os
> dados pelas variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`.

**3. Executar a aplicação**

```bash
./mvnw spring-boot:run
```

No Windows (PowerShell ou CMD):

```bat
mvnw.cmd spring-boot:run
```

Na primeira execução o Flyway cria todo o schema e insere os dados de
demonstração. O log mostra as quatro migrations sendo aplicadas:

```
Migrating schema "public" to version "1 - create core schema"
Migrating schema "public" to version "2 - create security schema"
Migrating schema "public" to version "3 - create vet link requests"
Migrating schema "public" to version "4 - seed demo data"
```

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

Todos usam a senha **`senha123`**. As contas são criadas pela migration `V4`, com
as senhas já armazenadas como hash BCrypt.

| Perfil | E-mail | O que enxerga |
|---|---|---|
| Tutor | `maria@email.com` | Rex e Mia — tem uma solicitação de vínculo pendente |
| Tutor | `joao@email.com` | Thor — com uma vacina vencida |
| Veterinário | `ana@clinicapet.com` | Rex como paciente e a solicitação da Mia para aprovar |
| Veterinário | `carlos@clinicapet.com` | Thor como paciente |
| Administrador | `admin@pethealth.com` | Painel consolidado e contas de acesso |

Os dados de demonstração usam datas relativas a hoje, então o cenário continua
coerente independentemente de quando o banco for criado. O Rex chega com uma
sequência de 4 dias, sendo o último check-in ontem — registrar um check-in leva
a sequência a 5 na hora.

---

## O que foi implementado

### 1. Frontend (Thymeleaf)

Renderização no servidor, com um layout central e páginas que declaram apenas o
próprio conteúdo.

- `layout.html` define o fragmento `pagina(titulo, conteudo)`, usado por todas as
  telas internas. Cabeçalho, menu, avisos e rodapé existem em um lugar só.
- O menu se adapta ao perfil via `sec:authorize`, do dialeto
  `thymeleaf-extras-springsecurity6`.
- Formulários usam `th:object` e `th:field`, com reexibição dos valores digitados
  e das mensagens de erro quando a validação falha.
- CSS próprio, sem framework externo, organizado por tokens e componentes.
  Layout responsivo com CSS Grid.

### 2. Flyway

Quatro migrations incrementais, sem `ddl-auto` gerando schema:

| Versão | Conteúdo |
|---|---|
| `V1__create_core_schema.sql` | Tabelas do domínio, chaves estrangeiras e índices |
| `V2__create_security_schema.sql` | Tabela `users` com constraint de coerência perfil/vínculo |
| `V3__create_vet_link_requests.sql` | Solicitações de vínculo, com índice único parcial |
| `V4__seed_demo_data.sql` | Carga inicial de demonstração |

O Hibernate roda com `ddl-auto: validate` — ele confere se as entidades batem
com o schema, mas nunca o altera.

### 3. Spring Security

- Três perfis: `TUTOR`, `VETERINARIAN` e `ADMIN`, com áreas separadas.
- Senhas em BCrypt (custo 10).
- Proteção de rotas por perfil e verificação adicional de propriedade do dado.
- Duas cadeias de filtros: sessão + CSRF + formulário para o site; autenticação
  básica sem estado para a API.
- Redirecionamento pós-login conforme o perfil e página própria de acesso negado.

### 4. Funcionalidades completas

Três fluxos com estado, transição e regra de negócio, além do CRUD — detalhados
na próxima seção. Validação em duas camadas: Bean Validation nos formulários e
regras de domínio nos services.

---

## Fluxos completos

### Fluxo 1 — Check-in diário e gamificação

**Quem:** tutor · **Onde:** `/tutor/pets/{id}`

O tutor registra uma atividade do pet. O `ActivityCheckInService` valida que
ainda não houve check-in hoje, grava o registro e recalcula a sequência:

- último check-in foi ontem → a sequência cresce;
- houve um dia de intervalo → a sequência volta a 1;
- o recorde histórico é atualizado quando superado;
- o nível (Iniciante → Bronze → Prata → … → Lendário) é derivado da sequência.

A regra "um check-in por pet por dia" está no service **e** como constraint única
no banco: a validação em código dá a mensagem clara, e a constraint protege
contra requisições concorrentes.

### Fluxo 2 — Solicitação e aprovação de vínculo

**Quem:** tutor e veterinário · **Onde:** `/tutor/vinculos` e `/vet/solicitacoes`

Resolve o problema de "vínculo fraco entre clínica e tutor" levantado na
proposta. Antes, o veterinário do pet era um campo que o tutor preenchia
sozinho, sem que o profissional soubesse.

```
        tutor solicita                 veterinário decide
  ────────────────────────►  PENDENTE  ────────────────────►  APROVADA
                                │                             (vínculo criado)
                                ├──────────────────────────►  RECUSADA
                                │
                                └── tutor desiste ─────────►  CANCELADA
```

Só a aprovação altera `pets.veterinarian_id`, e as duas escritas acontecem na
mesma transação. Os estados finais são imutáveis — a regra de transição vive no
próprio enum `LinkRequestStatus`, não espalhada em condicionais.

Efeito colateral visível: o vínculo vale 25 dos 100 pontos do score de saúde, e
o cache do resumo é invalidado na aprovação.

### Fluxo 3 — Atendimento clínico e retorno automático

**Quem:** veterinário e tutor · **Onde:** `/vet/pacientes/{id}`

O veterinário registra o atendimento. Na mesma transação, o
`ClinicalRecordService` agenda um lembrete de retorno para seis meses depois, e
o lembrete aparece na tela do tutor, que pode marcá-lo como concluído.

Acesso ao prontuário exige vínculo aprovado (fluxo 2): um veterinário não
enxerga o paciente de outro.

---

## Arquitetura

```
br.com.fiap.challenge
├── config/         SecurityConfig, CacheConfig, OpenApiConfig
├── controller/     @RestController — API REST (JSON)
├── web/            @Controller — telas Thymeleaf (HTML)
├── service/        regras de negócio e transações
├── repository/     Spring Data JPA
├── entity/         mapeamento objeto-relacional
├── dto/
│   ├── form/       formulários das telas
│   ├── request/    entrada da API
│   └── response/   saída da API
├── enums/          Role, Species, LinkRequestStatus, PetLevel, …
├── exception/      ApiExceptionHandler (JSON) e WebExceptionHandler (HTML)
└── security/       AuthenticatedUser, UserDetailsService, success handler
```

Decisões que valem registro:

**`web/` separado de `controller/`.** Os dois clientes têm necessidades
diferentes — um recebe HTML e redirects, o outro JSON e códigos de status. A
separação por pacote permite que cada um tenha o seu tratamento de erros, via
`@ControllerAdvice` com `basePackages`.

**Autenticação separada do domínio.** `User` guarda credenciais e perfil;
`Tutor` e `Veterinarian` continuam sendo os registros de negócio. Colocar senha
nas duas entidades duplicaria os campos de credencial e obrigaria o
`UserDetailsService` a consultar duas tabelas a cada login.

**`PetAccessGuard` como componente próprio.** Proteger a rota por perfil garante
apenas que quem entrou em `/tutor/**` é um tutor — não que aquele pet seja dele.
A verificação de propriedade do registro é usada por controllers e services, e
por isso vive em um lugar só.

**`open-in-view` desligado.** Evita consultas disparadas durante a renderização.
Em troca, cada consulta declara com `@EntityGraph` o que a tela vai usar.

---

## Banco de dados e migrations

### Modelo

```
users ──┬─(1:1)─► tutors ──(1:N)─► pets ──┬──(1:N)─► vaccines
        │                            │     ├──(1:N)─► clinical_records
        └─(1:1)─► veterinarians ◄────┤     ├──(1:N)─► reminders
                        ▲            │     ├──(1:N)─► activity_check_ins
                        │            │     └──(1:1)─► pet_streaks
                        └────────── vet_link_requests
```

### Integridade garantida no banco

| Constraint | O que impede |
|---|---|
| `ck_users_role_link` | Perfil incoerente com o vínculo (um `TUTOR` apontando para veterinário) |
| `uk_check_in_pet_date` | Dois check-ins do mesmo pet no mesmo dia |
| `uk_vet_link_pending` | Segunda solicitação pendente para o mesmo par pet/veterinário |
| `uk_users_email`, `uk_tutors_cpf`, `uk_veterinarians_crmv` | Duplicidade de identificadores |

`uk_vet_link_pending` é um índice único **parcial** (`WHERE status = 'PENDING'`):
bloqueia duplicatas pendentes, mas permite o histórico de solicitações já
respondidas para o mesmo par.

### Convenção de nomes

`V{versão}__{descrição_em_snake_case}.sql` — prefixo `V` maiúsculo, dois
underscores como separador.

> **Nunca** edite uma migration já aplicada. O Flyway guarda o checksum de cada
> arquivo em `flyway_schema_history` e recusa a inicialização se um script
> executado mudar. Alterações de schema entram sempre como uma nova versão.

---

## Segurança

### Regras de rota

| Rota | Quem acessa |
|---|---|
| `/`, `/login`, `/css/**` | Público |
| `/tutor/**` | `ROLE_TUTOR` |
| `/vet/**` | `ROLE_VETERINARIAN` |
| `/admin/**`, `/swagger-ui/**` | `ROLE_ADMIN` |
| `/api/**` | Autenticado (`DELETE` exige `ROLE_ADMIN`) |
| qualquer outra | Autenticado |

A ordem importa: as regras são avaliadas na sequência declarada e `anyRequest()`
fica por último. A política é negar por padrão.

### Duas camadas de autorização

Perfil e propriedade do dado são coisas diferentes. `/tutor/pets/42` só é
liberada para um tutor — mas o pet 42 pode ser de outra pessoa. Por isso toda
rota que recebe um id chama o `PetAccessGuard`, e o identificador do tutor vem
sempre de `@AuthenticationPrincipal`, nunca da URL ou do formulário.

### Proteção contra CSRF

Ativa na cadeia web. O Thymeleaf insere o token automaticamente em todo
formulário com `th:action` e `method="post"` — inclusive o de logout, que é um
POST justamente por isso.

Na cadeia da API o CSRF está desativado: ela é stateless e autenticada por
credencial em cada requisição, então não existe sessão de navegador para ser
explorada.

---

## API REST

A API da sprint anterior continua ativa em `/api/**`, agora autenticada.

```bash
curl -u ana@clinicapet.com:senha123 http://localhost:8080/api/pets
curl -u ana@clinicapet.com:senha123 http://localhost:8080/api/pets/1/health
```

O Swagger UI (`/swagger-ui.html`, perfil ADMIN) já vem configurado com o esquema
de autenticação básica — use o botão **Authorize** antes do "Try it out".

---

## Testes

```bash
./mvnw test
```

Os testes rodam no perfil `test`, com H2 em memória e Flyway desligado — não
exigem um PostgreSQL disponível na máquina.

---

## Deploy

A aplicação lê a configuração de banco por variáveis de ambiente, o que permite
publicar sem alterar código:

| Variável | Exemplo |
|---|---|
| `DB_URL` | `jdbc:postgresql://host:5432/pethealth` |
| `DB_USERNAME` | `pethealth` |
| `DB_PASSWORD` | *(senha do serviço)* |
| `PORT` | definida pela plataforma |

No Render, use o **Internal Hostname** do banco quando a aplicação estiver
hospedada na mesma rede, e a **External URL** apenas para acesso local com
DBeaver ou DataGrip. O Flyway aplica as migrations automaticamente na primeira
inicialização.

---

## Stack

Java 21 · Spring Boot 3.4.5 · Spring MVC · Spring Data JPA · Spring Security ·
Thymeleaf · Flyway · PostgreSQL 16 · Bean Validation · Lombok · springdoc-openapi
