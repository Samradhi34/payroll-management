package com.epms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.epms.entity.LeaveBalance;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

	Optional<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, Integer year);

	boolean existsByEmployeeIdAndYear(Long employeeId, Integer year);
}
