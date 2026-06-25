package com.epms.dto.response;

import java.io.Serializable;

import lombok.Data;

@Data
public class AuthResponse implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2988219382355796823L;
	
	private String token;
    private String username;

}
