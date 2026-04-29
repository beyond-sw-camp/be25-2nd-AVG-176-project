package com.example.team3Project.domain.product.coupang.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
// 체크박스로 선택한 등록 후보를 쿠팡 더미 상품으로 발행할 때 사용한다.
public class DummyCoupangPublishRequest {

    @NotEmpty
    private List<Long> registrationIds;
}
