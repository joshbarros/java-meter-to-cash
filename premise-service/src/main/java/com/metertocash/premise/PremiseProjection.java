package com.metertocash.premise;

import com.metertocash.common.AccountOpened;
import com.metertocash.common.Channels;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PremiseProjection {

    private final AccountViewRepository accounts;

    public PremiseProjection(AccountViewRepository accounts) {
        this.accounts = accounts;
    }

    @KafkaListener(topics = Channels.ACCOUNTS, groupId = "premise-accounts")
    @Transactional
    public void onAccount(AccountOpened event) {
        if (accounts.existsById(event.accountId())) {
            return;
        }
        AccountView row = new AccountView();
        row.setId(event.accountId());
        row.setAccountNumber(event.accountNumber());
        row.setName(event.name());
        accounts.save(row);
    }
}
