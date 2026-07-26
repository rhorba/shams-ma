package com.shamsma.api.shared;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {}
