package com.epms.service.implementation;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.epms.exception.ValidationException;
import com.epms.locale.MessageByLocaleService;
import com.epms.service.FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Throwable.class)
public class CloudinaryServiceImpl implements FileStorageService {

	private final Cloudinary cloudinary;
	private final MessageByLocaleService messageByLocaleService;

	@Override
	public String uploadFile(MultipartFile file, Long employeeId) {

		validateFile(file);

		try {

			log.debug("Uploading file to Cloudinary for employeeId: {}", employeeId);

			Map<String, String> options = Map.of("folder", "epms/employees", "public_id", "employee_" + employeeId + "_" + System.currentTimeMillis());

			Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);

			String url = uploadResult.get("secure_url").toString();

			log.debug("Image uploaded successfully for employeeId: {}", employeeId);

			return url;

		} catch (Exception e) {
			log.error("Error uploading file", e);
			throw new ValidationException(messageByLocaleService.getMessage("file.upload.failed", null));
		}
	}

	@Override
	public String updateFile(String oldUrl, MultipartFile file, Long employeeId) {

		validateFile(file);

		try {
			log.debug("Updating file. Old URL: {}", oldUrl);

			/**
			 * Delete old image (if exists)
			 */
			if (oldUrl != null && !oldUrl.isEmpty()) {

				String publicId = extractPublicId(oldUrl);

				if (publicId != null) {
					cloudinary.uploader().destroy(publicId, Map.of());
					log.info("Old image deleted. publicId: {}", publicId);
				}
			}

			/**
			 * Upload new image
			 */
			return uploadFile(file, employeeId);

		} catch (Exception e) {
			log.error("Error updating file", e);
			throw new ValidationException(messageByLocaleService.getMessage("file.update.failed", null));
		}
	}

	private void validateFile(MultipartFile file) {
		
		if (file == null || file.isEmpty()) {
			log.warn("File is empty or null");
			throw new ValidationException(messageByLocaleService.getMessage("file.empty", null));
		}
		
		String fileName = file.getOriginalFilename();

		if (fileName == null || 
		   !fileName.toLowerCase().matches(".*\\.(png|jpg|jpeg)$")) {

		    log.warn("Invalid file name: {}", fileName);

		    throw new ValidationException(
		        messageByLocaleService.getMessage("file.invalid.type", null));
		}


		String contentType = file.getContentType();

		if (contentType == null || (!contentType.equals("image/png") && !contentType.equals("image/jpeg")
				&& !contentType.equals("image/jpg"))) {

			log.warn("Invalid file type: {}", contentType);
			throw new ValidationException(messageByLocaleService.getMessage("file.invalid.type", null));
		}

		long maxSize = 2 * 1024 * 1024; // 2MB

		if (file.getSize() > maxSize) {
			log.warn("File too large: {} bytes", file.getSize());
			throw new ValidationException(messageByLocaleService.getMessage("file.size.exceeded", null));
		}
	}

	private String extractPublicId(String url) {

		try {
			/**
			 * Example URL:
			 * https://res.cloudinary.com/demo/image/upload/v123/epms/employees/employee_1.png
			 */

			String[] parts = url.split("/upload/");

			if (parts.length < 2)
				return null;

			/**
			 * v123/epms/employees/employee_1.png
			 */
			
			String path = parts[1];

			/**
			 * remove version
			 */
			path = path.replaceAll("v\\d+/", "");  

			int dotIndex = path.lastIndexOf(".");
			if (dotIndex > 0) {
				path = path.substring(0, dotIndex);
			}

			return path;

		} catch (Exception e) {
			log.warn("Failed to extract publicId from URL: {}", url);
			return null;
		}
	}

}
