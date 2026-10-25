package com.metertocash.account;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metertocash.common.AccountOpened;
import com.metertocash.common.Channels;
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
public class AccountController {

    private final AccountRepository accounts;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public AccountController(AccountRepository accounts, OutboxRepository outbox, ObjectMapper mapper) {
        this.accounts = accounts;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @PostMapping("/accounts")
    @Transactional
    public ResponseEntity<AccountEntity> create(@RequestBody CreateAccount request) {
        if (request == null || blank(request.accountNumber()) || blank(request.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "account fields are required");
        }
        String number = request.accountNumber().trim();
        if (accounts.existsByAccountNumber(number)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "account exists");
        }
        AccountEntity row = new AccountEntity();
        row.setId(UUID.randomUUID().toString());
        row.setAccountNumber(number);
        row.setName(request.name().trim());
        accounts.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.ACCOUNTS, row.getId(),
                new AccountOpened(row.getId(), row.getAccountNumber(), row.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/accounts/{id}")
    public AccountEntity account(@PathVariable String id) {
        return accounts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "account not found"));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record CreateAccount(String accountNumber, String name) {
    }
}
