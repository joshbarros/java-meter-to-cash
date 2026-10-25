package com.metertocash.meter;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metertocash.common.Channels;
import com.metertocash.common.MeterOpened;
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
public class MeterController {

    private final MeterRepository meters;
    private final PremiseViewRepository premises;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public MeterController(MeterRepository meters, PremiseViewRepository premises, OutboxRepository outbox,
            ObjectMapper mapper) {
        this.meters = meters;
        this.premises = premises;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @PostMapping("/meters")
    @Transactional
    public ResponseEntity<MeterEntity> create(@RequestBody CreateMeter request) {
        if (request == null || blank(request.serial()) || blank(request.premiseId()) || request.lastRead() == null
                || request.lastRead() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "meter fields are required");
        }
        if (!premises.existsById(request.premiseId().trim())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "premise not found");
        }
        String serial = request.serial().trim();
        if (meters.existsBySerial(serial)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "meter exists");
        }
        MeterEntity row = new MeterEntity();
        row.setId(UUID.randomUUID().toString());
        row.setPremiseId(request.premiseId().trim());
        row.setSerial(serial);
        row.setLastRead(request.lastRead());
        meters.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.METERS, row.getId(),
                new MeterOpened(row.getId(), row.getPremiseId(), row.getSerial(), row.getLastRead()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/meters/{id}")
    public MeterEntity meter(@PathVariable String id) {
        return meters.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "meter not found"));
    }

    @GetMapping("/directory/premises/{id}")
    public PremiseView premise(@PathVariable String id) {
        return premises.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "premise not found"));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record CreateMeter(String serial, String premiseId, Integer lastRead) {
    }
}
