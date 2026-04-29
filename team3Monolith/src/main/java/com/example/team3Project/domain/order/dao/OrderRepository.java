package com.example.team3Project.domain.order.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByAutoOrderStatus(String status);

    List<Order> findByUserId(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select o
            from Order o
            left join fetch o.shipment
            left join fetch o.dummyCoupangProduct
            """)
    List<Order> findAllWithShipment();

    @Query("""
            select o
            from Order o
            left join fetch o.shipment
            left join fetch o.dummyCoupangProduct p
            where p.userId = :userId
            """)
    List<Order> findAllWithShipmentByProductUserId(@Param("userId") Long userId);

    @Query("""
            select o
            from Order o
            where o.autoOrderStatus = :status
              and o.dummyCoupangProduct is not null
              and o.dummyCoupangProduct.userId = :userId
            """)
    List<Order> findByAutoOrderStatusAndProductUserId(
            @Param("status") String status,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT YEAR(o.createdAt), MONTH(o.createdAt),
                   SUM(o.quantity), SUM(o.totalAmount), SUM(o.margin)
            FROM Order o
            WHERE o.dummyCoupangProduct IS NOT NULL
              AND o.dummyCoupangProduct.userId = :userId
              AND o.status <> 'CANCELLED'
              AND o.createdAt IS NOT NULL
            GROUP BY YEAR(o.createdAt), MONTH(o.createdAt)
            ORDER BY YEAR(o.createdAt) ASC, MONTH(o.createdAt) ASC
            """)
    List<Object[]> findMonthlyRevenueByUserId(@Param("userId") Long userId);
}
