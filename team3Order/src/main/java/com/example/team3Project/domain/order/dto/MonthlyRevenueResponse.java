package com.example.team3Project.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonthlyRevenueResponse {
    private int year;
    private int month;
    private long orderCount;   // 해당 월 총 주문 수량
    private long sales;        // 매출 (totalAmount 합계)
    private long margin;       // 마진 (margin 합계)
    private double profitRate; // 수익률 (%) = margin / sales * 100
}
