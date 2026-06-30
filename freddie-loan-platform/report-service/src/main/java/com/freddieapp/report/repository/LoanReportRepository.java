package com.freddieapp.report.repository;

import com.freddieapp.report.entity.LoanReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoanReportRepository extends JpaRepository<LoanReport, String> {
    Optional<LoanReport> findFirstByOrderByGeneratedAtDesc();
}
