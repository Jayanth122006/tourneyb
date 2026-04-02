package com.example.back.service;

import com.example.back.entity.SupportQuery;
import com.example.back.repository.SupportQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportQueryService {

    private final SupportQueryRepository repository;

    public SupportQuery saveQuery(SupportQuery query) {
        query.setCreatedAt(LocalDateTime.now());
        return repository.save(query);
    }

    public List<SupportQuery> getAllQueries() {
        return repository.findAll();
    }
}
