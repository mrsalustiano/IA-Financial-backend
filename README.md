# IA-Financial

Sistema de controle financeiro pessoal com IA, construído com **Java 21**, **Spring Boot 3.5.16** e
**Arquitetura Hexagonal (Ports and Adapters)**.

Escopo atual: **FASES 1–5 concluídas** (CRUD, alteração/exclusão com auditoria, listagem, categorias,
análise com IA) e **FASE 6 — Saldo Diário**, **FASE 7 — Fechamento Mensal (scheduler)**,
**FASE 8 — Planejamento Financeiro com IA**, **FASE 9 — Assistente Financeiro com IA** e
**FASE 10 — Multiusuário com Autenticação JWT**, além da fundação transversal (tratamento de erros,
envelope de resposta, trilha de auditoria, soft delete, validadores de schema do MongoDB, OpenAPI,
adaptador Ollama e logs por camada com `logback-spring.xml`).

## Fases

| Fase | Escopo | Status |
|------|--------|--------|
| 1 | Cadastro de contas e receitas (CRUD) | ✅ Concluída |
| 2 | Alteração e exclusão lógica com auditoria | ✅ Concluída |
| 3 | Listagem com paginação, ordenação e filtros | ✅ Concluída |
| 4 | Categorias com classificação | ✅ Concluída |
| 5 | Análise financeira com IA (Ollama) | ✅ Concluída |
| 6 | Saldo diário acumulado (consulta otimizada) | ✅ Concluída |
| 7 | Fechamento mensal com scheduler e snapshot | ✅ Concluída |
| 8 | Planejamento financeiro com IA | ✅ Concluída |
| 9 | Assistente financeiro com IA | ✅ Concluída |
| 10 | Multiusuário com autenticação JWT e gestão de usuários | ✅ Concluída |

## Como executar

```bash
# com um MongoDB local em localhost:27017 (usuário admin conforme doc/ddl-mongodb.md):
mvn spring-boot:run
# Swagger UI: http://localhost:8080/swagger-ui.html
# Actuator:   http://localhost:8080/actuator/health
```

Perfil de desenvolvimento (logs coloridos/verbosos, JWT de 2h, refresh de 14 dias):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Em produção defina `JWT_SECRET` (>= 32 bytes) via variável de ambiente — o valor padrão no
`application.yml` é apenas para desenvolvimento local.

```bash
mvn clean test        # roda todos os testes + JaCoCo (falha abaixo de 90% de cobertura)
mvn clean verify      # idem, mais empacotamento
```

Os testes usam **MongoDB embutido** (flapdoodle 4.16.2, MongoDB 7.0) — não precisam de Docker.
O adaptador Ollama é testado com WireMock.

## Arquitetura

```
com.finance
├── domain          # entidades, enums, eventos, serviços de domínio, ports (in/out)
├── application     # casos de uso (CQRS-light), commands, queries, DTOs, mappers MapStruct, validadores
├── adapters.in.rest.v1      # controllers REST (/api/v1)
├── adapters.in.rest.filter  # JwtAuthenticationFilter (Authorization: Bearer)
├── adapters.out.mongodb     # documents, repositories Spring Data, persistence mappers, adapters
├── adapters.out.security    # JwtTokenProvider (jjwt), BcryptPasswordHasher, CurrentUser (SecurityContext)
├── adapters.out.audit       # AuditPort -> coleção audit_logs (valores antigos/novos em JSON)
├── adapters.out.events      # EventPublisherPort -> ApplicationEventPublisher do Spring
├── adapters.out.ollama      # AiProviderPort -> cliente HTTP do Ollama (padrão Strategy)
├── scheduler       # MonthlyClosingScheduler (multiusuário)
├── config          # MongoConfig (Decimal128), MongoSchemaInitializer ($jsonSchema + índices), SecurityConfig, JwtProperties
├── documentation   # OpenAPI
└── shared          # exceções, GlobalExceptionHandler, envelope de resposta, constantes, utils
```

