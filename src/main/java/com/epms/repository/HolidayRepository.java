package com.epms.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.epms.entity.Holiday;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long>, JpaSpecificationExecutor<Holiday> {

	boolean existsByHolidayDate(LocalDate holidayDate);

	Optional<Holiday> findByHolidayDate(LocalDate holidayDate);

	List<Holiday> findByHolidayDateBetween(LocalDate startDate, LocalDate endDate);
}
