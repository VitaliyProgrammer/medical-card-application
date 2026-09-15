package com.vitaliy.medcard.aspect;

import com.vitaliy.medcard.model.AuditEntry;
import com.vitaliy.medcard.repository.AuditEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class AuditEntryWriter {

    private final AuditEntryRepository auditEntryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AuditEntry entry) {
        auditEntryRepository.save(entry);
    }
}
