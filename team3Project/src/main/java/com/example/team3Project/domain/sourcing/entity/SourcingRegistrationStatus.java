package com.example.team3Project.domain.sourcing.entity;

/**
 * 소싱 데이터의 등록·정규화 진행 상태.
 * NORMALIZED일 때만 판매 상품 등록 플로우로 넘길 수 있도록 구분한다.
 */
public enum SourcingRegistrationStatus {

    /** DB 저장 완료, 정규화 진행 전·진행 중 */
    PENDING_NORMALIZATION,

    /** 정규화 완료 — 상품 등록 가능 */
    NORMALIZED,

    /** 정규화 단계에서 오류 발생 — 재시도·수동 처리 필요 */
    NORMALIZATION_FAILED,

    /** 판매 상품으로 등록 완료 (소싱 원장은 보관) */
    PRODUCT_REGISTERED,
    
    /** 정규화 완료, 상품 삭제*/
    PRODUCT_DELETED
}
