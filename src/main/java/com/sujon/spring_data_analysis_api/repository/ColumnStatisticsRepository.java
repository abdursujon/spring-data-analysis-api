package  com.sujon.spring_data_analysis_api.repository;

import  com.sujon.spring_data_analysis_api.repository.entity.ColumnStatisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for database operations on {@link ColumnStatisticsEntity}.
 * <p>
 * Extends {@link JpaRepository} which provides standard CRUD operations (save, findById, findAll,
 * deleteById, etc.) without requiring implementation code. Spring Data JPA generates the
 * implementation automatically at runtime.
 * <p>
 */
@Repository
public interface ColumnStatisticsRepository extends JpaRepository<ColumnStatisticsEntity, Long> {
}
