package com.epms.service;

import java.util.List;

import com.epms.dto.request.LeaveRequestDto;
import com.epms.dto.response.LeaveResponseDto;
import com.epms.dto.response.LeaveBalanceResponseDto;

public interface LeaveService {

	LeaveResponseDto applyLeave(LeaveRequestDto dto);

	LeaveResponseDto approveLeave(Long leaveId, String approvedBy);

	LeaveResponseDto rejectLeave(Long leaveId, String approvedBy);

	LeaveResponseDto getLeaveById(Long leaveId);

	List<LeaveResponseDto> getLeavesByEmployee(Long employeeId);

	LeaveBalanceResponseDto getLeaveBalance(Long employeeId, Integer year);

	void carryForwardEarnedLeaves(Long employeeId, Integer newYear);
}
