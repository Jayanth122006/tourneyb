package com.example.back.repository;

import com.example.back.entity.Squad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SquadRepository extends JpaRepository<Squad, Long> {
    boolean existsBySquadName(String squadName);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);

    Squad findBySquadName(String squadName);
    Squad findByPhone(String phone);
    Squad findByEmail(String email);
    Squad findByOrderId(String orderId);
}
