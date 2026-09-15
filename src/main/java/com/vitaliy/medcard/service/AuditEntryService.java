package com.vitaliy.medcard.service;

import com.vitaliy.medcard.dto.AuditEntryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditEntryService {

    Page<AuditEntryResponseDto> findAll(Pageable pageable);
}