As dependências apontam para dentro: `application` depende só de `domain`; adapters dependem de
ambos; o núcleo não conhece tipos de Spring Web/Data além dos registros de query com `Pageable`.

> **Autenticação (FASE 10)**: todos os endpoints, exceto `POST /api/v1/users`, `POST /api/v1/auth/login`,
> `POST /api/v1/auth/refresh-token`, health e Swagger, exigem `Authorization: Bearer <accessToken>`.
> O `userId` do token substitui o antigo `LOCAL_USER` em todos os casos de uso e adapters.

## APIs (v1)

| Método | Caminho | Descrição | Sucesso | Auth |
|--------|---------|-----------|---------|------|
| POST | `/api/v1/users` | Cadastrar usuário (e-mail, CPF, senha BCrypt, ativo) | 201 | pública |
| GET | `/api/v1/users?page=&size=&active=` | Listar usuários (paginação + filtro) | 200 | Bearer |
| GET | `/api/v1/users/{id}` | Detalhar usuário | 200 | Bearer |
| PUT | `/api/v1/users/{id}` | Editar e-mail/CPF (só o próprio) | 200 | Bearer |
| POST | `/api/v1/users/{id}/deactivate` | Desativar usuário (só o próprio) | 200 | Bearer |
| POST | `/api/v1/auth/login` | Autenticar (e-mail + senha) → accessToken + refreshToken | 200 | pública |
| POST | `/api/v1/auth/refresh-token` | Renovar accessToken (rotativo — token antigo revogado) | 200 | pública |
| POST | `/api/v1/auth/logout` | Revogar todos os refresh tokens do usuário | 200 | Bearer |
| GET | `/api/v1/auth/me` | Usuário autenticado (403 se inativo) | 200 | Bearer |
| POST | `/api/v1/accounts` | Criar conta | 201 + envelope | Bearer |
| GET | `/api/v1/accounts/{id}` | Buscar conta por id | 200 | Bearer |
| GET | `/api/v1/accounts?type=&active=&page=&size=&sort=` | Listar/filtrar/paginar/ordenar | 200 | Bearer |
| PUT | `/api/v1/accounts/{id}` | Alterar conta | 200 | Bearer |
| DELETE | `/api/v1/accounts/{id}` | Exclusão lógica | 204 | Bearer |
| POST | `/api/v1/incomes` | Registrar receita | 201 | Bearer |
| GET | `/api/v1/incomes/{id}` | Buscar receita por id | 200 | Bearer |
| GET | `/api/v1/incomes?accountId=&startDate=&endDate=&page=&size=&sort=` | Listar por conta/período | 200 | Bearer |
| PUT | `/api/v1/incomes/{id}` | Alterar receita | 200 | Bearer |
| DELETE | `/api/v1/incomes/{id}` | Exclusão lógica | 204 | Bearer |
| POST | `/api/v1/categories` | Criar categoria | 201 | Bearer |
| GET | `/api/v1/categories/{id}` | Buscar categoria por id | 200 | Bearer |
| GET | `/api/v1/categories?classification=&active=&page=&size=&sort=` | Listar/filtrar/paginar/ordenar | 200 | Bearer |
| PUT | `/api/v1/categories/{id}` | Alterar categoria | 200 | Bearer |
| DELETE | `/api/v1/categories/{id}` | Exclusão lógica | 204 | Bearer |
| POST | `/api/v1/financial-analysis` | Gerar análise financeira com IA | 201 | Bearer |
| GET | `/api/v1/financial-analysis/{id}` | Buscar análise por id | 200 | Bearer |
| GET | `/api/v1/financial-analysis?page=&size=` | Histórico de análises | 200 | Bearer |
| GET | `/api/v1/daily-balance?startDate=&endDate=` | Saldo diário acumulado | 200 | Bearer |
| POST | `/api/v1/monthly-closings` | Gerar fechamento mensal (snapshot + auditoria) | 201 | Bearer |
| GET | `/api/v1/monthly-closings/{id}` | Buscar fechamento por id | 200 | Bearer |
| GET | `/api/v1/monthly-closings` | Listar fechamentos | 200 | Bearer |
| POST | `/api/v1/financial-plans` | Criar plano de economia com IA | 201 | Bearer |
| GET | `/api/v1/financial-plans/{id}` | Buscar plano por id | 200 | Bearer |
| GET | `/api/v1/financial-plans?page=&size=` | Histórico de planos | 200 | Bearer |
| POST | `/api/v1/assistant/questions` | Perguntar ao assistente financeiro (IA) | 201 | Bearer |
| GET | `/api/v1/assistant/questions/{id}` | Buscar pergunta/resposta por id | 200 | Bearer |
| GET | `/api/v1/assistant/questions?page=&size=` | Histórico de perguntas | 200 | Bearer |

