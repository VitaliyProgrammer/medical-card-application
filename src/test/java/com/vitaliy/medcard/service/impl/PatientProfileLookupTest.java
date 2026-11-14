package com.vitaliy.medcard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.vitaliy.medcard.exception.PatientProfileNotFoundException;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientProfileLookupTest {

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @InjectMocks
    private PatientProfileLookup patientProfileLookup;

    @Test
    @DisplayName("getByUserId: returns the profile linked to the user")
    void getByUserId_found() {
        PatientProfile profile = new PatientProfile();
        when(patientProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        assertThat(patientProfileLookup.getByUserId(1L)).isSameAs(profile);
    }

    @Test
    @DisplayName("getByUserId: throws PatientProfileNotFoundException for a user without one")
    void getByUserId_missing() {
        when(patientProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(PatientProfileNotFoundException.class,
                () -> patientProfileLookup.getByUserId(1L));
    }
}
