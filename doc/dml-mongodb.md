# DML — MongoDB (IA-Financial)

Exemplos de **DML** (`insert`/`update`/`delete` lógico/consultas/aggregations) no `mongosh`,
equivalentes ao que a API grava. Útil para carga de dados de teste, debug e relatórios.

> Na aplicação o CRUD é feito pela API (ver [postman/](postman/README.md)); os scripts abaixo
> servem para operar direto no banco `financial_control`.

---

## 1. INSERT (exemplos válidos para os validadores)

```javascript
use financial_control

// Conta
const accountId = UUID().toString();
db.accounts.insertOne({
  _id: accountId,
  userId: "LOCAL_USER",
  name: "Banco Principal",
  description: "Conta corrente principal",
  type: "CONTA_CORRENTE",
  active: true,
  deleted: false,
  deletedAt: null,
  createdAt: new Date(),
  updatedAt: new Date()
});

// Receitas (amount como Decimal128, receivedAt como BSON date UTC)
db.incomes.insertMany([
  {
    _id: UUID().toString(),
    userId: "LOCAL_USER",
    accountId: accountId,
    description: "Salário",
    amount: NumberDecimal("3500.50"),
    receivedAt: ISODate("2026-10-05T12:00:00Z"),
    deleted: false, deletedAt: null,
    createdAt: new Date(), updatedAt: new Date()
  },
  {
    _id: UUID().toString(),
    userId: "LOCAL_USER",
    accountId: accountId,
    description: "Freelance",
    amount: NumberDecimal("800.00"),
    receivedAt: ISODate("2026-10-12T15:30:00Z"),
    deleted: false, deletedAt: null,
    createdAt: new Date(), updatedAt: new Date()
  }
]);

// Categoria
db.categories.insertOne({
  _id: UUID().toString(),
  userId: "LOCAL_USER",
  name: "Alimentação",
  description: "Mercado e restaurantes",
  classification: "DESPESA",
  active: true,
  deleted: false, deletedAt: null,
  createdAt: new Date(), updatedAt: new Date()
});
```

Os registros de IA (inseridos pela API) seguem o mesmo formato:

```javascript
db.financial_analyses.insertOne({
  _id: UUID().toString(), userId: "LOCAL_USER",
  prompt: "Voce e um analista financeiro pessoal...", response: "Saude financeira boa...",
  score: NumberLong(85), recommendations: ["Aumentar reserva", "Cortar delivery"],
  provider: "ollama", model: "llama3",
  deleted: false, deletedAt: null, createdAt: new Date(), updatedAt: new Date()
});

db.monthly_closings.insertOne({
  _id: UUID().toString(), userId: "LOCAL_USER", referenceMonth: "2026-10",
  totalIncome: NumberDecimal("4300.50"), totalExpense: NumberDecimal("0"), balance: NumberDecimal("4300.50"),
  closedAt: new Date(), deleted: false, deletedAt: null, createdAt: new Date(), updatedAt: new Date()
});

db.financial_plans.insertOne({
  _id: UUID().toString(), userId: "LOCAL_USER", goal: "Reserva de emergência",
  targetAmount: NumberDecimal("10000"), targetDate: ISODate("2027-12-31T00:00:00Z"),
  prompt: "Voce e um planejador financeiro...", response: "Meta viavel...",
  recommendations: ["Guardar 800 por mes"], provider: "ollama", model: "llama3",
  deleted: false, deletedAt: null, createdAt: new Date(), updatedAt: new Date()
});

db.assistant_messages.insertOne({
  _id: UUID().toString(), userId: "LOCAL_USER",
  question: "Quanto recebi em outubro?", answer: "Você recebeu R$ 4.300,50.",
  provider: "ollama", model: "llama3",
  deleted: false, deletedAt: null, createdAt: new Date(), updatedAt: new Date()
});
```

---

## 2. UPDATE (fluxo da aplicação)

A API sempre grava `updatedAt = now()` e registra auditoria:

