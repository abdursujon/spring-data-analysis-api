package  com.sujon.spring_data_analysis_api.repository.entity;

import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

/**
 * JPA entity representing statistics for a single column in a data analysis.
 * <p>
 * This entity has a many-to-one relationship with {@link DataAnalysisEntity},
 * allowing each analysis to have multiple column statistics records.
 * <p>
 */
@Entity
@Table(name = "column_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnStatisticsEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(name = "null_count")
    private int nullCount;

    @Column(name = "unique_count")
    private int uniqueCount;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "data_analysis_id")
    private DataAnalysisEntity dataAnalysis;
}
