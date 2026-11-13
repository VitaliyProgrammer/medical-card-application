package com.vitaliy.medcard.service.impl;

import com.vitaliy.medcard.aspect.Audited;
import com.vitaliy.medcard.dto.CareLinkRequestDto;
import com.vitaliy.medcard.dto.CareLinkResponseDto;
import com.vitaliy.medcard.dto.PatientSummaryDto;
import com.vitaliy.medcard.exception.CareLinkAlreadyExistsException;
import com.vitaliy.medcard.exception.CareLinkNotFoundException;
import com.vitaliy.medcard.exception.PatientProfileNotFoundException;
import com.vitaliy.medcard.exception.UserNotFoundException;
import com.vitaliy.medcard.mapper.CareLinkMapper;
import com.vitaliy.medcard.model.Allergy;
import com.vitaliy.medcard.model.CareLink;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.UserRole;
import com.vitaliy.medcard.repository.AllergyRepository;
import com.vitaliy.medcard.repository.CareLinkRepository;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.repository.UserRepository;
import com.vitaliy.medcard.repository.specification.CareLinkSpecifications;
import com.vitaliy.medcard.service.CareLinkService;
import com.vitaliy.medcard.util.AgeCalculator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CareLinkServiceImpl implements CareLinkService {

    private final CareLinkRepository careLinkRepository;

    private final PatientProfileRepository patientProfileRepository;

    private final UserRepository userRepository;

    private final AllergyRepository allergyRepository;

    private final CareLinkMapper careLinkMapper;

    @Override
    @Audited(action = "GRANT_DOCTOR_ACCESS")
    public CareLinkResponseDto grantDoctorAccess(
            User currentPatient, CareLinkRequestDto request) {
        PatientProfile patient = findPatientByUserId(currentPatient.getId());

        User doctor = userRepository.findByEmail(request.doctorEmail())
                .filter(user -> user.getRole() == UserRole.DOCTOR)
                .orElseThrow(() -> new UserNotFoundException(
                        "Doctor not found with email: " + request.doctorEmail() + "!"));

        if (careLinkRepository.existsByDoctorIdAndPatientId(doctor.getId(), patient.getId())) {
            throw new CareLinkAlreadyExistsException(
                    "This doctor already has access to your card!");
        }

        CareLink careLink = new CareLink();
        careLink.setDoctor(doctor);
        careLink.setPatient(patient);
        careLink.setAssignedAt(LocalDateTime.now());

        return careLinkMapper.toDto(careLinkRepository.save(careLink));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CareLinkResponseDto> getMyDoctors(User currentPatient) {
        PatientProfile patient = findPatientByUserId(currentPatient.getId());

        return careLinkRepository.findByPatientId(patient.getId()).stream()
                .map(careLinkMapper::toDto)
                .toList();
    }

    @Override
    @Audited(action = "REVOKE_DOCTOR_ACCESS")
    public void revokeDoctorAccess(User currentPatient, Long careLinkId) {
        PatientProfile patient = findPatientByUserId(currentPatient.getId());

        CareLink careLink = careLinkRepository.findByIdAndPatientId(careLinkId, patient.getId())
                .orElseThrow(() -> new CareLinkNotFoundException(
                        "Care link not found: " + careLinkId + "!"));

        careLinkRepository.delete(careLink);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientSummaryDto> searchMyPatients(
            User currentDoctor, String search, Pageable pageable) {

        Specification<CareLink> specification = Specification
                .where(CareLinkSpecifications.hasDoctorId(currentDoctor.getId()))
                .and(CareLinkSpecifications.patientFullNameContains(search));

        Page<CareLink> careLinkPage = careLinkRepository.findAll(specification, pageable);

        List<Long> patientIds = careLinkPage.getContent().stream()
                .map(link -> link.getPatient().getId())
                .toList();

        Map<Long, List<String>> allergiesByPatientId = allergyRepository
                .findByPatientIdIn(patientIds).stream()
                .collect(Collectors.groupingBy(
                        allergy -> allergy.getPatient().getId(),
                        Collectors.mapping(Allergy::getName, Collectors.toList())));

        return careLinkPage.map(link -> {
            PatientProfile patient = link.getPatient();
            return new PatientSummaryDto(
                    patient.getId(),
                    patient.getUser().getFullName(),
                    AgeCalculator.calculateAge(patient.getDateOfBirth()),
                    allergiesByPatientId.getOrDefault(patient.getId(), List.of()));
        });
    }

    private PatientProfile findPatientByUserId(Long userId) {
        return patientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new PatientProfileNotFoundException(
                        "Patient profile not found for user: " + userId + "!"));
    }
}
