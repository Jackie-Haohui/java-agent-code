package com.example.agent.memory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemoryRepository extends JpaRepository<MemoryRecord, UUID> {

    List<MemoryRecord> findTop10ByUserIdOrderByImportanceDescUpdatedAtDesc(String userId);

    List<MemoryRecord> findByUserId(String userId);
}
