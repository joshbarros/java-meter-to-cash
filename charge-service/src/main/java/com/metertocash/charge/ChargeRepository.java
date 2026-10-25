package com.metertocash.charge;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargeRepository extends JpaRepository<ChargeEntity, String> {
}
