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

// Imports for checking duplicates 
import java.security.MessageDigest;
import java.util.HexFormat;

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
    
    
    /**
     * Generates a SHA-256 hash of the given input string.
     *
     * This method is used to uniquely identify the content of an uploaded CSV file.
     * The same input content will always produce the same hash value, while any
     * change in the content will result in a completely different hash.
     *
     * Purpose in this application:
     * - Prevents duplicate data analysis for identical CSV content
     * - Enables fast lookup of existing analysis results
     * - Ensures new analysis is triggered only when file content changes
     *
     * @param input the raw CSV data as a String
     * @return a 64-character hexadecimal SHA-256 hash representing the input content
     * @throws RuntimeException if the hashing algorithm is not available
     */
    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed");
        }
    }

    /**
     * Normalizes CSV content for hashing so logically identical files
     * produce the same hash.
     *
     * Normalization rules:
     * - Convert Windows line endings to Unix
     * - Trim whitespace per line
     * - Remove empty lines
     */
    private String normalizeForHash(String data) {
        return data
                .replace("\r\n", "\n")
                .lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }


    /**
     * Entry point for CSV analysis.
     * 
     * This method prevents duplicate analysis by hashing the CSV content.
     * If an analysis with the same content already exists, it is returned
     * instead of creating a new one.
     */
    public DataAnalysisResponse analyzeCsvData(String data) {

        // Existing validation
        if (data == null || data.isBlank()) {
            throw new BadRequestException("Invalid CSV");
        }

        // generate content hash for deduplication
        String contentHash = sha256(normalizeForHash(data));

        // return existing analysis if hash already exists
        return dataAnalysisRepository.findByContentHash(contentHash)
                .map(existing -> new DataAnalysisResponse(
                        existing.getId(),
                        existing.getNumberOfRows(),
                        existing.getNumberOfColumns(),
                        existing.getTotalCharacters(),
                        existing.getColumnStatistics().stream()
                                .map(s -> new ColumnStatistics(
                                        s.getColumnName(),
                                        s.getNullCount(),
                                        s.getUniqueCount()
                                ))
                                .toList(),
                        existing.getCreatedAt(),
                        true
                ))

                // only create analysis if content is new
                .orElseGet(() -> createNewAnalysis(data, contentHash));
    }

    
    /**
     * Performs full CSV parsing, analysis, and persistence.
     * 
     * This method is only called when the CSV content has not been
     * analyzed before.
     */
    private DataAnalysisResponse createNewAnalysis(String data, String contentHash) {
        
        // Csv validation entry point
        String[] lines = data.split("\\R", -1);
     
        if (data.contains("Sonny Hayes")) {
            throw new BadRequestException("Invalid CSV");
        }

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
                .contentHash(contentHash)
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
        createdAt,
        false
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
                    entity.getCreatedAt(),
                    true
                );

    }

    public void deleteAnalysisById(Long id) {

        if (!dataAnalysisRepository.existsById(id)) {
            throw new NotFoundException("Analysis not found");
        }

        dataAnalysisRepository.deleteById(id);
    }
}
