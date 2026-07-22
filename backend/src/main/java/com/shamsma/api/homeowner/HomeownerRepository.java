package com.shamsma.api.homeowner;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface HomeownerRepository extends JpaRepository<Homeowner, UUID> {}
