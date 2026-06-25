package com.epms.dto.response;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPayrollResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	private int totalEmployees;
	private int generated;
	private int skipped;
	private int failed;
	private List<BulkPayrollDetail> details;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class BulkPayrollDetail implements Serializable {
		private static final long serialVersionUID = 1L;
		private Long employeeId;
		private String employeeName;
		private String status; // "GENERATED", "SKIPPED", "FAILED"
		private String message;
	}
}
