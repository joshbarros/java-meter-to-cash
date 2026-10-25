package com.metertocash.field;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class FieldController {

    private final CutRepository cuts;

    public FieldController(CutRepository cuts) {
        this.cuts = cuts;
    }

    @GetMapping("/cuts/{billId}")
    public CutEntity cut(@PathVariable String billId) {
        return cuts.findById(billId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "cut not found"));
    }
}
