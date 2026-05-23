# Challenge Pet — Plataforma de Saúde e Engajamento para Pets

> **FIAP — Java Advanced | Challenge 2025**
> Turma: 2TDSPM | Integrante: Leonardo Carvalho Santos

---

## Sumário

1. [Descrição Geral](#1-descrição-geral)
2. [Arquitetura da Aplicação](#2-arquitetura-da-aplicação)
3. [Diagrama de Entidade e Relacionamento (DER)](#3-diagrama-de-entidade-e-relacionamento-der)
4. [Diagrama de Classes das Entidades](#4-diagrama-de-classes-das-entidades)
5. [Stack Tecnológica](#5-stack-tecnológica)
6. [Pré-requisitos e Configuração](#6-pré-requisitos-e-configuração)
7. [Como Executar](#7-como-executar)
8. [Endpoints da API](#8-endpoints-da-api)
9. [Requisitos Técnicos Atendidos](#9-requisitos-técnicos-atendidos)
10. [Dados de Seed (Teste)](#10-dados-de-seed-teste)
11. [Documentação e Testes](#11-documentação-e-testes)
12. [Cronograma de Desenvolvimento](#12-cronograma-de-desenvolvimento)
13. [Distribuição da Pontuação](#13-distribuição-da-pontuação)

---

## 1. Descrição Geral

O **Challenge Pet** é uma API REST construída com **Java 21 + Spring Boot 3.4.5** que resolve o problema de fragmentação no cuidado de saúde animal. Tutores de pets frequentemente perdem controle sobre vacinas vencidas, retornos médicos esquecidos e falta de engajamento na rotina de atividades do animal.

### Problema resolvido

| Dor do tutor | Solução implementada |
|---|---|
| "Esqueci quando é a próxima vacina" | Lembretes automáticos criados ao cadastrar vacina com `nextDoseDate` |
| "Não sei se meu pet está saudável" | Score de saúde (0–100) calculado em tempo real por `PetHealthService` |
| "Falta consistência na rotina do pet" | Sistema de gamificação com streak diário e níveis (BEGINNER → LEGENDARY) |
| "Consultas clínicas espalhadas" | Prontuário eletrônico com histórico completo e busca por palavra-chave |
| "Preciso agendar retorno" | Lembrete de retorno criado automaticamente 6 meses após cada consulta |

### Diferenciais além do CRUD

- **Health Score** — métrica composta calculada a partir de 4 dimensões (veterinário vinculado, vacinas em dia, consulta recente, streak ativo)
- **Gamificação** — check-in diário com cálculo de streak, recorde pessoal e progressão de níveis
- **Auto-reminders** — criação automática de lembretes como side-effect de vacinas e prontuários
- **Cache inteligente** — resultado de saúde cacheado e invalidado seletivamente ao modificar dados do pet

---

## 2. Arquitetura da Aplicação

```
┌─────────────────────────────────────────────────────────────────┐
│                        Cliente (HTTP)                           │
│              Postman / Insomnia / Swagger UI                    │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTP/REST
┌───────────────────────────▼─────────────────────────────────────┐
│                     Controller Layer                            │
│  TutorController  PetController  VaccineController  ...         │
│           @RestController  @RequestMapping  @Valid              │
└───────────────────────────┬─────────────────────────────────────┘
                            │ DTO (records)
┌───────────────────────────▼─────────────────────────────────────┐
│                      Service Layer                              │
│  PetService  PetHealthService  ActivityCheckInService  ...      │
│         @Service  @Transactional  @Cacheable  @CacheEvict       │
└───────────────────────────┬─────────────────────────────────────┘
                            │ JPA Entities
┌───────────────────────────▼─────────────────────────────────────┐
│                    Repository Layer                             │
│  PetRepository  VaccineRepository  ClinicalRecordRepository ... │
│              JpaRepository  @Query (JPQL)                       │
└───────────────────────────┬─────────────────────────────────────┘
                            │ JDBC
┌───────────────────────────▼─────────────────────────────────────┐
│                   H2 In-Memory Database                         │
│               (schema gerado automaticamente pelo JPA)          │
└─────────────────────────────────────────────────────────────────┘

Componentes transversais:
  GlobalExceptionHandler (@RestControllerAdvice)  →  tratamento centralizado
  CacheConfig (@EnableCaching)                    →  ConcurrentMapCacheManager
  OpenApiConfig                                   →  Swagger UI
  DataInitializer (CommandLineRunner)             →  dados de seed
```

### Fluxo de uma requisição típica

```
POST /api/vaccines  →  VaccineController.create()
                    →  @Valid valida VaccineRequest (Bean Validation)
                    →  VaccineService.create()
                    →  vaccineRepository.save(vaccine)
                    →  [side-effect] reminderRepository.save(VACCINE reminder)
                    →  petHealthService.evictCache(petId)
                    →  retorna VaccineResponse (DTO) com 201 Created
```

---

## 3. Diagrama de Entidade e Relacionamento (DER)

```
┌─────────────────┐              ┌──────────────────────┐
│     tutors      │              │    veterinarians      │
│─────────────────│              │──────────────────────│
│ id         PK   │              │ id         PK        │
│ name            │              │ name                 │
│ email      UQ   │              │ crmv       UQ        │
│ phone           │              │ email                │
│ cpf        UQ   │              │ phone                │
│ created_at      │              │ specialty            │
└────────┬────────┘              └──────────┬───────────┘
         │ 1                               │ 0..1 (opcional)
         │                                 │
         │ N (obrigatório)                 │ N
         ▼                                 ▼
┌──────────────────────────────────────────────────────┐
│                        pets                          │
│──────────────────────────────────────────────────────│
│ id                    PK                             │
│ name                                                 │
│ species               (DOG|CAT|BIRD|RABBIT|...)      │
│ breed                                                │
│ birth_date                                           │
│ weight                DECIMAL(5,2)                   │
│ gender                (MALE|FEMALE)                  │
│ profile_public        DEFAULT false                  │
│ tutor_id              FK → tutors.id                 │
│ veterinarian_id       FK → veterinarians.id          │
│ created_at, updated_at                               │
└──┬────────┬──────────┬──────────────┬────────────────┘
   │        │          │              │
   │1       │1         │1             │1
   │        │          │              │
   │N       │N         │N             │N
   ▼        ▼          ▼              ▼
┌──────┐ ┌──────────┐ ┌─────────────┐ ┌──────────────────┐
│pet_  │ │ vaccines │ │  clinical_  │ │  activity_check  │
│streak│ │──────────│ │  records    │ │  ins             │
│──────│ │id     PK │ │─────────────│ │──────────────────│
│id PK │ │name      │ │id      PK   │ │id          PK    │
│curr. │ │manufact. │ │date         │ │date              │
│streak│ │app_date  │ │description  │ │activity_type     │
│long. │ │next_dose │ │diagnosis    │ │duration_min      │
│streak│ │cert_num  │ │treatment    │ │notes             │
│last_ │ │notes     │ │weight       │ │pet_id   FK       │
│check │ │pet_id FK │ │observations │ │UNIQUE(pet_id,dt) │
│level │ │vet_id FK │ │pet_id   FK  │ └──────────────────┘
│total │ └──────────┘ │vet_id   FK  │
│checks│              └─────────────┘
│pet_id│    ┌──────────────────────────┐
│ UNIQ │    │         reminders        │
└──────┘    │──────────────────────────│
            │ id          PK           │
            │ type  (VACCINE|CHECKUP|.)│
            │ due_date                 │
            │ message                  │
            │ status (PENDING|DONE|..) │
            │ pet_id   FK              │
            └──────────────────────────┘
```

### Relacionamentos e Constraints

| Relacionamento | Cardinalidade | Cascade | Observação |
|---|---|---|---|
| Tutor → Pet | 1:N | ALL + orphanRemoval | Deletar tutor remove todos os pets e sub-dados |
| Veterinarian → Pet | 1:N | nenhum | Veterinário pode ser nulo no pet |
| Pet → PetStreak | 1:1 | ALL + orphanRemoval | Criado automaticamente no primeiro check-in |
| Pet → Vaccine | 1:N | ALL + orphanRemoval | Vacinas pertencem ao pet |
| Pet → ClinicalRecord | 1:N | ALL + orphanRemoval | Prontuário pertence ao pet |
| Pet → ActivityCheckIn | 1:N | ALL + orphanRemoval | UNIQUE(pet_id, date) impede dois check-ins no mesmo dia |
| Pet → Reminder | 1:N | ALL + orphanRemoval | Lembretes manuais e automáticos |

---

## 4. Diagrama de Classes das Entidades

```
┌──────────────────────────────────────────────────────────────────┐
│                           <<Entity>>                             │
│                             Tutor                                │
│──────────────────────────────────────────────────────────────────│
│ - id: Long                                                       │
│ - name: String                                                   │
│ - email: String                                                  │
│ - phone: String                                                  │
│ - cpf: String                                                    │
│ - createdAt: LocalDateTime                                       │
│ - pets: List<Pet>                                                │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                           <<Entity>>                             │
│                          Veterinarian                            │
│──────────────────────────────────────────────────────────────────│
│ - id: Long                                                       │
│ - name: String                                                   │
│ - crmv: String                                                   │
│ - email: String                                                  │
│ - phone: String                                                  │
│ - specialty: String                                              │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                           <<Entity>>                             │
│                              Pet                                 │
│──────────────────────────────────────────────────────────────────│
│ - id: Long                                                       │
│ - name: String                                                   │
│ - species: Species (enum)                                        │
│ - breed: String                                                  │
│ - birthDate: LocalDate                                           │
│ - weight: BigDecimal                                             │
│ - gender: Gender (enum)                                          │
│ - profilePublic: boolean                                         │
│ - tutor: Tutor  (ManyToOne)                                      │
│ - veterinarian: Veterinarian  (ManyToOne)                        │
│ - streak: PetStreak  (OneToOne)                                  │
│ - vaccines: List<Vaccine>  (OneToMany)                           │
│ - clinicalRecords: List<ClinicalRecord>  (OneToMany)             │
│ - checkIns: List<ActivityCheckIn>  (OneToMany)                   │
│ - reminders: List<Reminder>  (OneToMany)                         │
│ - createdAt: LocalDateTime                                       │
│ - updatedAt: LocalDateTime                                       │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────┐   ┌──────────────────────────────────────┐
│    <<Entity>>        │   │             <<Entity>>               │
│      Vaccine         │   │           ClinicalRecord             │
│──────────────────────│   │──────────────────────────────────────│
│ id: Long             │   │ id: Long                             │
│ name: String         │   │ date: LocalDate                      │
│ manufacturer: String │   │ description: String                  │
│ applicationDate      │   │ diagnosis: String                    │
│ nextDoseDate         │   │ treatment: String                    │
│ certificateNumber    │   │ weight: BigDecimal                   │
│ notes: String        │   │ observations: String                 │
│ pet: Pet             │   │ pet: Pet                             │
│ veterinarian: Vet    │   │ veterinarian: Veterinarian           │
└──────────────────────┘   └──────────────────────────────────────┘

┌──────────────────────┐   ┌──────────────────────────────────────┐
│    <<Entity>>        │   │             <<Entity>>               │
│   ActivityCheckIn    │   │              PetStreak               │
│──────────────────────│   │──────────────────────────────────────│
│ id: Long             │   │ id: Long                             │
│ date: LocalDate      │   │ currentStreak: int                   │
│ activityType: enum   │   │ longestStreak: int                   │
│ durationMinutes: int │   │ lastCheckIn: LocalDate               │
│ notes: String        │   │ level: PetLevel (enum)               │
│ pet: Pet             │   │ totalCheckIns: int                   │
└──────────────────────┘   │ pet: Pet (OneToOne)                  │
                           └──────────────────────────────────────┘

┌──────────────────────┐
│    <<Entity>>        │
│      Reminder        │
│──────────────────────│
│ id: Long             │
│ type: ReminderType   │
│ dueDate: LocalDate   │
│ message: String      │
│ status: ReminderStatus│
│ pet: Pet             │
└──────────────────────┘
```

### Enums do domínio

| Enum | Valores |
|---|---|
| `Species` | DOG, CAT, BIRD, RABBIT, FISH, REPTILE, HAMSTER, OTHER |
| `Gender` | MALE, FEMALE |
| `ActivityType` | WALK, FEEDING, BATH, PLAY, TRAINING, VET_VISIT, MEDICATION, OTHER |
| `ReminderType` | VACCINE, CHECKUP, MEDICATION, RETURN, DEWORMING, OTHER |
| `ReminderStatus` | PENDING, SENT, DONE, DISMISSED |
| `PetLevel` | BEGINNER(0), BRONZE(7), SILVER(14), GOLD(30), PLATINUM(90), DIAMOND(180), LEGENDARY(365) |

---

## 5. Stack Tecnológica

| Componente | Tecnologia | Versão |
|---|---|---|
| Linguagem | Java | 21 (LTS) |
| Framework | Spring Boot | 3.4.5 |
| Persistência | Spring Data JPA / Hibernate | 6.x |
| Banco de dados | H2 In-Memory | Runtime |
| Validação | Jakarta Bean Validation | 3.x |
| Cache | Spring Cache (ConcurrentMapCacheManager) | — |
| Documentação | springdoc-openapi (Swagger UI) | 2.8.3 |
| Boilerplate | Lombok | — |
| Build | Maven | 3.9+ |
| Atualizações | Spring Boot Actuator | — |
| Dev | Spring Boot DevTools | — |

---

## 6. Pré-requisitos e Configuração

### Requisitos

- **JDK 21** — obrigatório (Spring Boot 3.x requer Java 17+; projeto usa features do 21)
- **Maven 3.9+** — ou use o wrapper incluído (`mvnw.cmd` / `mvnw`)

### Configurar JAVA_HOME (se necessário)

Se o sistema tiver o JRE 8 como padrão, é necessário apontar para o JDK 21 antes de compilar:

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME = "C:\Users\leona\.jdks\ms-21.0.11"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version   # deve exibir: openjdk 21...
```

**Linux/macOS:**
```bash
export JAVA_HOME=/path/to/jdk-21
export PATH=$JAVA_HOME/bin:$PATH
```

### Configurações da aplicação (`application.properties`)

```properties
# Banco de dados H2 em memória
spring.datasource.url=jdbc:h2:mem:petdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA — recria o schema a cada start
spring.jpa.hibernate.ddl-auto=create-drop

# Cache simples (ConcurrentMapCacheManager)
spring.cache.type=simple

# Swagger UI
springdoc.swagger-ui.path=/swagger-ui.html

# Porta
server.port=8080
```

---

## 7. Como Executar

### Compilar e executar

```powershell
# Windows (PowerShell) — configurar JDK 21 primeiro
$env:JAVA_HOME = "C:\Users\leona\.jdks\ms-21.0.11"

# Compilar
.\mvnw.cmd compile

# Executar
.\mvnw.cmd spring-boot:run
```

```bash
# Linux/macOS
./mvnw spring-boot:run
```

### Verificar que está rodando

Após o start, a aplicação carrega automaticamente os **dados de seed** (2 tutores, 2 veterinários, 3 pets com vacinas, prontuários e lembretes).

| Interface | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| H2 Console | http://localhost:8080/h2-console |
| Health Check | http://localhost:8080/actuator/health |

**H2 Console — configurações de conexão:**
- JDBC URL: `jdbc:h2:mem:petdb`
- Username: `sa`
- Password: *(vazio)*

### Executar testes

```powershell
$env:JAVA_HOME = "C:\Users\leona\.jdks\ms-21.0.11"
.\mvnw.cmd test
```

---

## 8. Endpoints da API

A API segue o **Richardson Maturity Model nível 2**: recursos identificados por URI, verbos HTTP semânticos (GET, POST, PUT, PATCH, DELETE) e códigos de status adequados (200, 201, 204, 400, 404, 500).

Base URL: `http://localhost:8080`

---

### 8.1 Tutores — `/api/tutors`

| Método | Endpoint | Descrição | Status |
|---|---|---|---|
| GET | `/api/tutors` | Listar tutores (paginado, filtros: name, email, cpf) | 200 |
| GET | `/api/tutors/{id}` | Buscar tutor por ID | 200 / 404 |
| POST | `/api/tutors` | Cadastrar tutor | 201 |
| PUT | `/api/tutors/{id}` | Atualizar tutor | 200 / 404 |
| DELETE | `/api/tutors/{id}` | Remover tutor | 204 / 404 |

**POST /api/tutors — body:**
```json
{
  "name": "Maria Silva",
  "email": "maria@email.com",
  "phone": "11912345678",
  "cpf": "12345678901"
}
```

**GET com paginação e filtros:**
```
GET /api/tutors?name=Maria&page=0&size=10&sort=name,asc
```

---

### 8.2 Veterinários — `/api/veterinarians`

| Método | Endpoint | Descrição | Status |
|---|---|---|---|
| GET | `/api/veterinarians` | Listar veterinários (paginado, filtros: name, specialty) | 200 |
| GET | `/api/veterinarians/{id}` | Buscar veterinário por ID | 200 / 404 |
| POST | `/api/veterinarians` | Cadastrar veterinário | 201 |
| PUT | `/api/veterinarians/{id}` | Atualizar veterinário | 200 / 404 |
| DELETE | `/api/veterinarians/{id}` | Remover veterinário | 204 / 404 |

**POST /api/veterinarians — body:**
```json
{
  "name": "Dra. Ana Souza",
  "crmv": "SP-12345",
  "email": "ana@clinicapet.com",
  "phone": "11987654321",
  "specialty": "Clinica Geral"
}
```

---

### 8.3 Pets — `/api/pets`

| Método | Endpoint | Descrição | Status |
|---|---|---|---|
| GET | `/api/pets` | Listar pets (paginado, filtros: tutorId, species, breed, name) | 200 |
| GET | `/api/pets/{id}` | Buscar pet por ID | 200 / 404 |
| GET | `/api/pets/{id}/health` | Score de saúde do pet *(cacheado)* | 200 / 404 |
| POST | `/api/pets` | Cadastrar pet | 201 |
| PUT | `/api/pets/{id}` | Atualizar pet | 200 / 404 |
| DELETE | `/api/pets/{id}` | Remover pet | 204 / 404 |

**POST /api/pets — body:**
```json
{
  "name": "Rex",
  "species": "DOG",
  "breed": "Labrador",
  "birthDate": "2020-03-15",
  "weight": 28.5,
  "gender": "MALE",
  "profilePublic": false,
  "tutorId": 1,
  "veterinarianId": 1
}
```

**GET /api/pets/{id}/health — resposta:**
```json
{
  "petId": 1,
  "petName": "Rex",
  "species": "DOG",
  "healthScore": 75,
  "healthMessage": "Saude boa! Mantenha os cuidados.",
  "profileCompletion": 88,
  "profileMessage": "Perfil quase completo.",
  "currentStreak": 5,
  "longestStreak": 7,
  "level": "BRONZE",
  "levelLabel": "Bronze",
  "totalVaccines": 2,
  "overdueVaccines": 0,
  "totalClinicalRecords": 1,
  "hasRecentCheckup": true,
  "pendingReminders": 1
}
```

---

### 8.4 Vacinas — `/api/vaccines`

| Método | Endpoint | Descrição | Status |
|---|---|---|---|
| GET | `/api/vaccines/pet/{petId}` | Listar vacinas do pet (paginado) | 200 |
| GET | `/api/vaccines/{id}` | Buscar vacina por ID | 200 / 404 |
| GET | `/api/vaccines/pet/{petId}/overdue` | Vacinas vencidas do pet | 200 |
| GET | `/api/vaccines/pet/{petId}/upcoming` | Vacinas nos próximos N dias | 200 |
| POST | `/api/vaccines` | Registrar vacina *(cria lembrete automático se nextDoseDate informado)* | 201 |
| PUT | `/api/vaccines/{id}` | Atualizar vacina | 200 / 404 |
| DELETE | `/api/vaccines/{id}` | Remover vacina | 204 / 404 |

**POST /api/vaccines — body:**
```json
{
  "name": "V10",
  "manufacturer": "MSD Animal Health",
  "applicationDate": "2025-05-15",
  "nextDoseDate": "2026-05-15",
  "certificateNumber": "CERT-001",
  "notes": "Sem reacoes adversas",
  "petId": 1,
  "veterinarianId": 1
}
```

---

### 8.5 Prontuários Clínicos — `/api/clinical-records`

| Método | Endpoint | Descrição | Status |
|---|---|---|---|
| GET | `/api/clinical-records/pet/{petId}` | Listar prontuários do pet (paginado, filtro keyword) | 200 |
| GET | `/api/clinical-records/{id}` | Buscar prontuário por ID | 200 / 404 |
| POST | `/api/clinical-records` | Criar prontuário *(cria lembrete de retorno automático em 6 meses)* | 201 |
| PUT | `/api/clinical-records/{id}` | Atualizar prontuário | 200 / 404 |
| DELETE | `/api/clinical-records/{id}` | Remover prontuário | 204 / 404 |

**POST /api/clinical-records — body:**
```json
{
  "date": "2025-05-20",
  "description": "Consulta de rotina anual",
  "diagnosis": "Saudavel",
  "treatment": "Nenhum tratamento necessario",
  "weight": 28.5,
  "observations": "Pet bem disposto",
  "petId": 1,
  "veterinarianId": 1
}
```

---

### 8.6 Check-ins (Gamificação) — `/api/checkins`

| Método | Endpoint | Descrição | Status |
|---|---|---|---|
| GET | `/api/checkins/pet/{petId}` | Listar check-ins (paginado, filtro por datas) | 200 |
| GET | `/api/checkins/pet/{petId}/streak` | Consultar streak atual, recorde e nível | 200 |
| POST | `/api/checkins/pet/{petId}` | Realizar check-in diário *(um por dia)* | 201 / 400 |
| DELETE | `/api/checkins/{id}` | Remover check-in | 204 / 404 |

**POST /api/checkins/pet/{petId} — body:**
```json
{
  "activityType": "WALK",
  "durationMinutes": 30,
  "notes": "Passeio no parque"
}
```

**GET /api/checkins/pet/{petId}/streak — resposta:**
```json
{
  "petId": 1,
  "petName": "Rex",
  "currentStreak": 7,
  "longestStreak": 14,
  "level": "BRONZE",
  "levelLabel": "Bronze",
  "totalCheckIns": 45,
  "checkedInToday": false,
  "nextLevel": "SILVER",
  "daysToNextLevel": 7
}
```

---

### 8.7 Lembretes — `/api/reminders`

| Método | Endpoint | Descrição | Status |
|---|---|---|---|
| GET | `/api/reminders/pet/{petId}` | Listar lembretes (paginado, filtros: status, type) | 200 |
| GET | `/api/reminders/{id}` | Buscar lembrete por ID | 200 / 404 |
| GET | `/api/reminders/pet/{petId}/upcoming` | Lembretes pendentes nos próximos N dias | 200 |
| POST | `/api/reminders` | Criar lembrete manual | 201 |
| PATCH | `/api/reminders/{id}/status` | Atualizar status do lembrete | 200 / 400 / 404 |
| DELETE | `/api/reminders/{id}` | Remover lembrete | 204 / 404 |

**POST /api/reminders — body:**
```json
{
  "type": "CHECKUP",
  "dueDate": "2026-01-15",
  "message": "Check-up anual",
  "petId": 1
}
```

**PATCH /api/reminders/{id}/status:**
```
PATCH /api/reminders/1/status?status=DONE
```

---

### Respostas de erro padronizadas

**400 Bad Request (validação):**
```json
{
  "timestamp": "2025-05-21T10:30:00",
  "status": 400,
  "error": "Validation Error",
  "message": "Erro de validacao nos campos enviados",
  "path": "/api/pets",
  "fieldErrors": [
    { "field": "email", "message": "must be a well-formed email address" },
    { "field": "cpf", "message": "CPF deve ter 11 digitos" }
  ]
}
```

**404 Not Found:**
```json
{
  "timestamp": "2025-05-21T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Pet nao encontrado(a) com id: 99",
  "path": "/api/pets/99",
  "fieldErrors": null
}
```

---

## 9. Requisitos Técnicos Atendidos

| Requisito | Implementação | Localização |
|---|---|---|
| **Bean Validation** | `@NotBlank`, `@Email`, `@Past`, `@Future`, `@PastOrPresent`, `@Pattern`, `@Size`, `@DecimalMin/Max`, `@Min/Max`, `@NotNull` | Todos os `*Request.java` + Entities |
| **Paginação** | `Pageable` + `Page<T>` em todos os endpoints de listagem | Todos os controllers |
| **Ordenação** | `?sort=name,asc` ou `?sort=dueDate,desc` via Spring Data | Todos os controllers com `@PageableDefault` |
| **Busca com parâmetros** | `@RequestParam` opcional com JPQL dinâmico | `PetRepository.findByFilters()`, `TutorRepository`, etc. |
| **Cache** | `@Cacheable("petHealthSummary")` + `@CacheEvict` seletivo | `PetHealthService`, `VeterinarianService` |
| **Tratamento de erros** | `@RestControllerAdvice` + `ResourceNotFoundException` + `BusinessException` | `GlobalExceptionHandler` |
| **DTOs** | Records Java 21 para request e response, com factory `from(Entity)` | `dto/request/`, `dto/response/` |
| **Swagger** | springdoc-openapi 2.8.3 com `@Tag` e `@Operation` | `OpenApiConfig` + controllers |
| **JPQL** | `@Query` com parâmetros opcionais via `:#{#param}` | Todos os repositórios |
| **JPA Mappings** | `@OneToMany`, `@ManyToOne`, `@OneToOne`, Cascade, LAZY | Todas as entidades |
| **POO** | Herança implícita, encapsulamento, abstração via interfaces | Toda a aplicação |
| **RESTful** | Verbos corretos (PATCH para atualização parcial), status codes semânticos | `ReminderController.updateStatus()` |

---

## 10. Dados de Seed (Teste)

A aplicação insere automaticamente dados de exemplo ao iniciar. Esses dados permitem testar todos os endpoints sem precisar cadastrar nada manualmente.

| Entidade | Dados inseridos |
|---|---|
| Veterinários | Dra. Ana Souza (SP-12345, Clínica Geral), Dr. Carlos Lima (SP-67890, Dermatologia) |
| Tutores | Maria Silva (CPF: 12345678901), João Santos (CPF: 98765432100) |
| Pets | Rex (Labrador, tutorId=1), Mia (Siamês, tutorId=1), Thor (Golden Retriever, tutorId=2) |
| Vacinas | V10 e Antirrábica para Rex; V4 Felina para Mia |
| Prontuários | Consulta de rotina para Rex; Dermatite para Thor |
| Lembretes | Check-up para Rex (em 15 dias); Reforço de vacina para Mia (em 7 dias) |

**IDs iniciais para testes:**
- `tutorId=1` → Maria Silva
- `tutorId=2` → João Santos
- `veterinarianId=1` → Dra. Ana Souza
- `veterinarianId=2` → Dr. Carlos Lima
- `petId=1` → Rex (DOG)
- `petId=2` → Mia (CAT)
- `petId=3` → Thor (DOG)

---

## 11. Documentação e Testes

### Swagger UI

Acesse `http://localhost:8080/swagger-ui.html` para explorar e testar todos os endpoints interativamente.

### Coleção Postman

O arquivo `docs/challenge_pet_postman.json` contém todas as requisições organizadas por recurso, prontas para importar no Postman ou Insomnia.

**Como importar no Postman:**
1. Abrir Postman
2. Clicar em **Import** (botão no canto superior esquerdo)
3. Selecionar o arquivo `docs/challenge_pet_postman.json`
4. A coleção **"Challenge Pet API"** aparecerá com todas as 30+ requisições organizadas

**Como importar no Insomnia:**
1. Abrir Insomnia
2. Ir em **File → Import Data → From File**
3. Selecionar `docs/challenge_pet_postman.json`

### Pasta `docs/`

```
docs/
├── challenge_pet_postman.json   — Coleção Postman com todos os endpoints
└── cronograma.md                — Cronograma de desenvolvimento
```

---

## 12. Cronograma de Desenvolvimento

Ver arquivo completo: [`docs/cronograma.md`](docs/cronograma.md)

| Sprint | Período | Atividade | Status |
|---|---|---|---|
| 1 | 28/04 – 05/05 | Definição do problema, modelagem de domínio, DER | Concluído |
| 2 | 06/05 – 12/05 | Setup do projeto, entidades JPA, repositórios | Concluído |
| 3 | 13/05 – 19/05 | Services, controllers, validações, DTOs | Concluído |
| 4 | 20/05 – 21/05 | Cache, Swagger, tratamento de erros, README, Postman | Concluído |

---

## 13. Distribuição da Pontuação

| Critério | Pontos | Implementação |
|---|---|---|
| Cronograma de desenvolvimento | até 5 pts | [`docs/cronograma.md`](docs/cronograma.md) |
| Arquitetura, DER, Diagrama de Classes | até 10 pts | Seções 2, 3 e 4 deste README |
| Implementação das Entidades JPA | até 40 pts | 8 entidades mapeadas com JPA, relacionamentos, constraints |
| RESTful (modelo de maturidade) | até 15 pts | Nível 2 — URIs, verbos HTTP, status codes corretos |
| Artefatos no Github | até 10 pts | Repositório público com todo o código-fonte |
| Link para repositório Github | até 10 pts | https://github.com/LeonardoCJS/petcare-java |
| Testes documentados (Postman/Insomnia) | até 10 pts | `docs/challenge_pet_postman.json` com 30+ requisições |

---

## Estrutura do Projeto

```
challenge/
├── src/
│   ├── main/
│   │   ├── java/br/com/fiap/challenge/
│   │   │   ├── ChallengeApplication.java
│   │   │   ├── config/
│   │   │   │   ├── CacheConfig.java
│   │   │   │   ├── DataInitializer.java
│   │   │   │   └── OpenApiConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── TutorController.java
│   │   │   │   ├── VeterinarianController.java
│   │   │   │   ├── PetController.java
│   │   │   │   ├── VaccineController.java
│   │   │   │   ├── ClinicalRecordController.java
│   │   │   │   ├── ActivityCheckInController.java
│   │   │   │   └── ReminderController.java
│   │   │   ├── dto/
│   │   │   │   ├── request/   (7 records)
│   │   │   │   └── response/  (9 records)
│   │   │   ├── entity/        (8 entidades JPA)
│   │   │   ├── enums/         (6 enums)
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── BusinessException.java
│   │   │   │   └── ErrorResponse.java
│   │   │   ├── repository/    (8 repositórios JPA)
│   │   │   └── service/       (8 services)
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/br/com/fiap/challenge/
│           └── ChallengeApplicationTests.java
├── docs/
│   ├── challenge_pet_postman.json
│   └── cronograma.md
├── pom.xml
└── README.md
```
