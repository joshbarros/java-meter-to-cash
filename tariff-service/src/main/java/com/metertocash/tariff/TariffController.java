package com.metertocash.tariff;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metertocash.common.Channels;
import com.metertocash.common.TariffOpened;
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
public class TariffController {

    private final TariffRepository tariffs;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public TariffController(TariffRepository tariffs, OutboxRepository outbox, ObjectMapper mapper) {
        this.tariffs = tariffs;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @PostMapping("/tariffs")
    @Transactional
    public ResponseEntity<TariffEntity> create(@RequestBody CreateTariff request) {
        if (request == null || blank(request.code()) || request.rateCents() == null || request.rateCents() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tariff fields are required");
        }
        String code = request.code().trim();
        if (tariffs.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "tariff exists");
        }
        TariffEntity row = new TariffEntity();
        row.setId(UUID.randomUUID().toString());
        row.setCode(code);
        row.setRateCents(request.rateCents());
        tariffs.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.TARIFFS, row.getId(),
                new TariffOpened(row.getId(), row.getCode(), row.getRateCents()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/tariffs/{id}")
    public TariffEntity tariff(@PathVariable String id) {
        return tariffs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "tariff not found"));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record CreateTariff(String code, Integer rateCents) {
    }
}
