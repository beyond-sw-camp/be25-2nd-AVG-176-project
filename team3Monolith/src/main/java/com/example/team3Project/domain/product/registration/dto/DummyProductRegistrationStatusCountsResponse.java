package com.example.team3Project.domain.product.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// 등록 관리 화면에서 사용자별 상태 개수를 바로 표시하기 위한 응답 DTO다.
public class DummyProductRegistrationStatusCountsResponse {
    private long readyCount;
    private long registeredCount;
    private long failedOrCanceledCount;
    private long totalCount;
    private long failedCount;
    private long canceledCount;
}
