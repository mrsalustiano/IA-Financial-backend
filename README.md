# IA-Financial

Sistema de controle financeiro pessoal com IA, construído com **Java 21**, **Spring Boot 3.5.6** e
**Arquitetura Hexagonal (Ports and Adapters)**.

Escopo atual: **FASE 1 — Cadastro de Contas e Receitas (CRUD)**, **FASE 2 — Alteração e Exclusão
(soft delete + auditoria)**, **FASE 3 — Listagem (paginação, ordenação e filtros)**, **FASE 4 —
Categorias** e **FASE 5 — Análise financeira com IA (Ollama)**, além da fundação transversal
(tratamento de erros, envelope de resposta, trilha de auditoria, soft delete, validadores de schema
do MongoDB, OpenAPI e adaptador Ollama).

## Fases

| Fase | Escopo | Status |
|------|--------|--------|
| 1 | Cadastro de contas e receitas (CRUD) | ✅ Concluída |
| 2 | Alteração e exclusão lógica com auditoria | ✅ Concluída |
| 3 | Listagem com paginação, ordenação e filtros | ✅ Concluída |
| 4 | Categorias com classificação | ✅ Concluída |
| 5 | Análise financeira com IA (Ollama) | ✅ Concluída |
| 6–9 | Saldo diário, fechamento mensal, planejamento, assistente | ⏳ Pendentes |

## Como executar

```bash
# com um MongoDB local em localhost:27017:
mvn spring-boot:run
# Swagger UI: http://localhost:8080/swagger-ui.html
# Actuator:   http://localhost:8080/actuator/health
```

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
├── adapters.out.mongodb     # documents, repositories Spring Data, persistence mappers, adapters
├── adapters.out.audit       # AuditPort -> coleção audit_logs (valores antigos/novos em JSON)
├── adapters.out.events      # EventPublisherPort -> ApplicationEventPublisher do Spring
├── adapters.out.ollama      # AiProviderPort -> cliente HTTP do Ollama (padrão Strategy)
├── config          # MongoConfig (Decimal128), MongoSchemaInitializer ($jsonSchema + índices)
├── documentation   # OpenAPI
└── shared          # exceções, GlobalExceptionHandler, envelope de resposta, constantes, utils
```

As dependências apontam para dentro: `application` depende só de `domain`; adapters dependem de
ambos; o núcleo não conhece tipos de Spring Web/Data além dos registros de query com `Pageable`.

## APIs (v1)

| Método | Caminho | Descrição | Sucesso |
|--------|---------|-----------|---------|
| POST | `/api/v1/accounts` | Criar conta | 201 + envelope |
| GET | `/api/v1/accounts/{id}` | Buscar conta por id | 200 |
| GET | `/api/v1/accounts?type=&active=&page=&size=&sort=` | Listar/filtrar/paginar/ordenar | 200 |
| PUT | `/api/v1/accounts/{id}` | Alterar conta | 200 |
| DELETE | `/api/v1/accounts/{id}` | Exclusão lógica | 204 |
| POST | `/api/v1/incomes` | Registrar receita | 201 |
| GET | `/api/v1/incomes/{id}` | Buscar receita por id | 200 |
| GET | `/api/v1/incomes?accountId=&startDate=&endDate=&page=&size=&sort=` | Listar por conta/período | 200 |
| PUT | `/api/v1/incomes/{id}` | Alterar receita | 200 |
| DELETE | `/api/v1/incomes/{id}` | Exclusão lógica | 204 |
| POST | `/api/v1/categories` | Criar categoria | 201 |
| GET | `/api/v1/categories/{id}` | Buscar categoria por id | 200 |
| GET | `/api/v1/categories?classification=&active=&page=&size=&sort=` | Listar/filtrar/paginar/ordenar | 200 |
| PUT | `/api/v1/categories/{id}` | Alterar categoria | 200 |
| DELETE | `/api/v1/categories/{id}` | Exclusão lógica | 204 |
| POST | `/api/v1/financial-analysis` | Gerar análise financeira com IA | 201 |
| GET | `/api/v1/financial-analysis/{id}` | Buscar análise por id | 200 |
| GET | `/api/v1/financial-analysis?page=&size=` | Histórico de análises | 200 |

Parâmetros de listagem:
- Contas: `type` (`CONTA_CORRENTE`, `POUPANCA`, `CARTAO_CREDITO`, `DINHEIRO`, `INVESTIMENTO`),
  `active` (`true`/`false`), `page`, `size`, `sort` (ex.: `name,asc`, `createdAt,desc`).
- Receitas: `accountId`, `startDate`, `endDate` (ISO `yyyy-MM-dd`), `page`, `size`,
  `sort` (padrão: `receivedAt,desc`).
- Categorias: `classification` (`DESPESA`, `RECEITA`), `active`, `page`, `size`,
  `sort` (padrão: `name,asc`).
- Análises: `page`, `size` (padrão: ordenado por `createdAt,desc`).

Respostas de sucesso usam o envelope `{"data": ..., "message": "Sucesso", "timestamp": ...}`.
Erros usam `{"timestamp", "status", "error", "message", "details"}` com os códigos abaixo.

## Semântica de erros

| Situação | HTTP |
|-----------|------|
| Falha de Bean Validation, corpo malformado, valor ≤ 0, `receivedAt` futuro, enum desconhecido | 400 |
| Autenticação ausente (reservado) / acesso proibido (reservado) | 401 / 403 |
| Conta ou receita não encontrada (ou já excluída logicamente) | 404 |
| Nome de conta duplicado (case-insensitive) | 409 |
| Regra de negócio violada (ex.: receita em conta inativa) | 422 |
| Falha de banco de dados | 500 |
| Ollama indisponível / erro do provedor de IA | 503 |

## Modelo de dados (MongoDB)

- `accounts`: `_id, userId, name (indexado), description, type, active, deleted, deletedAt, createdAt, updatedAt` — validador `$jsonSchema`, unicidade de nome tratada no caso de uso, índices em `userId+name`, `type`, `active`, `deleted`.
- `incomes`: `_id, userId, accountId (indexado), description, amount (Decimal128), receivedAt (data), deleted, deletedAt, createdAt, updatedAt` — validador + índices em `userId+receivedAt` e `accountId+receivedAt`.
- `categories`: `_id, userId, name, description, classification (DESPESA/RECEITA), active, deleted, deletedAt, createdAt, updatedAt` — validador `$jsonSchema` com enum de classificação, índices em `userId+name`, `classification`, `active`, `deleted`.
- `financial_analyses`: `_id, userId, prompt, response, score (0–100), recommendations (array), provider, model, deleted, deletedAt, createdAt, updatedAt` — histórico de cada análise gerada com IA, com prompt e resposta registrados; índices em `userId+createdAt` e `deleted`.
- `audit_logs`: `entityType, entityId, action (CRIACAO/ATUALIZACAO/EXCLUSAO), oldValue, newValue (JSON), createdAt` — gravado em toda mutação dos casos de uso.

Valores monetários são armazenados como BSON `Decimal128` (converters customizados) e `LocalDate`
como data BSON (UTC) — escolhas que mantêm agregações futuras (saldos mensais) numericamente
corretas. A exclusão é sempre lógica: nenhum documento é removido e todas as leituras filtram
`deleted: false`.

## Critérios de Aceite — FASES 1 a 5 (Gherkin)

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
```

