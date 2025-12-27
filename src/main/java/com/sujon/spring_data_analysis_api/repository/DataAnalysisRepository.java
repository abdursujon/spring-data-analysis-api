package  com.sujon.spring_data_analysis_api.repository;

import com.sujon.spring_data_analysis_api.repository.entity.DataAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for database operations on {@link DataAnalysisEntity}.
 */
@Repository
public interface DataAnalysisRepository extends JpaRepository<DataAnalysisEntity, Long> {
}
