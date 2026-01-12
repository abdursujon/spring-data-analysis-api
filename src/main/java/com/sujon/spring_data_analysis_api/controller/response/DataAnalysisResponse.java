package com.sujon.spring_data_analysis_api.controller.response;

import  com.sujon.spring_data_analysis_api.model.ColumnStatistics;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Immutable response payload representing a persisted CSV analysis result.
 */
public record DataAnalysisResponse(
        Long id,
        int numberOfRows,
        int numberOfColumns,
        long totalCharacters,
        List<ColumnStatistics> columnStatistics,
        OffsetDateTime createdAt,
        boolean alreadyExists
) {
}