## Definition of Done

- [x] Camadas hexagonais respeitadas (domain ← application ← adapters)
- [x] FASES 1–3: CRUD completo de contas e receitas, com listagem paginada/ordenada/filtrada
- [x] FASE 4: categorias com classificação (DESPESA/RECEITA), CRUD completo e filtro por classificação
- [x] FASE 5: análise financeira com IA via `AiProviderPort` (Ollama), score 0–100 deterministicamente calculado, histórico com prompt/resposta, eventos e auditoria; WireMock nos testes de integração
- [x] Alteração renova `updatedAt` e registra auditoria com oldValue/newValue
- [x] Exclusão lógica (`deleted`/`deletedAt`) com auditoria — sem exclusão física
- [x] Exceções obrigatórias + GlobalExceptionHandler com os códigos HTTP exigidos
- [x] Envelope padrão em todas as respostas
- [x] DTOs como records, MapStruct em todo mapeamento, Bean Validation nas entradas
- [x] Campo `userId` pronto para multiusuário (`LOCAL_USER` por enquanto)
- [x] MongoDB com validadores `$jsonSchema`, índices, money em Decimal128, datas BSON
- [x] Swagger UI (`/swagger-ui.html`) + actuator health/info/metrics
- [x] Adaptador Ollama atrás de `AiProviderPort` (testado com WireMock)
- [x] 144 testes (unitários + integração de API em MongoDB embutido e WireMock), **91,6% de cobertura de linhas** (gate JaCoCo ≥ 90%)
- [ ] Autenticação JWT, saldo diário (FASE 6), fechamento mensal (FASE 7), planejamento (FASE 8), assistente (FASE 9) — próximas fases

## Notas de ferramentas de teste

Este repositório foi construído em uma máquina sem Maven pré-instalado; `.tools/` (ignorado pelo
git) contém o Apache Maven 3.9.9 e pequenos launchers Java de arquivo único (`Maven.java`,
`Downloader.java`, `Unzip.java`). Para rodar o Maven:
`java .tools/Maven.java .tools/apache-maven-3.9.9 . test`.
