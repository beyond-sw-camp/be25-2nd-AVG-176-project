package com.example.team3Project.domain.product.registration.entity;

// 등록 상태
public enum RegistrationStatus {
    READY,       // 등록 대기
    REGISTERED,  // 등록 완료 (쿠팡 발행됨)
    BLOCKED,     // 차단 (판매 중지 등 사유로 발행 제외)
    FAILED,      // 등록 실패
    CANCELED     // 사용자가 직접 취소
}
