package com.tikzy.auth.service;

import com.tikzy.auth.dto.request.UpdateSecurityPolicyRequest;
import com.tikzy.auth.dto.response.SecurityPolicyResponse;
import com.tikzy.auth.entity.SecurityPolicy;
import com.tikzy.auth.repository.SecurityPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecurityPolicyService {

    private final SecurityPolicyRepository securityPolicyRepository;

    @Value("${security.login.max-failed-attempts:5}")
    private int fallbackMaxFailedLoginAttempts;

    @Transactional(readOnly = true)
    public int getMaxFailedLoginAttempts() {
        return securityPolicyRepository.findFirstByOrderByCreatedAtAsc()
                .map(SecurityPolicy::getMaxFailedLoginAttempts)
                .filter(value -> value != null && value > 0)
                .orElseGet(this::defaultMaxFailedLoginAttempts);
    }

    @Transactional(readOnly = true)
    public SecurityPolicyResponse getPolicy() {
        SecurityPolicy policy = securityPolicyRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(this::newDefaultPolicy);
        return toResponse(policy);
    }

    @Transactional
    public SecurityPolicyResponse updatePolicy(UpdateSecurityPolicyRequest request) {
        SecurityPolicy policy = securityPolicyRepository.findFirstForUpdate()
                .orElseGet(this::newDefaultPolicy);
        policy.setMaxFailedLoginAttempts(request.getMaxFailedLoginAttempts());
        return toResponse(securityPolicyRepository.save(policy));
    }

    private SecurityPolicy newDefaultPolicy() {
        return SecurityPolicy.builder()
                .maxFailedLoginAttempts(defaultMaxFailedLoginAttempts())
                .build();
    }

    private int defaultMaxFailedLoginAttempts() {
        return Math.max(1, fallbackMaxFailedLoginAttempts);
    }

    private SecurityPolicyResponse toResponse(SecurityPolicy policy) {
        return new SecurityPolicyResponse(policy.getMaxFailedLoginAttempts(), policy.getUpdatedAt());
    }
}
