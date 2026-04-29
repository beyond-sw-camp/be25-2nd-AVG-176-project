package com.example.team3Project.domain.order.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class OrderItem {
    @Id
    @GeneratedValue
    private Long id;

    private Long productId;
    private String productName;
    private int price;
    private int quantity;

    @ManyToOne
    @JsonIgnore
    private Order order;
}
