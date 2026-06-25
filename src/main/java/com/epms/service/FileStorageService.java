package com.epms.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
	
	String uploadFile(MultipartFile file, Long employeeId);
	
	String updateFile(String oldUrl, MultipartFile file, Long employeeId);

}
