package com.vitaliy.medcard.repository;

import com.vitaliy.medcard.model.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {
}
