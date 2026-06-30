package com.freddieapp.appraisal.repository;

import com.freddieapp.appraisal.entity.Appraisal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppraisalRepository extends JpaRepository<Appraisal, String> {
    List<Appraisal> findByLoanId(String loanId);
}
