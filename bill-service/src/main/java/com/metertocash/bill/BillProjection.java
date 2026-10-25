package com.metertocash.bill;

import com.metertocash.common.AccountOpened;
import com.metertocash.common.Channels;
import com.metertocash.common.MeterOpened;
import com.metertocash.common.PremiseOpened;
import com.metertocash.common.TariffOpened;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BillProjection {

    private final AccountDirectoryRepository accounts;
    private final PremiseDirectoryRepository premises;
    private final MeterDirectoryRepository meters;
    private final TariffDirectoryRepository tariffs;

    public BillProjection(AccountDirectoryRepository accounts, PremiseDirectoryRepository premises,
            MeterDirectoryRepository meters, TariffDirectoryRepository tariffs) {
        this.accounts = accounts;
        this.premises = premises;
        this.meters = meters;
        this.tariffs = tariffs;
    }

    @KafkaListener(topics = Channels.ACCOUNTS, groupId = "bill-accounts")
    @Transactional
    public void onAccount(AccountOpened event) {
        if (accounts.existsById(event.accountId())) {
            return;
        }
        AccountDirectory row = new AccountDirectory();
        row.setId(event.accountId());
        row.setAccountNumber(event.accountNumber());
        row.setName(event.name());
        accounts.save(row);
    }

    @KafkaListener(topics = Channels.PREMISES, groupId = "bill-premises")
    @Transactional
    public void onPremise(PremiseOpened event) {
        if (premises.existsById(event.premiseId())) {
            return;
        }
        PremiseDirectory row = new PremiseDirectory();
        row.setId(event.premiseId());
        row.setAccountId(event.accountId());
        row.setPremiseCode(event.premiseCode());
        row.setAddress(event.address());
        premises.save(row);
    }

    @KafkaListener(topics = Channels.METERS, groupId = "bill-meters")
    @Transactional
    public void onMeter(MeterOpened event) {
        if (meters.existsById(event.meterId())) {
            return;
        }
        MeterDirectory row = new MeterDirectory();
        row.setId(event.meterId());
        row.setPremiseId(event.premiseId());
        row.setSerial(event.serial());
        row.setLastRead(event.lastRead());
        meters.save(row);
    }

    @KafkaListener(topics = Channels.TARIFFS, groupId = "bill-tariffs")
    @Transactional
    public void onTariff(TariffOpened event) {
        if (tariffs.existsById(event.tariffId())) {
            return;
        }
        TariffDirectory row = new TariffDirectory();
        row.setId(event.tariffId());
        row.setCode(event.code());
        row.setRateCents(event.rateCents());
        tariffs.save(row);
    }
}
