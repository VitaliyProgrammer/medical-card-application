package com.vitaliy.medcard.service.impl;

import com.vitaliy.medcard.aspect.Audited;
import com.vitaliy.medcard.dto.ShareLinkResponseDto;
import com.vitaliy.medcard.exception.PatientProfileNotFoundException;
import com.vitaliy.medcard.exception.ShareLinkExpiredException;
import com.vitaliy.medcard.exception.ShareLinkNotFoundException;
import com.vitaliy.medcard.mapper.ShareLinkMapper;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.ShareLink;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.repository.ShareLinkRepository;
import com.vitaliy.medcard.service.PatientCardExportService;
import com.vitaliy.medcard.service.ShareLinkService;
import com.vitaliy.medcard.validation.PatientAccessValidator;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ShareLinkServiceImpl implements ShareLinkService {

    private static final long EXPIRATION_HOURS = 48;

    private final ShareLinkRepository shareLinkRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final PatientCardExportService patientCardExportService;
    private final ShareLinkMapper shareLinkMapper;
    private final PatientAccessValidator patientAccessValidator;

    @Override
    @Audited(action = "CREATE_SHARE_LINK")
    public ShareLinkResponseDto createShareLink(User currentDoctor, Long patientId) {
        patientAccessValidator.validateDoctorAccess(currentDoctor, patientId);

        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new PatientProfileNotFoundException(
                        "Patient profile not found: " + patientId + "!"));

        ShareLink shareLink = new ShareLink();
        shareLink.setPatient(patient);
        shareLink.setCreatedBy(currentDoctor);
        shareLink.setToken(UUID.randomUUID().toString());
        shareLink.setCreatedAt(LocalDateTime.now());
        shareLink.setExpiresAt(LocalDateTime.now().plusHours(EXPIRATION_HOURS));

        return shareLinkMapper.toDto(shareLinkRepository.save(shareLink));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportSharedCard(String token) {
        ShareLink shareLink = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new ShareLinkNotFoundException(
                        "Share link not found: " + token + "!"));

        if (shareLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ShareLinkExpiredException("This share link has expired!");
        }

        return patientCardExportService.exportCardById(shareLink.getPatient().getId());
    }
}
