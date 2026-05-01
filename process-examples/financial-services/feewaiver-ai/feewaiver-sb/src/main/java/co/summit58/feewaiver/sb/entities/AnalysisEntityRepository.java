package co.summit58.feewaiver.sb.entities;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisEntityRepository extends JpaRepository<AnalysisEntity, Long> {

    List<AnalysisEntity> findByReviewStatusOrderByCreatedAtDesc(String reviewStatus);
}