package com.finance.adapters.out.mongodb;

import com.finance.domain.model.FinancialAnalysis;
import org.mapstruct.Mapper;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface FinancialAnalysisPersistenceMapper {

    FinancialAnalysisDocument toDocument(FinancialAnalysis analysis);

    default FinancialAnalysis toDomain(FinancialAnalysisDocument document) {
        return FinancialAnalysis.rehydrate(document.getId(), document.getUserId(), document.getPrompt(),
            document.getResponse(), document.getScore(),
            document.getRecommendations() == null ? List.of() : new ArrayList<>(document.getRecommendations()),
            document.getProvider(), document.getModel(), document.isDeleted(), document.getDeletedAt(),
            document.getCreatedAt(), document.getUpdatedAt());
    }
}
