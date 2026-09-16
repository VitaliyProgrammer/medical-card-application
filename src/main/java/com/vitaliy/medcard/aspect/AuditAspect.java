package com.vitaliy.medcard.aspect;

import com.vitaliy.medcard.model.AuditEntry;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private static final String OUTCOME_SUCCESS = "SUCCESS";
    private static final String OUTCOME_FAILURE = "FAILURE";
    private static final String METRIC_NAME = "audit_entries_total";

    private final AuditEntryWriter auditEntryWriter;
    private final MeterRegistry meterRegistry;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        String outcome = OUTCOME_SUCCESS;
        try {
            return joinPoint.proceed();
        } catch (Throwable exception) {
            outcome = OUTCOME_FAILURE;
            throw exception;
        } finally {
            recordEntry(joinPoint, audited, outcome);
            meterRegistry.counter(METRIC_NAME, "action", audited.action(), "outcome", outcome)
                    .increment();
        }
    }

    private void recordEntry(ProceedingJoinPoint joinPoint, Audited audited, String outcome) {
        try {
            AuditEntry entry = new AuditEntry();
            entry.setPerformedByEmail(resolveCurrentUserEmail());
            entry.setAction(audited.action());
            entry.setMethodName(joinPoint.getSignature().toShortString());
            entry.setTargetId(resolveTargetId(joinPoint));
            entry.setOutcome(outcome);
            entry.setTimestamp(LocalDateTime.now());

            auditEntryWriter.write(entry);
        } catch (RuntimeException exception) {
            log.error("Failed to write an audit entry for action {}", audited.action(), exception);
        }
    }

    private String resolveCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private Long resolveTargetId(ProceedingJoinPoint joinPoint) {
        for (Object argument : joinPoint.getArgs()) {
            if (argument instanceof Long targetId) {
                return targetId;
            }
        }
        return null;
    }
}
