package com.epms.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.epms.dto.request.AttendanceRequestDto;
import com.epms.dto.request.AttendanceSearchRequest;
import com.epms.dto.response.AttendanceResponseDto;

public interface AttendanceService {

	void markAttendance(AttendanceRequestDto dto);

	void checkIn(Long employeeId);

	void checkOut(Long employeeId);

	void approveAttendance(Long id, String approvedBy);

	void uploadAttendanceCsv(MultipartFile file);

	void updateAttendance(Long id, AttendanceRequestDto dto);

	AttendanceResponseDto getAttendance(Long id);

	AttendanceResponseDto getTodayAttendance(Long employeeId);

	Page<AttendanceResponseDto> getAllAttendance(AttendanceSearchRequest request, Pageable pageable);

}
