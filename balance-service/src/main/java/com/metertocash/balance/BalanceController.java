package com.metertocash.balance;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class BalanceController {

    private final BalanceRepository balances;

    public BalanceController(BalanceRepository balances) {
        this.balances = balances;
    }

    @GetMapping("/balances/{billId}")
    public BalanceEntity balance(@PathVariable String billId) {
        return balances.findById(billId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "balance not found"));
    }
}
