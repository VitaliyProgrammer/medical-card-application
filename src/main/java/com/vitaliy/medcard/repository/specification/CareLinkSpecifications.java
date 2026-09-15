package com.vitaliy.medcard.repository.specification;

import com.vitaliy.medcard.model.CareLink;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public final class CareLinkSpecifications {

    private CareLinkSpecifications() {
    }

    public static Specification<CareLink> hasDoctorId(Long doctorId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("doctor").get("id"), doctorId);
    }

    public static Specification<CareLink> patientFullNameContains(String search) {

        return (root, query, criteriaBuilder) -> {
            if (search == null || search.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            Join<CareLink, PatientProfile> patientJoin = root.join("patient");
            Join<PatientProfile, User> userJoin = patientJoin.join("user");

            return criteriaBuilder.like(
                    criteriaBuilder.lower(userJoin.get("fullName")),
                    "%" + search.toLowerCase() + "%");
        };
    }
}
