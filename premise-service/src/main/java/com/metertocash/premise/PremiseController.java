package com.metertocash.premise;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metertocash.common.Channels;
import com.metertocash.common.PremiseOpened;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PremiseController {

    private final PremiseRepository premises;
    private final AccountViewRepository accounts;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public PremiseController(PremiseRepository premises, AccountViewRepository accounts, OutboxRepository outbox,
            ObjectMapper mapper) {
        this.premises = premises;
        this.accounts = accounts;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @PostMapping("/premises")
    @Transactional
    public ResponseEntity<PremiseEntity> create(@RequestBody CreatePremise request) {
        if (request == null || blank(request.premiseCode()) || blank(request.accountId()) || blank(request.address())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "premise fields are required");
        }
        if (!accounts.existsById(request.accountId().trim())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "account not found");
        }
        String code = request.premiseCode().trim();
        if (premises.existsByPremiseCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "premise exists");
        }
        PremiseEntity row = new PremiseEntity();
        row.setId(UUID.randomUUID().toString());
        row.setAccountId(request.accountId().trim());
        row.setPremiseCode(code);
        row.setAddress(request.address().trim());
        row.setServiceState("ON");
        premises.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.PREMISES, row.getId(),
                new PremiseOpened(row.getId(), row.getAccountId(), row.getPremiseCode(), row.getAddress()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/premises/{id}")
    public PremiseEntity premise(@PathVariable String id) {
        return premises.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "premise not found"));
    }

    @GetMapping("/directory/accounts/{id}")
    public AccountView account(@PathVariable String id) {
        return accounts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "account not found"));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record CreatePremise(String premiseCode, String accountId, String address) {
    }
}
