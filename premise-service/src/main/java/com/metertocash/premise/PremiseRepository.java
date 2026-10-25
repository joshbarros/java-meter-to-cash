package com.metertocash.premise;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PremiseRepository extends JpaRepository<PremiseEntity, String> {

    boolean existsByPremiseCode(String premiseCode);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "UPDATE premises SET service_state = 'OFF' WHERE id = :id AND service_state = 'ON'", nativeQuery = true)
    int markOff(@org.springframework.data.repository.query.Param("id") String id);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "UPDATE premises SET service_state = 'ON' WHERE id = :id AND service_state = 'OFF'", nativeQuery = true)
    int markOn(@org.springframework.data.repository.query.Param("id") String id);
}