Parâmetros de listagem:
- Contas: `type` (`CONTA_CORRENTE`, `POUPANCA`, `CARTAO_CREDITO`, `DINHEIRO`, `INVESTIMENTO`),
  `active` (`true`/`false`), `page`, `size`, `sort` (ex.: `name,asc`, `createdAt,desc`).
- Receitas: `accountId`, `startDate`, `endDate` (ISO `yyyy-MM-dd`), `page`, `size`,
  `sort` (padrão: `receivedAt,desc`).
- Categorias: `classification` (`DESPESA`, `RECEITA`), `active`, `page`, `size`,
  `sort` (padrão: `name,asc`).
- Análises, planos e perguntas: `page`, `size` (padrão: `createdAt,desc`).
- Saldo diário: `startDate`, `endDate` (ISO `yyyy-MM-dd`) — aggregation no MongoDB usando o índice
  `userId+receivedAt`, com saldo acumulado calculado em ordem de data.

Respostas de sucesso usam o envelope `{"data": ..., "message": "Sucesso", "timestamp": ...}`.
Erros usam `{"timestamp", "status", "error", "message", "details"}` com os códigos abaixo.

## Semântica de erros

| Situação | HTTP |
|-----------|------|
| Falha de Bean Validation, corpo malformado, valor ≤ 0, `receivedAt` futuro, enum desconhecido | 400 |
| Autenticação ausente, token inválido ou expirado (JWT) | 401 |
| Acesso proibido (editar/desativar usuário alheio, login de usuário inativo) | 403 |
| Conta, receita ou usuário não encontrado (ou já excluído logicamente) | 404 |
| Nome de conta duplicado, e-mail ou CPF já cadastrado (case-insensitive) | 409 |
| Regra de negócio violada (ex.: receita em conta inativa) | 422 |
| Falha de banco de dados | 500 |
| Ollama indisponível / erro do provedor de IA | 503 |

## Modelo de dados (MongoDB)

