package com.vitaliy.medcard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.vitaliy.medcard.dto.ShareLinkResponseDto;
import com.vitaliy.medcard.exception.ForbiddenActionException;
import com.vitaliy.medcard.exception.ShareLinkExpiredException;
import com.vitaliy.medcard.exception.ShareLinkNotFoundException;
import com.vitaliy.medcard.mapper.ShareLinkMapper;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.ShareLink;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.repository.ShareLinkRepository;
import com.vitaliy.medcard.service.PatientCardExportService;
import com.vitaliy.medcard.validation.PatientAccessValidator;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private ShareLinkMapper shareLinkMapper;

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
    @DisplayName("createShareLink: generates a token that expires 48 hours from now")
    void createShareLink_success() {
        ShareLinkResponseDto responseDto =
                new ShareLinkResponseDto(1L, "some-token", LocalDateTime.now().plusHours(48));

        doNothing().when(patientAccessValidator).validateDoctorAccess(doctor, patient.getId());
        when(patientProfileRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(shareLinkRepository.save(any(ShareLink.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(shareLinkMapper.toDto(any(ShareLink.class))).thenReturn(responseDto);

        ShareLinkResponseDto result = shareLinkService.createShareLink(doctor, patient.getId());

        assertThat(result).isEqualTo(responseDto);
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
    @DisplayName("exportSharedCard: returns the PDF for a still-valid token")
    void exportSharedCard_success() {
        ShareLink shareLink = new ShareLink();
        shareLink.setPatient(patient);
        shareLink.setToken("valid-token");
        shareLink.setExpiresAt(LocalDateTime.now().plusHours(1));

        byte[] pdfBytes = {1, 2, 3};

        when(shareLinkRepository.findByToken("valid-token")).thenReturn(Optional.of(shareLink));
        when(patientCardExportService.exportCardById(patient.getId())).thenReturn(pdfBytes);

        byte[] result = shareLinkService.exportSharedCard("valid-token");

        assertThat(result).isEqualTo(pdfBytes);
    }

    @Test
    @DisplayName("exportSharedCard: rejects an expired token")
    void exportSharedCard_expired() {
        ShareLink shareLink = new ShareLink();
        shareLink.setPatient(patient);
        shareLink.setToken("expired-token");
        shareLink.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(shareLinkRepository.findByToken("expired-token")).thenReturn(Optional.of(shareLink));

        assertThrows(ShareLinkExpiredException.class,
                () -> shareLinkService.exportSharedCard("expired-token"));
    }

    @Test
    @DisplayName("exportSharedCard: throws when the token doesn't exist")
    void exportSharedCard_notFound() {
        when(shareLinkRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

        assertThrows(ShareLinkNotFoundException.class,
                () -> shareLinkService.exportSharedCard("unknown-token"));
    }
}
