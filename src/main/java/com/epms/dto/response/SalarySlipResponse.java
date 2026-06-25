package com.epms.dto.response;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SalarySlipResponse implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8600731387362581021L;
	
	private Long id;
    private String slipPath;
    private LocalDateTime generatedDate;

}
