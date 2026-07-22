package com.shamsma.api.installer;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InstallerRepository extends JpaRepository<Installer, UUID> {}
