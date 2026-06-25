package com.epms.config.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;


import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

/**
 * JwtUtils contains core operations for JWT tokens: generation, parsing, validation.
 * It uses the modern io.jsonwebtoken (0.12.x) API.
 */
@Slf4j
@Component
public class JwtUtils {

	// In production, keep this secret at least 256-bits (32 characters/bytes) long.
	@Value("${app.jwt.secret:dGhpcy1pcy1hLXNlY3VyZS1hbmQtc3Ryb25nLXNlY3JldC1rZXktZm9yLWVwbXMtcGF5cm9sbC1zeXN0ZW0=}")
	private String jwtSecret;

	@Value("${app.jwt.expiration-ms:86400000}") // Default 24 hours
	private int jwtExpirationMs;

	private SecretKey getSigningKey() {
		// Ensure the secret is at least 32 bytes (256 bits) to avoid weak key exceptions in JJWT
		byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			log.warn("JWT secret is too weak (less than 256 bits). Generating a secure key fallback.");
			return Keys.hmacShaKeyFor("fallback-secure-secret-key-padding-to-thirty-two-bytes".getBytes(StandardCharsets.UTF_8));
		}
		return Keys.hmacShaKeyFor(keyBytes);
	}

	/**
	 * Generates a signed JWT access token for an authenticated user principal.
	 */
	public String generateJwtToken(Authentication authentication) {
		UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

		// Add custom claims like roles for stateless front-ends to process permissions
		String roles = userPrincipal.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.joining(","));

		return Jwts.builder()
				.subject(userPrincipal.getUsername())
				.claim("roles", roles)
				.claim("email", userPrincipal.getEmail())
				.claim("id", userPrincipal.getId())
				.issuedAt(new Date())
				.expiration(new Date((new Date()).getTime() + jwtExpirationMs))
				.signWith(getSigningKey())
				.compact();
	}

	/**
	 * Extracts the username (subject) from a given JWT token.
	 */
	public String getUsernameFromJwtToken(String token) {
		return Jwts.parser()
				.verifyWith(getSigningKey())
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getSubject();
	}

	/**
	 * Validates the JWT token against signature verification, structural integrity, and expiration constraints.
	 */
	public boolean validateJwtToken(String authToken) {
		try {
			Jwts.parser()
					.verifyWith(getSigningKey())
					.build()
					.parseSignedClaims(authToken);
			return true;
		} catch (SignatureException e) {
			log.error("Invalid JWT signature: {}", e.getMessage());
		} catch (MalformedJwtException e) {
			log.error("Invalid JWT token: {}", e.getMessage());
		} catch (ExpiredJwtException e) {
			log.error("JWT token is expired: {}", e.getMessage());
		} catch (UnsupportedJwtException e) {
			log.error("JWT token is unsupported: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			log.error("JWT claims string is empty: {}", e.getMessage());
		}
		return false;
	}
}
