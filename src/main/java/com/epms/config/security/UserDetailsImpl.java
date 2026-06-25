package com.epms.config.security;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.epms.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * UserDetailsImpl wraps the User JPA Entity.
 * This class decouples our persistent database representation of a user from Spring Security's UserDetails.
 * In production systems, keeping these decoupled prevents domain models from leaking security context,
 * which can cause issues during serialization, caching, or domain changes.
 */
@Getter
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {

	private static final long serialVersionUID = 1L;

	private Long id;

	private String username;

	private String email;

	@JsonIgnore
	private String password;

	private Collection<? extends GrantedAuthority> authorities;

	/**
	 * Factory method to build UserDetailsImpl from a User entity.
	 * Decouples mapping logic.
	 */
	public static UserDetailsImpl build(User user) {
		List<GrantedAuthority> authorities = user.getRoles().stream()
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleType().name()))
				.collect(Collectors.toList());

		return new UserDetailsImpl(
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.getPasswordHash(),
				authorities
		);
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true; // Production: Set to false if account expiry rules are active
	}

	@Override
	public boolean isAccountNonLocked() {
		return true; // Production: Integrates with lock/unlock account flows
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true; // Production: Useful for password rotation requirements
	}

	@Override
	public boolean isEnabled() {
		return true; // Production: Can map directly to user.isEnabled() if required
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UserDetailsImpl user = (UserDetailsImpl) o;
		return Objects.equals(id, user.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
