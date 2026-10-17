package com.civicsense.officer.service;

import com.civicsense.exception.ResourceNotFoundException;
import com.civicsense.officer.dto.OfficerProfileResponse;
import com.civicsense.officer.dto.ProvisionOfficerRequest;
import com.civicsense.officer.dto.UpdateOfficerVerificationRequest;
import com.civicsense.officer.entity.OfficerProfile;
import com.civicsense.officer.entity.OfficerVerificationStatus;
import com.civicsense.officer.repository.OfficerProfileRepository;
import com.civicsense.role.entity.Role;
import com.civicsense.role.repository.RoleRepository;
import com.civicsense.user.entity.User;
import com.civicsense.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service implementation for managing the lifecycle and identity of municipal officers.
 *
 * <p>Enforces:
 * <ul>
 *   <li>One-to-one mapping between User and OfficerProfile</li>
 *   <li>Scoped employee code uniqueness per municipality</li>
 *   <li>Verification lifecycle transition rules</li>
 *   <li>Separation of profile provisioning from role granting (ROLE_OFFICER is only granted upon VERIFIED state)</li>
 *   <li>Immediate revocation of ROLE_OFFICER upon SUSPENDED or REJECTED states</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfficerProfileServiceImpl implements OfficerProfileService {

    private static final String DEFAULT_MUNICIPALITY = "Municipal Corporation of Delhi (MCD)";
    private static final String OFFICER_ROLE_NAME = "OFFICER";

    private final OfficerProfileRepository officerProfileRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public OfficerProfileResponse provisionOfficer(ProvisionOfficerRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.userId()));

        if (officerProfileRepository.existsByUserId(user.getId())) {
            throw new IllegalStateException("User " + user.getId() + " already has an associated officer profile");
        }

        String municipality = (request.municipality() != null && !request.municipality().isBlank())
                ? request.municipality().trim()
                : DEFAULT_MUNICIPALITY;
        String employeeCode = request.employeeCode().trim();

        if (officerProfileRepository.existsByMunicipalityAndEmployeeCode(municipality, employeeCode)) {
            throw new IllegalArgumentException(
                    "Officer with employee code '" + employeeCode + "' already exists in municipality '" + municipality + "'"
            );
        }

        OfficerProfile profile = new OfficerProfile(
                user,
                employeeCode,
                municipality,
                request.designation().trim(),
                request.department().trim(),
                request.governmentEmail() != null ? request.governmentEmail().trim() : null
        );

        OfficerProfile saved = officerProfileRepository.save(profile);
        log.info("Provisioned officer profile ID {} for user {} [Code: {}, Municipality: {}, Status: {}]",
                saved.getId(), user.getId(), saved.getEmployeeCode(), saved.getMunicipality(), saved.getVerificationStatus());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public OfficerProfileResponse updateVerificationStatus(UUID officerProfileId, UpdateOfficerVerificationRequest request) {
        OfficerProfile profile = officerProfileRepository.findById(officerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Officer profile not found with ID: " + officerProfileId));

        OfficerVerificationStatus current = profile.getVerificationStatus();
        OfficerVerificationStatus target = request.targetStatus();

        if (current == target) {
            return mapToResponse(profile);
        }

        validateTransition(current, target);

        profile.setVerificationStatus(target);

        // Synchronize ROLE_OFFICER with User based on verification state
        User user = profile.getUser();
        if (target == OfficerVerificationStatus.VERIFIED) {
            Role officerRole = roleRepository.findByName(OFFICER_ROLE_NAME)
                    .orElseGet(() -> roleRepository.save(new Role(null, OFFICER_ROLE_NAME, "Authorized municipal officer")));
            user.getRoles().add(officerRole);
            userRepository.save(user);
            log.info("Granted ROLE_OFFICER to user {} following officer verification", user.getId());
        } else if (target == OfficerVerificationStatus.SUSPENDED || target == OfficerVerificationStatus.REJECTED) {
            boolean removed = user.getRoles().removeIf(role -> OFFICER_ROLE_NAME.equalsIgnoreCase(role.getName()));
            if (removed) {
                userRepository.save(user);
                log.info("Revoked ROLE_OFFICER from user {} following status change to {}", user.getId(), target);
            }
        }

        OfficerProfile saved = officerProfileRepository.save(profile);
        log.info("Updated officer profile {} verification status: {} -> {} (Reason: {})",
                saved.getId(), current, target, request.reason());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OfficerProfileResponse getOfficerProfileById(UUID id) {
        OfficerProfile profile = officerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Officer profile not found with ID: " + id));
        return mapToResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public OfficerProfileResponse getOfficerProfileByUserId(UUID userId) {
        OfficerProfile profile = officerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Officer profile not found for user ID: " + userId));
        return mapToResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfficerProfileResponse> getOfficers(String municipality, OfficerVerificationStatus status) {
        List<OfficerProfile> profiles;

        if (municipality != null && !municipality.isBlank() && status != null) {
            profiles = officerProfileRepository.findByMunicipalityAndVerificationStatus(municipality.trim(), status);
        } else if (municipality != null && !municipality.isBlank()) {
            profiles = officerProfileRepository.findByMunicipality(municipality.trim());
        } else if (status != null) {
            profiles = officerProfileRepository.findByVerificationStatus(status);
        } else {
            profiles = officerProfileRepository.findAll();
        }

        return profiles.stream().map(this::mapToResponse).toList();
    }

    // =========================================================================
    // Lifecycle Validation
    // =========================================================================

    private void validateTransition(OfficerVerificationStatus current, OfficerVerificationStatus target) {
        boolean valid = switch (current) {
            case PENDING_VERIFICATION -> (target == OfficerVerificationStatus.VERIFIED || target == OfficerVerificationStatus.REJECTED);
            case VERIFIED -> (target == OfficerVerificationStatus.SUSPENDED);
            case SUSPENDED -> (target == OfficerVerificationStatus.VERIFIED || target == OfficerVerificationStatus.REJECTED);
            case REJECTED -> (target == OfficerVerificationStatus.PENDING_VERIFICATION);
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Invalid officer verification status transition from " + current + " to " + target
            );
        }
    }

    // =========================================================================
    // DTO Mapper
    // =========================================================================

    private OfficerProfileResponse mapToResponse(OfficerProfile profile) {
        User user = profile.getUser();
        return new OfficerProfileResponse(
                profile.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getName() : null,
                user != null ? user.getMobileNumber() : null,
                user != null ? user.getEmail() : null,
                profile.getEmployeeCode(),
                profile.getMunicipality(),
                profile.getDesignation(),
                profile.getDepartment(),
                profile.getGovernmentEmail(),
                profile.getVerificationStatus(),
                profile.isActive(),
                profile.isOperationallyAuthorized(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}

