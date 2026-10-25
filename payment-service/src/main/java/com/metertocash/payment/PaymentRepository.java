package com.metertocash.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {

    java.util.List<PaymentEntity> findByBillIdOrderByIdAsc(String billId);
}
