package com.vitaliy.medcard.repository;

import com.vitaliy.medcard.model.ShareLink;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    Optional<ShareLink> findByTokenHash(String tokenHash);

    Optional<ShareLink> findByIdAndCreatedById(Long id, Long createdById);
}
