package com.damoyeo.meeting.repository;

import com.damoyeo.meeting.domain.MeetingChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingChatMessageRepository extends JpaRepository<MeetingChatMessage, Long> {
    List<MeetingChatMessage> findAllByMeetingIdOrderByIdAsc(Long meetingId);
}
