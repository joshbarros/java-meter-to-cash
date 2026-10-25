package com.metertocash.tariff;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TariffRepository extends JpaRepository<TariffEntity, String> {

    boolean existsByCode(String code);
}
