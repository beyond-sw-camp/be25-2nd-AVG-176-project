package com.example.team3Project.domain.product.registration.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class DummyProductRegistrationDeleteRequest {

    @NotEmpty
    private List<Long> registrationIds;
}
