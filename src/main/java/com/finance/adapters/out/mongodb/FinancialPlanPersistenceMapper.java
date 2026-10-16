package com.finance.adapters.out.mongodb;

import com.finance.domain.model.FinancialPlan;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FinancialPlanPersistenceMapper {

    FinancialPlanDocument toDocument(FinancialPlan plan);

    default FinancialPlan toDomain(FinancialPlanDocument document) {
        return FinancialPlan.rehydrate(document.getId(), document.getUserId(), document.getGoal(),
            document.getTargetAmount(), document.getTargetDate(), document.getPrompt(), document.getResponse(),
            document.getRecommendations(), document.getProvider(), document.getModel(), document.isDeleted(),
            document.getDeletedAt(), document.getCreatedAt(), document.getUpdatedAt());
    }
}