- `users`: `_id, email (único), cpf (único), passwordHash (BCrypt), active, deleted, deletedAt, createdAt, updatedAt` — credenciais de acesso; validador `$jsonSchema` + índices únicos em `email` e `cpf`. **Nunca exposto na API nem na auditoria** (`passwordHash` sai do snapshot de auditoria e do DTO de resposta).
- `refresh_tokens`: `_id, userId, tokenHash (único — SHA-256 do token), expiresAt (TTL index), revoked, revokedAt, deleted, deletedAt, createdAt, updatedAt` — rotação a cada refresh; logout revoga todos do usuário.
- `accounts`: `_id, userId, name (indexado), description, type, active, deleted, deletedAt, createdAt, updatedAt` — validador `$jsonSchema`, unicidade de nome tratada no caso de uso, índices em `userId+name`, `type`, `active`, `deleted`.
- `incomes`: `_id, userId, accountId (indexado), description, amount (Decimal128), receivedAt (data), deleted, deletedAt, createdAt, updatedAt` — validador + índices em `userId+receivedAt` e `accountId+receivedAt`.
- `categories`: `_id, userId, name, description, classification (DESPESA/RECEITA), active, deleted, deletedAt, createdAt, updatedAt` — validador `$jsonSchema` com enum de classificação, índices em `userId+name`, `classification`, `active`, `deleted`.
- `financial_analyses`: `_id, userId, prompt, response, score (0–100), recommendations (array), provider, model, deleted, deletedAt, createdAt, updatedAt` — histórico de cada análise gerada com IA, com prompt e resposta registrados; índices em `userId+createdAt` e `deleted`.
- `monthly_closings`: `_id, userId, referenceMonth ("yyyy-MM"), totalIncome, totalExpense, balance, closedAt, deleted, deletedAt, createdAt, updatedAt` — snapshot mensal gerado pelo scheduler (todo dia 1º às 00:05, mês anterior) ou por geração manual; reexecução no mesmo mês atualiza o snapshot (auditoria registra ATUALIZACAO); índices em `userId+referenceMonth` e `deleted`.
- `financial_plans`: `_id, userId, goal, targetAmount, targetDate, prompt, response, recommendations, provider, model, deleted, deletedAt, createdAt, updatedAt` — histórico dos planos de economia sugeridos pela IA; índices em `userId+createdAt` e `deleted`.
- `assistant_messages`: `_id, userId, question, answer, provider, model, deleted, deletedAt, createdAt, updatedAt` — histórico de perguntas e respostas do assistente; índices em `userId+createdAt` e `deleted`.
- `audit_logs`: `entityType, entityId, action (CRIACAO/ATUALIZACAO/EXCLUSAO), oldValue, newValue (JSON), createdAt` — gravado em toda mutação dos casos de uso.

Valores monetários são armazenados como BSON `Decimal128` (converters customizados) e `LocalDate`
como data BSON (UTC) — escolhas que mantêm agregações futuras (saldos mensais) numericamente
corretas. A exclusão é sempre lógica: nenhum documento é removido e todas as leituras filtram
`deleted: false`.

## Critérios de Aceite — FASES 1 a 9 (Gherkin)

