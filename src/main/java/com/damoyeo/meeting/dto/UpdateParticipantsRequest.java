package com.damoyeo.meeting.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UpdateParticipantsRequest(
        @NotEmpty(message = "참여자를 한 명 이상 선택해야 합니다.")
        Set<Long> groupMemberIds
) {
}
