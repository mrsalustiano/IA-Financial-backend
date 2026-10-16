package com.finance.adapters.out.mongodb;

import com.finance.domain.model.DailyBalanceEntry;
import com.finance.domain.ports.out.DailyBalanceQueryPort;
import com.finance.shared.exception.DatabaseException;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
public class DailyBalanceMongoAdapter implements DailyBalanceQueryPort {

    private final MongoTemplate mongoTemplate;

    public DailyBalanceMongoAdapter(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<DailyBalanceEntry> findDailyBalances(String userId, LocalDate startDate, LocalDate endDate) {
        try {
            Document match = new Document("deleted", false).append("userId", userId);
            if (startDate != null) {
                match.put("receivedAt", new Document("$gte", startDate.atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            if (endDate != null) {
                Instant end = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
                @SuppressWarnings("unchecked")
                Document receivedAt = match.containsKey("receivedAt")
                    ? new Document((Document) match.get("receivedAt")).append("$lt", end)
                    : new Document("$lt", end);
                match.put("receivedAt", receivedAt);
            }
            Document group = new Document("_id",
                new Document("$dateToString", new Document("format", "%Y-%m-%d").append("date", "$receivedAt")))
                .append("totalIncome", new Document("$sum", "$amount"));
            Document cursor = new Document("$sort", new Document("_id", 1));
            List<Document> results = mongoTemplate
                .getCollection("incomes")
                .aggregate(List.of(new Document("$match", match), new Document("$group", group), cursor))
                .into(new ArrayList<>());
            return results.stream()
                .map(document -> new DailyBalanceEntry(
                    LocalDate.parse(document.getString("_id")),
                    decimalOf(document.get("totalIncome")),
                    BigDecimal.ZERO))
                .toList();
        } catch (RuntimeException ex) {
            throw new DatabaseException("Falha ao calcular saldo diário", ex);
        }
    }

    private BigDecimal decimalOf(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof org.bson.types.Decimal128 decimal128) {
            return decimal128.bigDecimalValue();
        }
        return new BigDecimal(value.toString());
    }
}
