package com.metertocash.meter;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MeterRepository extends JpaRepository<MeterEntity, String> {

    boolean existsBySerial(String serial);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "UPDATE meters SET last_read = :next WHERE id = :id AND last_read = :previous", nativeQuery = true)
    int advance(@org.springframework.data.repository.query.Param("id") String id, @org.springframework.data.repository.query.Param("previous") int previous, @org.springframework.data.repository.query.Param("next") int next);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "UPDATE meters SET last_read = :previous WHERE id = :id AND last_read = :next", nativeQuery = true)
    int rewind(@org.springframework.data.repository.query.Param("id") String id, @org.springframework.data.repository.query.Param("previous") int previous, @org.springframework.data.repository.query.Param("next") int next);
}
