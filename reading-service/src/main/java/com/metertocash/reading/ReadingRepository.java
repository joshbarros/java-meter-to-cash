package com.metertocash.reading;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingRepository extends JpaRepository<ReadingEntity, String> {
}