```gherkin
Feature: Gerenciamento de contas

  Cenário: Criar conta
    Dado que o usuário informa nome e tipo válidos
    Quando executa o cadastro
    Então a resposta é 201 com o envelope da conta
    E existe um registro em audit_logs com ação CRIACAO e newValue preenchido
    E um AccountCreatedEvent foi publicado

  Cenário: Validar campos obrigatórios
    Dado que o usuário não informa o nome da conta
    Quando executa o cadastro
    Então a API retorna HTTP 400

  Cenário: Nome duplicado
    Dado que existe uma conta ativa chamada "Broker"
    Quando o usuário cadastra uma conta chamada "broker"
    Então a API retorna 409

  Cenário: Alterar conta
    Dado que a conta existe
    Quando o usuário altera seus dados
    Então os dados são atualizados
    E o campo updatedAt é renovado mantendo createdAt
    E a auditoria registra oldValue e newValue

  Cenário: Excluir conta
    Dado que a conta existe
    Quando o usuário solicita a exclusão
    Então a resposta é 204
    E GET /api/v1/accounts/{id} retorna 404
    E o documento continua no Mongo com deleted=true e deletedAt preenchido
    E a auditoria registra a ação EXCLUSAO com oldValue

  Cenário: Listar contas
    Dado que existam contas cadastradas
    Quando a listagem é executada
    Então as contas são retornadas com paginação (page, size, totalElements, totalPages)

  Cenário: Filtrar por tipo
    Dado que existam contas de tipos diferentes
    Quando o filtro type=CONTA_CORRENTE é aplicado
    Então somente contas desse tipo são retornadas

  Cenário: Filtrar por situação
    Dado que existam contas ativas e inativas
    Quando o filtro active=false é aplicado
    Então somente as contas inativas são retornadas

  Cenário: Ordenação
    Dado que existam contas com nomes distintos
    Quando sort=name,desc é informado
    Então os registros vêm em ordem decrescente de nome

Feature: Gerenciamento de receitas

  Cenário: Criar receita
    Dado que existe uma conta válida e ativa
    Quando o usuário cadastra uma receita com valor > 0 e receivedAt <= hoje
    Então a resposta é 201 com o envelope da receita
    E a auditoria registra a ação CRIACAO

  Cenário: Impedir valor inválido
    Dado que o valor informado é menor ou igual a zero
    Quando o usuário cadastra a receita
    Então a API retorna HTTP 400

  Cenário: Conta inexistente ou inativa
    Dado uma conta que não existe
    Quando o usuário cadastra uma receita para ela
    Então a API retorna 404
    Dado uma conta inativa
    Quando o usuário cadastra uma receita para ela
    Então a API retorna 422

  Cenário: Alterar receita
    Dado que a receita existe
    Quando o usuário envia novos dados
    Então os dados são persistidos
    E ao trocar de conta a nova conta é revalidada (ativa e existente)
    E a auditoria registra oldValue e newValue

  Cenário: Filtrar por período
    Dado que existam receitas em setembro e outubro de 2026
    Quando a consulta GET /api/v1/incomes?accountId=...&startDate=2026-09-01&endDate=2026-09-30 é feita
    Então somente as receitas de setembro são retornadas com totais de paginação corretos

  Cenário: Ordenação por data
    Dado que existam receitas em datas diferentes
    Quando sort=receivedAt,asc é informado
    Então a receita mais antiga vem primeiro

Feature: Gerenciamento de categorias

  Cenário: Criar categoria
    Dado que o usuário informa nome e classificação válidos
    Quando executa o cadastro
    Então a resposta é 201 com o envelope da categoria
    E a categoria é persistida no banco
    E a auditoria registra a ação CRIACAO

  Cenário: Classificação válida
    Dado que a classificação informada não é um valor válido do enum
    Quando executa o cadastro
    Então a API retorna HTTP 400

  Cenário: Nome duplicado
    Dado que existe uma categoria chamada "Mercado"
    Quando o usuário cadastra outra chamada "mercado"
    Então a API retorna 409

  Cenário: Alterar categoria
    Dado que a categoria existe
    Quando o usuário altera seus dados
    Então os dados são atualizados com auditoria oldValue/newValue

  Cenário: Excluir categoria
    Dado que a categoria existe
    Quando o usuário solicita a exclusão
    Então a resposta é 204
    E o documento permanece no Mongo com deleted=true
    E a auditoria registra a ação EXCLUSAO

  Cenário: Listar por classificação
    Dado que existam categorias de classificações diferentes
    Quando o filtro classification=DESPESA é aplicado com paginação
    Então somente categorias de despesa são retornadas com totais corretos

Feature: Análise financeira com IA

  Cenário: Gerar análise financeira
    Dado que o usuário possui contas e receitas registradas
    Quando solicita a geração da análise
    Então o prompt com o resumo financeiro é registrado no histórico
    E a resposta da IA é registrada no histórico
    E recomendações práticas são retornadas
    E um FinancialAnalysisGeneratedEvent é publicado
    E a auditoria registra a ação CRIACAO

  Cenário: Calcular score financeiro
    Dado que existam dados suficientes (receitas, contas e meses com movimentação)
    Quando a análise for processada
    Então um score entre 0 e 100 deve ser calculado por regras deterministicas

  Cenário: Dados insuficientes
    Dado que o usuário não possui receitas registradas
    Quando solicita a geração da análise
    Então a API retorna HTTP 400 informando que não há dados suficientes

  Cenário: Provedor de IA indisponível
    Dado que o Ollama não responde com sucesso
    Quando solicita a geração da análise
    Então a API retorna HTTP 503
    E nada é gravado no histórico

Feature: Saldo diário

  Cenário: Consultar saldo diário
    Dado que existam movimentações
    Quando o usuário consulta o saldo
    Então o saldo diário acumulado é calculado em ordem de data

  Cenário: Consultar saldo sem movimentações
    Dado que não existam movimentações no período
    Quando o usuário consulta o saldo
    Então a lista vem vazia com HTTP 200

Feature: Fechamento mensal

  Cenário: Gerar fechamento mensal
    Dado que o mês foi encerrado
    Quando o scheduler for executado (ou o fechamento manual for solicitado)
    Então o saldo mensal é persistido em monthly_closings
    E a auditoria registra a ação CRIACAO

  Cenário: Reexecutar fechamento do mesmo mês
    Dado que já existe snapshot do mês
    Quando o fechamento é gerado novamente
    Então o snapshot existente é atualizado
    E a auditoria registra ATUALIZACAO com oldValue e newValue

  Cenário: Mês inválido
    Dado um mês fora do formato yyyy-MM
    Quando o fechamento manual é solicitado
    Então a API retorna HTTP 400

Feature: Planejamento financeiro

  Cenário: Criar plano de economia
    Dado uma meta financeira
    Quando a IA processa os dados
    Então um plano com passos práticos é sugerido
    E o histórico com prompt e resposta é registrado
    E a auditoria registra a ação CRIACAO

  Cenário: Meta sem dados suficientes
    Dado que o usuário não possui receitas
    Quando solicita um plano
    Então a API retorna HTTP 400

Feature: Assistente financeiro

  Cenário: Consultar assistente financeiro
    Dado que existam dados financeiros
    Quando uma pergunta é realizada
    Então a IA responde utilizando o contexto dos dados cadastrados
    E a pergunta e a resposta ficam no histórico (assistant_messages)
    E a auditoria registra a ação CRIACAO

  Cenário: Pergunta vazia
    Dado que a pergunta é vazia ou em branco
    Quando a pergunta é enviada
    Então a API retorna HTTP 400

  Cenário: Provedor de IA indisponível
    Dado que o Ollama não responde
    Quando a pergunta é enviada
    Então a API retorna HTTP 503
    E nada é gravado no histórico
```