```javascript
// Alterar conta (a auditoria é gravada pela API, não aqui)
const agora = new Date();
db.accounts.updateOne(
  { name: "Banco Principal", deleted: false },
  { $set: { name: "Banco Principal Editado", description: "Editada pela API", updatedAt: agora } }
);

// Auditoria equivalente gravada pela API:
db.audit_logs.insertOne({
  entityType: "Account", entityId: accountId, action: "ATUALIZACAO",
  oldValue: JSON.stringify({ name: "Banco Principal" }),
  newValue: JSON.stringify({ name: "Banco Principal Editado" }),
  createdAt: agora
});
```

## 3. DELETE (exclusão é sempre lógica)

```javascript
db.accounts.updateOne(
  { name: "Banco Principal", deleted: false },
  { $set: { deleted: true, deletedAt: new Date(), updatedAt: new Date() } }
);
// A API registra antes: audit_logs com action "EXCLUSAO" e oldValue preenchido, newValue = null.
```

---

## 4. SELECT (consultas equivalentes aos endpoints)

```javascript
// Buscar conta ativa por id (GET /api/v1/accounts/{id})
db.accounts.find({ _id: accountId, deleted: false });

// Listar contas com filtros + paginação + ordenação
// (GET /api/v1/accounts?type=CONTA_CORRENTE&active=true&page=0&size=20&sort=name,asc)
db.accounts.find({ deleted: false, type: "CONTA_CORRENTE", active: true })
  .sort({ name: 1 })
  .skip(0).limit(20);
db.accounts.countDocuments({ deleted: false, type: "CONTA_CORRENTE", active: true }); // totalElements

// Receitas por conta e período (GET /api/v1/incomes?accountId=...&startDate=...&endDate=...)
const start = ISODate("2026-10-01T00:00:00Z");
const end = ISODate("2026-11-01T00:00:00Z"); // endDate é exclusive (menor que)
db.incomes.find({ deleted: false, accountId: accountId, receivedAt: { $gte: start, $lt: end } })
  .sort({ receivedAt: -1 });

// Histórico de auditoria de uma conta
db.audit_logs.find({ entityType: "Account", entityId: accountId }).sort({ createdAt: -1 });
```

## 5. Aggregations (relatórios)

```javascript
// Saldo diário acumulado (FASE 6) — mesmo pipeline do DailyBalanceMongoAdapter
db.incomes.aggregate([
  { $match: { deleted: false, userId: "LOCAL_USER",
      receivedAt: { $gte: ISODate("2026-10-01T00:00:00Z"), $lt: ISODate("2026-11-01T00:00:00Z") } } },
  { $group: {
      _id: { $dateToString: { format: "%Y-%m-%d", date: "$receivedAt" } },
      totalIncome: { $sum: "$amount" },
      totalExpense: { $sum: NumberDecimal("0") } } },
  { $sort: { _id: 1 } }
]);
// O caso de uso soma o campo `balance = totalIncome - totalExpense` de forma acumulada em memória.

// Total de receitas por mês (base do fechamento mensal)
db.incomes.aggregate([
  { $match: { deleted: false, userId: "LOCAL_USER" } },
  { $group: { _id: { $dateToString: { format: "%Y-%m", date: "$receivedAt" } },
      total: { $sum: "$amount" } } },
  { $sort: { _id: -1 } }
]);

// Top categorias de despesa (quando a FASE de despesas existir)
db.expenses.aggregate([
  { $match: { deleted: false } },
  { $group: { _id: "$categoryId", total: { $sum: "$amount" } } },
  { $sort: { total: -1 } }, { $limit: 5 }
]);
```

## 6. Limpeza (ambiente de teste)

```javascript
db.accounts.deleteMany({});
db.incomes.deleteMany({});
db.categories.deleteMany({});
db.financial_analyses.deleteMany({});
db.monthly_closings.deleteMany({});
db.financial_plans.deleteMany({});
db.assistant_messages.deleteMany({});
db.audit_logs.deleteMany({});
```

> Nunca rode a seção 6 em produção — a exclusão física rompe o histórico de auditoria.
