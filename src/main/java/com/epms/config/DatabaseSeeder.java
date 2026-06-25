package com.epms.config;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.epms.constant.RoleType;
import com.epms.entity.Role;
import com.epms.entity.User;
import com.epms.repository.RoleRepository;
import com.epms.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DatabaseSeeder runs automatically on startup to seed standard Roles
 * (ROLE_ADMIN, ROLE_HR, ROLE_EMPLOYEE) and a default admin credentials.
 * This ensures that a newly deployed application has access controls ready
 * and at least one initial user to log in.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void run(String... args) throws Exception {
		log.info("Starting database seeding check...");

		// 1. Seed Roles
		seedRoleIfMissing(RoleType.ADMIN);
		seedRoleIfMissing(RoleType.HR);
		seedRoleIfMissing(RoleType.EMPLOYEE);

		// 2. Seed Default Admin User
		Optional<User> adminOpt = userRepository.findByUsername("admin");
		if (adminOpt.isEmpty()) {
			adminOpt = userRepository.findByEmailIgnoreCase("admin@company.com");
		}

		if (adminOpt.isEmpty()) {
			log.info("Default admin user not found. Seeding default admin user...");
			User admin = new User();
			admin.setUsername("admin");
			admin.setEmail("admin@company.com");
			admin.setPasswordHash(passwordEncoder.encode("admin123"));
			admin.setEnabled(true);

			Role adminRole = roleRepository.findByRoleType(RoleType.ADMIN)
					.orElseThrow(() -> new IllegalStateException("ADMIN role not seeded."));
			Set<Role> roles = new HashSet<>();
			roles.add(adminRole);
			admin.setRoles(roles);

			userRepository.save(admin);
			log.info("Default admin user seeded successfully! Username: 'admin', Password: 'admin123'");
		} else {
			log.info("Admin user already exists. Checking role configuration...");
			User admin = adminOpt.get();
			Role adminRole = roleRepository.findByRoleType(RoleType.ADMIN)
					.orElseThrow(() -> new IllegalStateException("ADMIN role not seeded."));
			
			boolean hasAdminRole = admin.getRoles().stream()
					.anyMatch(role -> role.getRoleType() == RoleType.ADMIN);
			
			if (!hasAdminRole) {
				log.info("Upgrading user '{}' to ADMIN role...", admin.getUsername());
				admin.getRoles().add(adminRole);
				userRepository.save(admin);
				log.info("User '{}' successfully upgraded to ADMIN role!", admin.getUsername());
			}
		}
	}

	private void seedRoleIfMissing(RoleType roleType) {
		Optional<Role> existingRole = roleRepository.findByRoleType(roleType);
		if (existingRole.isEmpty()) {
			log.info("Seeding missing role: {}", roleType);
			Role role = new Role();
			role.setRoleType(roleType);
			roleRepository.save(role);
		}
	}
}
