# Arquitetura — IA-Financial

Sistema de controle financeiro pessoal com IA, em **Java 21 + Spring Boot 3.5.6**, usando
**Arquitetura Hexagonal (Ports and Adapters)** e **MongoDB**.

> Este documento é a visão de desenho (diagramas, fluxos e decisões). Os scripts de banco estão em
> [ddl-mongodb.md](ddl-mongodb.md) e [dml-mongodb.md](dml-mongodb.md); exemplos de uso em
> [postman/](postman/README.md).

---

## 1. Stack

| Camada | Tecnologia |
|---|---|
| Linguagem / Runtime | Java 21 |
| Framework | Spring Boot 3.5.16 (Web, Data MongoDB, Validation, Scheduling, Security, Actuator) |
| Banco | MongoDB 7.0 (coleções com validadores `$jsonSchema` + índices) |
| Autenticação | JWT HS256 (jjwt 0.13.0), BCrypt (spring-security-crypto), refresh token rotativo |
| Mapeamento | MapStruct (DTO ↔ domínio ↔ documento) |
| Documentação | springdoc-openapi (Swagger UI em `/swagger-ui.html`) |
| IA | Ollama (HTTP, `/api/generate`) atrás de `AiProviderPort` |
| Testes | JUnit 5, Mockito, AssertJ, MockMvc, flapdoodle (Mongo embutido), WireMock, JaCoCo (gate 90%) |

---

## 2. Visão Hexagonal

```mermaid
flowchart TD
    subgraph adapters["Camada de Adapters (infraestrutura)"]
        direction LR
        REST["REST Controllers<br/>/api/v1 (adapters.in.rest)"]
        JWT["JwtAuthenticationFilter<br/>Authorization: Bearer"]
        MONGO["MongoDB Adapters<br/>(adapters.out.mongodb)"]
        AUDIT["AuditAdapter<br/>audit_logs"]
        EVENTS["SpringEventPublisher<br/>(ApplicationEventPublisher)"]
        OLLAMA["OllamaAdapter<br/>AiProviderPort"]
        SEC["Security Adapters<br/>JwtTokenProvider, BcryptHasher,<br/>CurrentUser (SecurityContext)"]
        SCHED["MonthlyClosingScheduler<br/>@Scheduled multiusuário"]
    end

    subgraph application["Camada de Aplicação"]
        UC["Casos de uso<br/>(Authenticate, RegisterUser, CreateAccount,<br/>ListIncomes, GenerateFinancialAnalysis, ...)"]
        VAL["Validadores<br/>(AccountValidator, CategoryValidator,<br/>UserValidator, CpfValidator)"]
        MAP["Mappers MapStruct<br/>(DTO ↔ Command/Query)"]
    end

    subgraph domain["Camada de Domínio (núcleo)"]
        MODEL["Modelos<br/>(User, RefreshToken, Account, Income, Category,<br/>MonthlyClosing, FinancialPlan,<br/>AssistantMessage, FinancialAnalysis)"]
        PORTIN["Portas de entrada<br/>(Use Cases interfaces)"]
        PORTOUT["Portas de saída<br/>(RepositoryPort, AuditPort,<br/>AiProviderPort, EventPublisherPort,<br/>TokenProviderPort, PasswordHasherPort, CurrentUserPort)"]
        DSVC["Serviços de domínio<br/>(score, prompts, parsing)"]
    end

    DB[("MongoDB 7.0<br/>10 coleções")]

    REST --> JWT
    JWT --> PORTIN
    REST --> PORTIN
    SCHED --> PORTIN
    UC --> PORTOUT
    UC --> VAL
    UC --> MAP
    PORTIN -. "dependem de" .-> MODEL
    UC -. "dependem de" .-> MODEL
    DSVC -.-> MODEL
    MONGO --> DB
    MONGO -. "implementa" .-> PORTOUT
    AUDIT -. "implementa" .-> PORTOUT
    OLLAMA -. "implementa" .-> PORTOUT
    SEC -. "implementa" .-> PORTOUT
    EVENTS -. "implementa" .-> PORTOUT
```

**Regra de dependência:** `domain` não depende de nada externo; `application` depende só de
`domain`; `adapters` dependem das duas. As portas de entrada são implementadas pelos casos de uso
e consumidas por controllers e scheduler; as portas de saída são implementadas pelos adapters.

---

## 3. Coleções e relacionamentos

