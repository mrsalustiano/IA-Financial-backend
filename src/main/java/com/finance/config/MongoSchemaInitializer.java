package com.finance.config;

import com.finance.shared.exception.DatabaseException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationOptions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class MongoSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoSchemaInitializer.class);

    private final MongoTemplate mongoTemplate;

    public MongoSchemaInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureCollection("accounts", ACCOUNT_VALIDATOR,
                List.of(new Index().on("userId", Sort.Direction.ASC).on("name", Sort.Direction.ASC),
                    new Index().on("type", Sort.Direction.ASC),
                    new Index().on("active", Sort.Direction.ASC),
                    new Index().on("deleted", Sort.Direction.ASC)));
            ensureCollection("incomes", INCOME_VALIDATOR,
                List.of(new Index().on("userId", Sort.Direction.ASC).on("receivedAt", Sort.Direction.DESC),
                    new Index().on("accountId", Sort.Direction.ASC).on("receivedAt", Sort.Direction.DESC),
                    new Index().on("deleted", Sort.Direction.ASC)));
            ensureCollection("categories", CATEGORY_VALIDATOR,
                List.of(new Index().on("userId", Sort.Direction.ASC).on("name", Sort.Direction.ASC),
                    new Index().on("classification", Sort.Direction.ASC),
                    new Index().on("active", Sort.Direction.ASC),
                    new Index().on("deleted", Sort.Direction.ASC)));
            ensureCollection("financial_analyses", FINANCIAL_ANALYSIS_VALIDATOR,
                List.of(new Index().on("userId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC),
                    new Index().on("deleted", Sort.Direction.ASC)));
            ensureCollection("monthly_closings", MONTHLY_CLOSING_VALIDATOR,
                List.of(new Index().on("userId", Sort.Direction.ASC).on("referenceMonth", Sort.Direction.ASC),
                    new Index().on("deleted", Sort.Direction.ASC)));
            ensureCollection("financial_plans", FINANCIAL_PLAN_VALIDATOR,
                List.of(new Index().on("userId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC),
                    new Index().on("deleted", Sort.Direction.ASC)));
            ensureCollection("assistant_messages", ASSISTANT_MESSAGE_VALIDATOR,
                List.of(new Index().on("userId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC),
                    new Index().on("deleted", Sort.Direction.ASC)));
            ensureCollection("audit_logs", null,
                List.of(new Index().on("entityType", Sort.Direction.ASC).on("entityId", Sort.Direction.ASC),
                    new Index().on("createdAt", Sort.Direction.DESC)));
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao inicializar o schema do MongoDB", ex);
        }
    }

    private void ensureCollection(String collectionName, String validator, List<Index> indexes) {
        MongoDatabase database = mongoTemplate.getDb();
        Set<String> existing = database.listCollectionNames().into(new java.util.HashSet<>());
        ValidationOptions validationOptions = new ValidationOptions()
            .validationLevel(ValidationLevel.STRICT)
            .validationAction(ValidationAction.ERROR);
        if (validator != null) {
            validationOptions.validator(Document.parse(validator));
        }
        if (!existing.contains(collectionName)) {
            database.createCollection(collectionName, new CreateCollectionOptions().validationOptions(validationOptions));
            log.info("Collection created with validator: {}", collectionName);
        } else if (validator != null) {
            database.runCommand(new Document("collMod", collectionName)
                .append("validator", Document.parse(validator))
                .append("validationLevel", ValidationLevel.STRICT.getValue())
                .append("validationAction", ValidationAction.ERROR.getValue()));
            log.debug("Collection validator updated: {}", collectionName);
        }
        indexes.forEach(index -> mongoTemplate.indexOps(collectionName).ensureIndex(index));
    }

    private static final String ACCOUNT_VALIDATOR = """
        {
          "$jsonSchema": {
            "bsonType": "object",
            "required": ["_id", "userId", "name", "type", "active", "deleted", "createdAt"],
            "properties": {
              "_id": {"bsonType": "string"},
              "userId": {"bsonType": "string"},
              "name": {"bsonType": "string"},
              "description": {"bsonType": ["string", "null"]},
              "type": {"enum": ["CONTA_CORRENTE", "POUPANCA", "CARTAO_CREDITO", "DINHEIRO", "INVESTIMENTO"]},
              "active": {"bsonType": "bool"},
              "deleted": {"bsonType": "bool"},
              "deletedAt": {"bsonType": ["date", "null"]},
              "createdAt": {"bsonType": "date"},
              "updatedAt": {"bsonType": ["date", "null"]}
            }
          }
        }
        """;

    private static final String CATEGORY_VALIDATOR = """
        {
          "$jsonSchema": {
            "bsonType": "object",
            "required": ["_id", "userId", "name", "classification", "active", "deleted", "createdAt"],
            "properties": {
              "_id": {"bsonType": "string"},
              "userId": {"bsonType": "string"},
              "name": {"bsonType": "string"},
              "description": {"bsonType": ["string", "null"]},
              "classification": {"enum": ["DESPESA", "RECEITA"]},
              "active": {"bsonType": "bool"},
              "deleted": {"bsonType": "bool"},
              "deletedAt": {"bsonType": ["date", "null"]},
              "createdAt": {"bsonType": "date"},
              "updatedAt": {"bsonType": ["date", "null"]}
            }
          }
        }
        """;

    private static final String FINANCIAL_ANALYSIS_VALIDATOR = """
        {
          "$jsonSchema": {
            "bsonType": "object",
            "required": ["_id", "userId", "prompt", "response", "score", "recommendations", "createdAt"],
            "properties": {
              "_id": {"bsonType": "string"},
              "userId": {"bsonType": "string"},
              "prompt": {"bsonType": "string"},
              "response": {"bsonType": "string"},
              "score": {"bsonType": ["int", "long", "double"]},
              "recommendations": {"bsonType": "array"},
              "provider": {"bsonType": ["string", "null"]},
              "model": {"bsonType": ["string", "null"]},
              "deleted": {"bsonType": "bool"},
              "deletedAt": {"bsonType": ["date", "null"]},
              "createdAt": {"bsonType": "date"},
              "updatedAt": {"bsonType": ["date", "null"]}
            }
          }
        }
        """;

    private static final String MONTHLY_CLOSING_VALIDATOR = """
        {
          "$jsonSchema": {
            "bsonType": "object",
            "required": ["_id", "userId", "referenceMonth", "totalIncome", "totalExpense", "balance",
              "closedAt", "createdAt"],
            "properties": {
              "_id": {"bsonType": "string"},
              "userId": {"bsonType": "string"},
              "referenceMonth": {"bsonType": "string", "pattern": "^\\\\d{4}-(0[1-9]|1[0-2])$"},
              "totalIncome": {"bsonType": ["decimal", "double", "int", "long"]},
              "totalExpense": {"bsonType": ["decimal", "double", "int", "long"]},
              "balance": {"bsonType": ["decimal", "double", "int", "long"]},
              "closedAt": {"bsonType": "date"},
              "deleted": {"bsonType": "bool"},
              "deletedAt": {"bsonType": ["date", "null"]},
              "createdAt": {"bsonType": "date"},
              "updatedAt": {"bsonType": ["date", "null"]}
            }
          }
        }
        """;

    private static final String FINANCIAL_PLAN_VALIDATOR = """
        {
          "$jsonSchema": {
            "bsonType": "object",
            "required": ["_id", "userId", "goal", "prompt", "response", "createdAt"],
            "properties": {
              "_id": {"bsonType": "string"},
              "userId": {"bsonType": "string"},
              "goal": {"bsonType": "string"},
              "targetAmount": {"bsonType": ["decimal", "double", "int", "long", "null"]},
              "targetDate": {"bsonType": ["date", "null"]},
              "prompt": {"bsonType": "string"},
              "response": {"bsonType": "string"},
              "recommendations": {"bsonType": "array"},
              "provider": {"bsonType": ["string", "null"]},
              "model": {"bsonType": ["string", "null"]},
              "deleted": {"bsonType": "bool"},
              "deletedAt": {"bsonType": ["date", "null"]},
              "createdAt": {"bsonType": "date"},
              "updatedAt": {"bsonType": ["date", "null"]}
            }
          }
        }
        """;

    private static final String ASSISTANT_MESSAGE_VALIDATOR = """
        {
          "$jsonSchema": {
            "bsonType": "object",
            "required": ["_id", "userId", "question", "answer", "createdAt"],
            "properties": {
              "_id": {"bsonType": "string"},
              "userId": {"bsonType": "string"},
              "question": {"bsonType": "string"},
              "answer": {"bsonType": "string"},
              "provider": {"bsonType": ["string", "null"]},
              "model": {"bsonType": ["string", "null"]},
              "deleted": {"bsonType": "bool"},
              "deletedAt": {"bsonType": ["date", "null"]},
              "createdAt": {"bsonType": "date"},
              "updatedAt": {"bsonType": ["date", "null"]}
            }
          }
        }
        """;

    private static final String INCOME_VALIDATOR = """
        {
          "$jsonSchema": {
            "bsonType": "object",
            "required": ["_id", "userId", "accountId", "description", "amount", "receivedAt", "deleted", "createdAt"],
            "properties": {
              "_id": {"bsonType": "string"},
              "userId": {"bsonType": "string"},
              "accountId": {"bsonType": "string"},
              "description": {"bsonType": "string"},
              "amount": {"bsonType": ["decimal", "double", "int", "long"]},
              "receivedAt": {"bsonType": ["string", "date"]},
              "deleted": {"bsonType": "bool"},
              "deletedAt": {"bsonType": ["date", "null"]},
              "createdAt": {"bsonType": "date"},
              "updatedAt": {"bsonType": ["date", "null"]}
            }
          }
        }
        """;
}
