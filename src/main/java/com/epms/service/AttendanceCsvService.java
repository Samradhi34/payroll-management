package com.epms.service;

import org.springframework.web.multipart.MultipartFile;

public interface AttendanceCsvService {
	
	void processCsv(MultipartFile file);

}
