package com.metertocash.account;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, String> {

    boolean existsByAccountNumber(String accountNumber);
}
