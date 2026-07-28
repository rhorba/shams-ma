package com.shamsma.api.booking;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface QuoteRequestRepository extends JpaRepository<QuoteRequest, UUID> {

  List<QuoteRequest> findByInstallerIdOrderByCreatedAtDesc(UUID installerId);

  List<QuoteRequest> findByHomeownerIdOrderByCreatedAtDesc(UUID homeownerId);
}
