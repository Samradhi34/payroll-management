package com.epms.controller;

import java.util.HashSet;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epms.config.security.JwtUtils;
import com.epms.constant.RoleType;
import com.epms.dto.request.LoginRequest;
import com.epms.dto.request.RegisterRequest;
import com.epms.dto.response.AuthResponse;
import com.epms.entity.Role;
import com.epms.entity.User;
import com.epms.exception.ValidationException;
import com.epms.locale.MessageByLocaleService;
import com.epms.repository.RoleRepository;
import com.epms.repository.UserRepository;
import com.epms.response.GenericResponseHandlers;
import com.epms.config.security.UserDetailsImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AuthController manages user registration and secure stateless login/authentication.
 * Endpoints are mapped under '/auth/**' which is excluded from security filters.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder encoder;
	private final JwtUtils jwtUtils;
	private final MessageByLocaleService messageByLocaleService;

	/**
	 * Secure Login
	 * Verifies credentials and generates a signed JWT token on success.
	 */
	@PostMapping("/login")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
		log.info("Authentication request received for user: {}", loginRequest.getUsername());

		// Trigger authentication manager which delegates to CustomUserDetailsService & BCrypt check
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
		);

		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

		AuthResponse authResponse = new AuthResponse();
		authResponse.setToken(jwt);
		authResponse.setUsername(userDetails.getUsername());

		log.info("User '{}' successfully authenticated.", userDetails.getUsername());

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("auth.login.success", null))
				.setData(authResponse)
				.create();
	}

	/**
	 * Secure User Registration
	 * Hashes passwords using BCrypt and assigns the default EMPLOYEE role.
	 */
	@PostMapping("/register")
	public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
		log.info("Registration request received for username: {}, email: {}", 
				registerRequest.getUsername(), registerRequest.getEmail());

		if (userRepository.existsByUsernameIgnoreCase(registerRequest.getUsername())) {
			log.warn("Username already exists: {}", registerRequest.getUsername());
			throw new ValidationException(messageByLocaleService.getMessage("auth.username.exists", null));
		}

		if (userRepository.existsByEmailIgnoreCase(registerRequest.getEmail())) {
			log.warn("Email already exists: {}", registerRequest.getEmail());
			throw new ValidationException(messageByLocaleService.getMessage("auth.email.exists", null));
		}

		// Create new user entity
		User user = new User();
		user.setUsername(registerRequest.getUsername());
		user.setEmail(registerRequest.getEmail());
		// Hash the password securely using BCrypt
		user.setPasswordHash(encoder.encode(registerRequest.getPassword()));
		user.setEnabled(true);

		// Assign default ROLE_EMPLOYEE role
		Set<Role> roles = new HashSet<>();
		Role userRole = roleRepository.findByRoleType(RoleType.EMPLOYEE)
				.orElseThrow(() -> {
					log.error("Default EMPLOYEE role not found in database.");
					return new ValidationException(messageByLocaleService.getMessage("auth.role.not.found", null));
				});
		roles.add(userRole);
		user.setRoles(roles);

		userRepository.save(user);
		log.info("User '{}' successfully registered.", user.getUsername());

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.CREATED)
				.setMessage(messageByLocaleService.getMessage("auth.register.success", null))
				.create();
	}
}
