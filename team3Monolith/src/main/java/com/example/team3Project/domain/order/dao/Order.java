package com.example.team3Project.domain.order.dao;

import com.example.team3Project.domain.dummyMarket.dao.DummyCoupangProduct;
import com.example.team3Project.domain.shipment.enums.Shipment;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    private Long userId;
    private int totalAmount;
    private String status; // created / paid / canceled

    // 화면설계서
    private String customerName;    // 고객 이름
    private String customerPhone;   // 고객 번호

    @Column(name = "customer_address")
    private String customerAddress; // 배송 주소

    @Column(name = "customs_number")
    private String customsNumber;   // 통관번호

    private String autoOrderStatus; // 자동 주문 상태 Ready / Ordered/ Failed / Shipping

    private final String overseasMall = "Amazon"; // 해외몰 정보 (지금은 Amazon 1개)

    private int margin; // 마진

    // 주문 상품 (dummy_coupang_product FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dummy_coupang_product_id")
    @JsonIgnore
    private DummyCoupangProduct dummyCoupangProduct;

    // JSON 응답에 ID만 노출
    public Long getDummyCoupangProductId() {
        return dummyCoupangProduct != null ? dummyCoupangProduct.getId() : null;
    }

    @Column(name = "product_name")
    private String productName;     // 주문 시점 상품명 (비정규화)

    private int quantity;           // 주문 수량

    @JsonIgnore
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;

    @JsonIgnore
    @OneToOne(mappedBy = "order")
    private Shipment shipment;
}
