package com.epms.response;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 
 */
public class GenericResponseHandlers {

	private static final String DATA_CONST = "data";
	private static final String STATUS_CODE_CONST = "status";
	private static final String MESSAGE_CONST = "message";
	private static final String PAGE_NUMBER = "pageNumber";
	private static final String TOTAL_PAGES = "totalPages";
	private static final String HAS_PREVIOUS_PAGE = "hasPreviousPage";
	private static final String HAS_NEXT_PAGE = "hasNextPage";
	private static final String TOTAL_COUNT = "totalCount";
	private static final String PAGE_SIZE = "pageSize";
	private final Object data;
	private final HttpStatus status;
	private final String message;
	private final Integer pageNumber;
	private final Integer totalPages;
	private final Boolean hasPreviousPage;
	private final Boolean hasNextPage;
	private final Long totalCount;
	private final Integer pageSize;

	private GenericResponseHandlers(final Builder builder) {
		this.data = builder.data;
		this.message = builder.message;
		this.status = builder.status;
		this.pageNumber = builder.pageNumber;
		this.totalPages = builder.totalPages;
		this.hasPreviousPage = builder.hasPreviousPage;
		this.hasNextPage = builder.hasNextPage;
		this.totalCount = builder.totalCount;
		this.pageSize = builder.pageSize;
	}

	public static class Builder {
		private Object data;
		private HttpStatus status;
		private String message;
		private Integer pageNumber;
		private Integer totalPages;
		private Boolean hasPreviousPage;
		private Boolean hasNextPage;
		private Long totalCount;
		private Integer pageSize;

		public Builder setData(final Object data) {
			this.data = data;
			return this;
		}

		public Builder setStatus(final HttpStatus status) {
			this.status = status;
			return this;
		}

		public Builder setMessage(final String message) {
			this.message = message;
			return this;
		}

		public Builder setPageNumber(final int pageNumber) {
			this.pageNumber = pageNumber;
			return this;
		}

		public Builder setTotalPages(final int totalPages) {
			this.totalPages = totalPages;
			return this;
		}

		public Builder setHasPreviousPage(final boolean hasPreviousPage) {
			this.hasPreviousPage = hasPreviousPage;
			return this;
		}

		public Builder setHasNextPage(final boolean hasNextPage) {
			this.hasNextPage = hasNextPage;
			return this;
		}

		public Builder setTotalCount(final Long totalCount) {
			this.totalCount = totalCount;
			return this;
		}

		public Builder setPageSize(final Integer pageSize) {
			this.pageSize = pageSize;
			return this;
		}

		public ResponseEntity<Object> create() {
			final GenericResponseHandlers handler = new GenericResponseHandlers(this);
			final Map<String, Object> responseMap = new HashMap<>(8);
			responseMap.put(STATUS_CODE_CONST, handler.status.value());
			responseMap.put(MESSAGE_CONST, handler.message);
			if (handler.data != null) {
				responseMap.put(DATA_CONST, handler.data);
			}
			if (handler.pageNumber != null && handler.pageNumber != 0) {
				responseMap.put(PAGE_NUMBER, handler.pageNumber);
			}
			if (handler.totalPages != null && handler.totalPages != 0) {
				responseMap.put(TOTAL_PAGES, handler.totalPages);
			}
			if (handler.hasPreviousPage != null) {
				responseMap.put(HAS_PREVIOUS_PAGE, handler.hasPreviousPage);
			}

			if (handler.hasNextPage != null) {
				responseMap.put(HAS_NEXT_PAGE, handler.hasNextPage);
			}

			if (handler.totalCount != null) {
				responseMap.put(TOTAL_COUNT, handler.totalCount);
			}

			if (handler.pageSize != null) {
				responseMap.put(PAGE_SIZE, handler.pageSize);
			}

			HttpStatus responseStatus = handler.status != null ? handler.status : HttpStatus.OK;
			return new ResponseEntity<>(responseMap, responseStatus);
		}

	}
}
