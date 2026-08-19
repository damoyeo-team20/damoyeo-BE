package com.damoyeo.meeting.repository;

import com.damoyeo.meeting.domain.MeetingAvailableDate;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingAvailableDateRepository extends JpaRepository<MeetingAvailableDate, Long> {
    void deleteAllByMeetingParticipantId(Long meetingParticipantId);

    List<MeetingAvailableDate> findAllByMeetingParticipantIdOrderByAvailableDateAsc(Long meetingParticipantId);
}
