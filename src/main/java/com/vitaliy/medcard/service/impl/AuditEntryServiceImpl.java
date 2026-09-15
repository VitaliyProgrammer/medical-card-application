package com.vitaliy.medcard.service.impl;

import com.vitaliy.medcard.dto.AuditEntryResponseDto;
import com.vitaliy.medcard.mapper.AuditEntryMapper;
import com.vitaliy.medcard.repository.AuditEntryRepository;
import com.vitaliy.medcard.service.AuditEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditEntryServiceImpl implements AuditEntryService {

    private final AuditEntryRepository auditEntryRepository;

    private final AuditEntryMapper auditEntryMapper;

    @Override
    public Page<AuditEntryResponseDto> findAll(Pageable pageable) {
        return auditEntryRepository.findAll(pageable).map(auditEntryMapper::toDto);
    }
}
