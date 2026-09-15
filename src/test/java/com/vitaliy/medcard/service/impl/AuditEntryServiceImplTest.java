package com.vitaliy.medcard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vitaliy.medcard.dto.AuditEntryResponseDto;
import com.vitaliy.medcard.mapper.AuditEntryMapper;
import com.vitaliy.medcard.model.AuditEntry;
import com.vitaliy.medcard.repository.AuditEntryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AuditEntryServiceImplTest {

    @Mock
    private AuditEntryRepository auditEntryRepository;

    @Mock
    private AuditEntryMapper auditEntryMapper;

    @InjectMocks
    private AuditEntryServiceImpl auditEntryService;

    @Test
    @DisplayName("findAll: returns a mapped, paginated list of audit entries")
    void findAll_success() {
        AuditEntry entry = new AuditEntry();
        entry.setId(1L);
        entry.setAction("ADD_ALLERGY");
        entry.setOutcome("SUCCESS");
        entry.setTimestamp(LocalDateTime.now());

        AuditEntryResponseDto responseDto = new AuditEntryResponseDto(
                1L, "patient@test.com", "ADD_ALLERGY", "AllergyServiceImpl.addAllergy(..)",
                null, "SUCCESS", entry.getTimestamp());

        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditEntry> entryPage = new PageImpl<>(List.of(entry), pageable, 1);

        when(auditEntryRepository.findAll(pageable)).thenReturn(entryPage);
        when(auditEntryMapper.toDto(entry)).thenReturn(responseDto);

        Page<AuditEntryResponseDto> result = auditEntryService.findAll(pageable);

        assertThat(result.getContent()).containsExactly(responseDto);
    }
}
