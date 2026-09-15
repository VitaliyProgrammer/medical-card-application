package com.vitaliy.medcard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_entries")
@Getter
@Setter
@NoArgsConstructor
public class AuditEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "performed_by_email")
    private String performedByEmail;

    @Column(nullable = false)
    private String action;

    @Column(name = "method_name", nullable = false)
    private String methodName;

    @Column(name = "target_id")
    private Long targetId;

    @Column(nullable = false)
    private String outcome;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
