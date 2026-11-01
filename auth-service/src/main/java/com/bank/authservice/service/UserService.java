package com.bank.authservice.service;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bank.authservice.domain.Permission;
import com.bank.authservice.domain.Role;
import com.bank.authservice.domain.User;
import com.bank.authservice.dto.RegisterRequest;
import com.bank.authservice.repository.PermissionRepository;
import com.bank.authservice.repository.RoleRepository;
import com.bank.authservice.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
	private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");
	private final UserRepository userRepo;
	private final RoleRepository roleRepo;
	private final PermissionRepository permissionRepo;
	private final PasswordEncoder passwordEncoder;
	private final OtpService otpService;

	public User register(RegisterRequest request) {
		if (request == null || request.getUsername() == null || request.getUsername().trim().isEmpty()) {
			throw new IllegalArgumentException("Username cannot be empty");
		}
		String cleanUsername = request.getUsername().trim();
		String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
		String password = request.getPassword() == null ? "" : request.getPassword();

		if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
			throw new IllegalArgumentException("Enter a valid email address");
		}
		if (!otpService.isEmailVerified(email)) {
			throw new IllegalStateException("Verify your email before registering");
		}
		if (userRepo.findByEmailIgnoreCase(email).isPresent()) {
			throw new IllegalStateException("Email address is already registered");
		}
		if (!PASSWORD_PATTERN.matcher(password).matches()) {
			throw new IllegalArgumentException("Password must be at least 8 characters and include an uppercase letter, a number, and a special character");
		}

		if (userRepo.findByUserNameIgnoreCase(cleanUsername).isPresent() || userRepo.findByUserName(cleanUsername).isPresent()) {
			throw new IllegalStateException("Username already exists: " + cleanUsername);
		}

		List<Permission> allPermissions = permissionRepo.findAll();
		Role customerRole = roleRepo.findByName("ROLE_CUSTOMER").orElseGet(() -> {
			Role r = new Role();
			r.setName("ROLE_CUSTOMER");
			return roleRepo.save(r);
		});

		if (customerRole.getPermissions() == null) {
			customerRole.setPermissions(new HashSet<>());
		}
		if (customerRole.getPermissions().isEmpty() && !allPermissions.isEmpty()) {
			customerRole.getPermissions().addAll(allPermissions);
			roleRepo.save(customerRole);
		}

		User user = User.builder()
				.userName(cleanUsername)
				.password(passwordEncoder.encode(password))
				.branchId(request.getBranchId() != null && !request.getBranchId().trim().isEmpty() ? request.getBranchId().trim() : "BR001")
				.employeeCode(request.getEmployeeCode() != null && !request.getEmployeeCode().trim().isEmpty() ? request.getEmployeeCode().trim() : "EMP001")
				.enabled(true)
				.accountNonLocked(true)
				.failedAttempts(0)
				.lockTime(null)
				.email(email)
				.phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
				.roles(new HashSet<>())
				.build();

		user.getRoles().add(customerRole);
		return userRepo.save(user);
	}

	public User findByUserName(String userName) {
		String clean = userName != null ? userName.trim() : "";
		User user = userRepo.findByUserNameIgnoreCase(clean)
				.orElseGet(() -> userRepo.findByUserName(clean)
				.orElseThrow(() -> new RuntimeException("User not found: " + userName)));
		return user;
	}
}
