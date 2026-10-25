package com.metertocash.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditEntry, String> {

    java.util.List<AuditEntry> findByBillIdOrderByRecordedAtAsc(String billId);
}
