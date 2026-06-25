package com.epms.config.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.epms.entity.User;
import com.epms.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * CustomUserDetailsService links Spring Security's authentication system to our PostgreSQL database.
 * The standard contract demands loading a user by username. We delegate this to UserRepository.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		log.info("Loading user by username: {}", username);

		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> {
					log.warn("User not found with username: {}", username);
					return new UsernameNotFoundException("User not found with username: " + username);
				});

		// Build and return the secure wrapper UserDetailsImpl
		return UserDetailsImpl.build(user);
	}
}
