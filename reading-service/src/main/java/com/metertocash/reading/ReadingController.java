package com.metertocash.reading;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
public class ReadingController {

    private final ReadingRepository readings;

    public ReadingController(ReadingRepository readings) {
        this.readings = readings;
    }

    @GetMapping("/readings/{billId}")
    public ReadingEntity reading(@PathVariable String billId) {
        return readings.findById(billId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "reading not found"));
    }
}
