package com.vitaliy.medcard.service;

import com.vitaliy.medcard.dto.ShareLinkResponseDto;
import com.vitaliy.medcard.model.User;

public interface ShareLinkService {

    ShareLinkResponseDto createShareLink(User currentDoctor, Long patientId);

    void revokeShareLink(User currentDoctor, Long shareLinkId);

    byte[] exportSharedCard(String token);
}
