package com.metertocash.audit;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditController {

    private final AuditRepository entries;

    public AuditController(AuditRepository entries) {
        this.entries = entries;
    }

    @GetMapping("/audits/{billId}")
    public List<AuditEntry> audits(@PathVariable String billId) {
        return entries.findByBillIdOrderByRecordedAtAsc(billId);
    }
}
