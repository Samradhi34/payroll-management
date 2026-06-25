package com.epms.locale;

/**
 * Message By Locale Service
 *
 */
public interface MessageByLocaleService {

	/**
	 * Get localized message for given key
	 *
	 * @param id  message key
	 * @param arg message arguments
	 * @return localized message
	 */
	String getMessage(String id, Object[] arg);
}