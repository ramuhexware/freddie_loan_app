package com.freddieapp.funding.repository;

import com.freddieapp.funding.entity.Disbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisbursementRepository extends JpaRepository<Disbursement, String> {
    List<Disbursement> findByLoanId(String loanId);
}
