package com.metertocash.adjustment;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AdjustmentController {

    private final AdjustmentRepository adjustments;

    public AdjustmentController(AdjustmentRepository adjustments) {
        this.adjustments = adjustments;
    }

    @GetMapping("/adjustments/{billId}")
    public AdjustmentEntity adjustment(@PathVariable String billId) {
        return adjustments.findById(billId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "adjustment not found"));
    }
}
