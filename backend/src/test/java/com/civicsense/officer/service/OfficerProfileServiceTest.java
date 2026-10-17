package com.civicsense.officer.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfficerProfileServiceTest {

    @Mock
    private OfficerProfileRepository officerProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private OfficerProfileServiceImpl officerProfileService;

    private User testUser;
    private Role citizenRole;
    private Role officerRole;
    private UUID userId;
    private UUID profileId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        profileId = UUID.randomUUID();

        citizenRole = new Role(UUID.randomUUID(), "CITIZEN", "Citizen role");
        officerRole = new Role(UUID.randomUUID(), "OFFICER", "Officer role");

        testUser = new User();
        testUser.setId(userId);
        testUser.setName("Rajesh Sharma");
        testUser.setMobileNumber("9876543210");
        testUser.setEmail("rajesh.sharma@example.com");
        testUser.setRoles(new HashSet<>(Set.of(citizenRole)));
    }

    @Test
    @DisplayName("Should provision officer profile with PENDING_VERIFICATION and without granting ROLE_OFFICER")
    void testProvisionOfficer_Success() {
        ProvisionOfficerRequest request = new ProvisionOfficerRequest(
                userId, "EMP-MCD-1001", "Municipal Corporation of Delhi (MCD)",
                "Junior Engineer", "ROADS_AND_TRAFFIC", "rajesh.sharma@mcd.gov.in"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(officerProfileRepository.existsByUserId(userId)).thenReturn(false);
        when(officerProfileRepository.existsByMunicipalityAndEmployeeCode("Municipal Corporation of Delhi (MCD)", "EMP-MCD-1001")).thenReturn(false);
        when(officerProfileRepository.save(any(OfficerProfile.class))).thenAnswer(invocation -> {
            OfficerProfile p = invocation.getArgument(0);
            p.setId(profileId);
            return p;
        });

        OfficerProfileResponse response = officerProfileService.provisionOfficer(request);

        assertNotNull(response);
        assertEquals("EMP-MCD-1001", response.employeeCode());
        assertEquals("Municipal Corporation of Delhi (MCD)", response.municipality());
        assertEquals(OfficerVerificationStatus.PENDING_VERIFICATION, response.verificationStatus());
        assertFalse(response.operationallyAuthorized());

        // Verify that ROLE_OFFICER is NOT granted on initial provisioning
        boolean hasOfficerRole = testUser.getRoles().stream().anyMatch(r -> "OFFICER".equalsIgnoreCase(r.getName()));
        assertFalse(hasOfficerRole, "ROLE_OFFICER must not be granted merely because profile was provisioned");
    }

    @Test
    @DisplayName("Should reject provisioning if user already has an officer profile")
    void testProvisionOfficer_DuplicateUser_ThrowsException() {
        ProvisionOfficerRequest request = new ProvisionOfficerRequest(
                userId, "EMP-MCD-1002", "Municipal Corporation of Delhi (MCD)",
                "Junior Engineer", "ROADS_AND_TRAFFIC", null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(officerProfileRepository.existsByUserId(userId)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> officerProfileService.provisionOfficer(request));
        assertTrue(ex.getMessage().contains("already has an associated officer profile"));
        verify(officerProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject duplicate employee code in same municipality")
    void testProvisionOfficer_DuplicateEmployeeCode_ThrowsException() {
        ProvisionOfficerRequest request = new ProvisionOfficerRequest(
                userId, "EMP-MCD-1001", "Municipal Corporation of Delhi (MCD)",
                "Junior Engineer", "ROADS_AND_TRAFFIC", null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(officerProfileRepository.existsByUserId(userId)).thenReturn(false);
        when(officerProfileRepository.existsByMunicipalityAndEmployeeCode("Municipal Corporation of Delhi (MCD)", "EMP-MCD-1001")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> officerProfileService.provisionOfficer(request));
        assertTrue(ex.getMessage().contains("already exists in municipality"));
        verify(officerProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should grant ROLE_OFFICER when verification status transitions to VERIFIED")
    void testUpdateVerificationStatus_Verify_GrantsOfficerRole() {
        OfficerProfile profile = new OfficerProfile(
                testUser, "EMP-MCD-1001", "Municipal Corporation of Delhi (MCD)",
                "Junior Engineer", "ROADS_AND_TRAFFIC", "rajesh.sharma@mcd.gov.in"
        );
        profile.setId(profileId);
        profile.setVerificationStatus(OfficerVerificationStatus.PENDING_VERIFICATION);

        when(officerProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(roleRepository.findByName("OFFICER")).thenReturn(Optional.of(officerRole));
        when(officerProfileRepository.save(any(OfficerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateOfficerVerificationRequest request = new UpdateOfficerVerificationRequest(
                OfficerVerificationStatus.VERIFIED, "Official ID verified"
        );

        OfficerProfileResponse response = officerProfileService.updateVerificationStatus(profileId, request);

        assertEquals(OfficerVerificationStatus.VERIFIED, response.verificationStatus());
        assertTrue(response.operationallyAuthorized());

        boolean hasOfficerRole = testUser.getRoles().stream().anyMatch(r -> "OFFICER".equalsIgnoreCase(r.getName()));
        assertTrue(hasOfficerRole, "ROLE_OFFICER must be added to user upon verification");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should revoke ROLE_OFFICER when verification status transitions to SUSPENDED")
    void testUpdateVerificationStatus_Suspend_RevokesOfficerRole() {
        testUser.getRoles().add(officerRole);

        OfficerProfile profile = new OfficerProfile(
                testUser, "EMP-MCD-1001", "Municipal Corporation of Delhi (MCD)",
                "Junior Engineer", "ROADS_AND_TRAFFIC", "rajesh.sharma@mcd.gov.in"
        );
        profile.setId(profileId);
        profile.setVerificationStatus(OfficerVerificationStatus.VERIFIED);

        when(officerProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(officerProfileRepository.save(any(OfficerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateOfficerVerificationRequest request = new UpdateOfficerVerificationRequest(
                OfficerVerificationStatus.SUSPENDED, "Pending disciplinary review"
        );

        OfficerProfileResponse response = officerProfileService.updateVerificationStatus(profileId, request);

        assertEquals(OfficerVerificationStatus.SUSPENDED, response.verificationStatus());
        assertFalse(response.operationallyAuthorized());

        boolean hasOfficerRole = testUser.getRoles().stream().anyMatch(r -> "OFFICER".equalsIgnoreCase(r.getName()));
        assertFalse(hasOfficerRole, "ROLE_OFFICER must be revoked from user upon suspension");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should restore ROLE_OFFICER when suspended officer is reinstated to VERIFIED")
    void testUpdateVerificationStatus_Reinstatement_RestoresOfficerRole() {
        OfficerProfile profile = new OfficerProfile(
                testUser, "EMP-MCD-1001", "Municipal Corporation of Delhi (MCD)",
                "Junior Engineer", "ROADS_AND_TRAFFIC", "rajesh.sharma@mcd.gov.in"
        );
        profile.setId(profileId);
        profile.setVerificationStatus(OfficerVerificationStatus.SUSPENDED);

        when(officerProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(roleRepository.findByName("OFFICER")).thenReturn(Optional.of(officerRole));
        when(officerProfileRepository.save(any(OfficerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateOfficerVerificationRequest request = new UpdateOfficerVerificationRequest(
                OfficerVerificationStatus.VERIFIED, "Reinstated following inquiry"
        );

        OfficerProfileResponse response = officerProfileService.updateVerificationStatus(profileId, request);

        assertEquals(OfficerVerificationStatus.VERIFIED, response.verificationStatus());
        assertTrue(response.operationallyAuthorized());

        boolean hasOfficerRole = testUser.getRoles().stream().anyMatch(r -> "OFFICER".equalsIgnoreCase(r.getName()));
        assertTrue(hasOfficerRole, "ROLE_OFFICER must be restored upon reinstatement");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should reject invalid lifecycle transitions")
    void testUpdateVerificationStatus_InvalidTransition_ThrowsException() {
        OfficerProfile profile = new OfficerProfile(
                testUser, "EMP-MCD-1001", "Municipal Corporation of Delhi (MCD)",
                "Junior Engineer", "ROADS_AND_TRAFFIC", null
        );
        profile.setId(profileId);
        profile.setVerificationStatus(OfficerVerificationStatus.VERIFIED);

        when(officerProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));

        // Invalid: VERIFIED cannot transition directly back to PENDING_VERIFICATION
        UpdateOfficerVerificationRequest request = new UpdateOfficerVerificationRequest(
                OfficerVerificationStatus.PENDING_VERIFICATION, "Invalid rollback"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                officerProfileService.updateVerificationStatus(profileId, request));
        assertTrue(ex.getMessage().contains("Invalid officer verification status transition"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Operational authorization helper returns true only for active and VERIFIED officers")
    void testOperationalAuthorizationHelper() {
        OfficerProfile profile = new OfficerProfile(
                testUser, "EMP-01", "Municipal Corporation of Delhi (MCD)",
                "Inspector", "SANITATION", null
        );

        // PENDING_VERIFICATION -> false
        profile.setVerificationStatus(OfficerVerificationStatus.PENDING_VERIFICATION);
        profile.setActive(true);
        assertFalse(profile.isOperationallyAuthorized());

        // VERIFIED + active -> true
        profile.setVerificationStatus(OfficerVerificationStatus.VERIFIED);
        assertTrue(profile.isOperationallyAuthorized());

        // VERIFIED + inactive -> false
        profile.setActive(false);
        assertFalse(profile.isOperationallyAuthorized());

        // SUSPENDED -> false
        profile.setActive(true);
        profile.setVerificationStatus(OfficerVerificationStatus.SUSPENDED);
        assertFalse(profile.isOperationallyAuthorized());

        // REJECTED -> false
        profile.setVerificationStatus(OfficerVerificationStatus.REJECTED);
        assertFalse(profile.isOperationallyAuthorized());
    }
}

