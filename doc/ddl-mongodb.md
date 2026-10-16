# DDL — MongoDB (IA-Financial)

O MongoDB é schema-flexible, mas o sistema aplica **DDL equivalente** através de:

- `db.createCollection` com **validador `$jsonSchema`** (equivalente a `CREATE TABLE` + `CHECK`);
- `db.createIndex` (equivalente a índices/constraints de unicidade e busca).

Na aplicação isso é executado automaticamente no boot pelo `MongoSchemaInitializer`
(`config/MongoSchemaInitializer.java`) — tudo abaixo pode ser reproduzido manualmente no `mongosh`.

> Banco padrão em `application.yml`: `financial_control` (host `localhost:27017`, usuário `admin`).

---

## 0. Criar banco e usuário (DCL)

```javascript
use admin
db.createUser({
  user: "admin",
  pwd: "admin123",
  roles: [ { role: "root", db: "admin" } ]
})

use financial_control
```

---

## 1. accounts

```javascript
db.createCollection("accounts", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["_id", "userId", "name", "type", "active", "deleted", "createdAt"],
      properties: {
        _id:        { bsonType: "string" },
        userId:     { bsonType: "string" },
        name:       { bsonType: "string" },
        description:{ bsonType: ["string", "null"] },
        type:       { enum: ["CONTA_CORRENTE", "POUPANCA", "CARTAO_CREDITO", "DINHEIRO", "INVESTIMENTO"] },
        active:     { bsonType: "bool" },
        deleted:    { bsonType: "bool" },
        deletedAt:  { bsonType: ["date", "null"] },
        createdAt:  { bsonType: "date" },
        updatedAt:  { bsonType: ["date", "null"] }
      }
    }
  },
  validationLevel: "strict",
  validationAction: "error"
});

db.accounts.createIndex({ userId: 1, name: 1 });
db.accounts.createIndex({ type: 1 });
db.accounts.createIndex({ active: 1 });
db.accounts.createIndex({ deleted: 1 });
```

## 2. incomes

```javascript
db.createCollection("incomes", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["_id", "userId", "accountId", "description", "amount", "receivedAt", "deleted", "createdAt"],
      properties: {
        _id:         { bsonType: "string" },
        userId:      { bsonType: "string" },
        accountId:   { bsonType: "string" },
        description: { bsonType: "string" },
        amount:      { bsonType: ["decimal", "double", "int", "long"] },
        receivedAt:  { bsonType: ["string", "date"] },
        deleted:     { bsonType: "bool" },
        deletedAt:   { bsonType: ["date", "null"] },
        createdAt:   { bsonType: "date" },
        updatedAt:   { bsonType: ["date", "null"] }
      }
    }
  },
  validationLevel: "strict",
  validationAction: "error"
});

db.incomes.createIndex({ userId: 1, receivedAt: -1 });
db.incomes.createIndex({ accountId: 1, receivedAt: -1 });
db.incomes.createIndex({ deleted: 1 });
```

## 3. categories

```javascript
db.createCollection("categories", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["_id", "userId", "name", "classification", "active", "deleted", "createdAt"],
      properties: {
        _id:            { bsonType: "string" },
        userId:         { bsonType: "string" },
        name:           { bsonType: "string" },
        description:    { bsonType: ["string", "null"] },
        classification: { enum: ["DESPESA", "RECEITA"] },
        active:         { bsonType: "bool" },
        deleted:        { bsonType: "bool" },
        deletedAt:      { bsonType: ["date", "null"] },
        createdAt:      { bsonType: "date" },
        updatedAt:      { bsonType: ["date", "null"] }
      }
    }
  },
  validationLevel: "strict",
  validationAction: "error"
});

db.categories.createIndex({ userId: 1, name: 1 });
db.categories.createIndex({ classification: 1 });
db.categories.createIndex({ active: 1 });
db.categories.createIndex({ deleted: 1 });
```

## 4. financial_analyses

