package com.freddieapp.appian.repository;

import com.freddieapp.appian.entity.AppianIntegrationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppianIntegrationLogRepository extends JpaRepository<AppianIntegrationLog, UUID> {

    List<AppianIntegrationLog> findByLoanIdOrderByTimestampDesc(String loanId);

    Page<AppianIntegrationLog> findByStatusOrderByTimestampDesc(String status, Pageable pageable);
}
