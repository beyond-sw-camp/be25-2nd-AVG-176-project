package com.example.team3Project.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionUser implements Serializable {
    private Long id;
    private String username;
    private String nickname;
}
