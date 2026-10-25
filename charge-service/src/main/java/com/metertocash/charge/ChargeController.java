package com.metertocash.charge;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ChargeController {

    private final ChargeRepository charges;

    public ChargeController(ChargeRepository charges) {
        this.charges = charges;
    }

    @GetMapping("/charges/{billId}")
    public ChargeEntity charge(@PathVariable String billId) {
        return charges.findById(billId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "charge not found"));
    }
}
