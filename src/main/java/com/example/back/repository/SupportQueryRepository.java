package com.example.back.repository;

import com.example.back.entity.SupportQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportQueryRepository extends JpaRepository<SupportQuery, Long> {
}