```mermaid
erDiagram
    USERS ||--o{ REFRESH_TOKENS : "userId"
    ACCOUNTS ||--o{ INCOMES : "accountId"
    ACCOUNTS }o--|| USERS : "userId (do JWT)"
    CATEGORIES {
        string _id
        string userId
        string name
        string classification "DESPESA|RECEITA"
        bool active
        bool deleted
    }
    USERS {
        string _id
        string email "único"
        string cpf "único"
        string passwordHash "BCrypt"
        bool active
        bool deleted
    }
    REFRESH_TOKENS {
        string _id
        string userId
        string tokenHash "SHA-256, único"
        date expiresAt "índice TTL"
        bool revoked
    }
    ACCOUNTS {
        string _id
        string userId
        string name "único por usuário (case-insensitive)"
        string type "CONTA_CORRENTE|POUPANCA|CARTAO_CREDITO|DINHEIRO|INVESTIMENTO"
        bool active
        bool deleted
        date deletedAt
    }
    INCOMES {
        string _id
        string accountId
        string description
        decimal amount "Decimal128"
        date receivedAt "BSON date UTC"
        bool deleted
    }
    MONTHLY_CLOSINGS {
        string _id
        string referenceMonth "yyyy-MM"
        decimal totalIncome
        decimal totalExpense
        decimal balance
        date closedAt
    }
    FINANCIAL_ANALYSES {
        string _id
        string prompt
        string response
        long score "0-100"
        array recommendations
        string provider
    }
    FINANCIAL_PLANS {
        string _id
        string goal
        decimal targetAmount
        date targetDate
        string prompt
        string response
        array recommendations
    }
    ASSISTANT_MESSAGES {
        string _id
        string question
        string answer
        string provider
    }
    AUDIT_LOGS {
        string entityType
        string entityId
        string action "CRIACAO|ATUALIZACAO|EXCLUSAO"
        string oldValue
        string newValue
    }
    CATEGORIES }o--|| USERS : "userId"
    MONTHLY_CLOSINGS }o--|| USERS : "userId"
    FINANCIAL_ANALYSES }o--|| USERS : "userId"
    FINANCIAL_PLANS }o--|| USERS : "userId"
    ASSISTANT_MESSAGES }o--|| USERS : "userId"
    AUDIT_LOGS }o--|| USERS : "entidade auditada"
```

Nota: em MongoDB não há chaves estrangeiras; as relações são lógicas (`accountId`, `userId`) e
garantidas pelos casos de uso.

---

## 4. Fluxos principais

### 4.1 Cadastro de receita (fluxo síncrono completo)

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente HTTP
    participant R as IncomeController
    participant U as CreateIncomeUseCaseImpl
    participant V as AccountDomainService
    participant P as IncomeRepositoryPort<br/>(IncomeMongoAdapter)
    participant A as AuditPort
    participant E as EventPublisherPort
    C->>R: POST /api/v1/incomes (DTO)
    R->>R: Bean Validation (@Valid)
    R->>U: handle(CreateIncomeCommand)
    U->>V: requireActiveAccount(accountId)
    V-->>U: Account (404 se não existe / 422 se inativa)
    U->>U: Income.create(...) — regras de valor/data
    U->>P: save(Income)
    P->>P: Domain -> Document (MapStruct)
    P-->>U: Income persistido (Decimal128 + BSON date)
    U->>A: record(CRIACAO, oldValue=null, newValue)
    U->>E: publish(IncomeCreatedEvent)
    U-->>R: Income (domínio)
    R-->>C: 201 + envelope {data, message, timestamp}
```

### 4.2 Análise financeira com IA (FASE 5) — mesmo padrão vale para FASES 8 e 9

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente HTTP
    participant R as FinancialAnalysisController
    participant U as GenerateFinancialAnalysisUseCaseImpl
    participant D as FinancialAnalysisDomainService
    participant Q as Portas de consulta<br/>(contas + receitas)
    participant O as AiProviderPort (Ollama)
    participant H as FinancialAnalysisRepositoryPort
    C->>R: POST /api/v1/financial-analysis {"focus": "..."}
    R->>U: handle(command)
    U->>Q: contas ativas + receitas (até 500 cada)
    alt sem receitas
        U-->>C: ValidationException -> 400
    end
    U->>D: buildPrompt(resumo financeiro + foco)
    U->>O: complete(AiPrompt)
    alt Ollama indisponível / erro HTTP
        O-->>U: ExternalServiceException -> 503 (nada é gravado)
    else sucesso
        O-->>U: AiResponse(content, provider, model)
    end
    U->>D: parseRecommendations + calculateScore(0-100)
    U->>H: save(FinancialAnalysis) — histórico com prompt e resposta
    U->>U: audit CRIACAO + publish(FinancialAnalysisGeneratedEvent)
    U-->>C: 201 + envelope {data: análise, score, recommendations}
```

