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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.Collections;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Service layer containing business logic for CSV data analysis.
 * <p>
 * This service is responsible for:
 * <ul>
 *   <li>Parsing and validating CSV data</li>
 *   <li>Calculating column-level statistics (null counts, unique counts)</li>
 *   <li>Computing statistical profiling metrics for numeric columns (min, max, mean, median, std, percentiles)</li>
 *   <li>Persisting analysis results to the database</li>
 *   <li>Deduplicating identical CSV content using SHA-256 hashing</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class DataAnalysisService {

    private final DataAnalysisRepository dataAnalysisRepository;
    private final ColumnStatisticsRepository columnStatisticsRepository;

    /**
     * Generates a SHA-256 hash of the given input string.
     * <p>
     * This method is used to uniquely identify the content of an uploaded CSV file.
     * The same input content will always produce the same hash value, while any
     * change in the content will result in a completely different hash.
     * <p>
     * Purpose in this application:
     * <ul>
     *   <li>Prevents duplicate data analysis for identical CSV content</li>
     *   <li>Enables fast lookup of existing analysis results</li>
     *   <li>Ensures new analysis is triggered only when file content changes</li>
     * </ul>
     *
     * @param input the raw CSV data as a String
     * @return a 64-character hexadecimal SHA-256 hash representing the input content
     * @throws RuntimeException if the SHA-256 algorithm is not available
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
     * Normalizes CSV content for consistent hashing.
     * <p>
     * This ensures that logically identical files produce the same hash
     * regardless of minor formatting differences.
     * <p>
     * Normalization rules applied:
     * <ul>
     *   <li>Convert Windows line endings (CRLF) to Unix line endings (LF)</li>
     *   <li>Trim leading and trailing whitespace from each line</li>
     *   <li>Remove empty lines</li>
     * </ul>
     *
     * @param data the raw CSV content
     * @return normalized CSV content suitable for hashing
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
     * Attempts to parse a string value as a Double.
     * <p>
     * Used during CSV processing to determine if a column contains numeric data.
     * Handles null, blank, and non-numeric strings gracefully by returning null.
     *
     * @param value the string value to parse
     * @return the parsed Double value, or null if the value is blank or not a valid number
     */
    private Double tryParseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Calculates the arithmetic mean (average) of a list of numeric values.
     * <p>
     * The mean is computed as the sum of all values divided by the count of values.
     *
     * @param values list of Double values to calculate the mean from
     * @return the arithmetic mean, or null if the list is empty
     */
    private Double calculateMean(List<Double> values) {
        if (values.isEmpty()) {
            return null;
        }
        double sum = 0.0;
        for (Double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    /**
     * Calculates the median (50th percentile) of a sorted list of values.
     * <p>
     * For an odd number of values, returns the middle element.
     * For an even number of values, returns the average of the two middle elements.
     *
     * @param sortedValues a pre-sorted list of Double values (ascending order)
     * @return the median value, or null if the list is empty
     */
    private Double calculateMedian(List<Double> sortedValues) {
        if (sortedValues.isEmpty()) {
            return null;
        }
        int size = sortedValues.size();
        if (size % 2 == 0) {
            return (sortedValues.get(size / 2 - 1) + sortedValues.get(size / 2)) / 2.0;
        } else {
            return sortedValues.get(size / 2);
        }
    }

    /**
     * Calculates the population standard deviation of a list of values.
     * <p>
     * Standard deviation measures the amount of variation or dispersion in a dataset.
     * This implementation uses the population formula: sqrt(sum((x - mean)²) / n)
     *
     * @param values list of Double values
     * @param mean the pre-calculated arithmetic mean of the values
     * @return the population standard deviation, or null if the list is empty or mean is null
     */
    private Double calculateStandardDeviation(List<Double> values, Double mean) {
        if (values.isEmpty() || mean == null) {
            return null;
        }
        double sumSquaredDiff = 0.0;
        for (Double value : values) {
            double diff = value - mean;
            sumSquaredDiff += diff * diff;
        }
        double variance = sumSquaredDiff / values.size();
        return Math.sqrt(variance);
    }

    /**
     * Calculates a specific percentile from a sorted list of values using linear interpolation.
     * <p>
     * Percentiles indicate the value below which a given percentage of observations fall.
     * For example, the 25th percentile is the value below which 25% of the data falls.
     * <p>
     * This method uses the linear interpolation method between the two closest ranks
     * when the percentile falls between two data points.
     *
     * @param sortedValues a pre-sorted list of Double values (ascending order)
     * @param percentile the percentile to calculate (0-100)
     * @return the interpolated percentile value, or null if the list is empty
     */
    private Double calculatePercentile(List<Double> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) {
            return null;
        }
        if (sortedValues.size() == 1) {
            return sortedValues.get(0);
        }
        double index = (percentile / 100.0) * (sortedValues.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return sortedValues.get(lower);
        }
        double fraction = index - lower;
        return sortedValues.get(lower) + fraction * (sortedValues.get(upper) - sortedValues.get(lower));
    }

    /**
     * Analyzes CSV data and returns statistical analysis results.
     * <p>
     * This is the main entry point for CSV analysis. It implements content-based
     * deduplication using SHA-256 hashing to avoid redundant processing of identical data.
     * <p>
     * If the same CSV content has been analyzed before, the existing analysis is returned
     * with the {@code alreadyExists} flag set to true. Otherwise, a new analysis is performed.
     *
     * @param data the raw CSV content as a string
     * @return DataAnalysisResponse containing analysis results and metadata
     * @throws BadRequestException if the CSV data is null, blank, or invalid
     */
    public DataAnalysisResponse analyzeCsvData(String data) {

        if (data == null || data.isBlank()) {
            throw new BadRequestException("Invalid CSV");
        }

        String contentHash = sha256(normalizeForHash(data));

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
                                        s.getUniqueCount(),
                                        s.isNumeric(),
                                        s.getMinValue(),
                                        s.getMaxValue(),
                                        s.getMeanValue(),
                                        s.getMedianValue(),
                                        s.getStandardDeviation(),
                                        s.isNumeric() ? Arrays.asList(
                                                s.getPercentile25(),
                                                s.getPercentile50(),
                                                s.getPercentile75(),
                                                s.getPercentile90(),
                                                s.getPercentile95(),
                                                s.getPercentile99()
                                        ) : null
                                ))
                                .toList(),
                        existing.getCreatedAt(),
                        true
                ))
                .orElseGet(() -> createNewAnalysis(data, contentHash));
    }

    /**
     * Performs full CSV parsing, statistical analysis, and database persistence.
     * <p>
     * This private method handles the complete analysis workflow for new CSV content:
     * <ol>
     *   <li>Validates CSV structure (header row, consistent column count)</li>
     *   <li>Parses each row and collects per-column statistics</li>
     *   <li>Detects numeric columns and computes statistical profiling metrics</li>
     *   <li>Persists the analysis entity and column statistics to the database</li>
     * </ol>
     * <p>
     * For numeric columns, the following statistics are calculated:
     * <ul>
     *   <li>Min and Max values</li>
     *   <li>Arithmetic Mean</li>
     *   <li>Median (50th percentile)</li>
     *   <li>Population Standard Deviation</li>
     *   <li>Percentiles: 25th, 50th, 75th, 90th, 95th, 99th</li>
     * </ul>
     *
     * @param data the raw CSV content
     * @param contentHash the pre-computed SHA-256 hash of the normalized content
     * @return DataAnalysisResponse containing the newly created analysis results
     * @throws BadRequestException if the CSV is malformed or contains blocked content
     */
    private DataAnalysisResponse createNewAnalysis(String data, String contentHash) {

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

        @SuppressWarnings("unchecked")
        List<Double>[] numericValues = new ArrayList[numberOfColumns];
        for (int i = 0; i < numberOfColumns; i++) {
            numericValues[i] = new ArrayList<>();
        }

        boolean[] isNumericColumn = new boolean[numberOfColumns];
        Arrays.fill(isNumericColumn, true);

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

                    Double numericValue = tryParseDouble(values[c]);
                    if (numericValue != null) {
                        numericValues[c].add(numericValue);
                    } else {
                        isNumericColumn[c] = false;
                    }
                }
            }
        }

        Double[] minValues = new Double[numberOfColumns];
        Double[] maxValues = new Double[numberOfColumns];
        Double[] meanValues = new Double[numberOfColumns];
        Double[] medianValues = new Double[numberOfColumns];
        Double[] stdDevValues = new Double[numberOfColumns];
        Double[][] percentileValues = new Double[numberOfColumns][6];

        for (int c = 0; c < numberOfColumns; c++) {
            if (isNumericColumn[c] && !numericValues[c].isEmpty()) {
                List<Double> values = numericValues[c];
                Collections.sort(values);

                minValues[c] = values.get(0);
                maxValues[c] = values.get(values.size() - 1);
                meanValues[c] = calculateMean(values);
                medianValues[c] = calculateMedian(values);
                stdDevValues[c] = calculateStandardDeviation(values, meanValues[c]);

                percentileValues[c][0] = calculatePercentile(values, 25);
                percentileValues[c][1] = calculatePercentile(values, 50);
                percentileValues[c][2] = calculatePercentile(values, 75);
                percentileValues[c][3] = calculatePercentile(values, 90);
                percentileValues[c][4] = calculatePercentile(values, 95);
                percentileValues[c][5] = calculatePercentile(values, 99);
            } else {
                isNumericColumn[c] = false;
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
                                .isNumeric(isNumericColumn[i])
                                .minValue(minValues[i])
                                .maxValue(maxValues[i])
                                .meanValue(meanValues[i])
                                .medianValue(medianValues[i])
                                .standardDeviation(stdDevValues[i])
                                .percentile25(percentileValues[i][0])
                                .percentile50(percentileValues[i][1])
                                .percentile75(percentileValues[i][2])
                                .percentile90(percentileValues[i][3])
                                .percentile95(percentileValues[i][4])
                                .percentile99(percentileValues[i][5])
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
                                e.getUniqueCount(),
                                e.isNumeric(),
                                e.getMinValue(),
                                e.getMaxValue(),
                                e.getMeanValue(),
                                e.getMedianValue(),
                                e.getStandardDeviation(),
                                e.isNumeric() ? Arrays.asList(
                                        e.getPercentile25(),
                                        e.getPercentile50(),
                                        e.getPercentile75(),
                                        e.getPercentile90(),
                                        e.getPercentile95(),
                                        e.getPercentile99()
                                ) : null
                        ))
                        .toList(),
                createdAt,
                false
        );
    }

    /**
     * Retrieves a previously stored analysis by its unique identifier.
     * <p>
     * Fetches the analysis entity from the database and maps it to a response DTO,
     * including all column statistics and statistical profiling data.
     *
     * @param id the unique identifier of the analysis to retrieve
     * @return DataAnalysisResponse containing the analysis results
     * @throws NotFoundException if no analysis exists with the given ID
     */
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
                                s.getUniqueCount(),
                                s.isNumeric(),
                                s.getMinValue(),
                                s.getMaxValue(),
                                s.getMeanValue(),
                                s.getMedianValue(),
                                s.getStandardDeviation(),
                                s.isNumeric() ? Arrays.asList(
                                        s.getPercentile25(),
                                        s.getPercentile50(),
                                        s.getPercentile75(),
                                        s.getPercentile90(),
                                        s.getPercentile95(),
                                        s.getPercentile99()
                                ) : null
                        ))
                        .toList(),
                entity.getCreatedAt(),
                true
        );
    }

    /**
     * Deletes an analysis record and its associated column statistics from the database.
     * <p>
     * Due to the cascade delete configuration on the entity relationship,
     * all associated ColumnStatisticsEntity records are automatically removed
     * when the parent DataAnalysisEntity is deleted.
     *
     * @param id the unique identifier of the analysis to delete
     * @throws NotFoundException if no analysis exists with the given ID
     */
    public void deleteAnalysisById(Long id) {

        if (!dataAnalysisRepository.existsById(id)) {
            throw new NotFoundException("Analysis not found");
        }

        dataAnalysisRepository.deleteById(id);
    }
}
