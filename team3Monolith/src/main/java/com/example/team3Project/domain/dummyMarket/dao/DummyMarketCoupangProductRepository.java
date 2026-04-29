package com.example.team3Project.domain.dummyMarket.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DummyMarketCoupangProductRepository extends JpaRepository<DummyCoupangProduct, Long> {
    Page<DummyCoupangProduct> findAllByOrderByIdAsc(Pageable pageable);

    Page<DummyCoupangProduct> findByUserIdOrderByIdAsc(Long userId, Pageable pageable);
}