### 4.3 Autenticação (login + refresh rotativo) — FASE 10

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente HTTP
    participant F as JwtAuthenticationFilter
    participant A as AuthController
    participant U as AuthenticateUseCaseImpl
    participant H as PasswordHasherPort (BCrypt)
    participant R as RefreshTokenRepositoryPort
    participant T as TokenProviderPort (jjwt)
    C->>A: POST /api/v1/auth/login {"email", "password"}
    A->>U: handle(LoginCommand)
    U->>U: findByEmail (401 se não existe)
    U->>H: matches(password, passwordHash)
    U->>U: active? (403 se inativo)
    U->>T: generateAccessToken(userId, email)
    U->>T: generateRefreshToken()
    U->>R: save(tokenHash=SHA-256, expiresAt=+7d)
    U-->>A: AuthSession
    A-->>C: 200 {data: {accessToken, refreshToken, user}}
    Note over C,T: Requisições seguintes: Authorization: Bearer accessToken<br/>F valida assinatura/expiração e injeta userId no contexto
    C->>A: POST /api/v1/auth/refresh-token {"refreshToken"}
    A->>U: handle(RefreshCommand)
    U->>R: findByTokenHash (SHA-256) — 401 se revogado/expirado
    U->>R: revoke(old) + save(new hash)
    U->>T: novo accessToken
    A-->>C: 200 {data: {accessToken, refreshToken (novo)}}
```

### 4.4 Fechamento mensal agendado (FASE 7)

```mermaid
flowchart TD
    A["Cron 0 5 0 1 * *<br/>(todo dia 1º às 00:05)"] --> S["MonthlyClosingScheduler"]
    S --> U["GenerateMonthlyClosingUseCaseImpl"]
    U --> M1{"referenceMonth<br/>válido yyyy-MM?"}
    M1 -- "não" --> X["ValidationException -> 400<br/>(geração manual)"]
    M1 -- "sim" --> SUM["Soma receitas do mês<br/>(paginação até consumir tudo)"]
    SUM --> EX{"Já existe snapshot<br/>do mês?"}
    EX -- "não" --> NEW["Cria snapshot<br/>audit CRIACAO"]
    EX -- "sim" --> UPD["Atualiza snapshot<br/>audit ATUALIZACAO oldValue/newValue"]
    NEW --> EV["MonthlyClosingGeneratedEvent"]
    UPD --> EV
    EV --> DB[("monthly_closings")]
    S -. "falha agendada" .-> LOG["Log ERROR sem interromper app"]
