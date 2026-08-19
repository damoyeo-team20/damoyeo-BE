package com.damoyeo.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record JoinGroupRequest(
        @NotBlank(message = "초대 코드는 필수입니다.")
        @Pattern(regexp = "[A-Za-z0-9]{8}", message = "초대 코드는 영문과 숫자 8자리여야 합니다.")
        String inviteCode
) {
}
