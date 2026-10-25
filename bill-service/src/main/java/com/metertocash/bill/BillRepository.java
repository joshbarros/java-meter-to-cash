package com.metertocash.bill;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BillRepository extends JpaRepository<BillEntity, String> {

    java.util.Optional<BillEntity> findByRequestId(String requestId);
}
