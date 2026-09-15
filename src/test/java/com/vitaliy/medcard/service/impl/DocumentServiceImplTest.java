package com.vitaliy.medcard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vitaliy.medcard.dto.DocumentFileDto;
import com.vitaliy.medcard.dto.DocumentResponseDto;
import com.vitaliy.medcard.exception.DocumentNotFoundException;
import com.vitaliy.medcard.exception.ForbiddenActionException;
import com.vitaliy.medcard.exception.UnsupportedFileTypeException;
import com.vitaliy.medcard.mapper.DocumentMapper;
import com.vitaliy.medcard.model.Document;
import com.vitaliy.medcard.model.status.DocumentType;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.repository.DocumentRepository;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.service.FileStorageService;
import com.vitaliy.medcard.validation.PatientAccessValidator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private PatientAccessValidator patientAccessValidator;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private User patientUser;

    private PatientProfile patient;

    private Document document;

    @BeforeEach
    void setUp() {
        patientUser = new User();
        patientUser.setId(1L);

        patient = new PatientProfile();
        patient.setId(10L);
        patient.setUser(patientUser);

        document = new Document();
        document.setId(100L);
        document.setPatient(patient);
        document.setOriginalFileName("blood-test.pdf");
        document.setStoredFileName("uuid_blood-test.pdf");
        document.setContentType("application/pdf");
    }

    @Test
    @DisplayName("uploadMyDocument: stores the file and saves its metadata")
    void uploadMyDocument_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "blood-test.pdf", "application/pdf", "content".getBytes());
        DocumentResponseDto responseDto = new DocumentResponseDto(
                100L, DocumentType.LAB_RESULT, "blood-test.pdf", "application/pdf", 7L,
                "Jane Patient", null);

        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.of(patient));
        when(fileStorageService.store(file)).thenReturn("uuid_blood-test.pdf");
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDto(document)).thenReturn(responseDto);

        DocumentResponseDto result =
                documentService.uploadMyDocument(patientUser, file, DocumentType.LAB_RESULT);

        assertThat(result).isEqualTo(responseDto);
        verify(fileStorageService).store(file);
    }

    @Test
    @DisplayName("uploadMyDocument: rejects an unsupported content type")
    void uploadMyDocument_unsupportedType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/x-msdownload", "content".getBytes());

        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.of(patient));

        assertThrows(UnsupportedFileTypeException.class,
                () -> documentService.uploadMyDocument(patientUser, file, DocumentType.OTHER));

        verify(fileStorageService, never()).store(any());
    }

    @Test
    @DisplayName("uploadMyDocument: rejects an empty file")
    void uploadMyDocument_emptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.of(patient));

        assertThrows(UnsupportedFileTypeException.class,
                () -> documentService.uploadMyDocument(patientUser, emptyFile, DocumentType.OTHER));
    }

    @Test
    @DisplayName("deleteMyDocument: deletes the stored file and the database row")
    void deleteMyDocument_success() {
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        documentService.deleteMyDocument(patientUser, document.getId());

        verify(fileStorageService).delete(document.getStoredFileName());
        verify(documentRepository).delete(document);
    }

    @Test
    @DisplayName("deleteMyDocument: rejects deleting someone else's document")
    void deleteMyDocument_notOwner() {
        User anotherPatient = new User();
        anotherPatient.setId(2L);

        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        assertThrows(ForbiddenActionException.class,
                () -> documentService.deleteMyDocument(anotherPatient, document.getId()));

        verify(fileStorageService, never()).delete(any());
    }

    @Test
    @DisplayName("deleteMyDocument: throws when the document doesn't exist")
    void deleteMyDocument_notFound() {
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class,
                () -> documentService.deleteMyDocument(patientUser, 999L));
    }

    @Test
    @DisplayName("downloadMyDocument: returns the file resource for the owner")
    void downloadMyDocument_success() {
        Resource resource = org.mockito.Mockito.mock(Resource.class);

        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(fileStorageService.load(document.getStoredFileName())).thenReturn(resource);

        DocumentFileDto result = documentService.downloadMyDocument(patientUser, document.getId());

        assertThat(result.resource()).isEqualTo(resource);
        assertThat(result.fileName()).isEqualTo("blood-test.pdf");
    }

    @Test
    @DisplayName("getDocumentsForDoctor: returns the list once access is validated")
    void getDocumentsForDoctor_success() {
        User doctor = new User();
        doctor.setId(5L);

        DocumentResponseDto responseDto = new DocumentResponseDto(
                100L, DocumentType.LAB_RESULT, "blood-test.pdf", "application/pdf", 7L,
                "Jane Patient", null);

        doNothing().when(patientAccessValidator).validateDoctorAccess(doctor, patient.getId());
        when(documentRepository.findByPatientId(patient.getId())).thenReturn(List.of(document));
        when(documentMapper.toDto(document)).thenReturn(responseDto);

        List<DocumentResponseDto> result =
                documentService.getDocumentsForDoctor(doctor, patient.getId(), null);

        assertThat(result).containsExactly(responseDto);
    }
}
