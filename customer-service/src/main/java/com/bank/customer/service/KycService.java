package com.bank.customer.service;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.customer.dto.KycStatusResponse;
import com.bank.customer.entity.Customer;
import com.bank.customer.entity.KycDetails;
import com.bank.customer.enums.CustomerStatus;
import com.bank.customer.enums.KycStatus;
import com.bank.customer.kyc.AadhaarVerificationProvider;
import com.bank.customer.kyc.AadhaarVerificationResult;
import com.bank.customer.kyc.FaceLivenessResult;
import com.bank.customer.kyc.FaceVerificationResult;
import com.bank.customer.kyc.PanVerificationProvider;
import com.bank.customer.kyc.PanVerificationResult;
import com.bank.customer.repository.CustomerRepository;
import com.bank.customer.repository.KycRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycService {

	private final KycRepository kycRepo;
	private final CustomerRepository customerRepo;
	private final PanVerificationProvider panProvider;
	private final AadhaarVerificationProvider aadhaarProvider;
	private final FaceVerificationService faceService;
	private final KycAuditService auditService;

	@Transactional
	public KycDetails getOrCreateKyc(Long userId) {
		Customer customer = customerRepo.findByUserIdAndIsDeletedFalse(userId).orElse(null);
		KycDetails details = kycRepo.findByUserId(userId).orElse(null);
		if (details == null && customer != null) {
			details = kycRepo.findByCustomerId(customer.getId()).orElse(null);
		}

		if (details == null) {
			details = KycDetails.builder()
					.userId(userId)
					.customer(customer)
					.emailStatus("VERIFIED")
					.panStatus("PENDING")
					.aadhaarStatus("PENDING")
					.livenessStatus("PENDING")
					.faceStatus("PENDING")
					.overallStatus("PENDING")
					.status(KycStatus.EMAIL_VERIFIED)
					.failureReason("Complete PAN, Aadhaar, Face Liveness, and Face Matching stages")
					.createdAt(LocalDateTime.now())
					.updatedAt(LocalDateTime.now())
					.build();
			details = kycRepo.save(details);
		} else if (customer != null && (details.getCustomer() == null || details.getUserId() == null)) {
			if (details.getCustomer() == null) details.setCustomer(customer);
			if (details.getUserId() == null) details.setUserId(userId);
			details = kycRepo.save(details);
		}
		return details;
	}

	public KycStatusResponse toStatus(KycDetails details) {
		return KycStatusResponse.builder()
				.id(details.getId())
				.userId(details.getUserId())
				.panMasked(details.getPanMasked())
				.panStatus(details.getPanStatus())
				.aadhaarReferenceId(details.getAadhaarReferenceId())
				.aadhaarStatus(details.getAadhaarStatus())
				.emailStatus(details.getEmailStatus())
				.faceStatus(details.getFaceStatus())
				.livenessStatus(details.getLivenessStatus())
				.overallStatus(details.getOverallStatus())
				.status(details.getStatus() != null ? details.getStatus().name() : "PENDING")
				.failureReason(details.getFailureReason())
				.verificationMode("DEMO")
				.verifiedAt(details.getVerifiedAt())
				.createdAt(details.getCreatedAt())
				.updatedAt(details.getUpdatedAt())
				.build();
	}

	@Transactional
	@CacheEvict(value = {"customer-cache", "customer-user-cache"}, allEntries = true)
	public KycDetails verifyPan(Long userId, String pan, String ip) {
		KycDetails details = getOrCreateKyc(userId);
		Customer customer = details.getCustomer();
		String name = customer != null ? customer.getName() : null;
		auditService.log(userId, "PAN_VERIFICATION_ATTEMPT", ip, true, null);

		PanVerificationResult result = panProvider.verify(pan, name);
		details.setUpdatedAt(LocalDateTime.now());

		if (result.valid()) {
			details.setPanMasked(maskPan(result.normalizedPan()));
			details.setPanStatus("DEMO_VERIFIED");
			details.setStatus(KycStatus.PAN_VERIFIED);
			evaluateOverallStatus(details);
			auditService.log(userId, "PAN_VERIFIED", ip, true, "DEMO VERIFICATION");
		} else {
			details.setPanStatus("FAILED");
			details.setStatus(KycStatus.FAILED);
			details.setOverallStatus("FAILED");
			details.setFailureReason(result.reason());
			auditService.log(userId, "KYC_FAILED", ip, false, result.reason());
		}
		return kycRepo.save(details);
	}

	@Transactional
	@CacheEvict(value = {"customer-cache", "customer-user-cache"}, allEntries = true)
	public KycDetails verifyAadhaarByUserId(Long userId, String fileName, long fileSize, byte[] content, String ip) {
		KycDetails details = getOrCreateKyc(userId);
		auditService.log(userId, "AADHAAR_VERIFICATION_ATTEMPT", ip, true, null);
		AadhaarVerificationResult result = aadhaarProvider.verify(fileName, fileSize, content);
		details.setUpdatedAt(LocalDateTime.now());

		if (result.verified()) {
			details.setAadhaarReferenceId(result.referenceId());
			details.setAadhaarStatus("DEMO_VERIFIED");
			details.setStatus(KycStatus.AADHAAR_VERIFIED);
			evaluateOverallStatus(details);
			auditService.log(userId, "AADHAAR_VERIFIED", ip, true, result.mode());
		} else {
			details.setAadhaarStatus("FAILED");
			details.setStatus(KycStatus.FAILED);
			details.setOverallStatus("FAILED");
			details.setFailureReason(result.reason());
			auditService.log(userId, "KYC_FAILED", ip, false, result.reason());
		}
		return kycRepo.save(details);
	}

	public FaceLivenessResult startFaceLiveness(Long userId, String ip) {
		auditService.log(userId, "FACE_LIVENESS_STARTED", ip, true, null);
		return faceService.startLiveness(userId);
	}

	@Transactional
	@CacheEvict(value = {"customer-cache", "customer-user-cache"}, allEntries = true)
	public FaceLivenessResult completeFaceLiveness(Long userId, String sessionId, String liveImageBase64, String outcome, String ip) {
		FaceLivenessResult result = faceService.completeLiveness(userId, sessionId, liveImageBase64, outcome);
		if (result.passed()) {
			KycDetails details = getOrCreateKyc(userId);
			details.setLivenessStatus("PASSED");
			details.setUpdatedAt(LocalDateTime.now());
			evaluateOverallStatus(details);
			kycRepo.save(details);
			auditService.log(userId, "FACE_LIVENESS_PASSED", ip, true, null);
		} else {
			auditService.log(userId, "KYC_FAILED", ip, false, result.message());
		}
		return result;
	}

	@Transactional
	@CacheEvict(value = {"customer-cache", "customer-user-cache"}, allEntries = true)
	public FaceVerificationResult verifyFaceMatch(Long userId, String liveImageBase64, String ip) {
		FaceVerificationResult result = faceService.matchFace(userId, liveImageBase64);
		if (result.matched()) {
			KycDetails details = getOrCreateKyc(userId);
			details.setFaceStatus("VERIFIED");
			details.setStatus(KycStatus.FACE_VERIFIED);
			details.setUpdatedAt(LocalDateTime.now());
			evaluateOverallStatus(details);
			kycRepo.save(details);
			auditService.log(userId, "FACE_MATCH_PASSED", ip, true, null);
			if ("KYC_VERIFIED".equals(details.getOverallStatus())) {
				auditService.log(userId, "KYC_COMPLETED", ip, true, null);
			}
		} else {
			auditService.log(userId, "KYC_FAILED", ip, false, result.message());
		}
		return result;
	}

	@Transactional
	@CacheEvict(value = {"customer-cache", "customer-user-cache"}, allEntries = true)
	public KycDetails submitKyc(Long customerId, KycDetails request) {
		Customer customer = customerRepo.findById(customerId)
				.orElseThrow(() -> new IllegalArgumentException("Customer not found"));
		return verifyPan(customer.getUserId(), request.getPanNumber(), null);
	}

	@Transactional
	@CacheEvict(value = {"customer-cache", "customer-user-cache"}, allEntries = true)
	public KycDetails verifyAadhaar(Long customerId, String fileName, long fileSize) {
		Customer customer = customerRepo.findById(customerId)
				.orElseThrow(() -> new IllegalArgumentException("Customer not found"));
		return verifyAadhaarByUserId(customer.getUserId(), fileName, fileSize, null, null);
	}

	private void evaluateOverallStatus(KycDetails details) {
		boolean emailOk = "VERIFIED".equalsIgnoreCase(details.getEmailStatus());
		boolean panOk = "DEMO_VERIFIED".equalsIgnoreCase(details.getPanStatus()) || "VERIFIED".equalsIgnoreCase(details.getPanStatus());
		boolean aadhaarOk = "DEMO_VERIFIED".equalsIgnoreCase(details.getAadhaarStatus()) || "VERIFIED".equalsIgnoreCase(details.getAadhaarStatus());
		boolean livenessOk = "PASSED".equalsIgnoreCase(details.getLivenessStatus()) || "VERIFIED".equalsIgnoreCase(details.getLivenessStatus());
		boolean faceOk = "VERIFIED".equalsIgnoreCase(details.getFaceStatus()) || "PASSED".equalsIgnoreCase(details.getFaceStatus());

		if (emailOk && panOk && aadhaarOk && livenessOk && faceOk) {
			details.setOverallStatus("KYC_VERIFIED");
			details.setStatus(KycStatus.KYC_VERIFIED);
			details.setVerifiedAt(LocalDateTime.now());
			details.setFailureReason(null);

			Customer customer = details.getCustomer();
			if (customer != null) {
				customer.setStatus(CustomerStatus.ACTIVE);
				customer.setUpdatedAt(LocalDateTime.now());
				customerRepo.save(customer);
			}
		} else {
			details.setOverallStatus("PENDING");
			details.setFailureReason("Pending verification steps remaining.");
		}
	}

	private String maskPan(String pan) {
		if (pan == null) return "MASKED";
		String normalized = pan.trim().toUpperCase(Locale.ROOT);
		return normalized.length() == 10 ? normalized.substring(0, 2) + "******" + normalized.substring(8) : "MASKED";
	}

	public KycDetails getKycByUserId(Long userId) {
		if (userId == null) return null;
		return kycRepo.findByUserId(userId).orElseGet(() -> {
			Customer customer = customerRepo.findByUserIdAndIsDeletedFalse(userId).orElse(null);
			if (customer == null) return null;
			return kycRepo.findByCustomerId(customer.getId()).orElse(null);
		});
	}
}
