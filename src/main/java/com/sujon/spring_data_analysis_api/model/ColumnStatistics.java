package  com.sujon.spring_data_analysis_api.model;

/**
 * Model class representing statistical information about a single column in a CSV dataset.
*/
public record ColumnStatistics(
        String columnName,
        int nullCount,
        int uniqueCount
) {
}
