package com.geekparser.contentplatform.publishing.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramConfigRepository extends JpaRepository<TelegramConfig, Long> {

    Optional<TelegramConfig> findFirstByActiveTrueOrderByUpdatedAtDesc();
}
