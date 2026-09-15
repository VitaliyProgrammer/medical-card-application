package com.vitaliy.medcard.mapper;

import com.vitaliy.medcard.dto.AuditEntryResponseDto;
import com.vitaliy.medcard.model.AuditEntry;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditEntryMapper {

    AuditEntryResponseDto toDto(AuditEntry auditEntry);
}
