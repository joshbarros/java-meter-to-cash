package com.metertocash.meter;

import com.metertocash.common.Channels;
import com.metertocash.common.PremiseOpened;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MeterProjection {

    private final PremiseViewRepository premises;

    public MeterProjection(PremiseViewRepository premises) {
        this.premises = premises;
    }

    @KafkaListener(topics = Channels.PREMISES, groupId = "meter-premises")
    @Transactional
    public void onPremise(PremiseOpened event) {
        if (premises.existsById(event.premiseId())) {
            return;
        }
        PremiseView row = new PremiseView();
        row.setId(event.premiseId());
        row.setAccountId(event.accountId());
        row.setPremiseCode(event.premiseCode());
        premises.save(row);
    }
}
