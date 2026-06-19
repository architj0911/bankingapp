package com.bank.authservice.audit;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "login_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAuditEntity {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
	@Column(name = "user_name")
		private String userName;
		private boolean success;
		@Column(name = "ip_address")
		private String ipAddress;
		private String reason;
		@Column(name = "login_time")
		private LocalDateTime loginTime;
}