```gherkin
Feature: Autenticação e usuários (FASE 10)

  Cenário: Cadastrar usuário
    Dado que o usuário informa e-mail, CPF válido e senha
    Quando executa o cadastro
    Então a senha é armazenada como hash BCrypt (nunca em texto puro)
    E o campo active pode ser true ou false (padrão true)
    E a resposta nunca contém passwordHash
    E a auditoria registra CRIACAO com snapshot sem senha

  Cenário: E-mail ou CPF duplicado
    Dado que já existe usuário com o mesmo e-mail ou CPF
    Quando executa o cadastro
    Então a API retorna 409

  Cenário: Login
    Dado credenciais válidas e usuário ativo
    Quando executa POST /api/v1/auth/login
    Então a resposta contém accessToken (15 min) e refreshToken (7 dias, rotativo)

  Cenário: Login negado
    Dado senha incorreta, usuário inexistente ou usuário inativo
    Quando executa o login
    Então a API retorna 401 ou 403 sem detalhes internos

  Cenário: Renovar token
    Dado um refreshToken válido e não revogado
    Quando executa POST /api/v1/auth/refresh-token
    Então um novo accessToken é emitido
    E o refreshToken antigo é revogado (rotação)

  Cenário: Logout
    Dado um usuário autenticado
    Quando executa POST /api/v1/auth/logout
    Então todos os refresh tokens do usuário são revogados

  Cenário: Requisições autenticadas
    Dado um accessToken válido no header Authorization: Bearer
    Quando qualquer endpoint protegido é chamado
    Então o userId do token é usado em todas as leituras e escritas
    E cada usuário só enxerga os próprios dados

  Cenário: Editar usuário
    Dado que o usuário autenticado é o dono do cadastro
    Quando executa PUT /api/v1/users/{id} com novo e-mail/CPF
    Então os dados são atualizados
    E a auditoria registra ATUALIZACAO com snapshot sem senha
    Dado que o id pertence a outro usuário
    Quando executa PUT /api/v1/users/{id}
    Então a API retorna 403 (antes de revelar existência)

  Cenário: Desativar usuário
    Dado que o usuário é o dono da conta
    Quando executa POST /api/v1/users/{id}/deactivate
    Então active=false com auditoria
    E o login deixa de ser aceito (403)
    E /me com token ainda válido retorna 403
```