```

---

## 5. Modelo de dados (resumo)

| Coleção | Uso | Índices |
|---|---|---|
| `users` | Credenciais de acesso (BCrypt; e-mail e CPF únicos) | `email` único, `cpf` único, `active`, `deleted` |
| `refresh_tokens` | Refresh tokens rotativos (hash SHA-256, TTL) | `tokenHash` único, `userId+createdAt DESC`, `expiresAt` (TTL) |
| `accounts` | Contas financeiras do usuário | `userId+name`, `type`, `active`, `deleted` |
| `incomes` | Receitas (valores em Decimal128, `receivedAt` como BSON date UTC) | `userId+receivedAt DESC`, `accountId+receivedAt DESC`, `deleted` |
| `categories` | Categorias com classificação `DESPESA`/`RECEITA` | `userId+name`, `classification`, `active`, `deleted` |
| `financial_analyses` | Histórico das análises com IA (prompt, resposta, score, recomendações) | `userId+createdAt DESC`, `deleted` |
| `monthly_closings` | Snapshots mensais do saldo | `userId+referenceMonth`, `deleted` |
| `financial_plans` | Histórico de planos de economia gerados por IA | `userId+createdAt DESC`, `deleted` |
| `assistant_messages` | Histórico de perguntas/respostas do assistente | `userId+createdAt DESC`, `deleted` |
| `audit_logs` | Trilha de auditoria de todas as mutações | `entityType+entityId`, `createdAt DESC` |

DDL completo (validadores `$jsonSchema` + `createIndex`) em [ddl-mongodb.md](ddl-mongodb.md).

### Decisões de desenho

1. **Dinheiro como `Decimal128`**: evita erro de arredondamento de `double` e permite `$sum` em
   aggregations; Spring Data serializaria `BigDecimal` como string por padrão — conversores
   customizados (`MongoConfig`) corrigem isso.
2. **`LocalDate` como BSON date (UTC)**: garante que filtros `gte/lte` por período funcionem
   numericamente (não como string).
3. **Exclusão sempre lógica** (`deleted`, `deletedAt`): histórico preservado; todas as leituras
   filtram `deleted: false`.
4. **Auditoria em coleção separada** (`audit_logs`) com `oldValue`/`newValue` serializados em JSON —
   o caso de uso decide quando registrar (`CRIACAO` = newValue, `ATUALIZACAO` = ambos,
   `EXCLUSAO` = oldValue).
5. **IA atrás de porta** (`AiProviderPort`): trocar Ollama por outro provedor não toca no domínio;
   timeouts e cliente HTTP isolados no adapter.
6. **Score deterministicamente calculado** (receitas 40 pts, contas 30, meses 30): a nota não
   depende do humor do LLM; o LLM fornece texto e recomendações.
7. **Envelope único** `{"data", "message", "timestamp"}` e erros `{"timestamp","status","error",
   "message","details"}`.
8. **Senhas com BCrypt, tokens com SHA-256**: o banco nunca guarda a senha nem o refresh token em
   claro — só o hash; o snapshot de auditoria do usuário (CRIACAO/ATUALIZACAO) contém apenas
   `id/email/cpf/active`, nunca o `passwordHash`.
9. **JWT HS256 atrás de portas** (`TokenProviderPort`, `PasswordHasherPort`, `CurrentUserPort`):
   trocar jjwt/BCrypt/SecurityContext não toca no domínio; o segredo vem de `JWT_SECRET`.
10. **Refresh token rotativo**: cada uso revoga o antigo e emite um novo (`tokenHash` único +
    índice TTL em `expiresAt`); logout revoga todos os tokens do usuário.
11. **Multiusuário pelo token**: toda leitura/escrita usa o `userId` do JWT (`CurrentUserPort`);
    controllers e adapters não recebem `userId` do cliente — usuário só edita/desativa a própria conta.

---

## 6. Semântica de erros

| Situação | HTTP | Exceção |
|---|---|---|
| Bean Validation, corpo malformado, valor ≤ 0, data futura, enum desconhecido | 400 | `ValidationException` / `MalformedRequestException` / `InvalidEnumException` |
| Não autenticado, token inválido ou expirado | 401 | `UnauthorizedException` / entrada do `JwtAuthenticationFilter` |
| Acesso proibido (usuário alheio, usuário inativo) | 403 | `ForbiddenException` |
| Recurso inexistente ou já excluído logicamente | 404 | `NotFoundException` |
| Nome duplicado (conta/categoria) ou e-mail/CPF já cadastrado | 409 | `ConflictException` |
| Regra de negócio (receita em conta inativa) | 422 | `BusinessRuleException` |
| Falha de banco | 500 | `DatabaseException` |
| Provedor de IA indisponível | 503 | `ExternalServiceException` |

Tratados centralizadamente em `GlobalExceptionHandler` (`shared.exception`).

---

## 7. Organização de pacotes

```
com.finance
├── domain          # entidades, enums, eventos, ports (in/out), serviços de domínio
├── application     # use cases (CQRS-light), commands, queries, DTOs, mappers, validadores
├── adapters.in.rest.v1          # controllers REST versionados
├── adapters.in.rest.filter      # JwtAuthenticationFilter
├── adapters.out.mongodb         # documents, repositories, persistence mappers, adapters
├── adapters.out.security        # JwtTokenProvider, BcryptPasswordHasher, CurrentUser
├── adapters.out.audit           # AuditPort -> audit_logs
├── adapters.out.events          # EventPublisherPort -> Spring events
├── adapters.out.ollama          # AiProviderPort -> Ollama HTTP
├── scheduler       # MonthlyClosingScheduler (multiusuário)
├── config          # MongoConfig, MongoSchemaInitializer, SchedulingConfig, SecurityConfig, JwtProperties, OpenAPI
├── documentation   # anotações OpenAPI
└── shared          # exceptions, handler, envelope, constants, utils
```

## 8. Como executar / testar

```bash
mvn spring-boot:run                        # aplica + Mongo local localhost:27017
mvn spring-boot:run -Dspring-boot.run.profiles=dev   # perfil dev (logs coloridos, JWT 2h)
mvn clean test               # 256 testes + gate JaCoCo 90% (Mongo embutido + WireMock)
```
