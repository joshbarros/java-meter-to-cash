package com.metertocash.bill;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metertocash.common.Channels;
import com.metertocash.common.FieldCutCommand;
import com.metertocash.common.ReadCommand;
import com.metertocash.common.TenderCommand;
import com.metertocash.common.ActualCommand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
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
public class BillController {

    private static final Set<String> PAYABLE = Set.of("NOTICED", "CUT", "CUT_OFF", "CUT_PAID");

    private final BillRepository bills;
    private final BillActionRepository actions;
    private final AccountDirectoryRepository accounts;
    private final PremiseDirectoryRepository premises;
    private final MeterDirectoryRepository meters;
    private final TariffDirectoryRepository tariffs;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public BillController(BillRepository bills, BillActionRepository actions, AccountDirectoryRepository accounts,
            PremiseDirectoryRepository premises, MeterDirectoryRepository meters, TariffDirectoryRepository tariffs,
            OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.bills = bills;
        this.actions = actions;
        this.accounts = accounts;
        this.premises = premises;
        this.meters = meters;
        this.tariffs = tariffs;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @PostMapping("/bills")
    @Transactional
    public ResponseEntity<BillEntity> create(@RequestBody CreateBill request) {
        if (request == null || blank(request.requestId()) || blank(request.accountId()) || blank(request.premiseId())
                || blank(request.meterId()) || blank(request.tariffId()) || blank(request.route())
                || request.readValue() == null || request.readValue() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bill fields are required");
        }
        BillEntity existing = bills.findByRequestId(request.requestId().trim()).orElse(null);
        if (existing != null) {
            return ResponseEntity.ok(existing);
        }
        AccountDirectory account = accounts.findById(request.accountId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "account not found"));
        PremiseDirectory premise = premises.findById(request.premiseId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "premise not found"));
        MeterDirectory meter = meters.findById(request.meterId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "meter not found"));
        TariffDirectory tariff = tariffs.findById(request.tariffId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "tariff not found"));
        if (!account.getId().equals(premise.getAccountId()) || !premise.getId().equals(meter.getPremiseId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "meter does not sit on that account");
        }
        BillEntity row = new BillEntity();
        row.setId(UUID.randomUUID().toString());
        row.setRequestId(request.requestId().trim());
        row.setAccountId(account.getId());
        row.setPremiseId(premise.getId());
        row.setMeterId(meter.getId());
        row.setTariffId(tariff.getId());
        row.setPreviousRead(meter.getLastRead());
        row.setReadValue(request.readValue());
        row.setEstimatedRead(0);
        row.setConsumption(0);
        row.setRateCents(tariff.getRateCents());
        row.setAmountCents(0);
        row.setOpenCents(0);
        row.setRoute(request.route().trim());
        row.setEstimated(false);
        row.setDialMoved(false);
        row.setServiceOff(false);
        row.setActualRequested(false);
        row.setStatus("READ");
        row.setReason("");
        bills.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.BILLS, row.getId(),
                new com.metertocash.common.BillFact(row.getId(), row.getStatus(), row.getReason()));
        OutboxWriter.rabbit(outbox, mapper, Channels.READ, row.getId(),
                new ReadCommand(row.getId(), row.getPreviousRead(), row.getReadValue(), row.getRoute()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @PostMapping("/bills/{id}/readings")
    @Transactional
    public ResponseEntity<BillEntity> actual(@PathVariable String id, @RequestBody ActualReading request) {
        BillEntity row = lock(id);
        if (request == null || request.readValue() == null || request.readValue() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "read is required");
        }
        if (!"RATED".equals(row.getStatus()) || !row.isEstimated() || row.isActualRequested()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "actual reading is closed");
        }
        row.setActualRequested(true);
        OutboxWriter.rabbit(outbox, mapper, Channels.ACTUAL, row.getId(),
                new ActualCommand(row.getId(), row.getPreviousRead(), row.getEstimatedRead(), request.readValue()));
        return ResponseEntity.accepted().body(row);
    }

    @PostMapping("/bills/{id}/payments")
    @Transactional
    public ResponseEntity<BillEntity> pay(@PathVariable String id, @RequestBody PayRequest request) {
        if (request == null || blank(request.requestId()) || blank(request.payToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payment fields are required");
        }
        BillEntity row = lock(id);
        BillActionEntity prior = actions.findById(request.requestId().trim()).orElse(null);
        if (prior != null) {
            if (!prior.getBillId().equals(row.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "request belongs to another bill");
            }
            return ResponseEntity.ok(row);
        }
        if (!PAYABLE.contains(row.getStatus()) || row.getOpenCents() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "bill is not payable");
        }
        BillActionEntity action = new BillActionEntity();
        action.setId(request.requestId().trim());
        action.setBillId(row.getId());
        action.setKind("PAY");
        actions.save(action);
        OutboxWriter.rabbit(outbox, mapper, Channels.TENDER, row.getId(),
                new TenderCommand(row.getId(), action.getId(), row.getOpenCents(), request.payToken().trim()));
        return ResponseEntity.accepted().body(row);
    }

    @PostMapping("/bills/{id}/disconnects")
    @Transactional
    public ResponseEntity<BillEntity> disconnect(@PathVariable String id, @RequestBody DisconnectRequest request) {
        if (request == null || blank(request.requestId()) || blank(request.sealToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "disconnect fields are required");
        }
        BillEntity row = lock(id);
        BillActionEntity prior = actions.findById(request.requestId().trim()).orElse(null);
        if (prior != null) {
            if (!prior.getBillId().equals(row.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "request belongs to another bill");
            }
            return ResponseEntity.ok(row);
        }
        if (!"NOTICED".equals(row.getStatus()) || row.getOpenCents() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "bill cannot be cut");
        }
        BillActionEntity action = new BillActionEntity();
        action.setId(request.requestId().trim());
        action.setBillId(row.getId());
        action.setKind("CUT");
        actions.save(action);
        OutboxWriter.rabbit(outbox, mapper, Channels.FIELD_CUT, row.getId(),
                new FieldCutCommand(row.getId(), request.sealToken().trim()));
        return ResponseEntity.accepted().body(row);
    }

    @GetMapping("/bills/{id}")
    public BillEntity bill(@PathVariable String id) {
        return bills.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "bill not found"));
    }

    @GetMapping("/directory/accounts/{id}")
    public AccountDirectory account(@PathVariable String id) {
        return accounts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "account not found"));
    }

    @GetMapping("/directory/premises/{id}")
    public PremiseDirectory premise(@PathVariable String id) {
        return premises.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "premise not found"));
    }

    @GetMapping("/directory/meters/{id}")
    public MeterDirectory meter(@PathVariable String id) {
        return meters.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "meter not found"));
    }

    @GetMapping("/directory/tariffs/{id}")
    public TariffDirectory tariff(@PathVariable String id) {
        return tariffs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "tariff not found"));
    }

    private BillEntity lock(String id) {
        BillEntity row = em.find(BillEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (row == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "bill not found");
        }
        return row;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record CreateBill(String requestId, String accountId, String premiseId, String meterId, String tariffId,
            Integer readValue, String route) {
    }

    public record ActualReading(Integer readValue) {
    }

    public record PayRequest(String requestId, String payToken) {
    }

    public record DisconnectRequest(String requestId, String sealToken) {
    }
}