## Definition of Done

- [x] Camadas hexagonais respeitadas (domain ← application ← adapters)
- [x] FASES 1–3: CRUD completo de contas e receitas, com listagem paginada/ordenada/filtrada
- [x] FASE 4: categorias com classificação (DESPESA/RECEITA), CRUD completo e filtro por classificação
- [x] FASE 5: análise financeira com IA via `AiProviderPort` (Ollama), score 0–100 deterministicamente calculado, histórico com prompt/resposta, eventos e auditoria; WireMock nos testes de integração
- [x] FASE 6: saldo diário acumulado via aggregation no MongoDB (índice `userId+receivedAt`), filtrável por período
- [x] FASE 7: fechamento mensal com scheduler (`@Scheduled` cron, mês anterior), snapshot persistido em `monthly_closings`, idempotente por mês e com auditoria CRIACAO/ATUALIZACAO
- [x] FASE 8: plano de economia com IA a partir de uma meta (valor/data alvo opcionais), histórico em `financial_plans`, auditoria e evento
- [x] FASE 9: assistente financeiro que responde com o contexto dos dados cadastrados, histórico em `assistant_messages`, auditoria e evento
- [x] FASE 10: multiusuário com autenticação JWT — cadastro de usuário (BCrypt + CPF validado), login/refresh rotativo/logout/me, gestão de usuários (listagem, edição, desativação), auditoria sem senha, `userId` do token em todos os casos de uso e scheduler multiusuário
- [x] Alteração renova `updatedAt` e registra auditoria com oldValue/newValue
- [x] Exclusão lógica (`deleted`/`deletedAt`) com auditoria — sem exclusão física
- [x] Exceções obrigatórias + GlobalExceptionHandler com os códigos HTTP exigidos
- [x] Envelope padrão em todas as respostas
- [x] DTOs como records, MapStruct em todo mapeamento, Bean Validation nas entradas
- [x] MongoDB com validadores `$jsonSchema`, índices, money em Decimal128, datas BSON
- [x] Swagger UI (`/swagger-ui.html`) + actuator health/info/metrics
- [x] Adaptador Ollama atrás de `AiProviderPort` (testado com WireMock)
- [x] Logs coloridos por nível (ERROR/WARN/INFO/DEBUG) e por camada (app, Mongo, IA) via `logback-spring.xml`
- [x] 256 testes (unitários + integração de API em MongoDB embutido e WireMock), gate JaCoCo ≥ 90% de cobertura
- [x] Dependências sem vulnerabilidades conhecidas (Spring Boot 3.5.16 + overrides `commons-lang3` 3.21.0 e `log4j-api` 2.26.1)

## Notas de ferramentas de teste

Este repositório foi construído em uma máquina sem Maven pré-instalado; `.tools/` (ignorado pelo
git) contém o Apache Maven 3.9.9 e pequenos launchers Java de arquivo único (`Maven.java`,
`Downloader.java`, `Unzip.java`). Para rodar o Maven:
`java .tools/Maven.java .tools/apache-maven-3.9.9 . test`.
