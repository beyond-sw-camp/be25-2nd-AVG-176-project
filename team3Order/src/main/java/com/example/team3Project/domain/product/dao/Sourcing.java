package com.example.team3Project.domain.product.dao;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

// sourcing 테이블 (description 이미지 폴백용)
@Entity
@Table(name = "sourcing")
@Getter
public class Sourcing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // dummy_coupang_product.source_product_id 와 매칭
    @Column(name = "product_id")
    private String productId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "sourcing_description_images", joinColumns = @JoinColumn(name = "sourcing_id"))
    @Column(name = "description_images")
    private List<String> descriptionImages = new ArrayList<>();
}
