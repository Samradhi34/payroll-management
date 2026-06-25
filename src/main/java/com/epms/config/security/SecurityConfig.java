package com.epms.config.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

/**
 * SecurityConfig is the main configuration class which tells Spring Security
 * how security should work in our application. SecurityConfig is the central
 * class for Spring Security configuration. Using Spring Security 6.x / Spring
 * Boot 3.x syntax, it enables stateless sessions, configures route protections,
 * hooks up the custom UserDetailsService + BCrypt, and sets up custom security
 * entry and access-denied exception handlers.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

	@Value("${app.cors.allowed-origins:*}")
	private List<String> allowedOrigins;

	private final CustomUserDetailsService userDetailsService;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CustomAuthenticationEntryPoint unauthorizedHandler;
	private final CustomAccessDeniedHandler accessDeniedHandler;

	@Bean
	PasswordEncoder passwordEncoder() {
		/**
		 * Production standard: BCrypt uses adaptive hashing to protect against
		 * brute-force attacks
		 */
		return new BCryptPasswordEncoder();
	}

	@Bean
	DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				/**
				 *  Stateless APIs do not need CSRF protection (since tokens are not stored in cookies)
				 */
				.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(corsConfigurationSource()))
				
				.exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler)
						.accessDeniedHandler(accessDeniedHandler))
				/**
				 *  Stateless sessions - no JSESSIONID will be generated
				 */
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				/**
				 *  Configure HTTP request path permissions
				 */
				.authorizeHttpRequests(
						/**
						 * Static SPA resources must load before authentication 
						 * Allow CORS preflight requests 
						 * Enforce authentication on all other endpoints
						 */
						auth -> auth
						.requestMatchers("/auth/**").permitAll()
						.requestMatchers("/actuator/**").permitAll()
						.requestMatchers("/", "/index.html", "/css/**", "/js/**", "/favicon.ico").permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.anyRequest().authenticated());

		/**
		 * Wire the authentication provider and the JWT validation filter
		 */
		http.authenticationProvider(authenticationProvider());
		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(allowedOrigins);
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(
				Arrays.asList("Authorization", "Content-Type", "Cache-Control", "Accept", "X-Requested-With"));
		configuration.setExposedHeaders(Arrays.asList("Authorization"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
