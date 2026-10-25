package com.metertocash.reading;

import jakarta.persistence.EntityManager;

final class InboxClaims {
    private InboxClaims() {
    }

    static boolean claim(EntityManager em, String id) {
        return em.createNativeQuery("INSERT INTO inbox (id) VALUES (:id) ON CONFLICT DO NOTHING")
                .setParameter("id", id)
                .executeUpdate() == 1;
    }
}
