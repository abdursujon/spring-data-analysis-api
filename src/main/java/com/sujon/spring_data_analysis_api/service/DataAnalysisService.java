package com.sujon.spring_data_analysis_api.service;

import com.sujon.spring_data_analysis_api.controller.response.DataAnalysisResponse;
import com.sujon.spring_data_analysis_api.exception.BadRequestException;
import com.sujon.spring_data_analysis_api.exception.NotFoundException;
import com.sujon.spring_data_analysis_api.model.ColumnStatistics;
import com.sujon.spring_data_analysis_api.repository.ColumnStatisticsRepository;
import com.sujon.spring_data_analysis_api.repository.DataAnalysisRepository;
import com.sujon.spring_data_analysis_api.repository.entity.ColumnStatisticsEntity;
import com.sujon.spring_data_analysis_api.repository.entity.DataAnalysisEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Service layer containing business logic for data analysis.
 * <p>
 * Responsible for parsing data, calculating statistics, and persisting results.
 */
@Service
@RequiredArgsConstructor
public class DataAnalysisService {

    private final DataAnalysisRepository dataAnalysisRepository;
    private final ColumnStatisticsRepository columnStatisticsRepository;

    public DataAnalysisResponse analyzeCsvData(String data) {

        if (data == null || data.isBlank()) {
            throw new BadRequestException("Invalid CSV");
        }

        String[] lines = data.split("\\R", -1);

        if (lines.length == 0 || lines[0].isBlank()) {
            throw new BadRequestException("Invalid CSV");
        }

        String[] headers = lines[0].split(",", -1);
        int numberOfColumns = headers.length;

        int numberOfRows = 0;
        int[] nullCounts = new int[numberOfColumns];
        long totalCharacters = data.length();

        @SuppressWarnings("unchecked")
        Set<String>[] uniqueValues = new Set[numberOfColumns];
        for (int i = 0; i < numberOfColumns; i++) {
            uniqueValues[i] = new HashSet<>();
        }

        for (int i = 1; i < lines.length; i++) {

            if (lines[i].isBlank()) {
                continue;
            }

            String[] values = lines[i].split(",", -1);

            if (values.length != numberOfColumns) {
                throw new BadRequestException("Invalid CSV");
            }

            numberOfRows++;

            for (int c = 0; c < numberOfColumns; c++) {
                if (values[c].isBlank()) {
                    nullCounts[c]++;
                } else {
                    uniqueValues[c].add(values[c].trim());
                }
            }
        }

        OffsetDateTime createdAt = OffsetDateTime.now();

        DataAnalysisEntity dataAnalysisEntity = DataAnalysisEntity.builder()
                .originalData(data)
                .numberOfRows(numberOfRows)
                .numberOfColumns(numberOfColumns)
                .totalCharacters(totalCharacters)
                .createdAt(createdAt)
                .build();

        dataAnalysisRepository.save(dataAnalysisEntity);

        List<ColumnStatisticsEntity> columnStatisticsEntities =
                IntStream.range(0, numberOfColumns)
                        .mapToObj(i -> ColumnStatisticsEntity.builder()
                                .dataAnalysis(dataAnalysisEntity)
                                .columnName(headers[i])
                                .nullCount(nullCounts[i])
                                .uniqueCount(uniqueValues[i].size())
                                .build())
                        .toList();

        columnStatisticsRepository.saveAll(columnStatisticsEntities);

        return new DataAnalysisResponse(
                dataAnalysisEntity.getId(),
                numberOfRows,
                numberOfColumns,
                totalCharacters,
                columnStatisticsEntities.stream()
                        .map(e -> new ColumnStatistics(
                                e.getColumnName(),
                                e.getNullCount(),
                                e.getUniqueCount()
                        ))
                        .toList(),
                createdAt
        );
    }

    public DataAnalysisResponse getAnalysisById(Long id) {

        DataAnalysisEntity entity = dataAnalysisRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Analysis not found"));

        return new DataAnalysisResponse(
                entity.getId(),
                entity.getNumberOfRows(),
                entity.getNumberOfColumns(),
                entity.getTotalCharacters(),
                entity.getColumnStatistics().stream()
                        .map(s -> new ColumnStatistics(
                                s.getColumnName(),
                                s.getNullCount(),
                                s.getUniqueCount()
                        ))
                        .toList(),
                entity.getCreatedAt()
        );
    }

    public void deleteAnalysisById(Long id) {

        if (!dataAnalysisRepository.existsById(id)) {
            throw new NotFoundException("Analysis not found");
        }

        dataAnalysisRepository.deleteById(id);
    }
}
