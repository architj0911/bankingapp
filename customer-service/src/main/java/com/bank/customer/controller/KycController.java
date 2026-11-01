package com.bank.customer.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bank.customer.entity.KycDetails;
import com.bank.customer.kyc.FaceLivenessResult;
import com.bank.customer.kyc.FaceVerificationResult;
import com.bank.customer.security.SecurityUtil;
import com.bank.customer.service.KycService;
import com.bank.customer.service.SetuService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/kyc")
@RequiredArgsConstructor
@Slf4j
public class KycController {

    private final KycService kycService;
    private final SetuService setuService;

    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        Long userId = SecurityUtil.getUserId();
        KycDetails details = kycService.getOrCreateKyc(userId);
        return ResponseEntity.ok(details);
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return getMe();
    }

    @PostMapping("/pan/verify")
    public ResponseEntity<?> verifyPanStandard(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String pan) {
        Long userId = SecurityUtil.getUserId();
        String panValue = pan != null ? pan : (body != null ? body.get("pan") : null);
        if (panValue == null || panValue.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "PAN number is required"));
        }
        KycDetails details = kycService.verifyPan(userId, panValue, null);
        return ResponseEntity.ok(details);
    }

    @PostMapping("/aadhaar/upload")
    public ResponseEntity<?> uploadAadhaarPackage(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "fileName", required = false) String fileName,
            @RequestParam(value = "fileSize", required = false, defaultValue = "0") long fileSize) throws java.io.IOException {
        Long userId = SecurityUtil.getUserId();
        String effectiveName = file != null ? file.getOriginalFilename() : fileName;
        long effectiveSize = file != null ? file.getSize() : fileSize;

        if (effectiveName == null || effectiveName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Aadhaar e-KYC package file is required"));
        }
        byte[] content = file != null ? file.getBytes() : new byte[0];
        KycDetails details = kycService.verifyAadhaarByUserId(userId, effectiveName,
                effectiveSize > 0 ? effectiveSize : 1024, content, null);
        return ResponseEntity.ok(details);
    }

    @PostMapping("/face/liveness/start")
    public ResponseEntity<FaceLivenessResult> startLiveness() {
        Long userId = SecurityUtil.getUserId();
        FaceLivenessResult result = kycService.startFaceLiveness(userId, null);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/face/liveness/complete")
    public ResponseEntity<FaceLivenessResult> completeLiveness(@RequestBody Map<String, String> body) {
        Long userId = SecurityUtil.getUserId();
        String sessionId = body.get("sessionId");
        String image = body.get("image");
        String outcome = body.get("outcome");
        FaceLivenessResult result = kycService.completeFaceLiveness(userId, sessionId, image, outcome, null);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/face/match")
    public ResponseEntity<FaceVerificationResult> matchFace(@RequestBody(required = false) Map<String, String> body) {
        Long userId = SecurityUtil.getUserId();
        String liveImage = body != null ? body.get("liveImage") : null;
        FaceVerificationResult result = kycService.verifyFaceMatch(userId, liveImage, null);
        return ResponseEntity.ok(result);
    }

    // Preserved existing endpoints for backwards compatibility
    @PostMapping("/{customerId}")
    public KycDetails submitKyc(@PathVariable Long customerId, @RequestBody KycDetails request) {
        return kycService.submitKyc(customerId, request);
    }

    @PostMapping("/{customerId}/aadhaar/upload")
    public KycDetails uploadAadhaar(@PathVariable Long customerId,
            @RequestParam String fileName, @RequestParam long fileSize) {
        return kycService.verifyAadhaar(customerId, fileName, fileSize);
    }

    @PostMapping("/verify_pan")
    public ResponseEntity<?> verifyPanLegacy(@RequestParam String pan) {
        String response = setuService.verifyPan(pan);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/digilocker")
    public ResponseEntity<?> digilocker() {
        String response = setuService.createDigilockerRequest();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam("id") String requestId,
            @RequestParam boolean success,
            @RequestParam String scope) {
        if (!success) {
            return ResponseEntity.ok("user denied consent");
        }
        if (scope != null && scope.contains("ADHAR")) {
            String aadharData = setuService.fetchAadhar(requestId);
            log.info("Aadhaar data received in callback: {}", aadharData);
        }
        return ResponseEntity.ok("callback received");
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<?> checkStatus(@PathVariable String id) {
        String status = setuService.checkStatus(id);
        return ResponseEntity.ok(status);
    }
}