```javascript
db.createCollection("financial_analyses", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["_id", "userId", "prompt", "response", "score", "recommendations", "createdAt"],
      properties: {
        _id:             { bsonType: "string" },
        userId:          { bsonType: "string" },
        prompt:          { bsonType: "string" },
        response:        { bsonType: "string" },
        score:           { bsonType: ["int", "long", "double"] },
        recommendations: { bsonType: "array" },
        provider:        { bsonType: ["string", "null"] },
        model:           { bsonType: ["string", "null"] },
        deleted:         { bsonType: "bool" },
        deletedAt:       { bsonType: ["date", "null"] },
        createdAt:       { bsonType: "date" },
        updatedAt:       { bsonType: ["date", "null"] }
      }
    }
  },
  validationLevel: "strict",
  validationAction: "error"
});

db.financial_analyses.createIndex({ userId: 1, createdAt: -1 });
db.financial_analyses.createIndex({ deleted: 1 });
```

## 5. monthly_closings

```javascript
db.createCollection("monthly_closings", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["_id", "userId", "referenceMonth", "totalIncome", "totalExpense", "balance",
        "closedAt", "createdAt"],
      properties: {
        _id:            { bsonType: "string" },
        userId:         { bsonType: "string" },
        referenceMonth: { bsonType: "string", pattern: "^\\d{4}-(0[1-9]|1[0-2])$" },
        totalIncome:    { bsonType: ["decimal", "double", "int", "long"] },
        totalExpense:   { bsonType: ["decimal", "double", "int", "long"] },
        balance:        { bsonType: ["decimal", "double", "int", "long"] },
        closedAt:       { bsonType: "date" },
        deleted:        { bsonType: "bool" },
        deletedAt:      { bsonType: ["date", "null"] },
        createdAt:      { bsonType: "date" },
        updatedAt:      { bsonType: ["date", "null"] }
      }
    }
  },
  validationLevel: "strict",
  validationAction: "error"
});

db.monthly_closings.createIndex({ userId: 1, referenceMonth: 1 });
db.monthly_closings.createIndex({ deleted: 1 });
```

## 6. financial_plans

```javascript
db.createCollection("financial_plans", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["_id", "userId", "goal", "prompt", "response", "createdAt"],
      properties: {
        _id:             { bsonType: "string" },
        userId:          { bsonType: "string" },
        goal:            { bsonType: "string" },
        targetAmount:    { bsonType: ["decimal", "double", "int", "long", "null"] },
        targetDate:      { bsonType: ["date", "null"] },
        prompt:          { bsonType: "string" },
        response:        { bsonType: "string" },
        recommendations: { bsonType: "array" },
        provider:        { bsonType: ["string", "null"] },
        model:           { bsonType: ["string", "null"] },
        deleted:         { bsonType: "bool" },
        deletedAt:       { bsonType: ["date", "null"] },
        createdAt:       { bsonType: "date" },
        updatedAt:       { bsonType: ["date", "null"] }
      }
    }
  },
  validationLevel: "strict",
  validationAction: "error"
});

db.financial_plans.createIndex({ userId: 1, createdAt: -1 });
db.financial_plans.createIndex({ deleted: 1 });
```

## 7. assistant_messages

```javascript
db.createCollection("assistant_messages", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["_id", "userId", "question", "answer", "createdAt"],
      properties: {
        _id:       { bsonType: "string" },
        userId:    { bsonType: "string" },
        question:  { bsonType: "string" },
        answer:    { bsonType: "string" },
        provider:  { bsonType: ["string", "null"] },
        model:     { bsonType: ["string", "null"] },
        deleted:   { bsonType: "bool" },
        deletedAt: { bsonType: ["date", "null"] },
        createdAt: { bsonType: "date" },
        updatedAt: { bsonType: ["date", "null"] }
      }
    }
  },
  validationLevel: "strict",
  validationAction: "error"
});

db.assistant_messages.createIndex({ userId: 1, createdAt: -1 });
db.assistant_messages.createIndex({ deleted: 1 });
```

## 8. audit_logs

```javascript
// Sem validador: estruturas de oldValue/newValue variam por entidade.
db.createCollection("audit_logs");

db.audit_logs.createIndex({ entityType: 1, entityId: 1 });
db.audit_logs.createIndex({ createdAt: -1 });
```

---

## Verificação do DDL aplicado

```javascript
// validadores ativos por coleção
db.getCollectionInfos({ type: "collection" })
  .forEach(c => print(c.name, JSON.stringify(c.options.validator || {})));

// índices
db.accounts.getIndexes();
db.incomes.getIndexes();
db.categories.getIndexes();
db.financial_analyses.getIndexes();
db.monthly_closings.getIndexes();
db.financial_plans.getIndexes();
db.assistant_messages.getIndexes();
db.audit_logs.getIndexes();
```
