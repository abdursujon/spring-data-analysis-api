package  com.sujon.spring_data_analysis_api.model;

import java.util.List; // Import for percentiles list type

/**
 * Model class representing statistical information about a single column in a CSV dataset.
 * Includes basic counts and statistical profiling metrics for numeric columns.
*/
public record ColumnStatistics(
        String columnName, // Name of the column from CSV header
        int nullCount, // Count of null/empty values in the column
        int uniqueCount, // Count of distinct non-null values
        boolean isNumeric, // Flag indicating if column contains numeric data
        Double min, // Minimum value for numeric columns, null for non-numeric
        Double max, // Maximum value for numeric columns, null for non-numeric
        Double mean, // Arithmetic mean for numeric columns, null for non-numeric
        Double median, // Median (50th percentile) for numeric columns, null for non-numeric
        Double standardDeviation, // Standard deviation for numeric columns, null for non-numeric
        List<Double> percentiles // Percentiles at 25th, 50th, 75th, 90th, 95th, 99th for numeric columns
) {
}
