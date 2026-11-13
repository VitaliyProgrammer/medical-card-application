package com.vitaliy.medcard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vitaliy.medcard.dto.ShareLinkResponseDto;
import com.vitaliy.medcard.exception.ForbiddenActionException;
import com.vitaliy.medcard.exception.ShareLinkExpiredException;
import com.vitaliy.medcard.exception.ShareLinkNotFoundException;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.ShareLink;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.repository.ShareLinkRepository;
import com.vitaliy.medcard.service.PatientCardExportService;
import com.vitaliy.medcard.util.TokenHasher;
import com.vitaliy.medcard.validation.PatientAccessValidator;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShareLinkServiceImplTest {

    @Mock
    private ShareLinkRepository shareLinkRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private PatientCardExportService patientCardExportService;

    @Mock
    private PatientAccessValidator patientAccessValidator;

    @InjectMocks
    private ShareLinkServiceImpl shareLinkService;

    private User doctor;

    private PatientProfile patient;

    @BeforeEach
    void setUp() {
        doctor = new User();
        doctor.setId(1L);

        patient = new PatientProfile();
        patient.setId(10L);
    }

    @Test
    @DisplayName("createShareLink: returns the raw token once and stores only its hash")
    void createShareLink_success() {
        doNothing().when(patientAccessValidator).validateDoctorAccess(doctor, patient.getId());
        when(patientProfileRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(shareLinkRepository.save(any(ShareLink.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShareLinkResponseDto result = shareLinkService.createShareLink(doctor, patient.getId());

        ArgumentCaptor<ShareLink> captor = ArgumentCaptor.forClass(ShareLink.class);
        verify(shareLinkRepository).save(captor.capture());
        ShareLink saved = captor.getValue();

        assertThat(result.token()).isNotBlank();
        assertThat(saved.getTokenHash())
                .isNotEqualTo(result.token())
                .isEqualTo(TokenHasher.sha256Hex(result.token()));
        assertThat(saved.getExpiresAt())
                .isBetween(LocalDateTime.now().plusHours(47), LocalDateTime.now().plusHours(49));
    }

    @Test
    @DisplayName("createShareLink: rejects an unassigned doctor")
    void createShareLink_notAssigned() {
        doThrow(new ForbiddenActionException("You are not assigned to this patient's care!"))
                .when(patientAccessValidator).validateDoctorAccess(doctor, patient.getId());

        assertThrows(ForbiddenActionException.class,
                () -> shareLinkService.createShareLink(doctor, patient.getId()));
    }

    @Test
    @DisplayName("revokeShareLink: marks the doctor's own link as revoked")
    void revokeShareLink_success() {
        ShareLink shareLink = activeLink("token");

        when(shareLinkRepository.findByIdAndCreatedById(5L, doctor.getId()))
                .thenReturn(Optional.of(shareLink));

        shareLinkService.revokeShareLink(doctor, 5L);

        assertThat(shareLink.getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("revokeShareLink: a link created by someone else is not found")
    void revokeShareLink_notOwner() {
        when(shareLinkRepository.findByIdAndCreatedById(5L, doctor.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ShareLinkNotFoundException.class,
                () -> shareLinkService.revokeShareLink(doctor, 5L));
    }

    @Test
    @DisplayName("exportSharedCard: returns the PDF for a still-valid token")
    void exportSharedCard_success() {
        byte[] pdfBytes = {1, 2, 3};

        when(shareLinkRepository.findByTokenHash(TokenHasher.sha256Hex("valid-token")))
                .thenReturn(Optional.of(activeLink("valid-token")));
        when(patientCardExportService.exportCardById(patient.getId())).thenReturn(pdfBytes);

        assertThat(shareLinkService.exportSharedCard("valid-token")).isEqualTo(pdfBytes);
    }

    @Test
    @DisplayName("exportSharedCard: rejects an expired token")
    void exportSharedCard_expired() {
        ShareLink shareLink = activeLink("expired-token");
        shareLink.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(shareLinkRepository.findByTokenHash(TokenHasher.sha256Hex("expired-token")))
                .thenReturn(Optional.of(shareLink));

        assertThrows(ShareLinkExpiredException.class,
                () -> shareLinkService.exportSharedCard("expired-token"));
    }

    @Test
    @DisplayName("exportSharedCard: rejects a revoked token even before it expires")
    void exportSharedCard_revoked() {
        ShareLink shareLink = activeLink("revoked-token");
        shareLink.setRevokedAt(LocalDateTime.now().minusMinutes(5));

        when(shareLinkRepository.findByTokenHash(TokenHasher.sha256Hex("revoked-token")))
                .thenReturn(Optional.of(shareLink));

        assertThrows(ShareLinkExpiredException.class,
                () -> shareLinkService.exportSharedCard("revoked-token"));
    }

    @Test
    @DisplayName("exportSharedCard: an unknown token is not found and isn't echoed back")
    void exportSharedCard_notFound() {
        when(shareLinkRepository.findByTokenHash(TokenHasher.sha256Hex("unknown-token")))
                .thenReturn(Optional.empty());

        ShareLinkNotFoundException exception = assertThrows(ShareLinkNotFoundException.class,
                () -> shareLinkService.exportSharedCard("unknown-token"));

        assertThat(exception.getMessage()).doesNotContain("unknown-token");
    }

    private ShareLink activeLink(String rawToken) {
        ShareLink shareLink = new ShareLink();
        shareLink.setPatient(patient);
        shareLink.setCreatedBy(doctor);
        shareLink.setTokenHash(TokenHasher.sha256Hex(rawToken));
        shareLink.setExpiresAt(LocalDateTime.now().plusHours(1));
        return shareLink;
    }
}
