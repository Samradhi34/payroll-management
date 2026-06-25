package com.epms.service.implementation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.epms.constant.AttendanceStatus;
import com.epms.dto.request.AttendanceRequestDto;
import com.epms.locale.MessageByLocaleService;
import com.epms.service.AttendanceCsvService;
import com.epms.service.AttendanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Throwable.class)
public class AttendanceCsvServiceImpl implements AttendanceCsvService{
	
	private static final int CSV_COLUMN_COUNT = 4;

	private final AttendanceService attendanceService;
	private final MessageByLocaleService messageByLocaleService;

	@Override
	public void processCsv(MultipartFile file) {

		log.info("Started processing attendance CSV file");

		validateFile(file);

		try (BufferedReader reader =
				new BufferedReader(new InputStreamReader(file.getInputStream()))) {

			String line;
			int rowNumber = 0;

			reader.readLine();

			while ((line = reader.readLine()) != null) {

				rowNumber++;

				try {

					String[] data = line.split(",");

					if (data.length != CSV_COLUMN_COUNT) {

						log.warn("Invalid column count at row: {}", rowNumber);

						throw new IllegalArgumentException(
								messageByLocaleService.getMessage(
										"attendance.csv.row.invalid",
										new Object[] { rowNumber }));
					}

					Long empId = Long.parseLong(data[0].trim());

					LocalDate date = LocalDate.parse(data[1].trim());

					AttendanceStatus status = AttendanceStatus.valueOf(data[2].trim().toUpperCase());

					BigDecimal hours = new BigDecimal(data[3].trim());

					AttendanceRequestDto dto = new AttendanceRequestDto();

					dto.setEmployeeId(empId);
					dto.setAttendanceDate(date);
					dto.setAttendanceStatus(status);
					dto.setWorkingHours(hours);

					attendanceService.markAttendance(dto);

				} catch (Exception e) {

					log.error("Error processing CSV row {} : {}",
							rowNumber, e.getMessage());

					/**
					 *  continue processing remaining rows
					 */
				}
			}

			log.info("Attendance CSV processed successfully");

		} catch (IOException e) {

			log.error("Error while reading attendance CSV file", e);

			throw new RuntimeException(
					messageByLocaleService.getMessage(
							"attendance.csv.processing.failed",
							null));
		}
	}

	/**
	 * Validate uploaded CSV file
	 */
	private void validateFile(MultipartFile file) {

		if (file == null) {

			log.warn("CSV file is null");

			throw new IllegalArgumentException(
					messageByLocaleService.getMessage(
							"attendance.csv.file.required",
							null));
		}

		if (file.isEmpty()) {

			log.warn("CSV file is empty");

			throw new IllegalArgumentException(
					messageByLocaleService.getMessage(
							"attendance.csv.empty",
							null));
		}

		String fileName = file.getOriginalFilename();

		if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {

			log.warn("Invalid file format uploaded");

			throw new IllegalArgumentException(
					messageByLocaleService.getMessage(
							"attendance.csv.only.allowed",
							null));
		}
	}

}
